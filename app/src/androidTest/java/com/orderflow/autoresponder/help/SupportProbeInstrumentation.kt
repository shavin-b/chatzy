package com.orderflow.autoresponder.help

import android.app.Instrumentation
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.orderflow.autoresponder.presentation.help.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first

/** Explicit opt-in live probe. Test APK only; never prints support text, tokens or exception bodies. */
class SupportProbeInstrumentation : Instrumentation() {
    private var registrationProbe = false
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        registrationProbe = arguments?.getString("deviceRegistration") == "true"
        if (registrationProbe) { start(); return }
        if (arguments?.getString("liveSupport") != "true") {
            finish(1, Bundle().apply { putString("status", "Pass liveSupport=true to run the real endpoint probe") })
            return
        }
        start()
    }

    override fun onStart() {
        if (registrationProbe) { DeviceRegistrationProbe.run(this); return }
        val result = Bundle()
        val context = targetContext
        if (!DeviceSupportConsent(context).enabled) {
            finish(1, Bundle().apply { putString("status", "consent-required: enable AI chat in Help first") })
            return
        }
        runBlocking {
            val auth = FirebaseAuth.getInstance()
            result.putString("project", auth.app.options.projectId)
            result.putString("applicationId", context.packageName)
            result.putString("firebaseAppId", auth.app.options.applicationId)
            val client = SupportClient(auth, FirebaseFunctions.getInstance("us-central1"), context)
            try {
                var lastFailure: Exception? = null
                val gateway = SupportGateway { turns ->
                    try { client.ask(turns) } catch (error: Exception) { lastFailure = error; throw error }
                }
                val vm = withContext(Dispatchers.Main) { HelpViewModel(gateway, DeviceSupportConsent(context)) }
                withContext(Dispatchers.Main) { vm.updateDraft("auto reply wada nahane"); vm.send() }
                withTimeout(35_000) { vm.uiState.first { !it.busy } }
                val id = vm.uiState.value.messages.first { it.fromUser }.id
                if (vm.uiState.value.messages.first { it.id == id }.delivery == HelpDelivery.FAILED) {
                    repeat(2) {
                        withContext(Dispatchers.Main) { vm.retry(id) }
                        withTimeout(35_000) { vm.uiState.first { !it.busy } }
                    }
                    result.putBoolean("retryStableMessage", vm.uiState.value.messages.count { it.fromUser } == 1 && vm.uiState.value.messages.any { it.id == id })
                    result.putInt("inlineErrors", vm.uiState.value.messages.count { it.error != null })
                    if (vm.uiState.value.messages.first { it.id == id }.delivery == HelpDelivery.FAILED)
                        throw checkNotNull(lastFailure)
                }
                result.putBoolean("firstReply", vm.uiState.value.messages.any { it.reply != null })
                withContext(Dispatchers.Main) { vm.updateDraft("ow enabled"); vm.send() }
                withTimeout(35_000) { vm.uiState.first { !it.busy } }
                if (vm.uiState.value.messages.any { it.delivery == HelpDelivery.FAILED }) throw checkNotNull(lastFailure)
                result.putBoolean("followupReply", vm.uiState.value.messages.count { it.reply != null } == 2)
                result.putString("status", "live-client-contract-passed; visual rendering still needs an unlocked screen")
                finish(0, result)
            } catch (failure: Exception) {
                val original = (failure as? SupportRequestException)?.original ?: failure
                result.putString("status", SupportClient.diagnosticCode(original))
                (failure as? SupportRequestException)?.let { result.putString("requestId", it.requestId) }
                finish(1, result)
            }
        }
    }
}
