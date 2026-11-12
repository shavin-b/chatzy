package com.orderflow.autoresponder.help

import android.app.Instrumentation
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import com.orderflow.autoresponder.BuildConfig
import com.orderflow.autoresponder.core.security.SecureStorage
import com.orderflow.autoresponder.presentation.help.SupportClient
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** Test APK only. Checks this installation, never lists devices or prints credentials/records. */
object DeviceRegistrationProbe {
    fun run(instrumentation: Instrumentation) = runBlocking {
        val result = Bundle()
        try {
            withTimeout(35_000) {
                val context = instrumentation.targetContext
                val auth = FirebaseAuth.getInstance()
                val id = SecureStorage(context).getDeviceId()
                result.putString("project", auth.app.options.projectId)
                result.putBoolean("enrollmentEnabled", BuildConfig.DEVICE_ENROLLMENT_ENABLED)
                if (id == null) { result.putString("status", "local-device-id-missing"); return@withTimeout }
                result.putString("devicePath", "devices/$id")
                val user = auth.currentUser
                result.putBoolean("hasAuthIdentity", user != null)
                if (user != null) {
                    val token = user.getIdToken(false).await()
                    result.putBoolean("anonymous", user.isAnonymous)
                    result.putBoolean("tokenAudienceMatches", token.claims["aud"] == auth.app.options.projectId)
                    result.putBoolean("deviceClaimMatches", token.claims["device_id"] == id)
                }
                try {
                    val doc = FirebaseFirestore.getInstance().collection("devices").document(id).get(Source.SERVER).await()
                    result.putBoolean("deviceExists", doc.exists())
                    result.putBoolean("hasLastSeen", doc.contains("lastSeen"))
                } catch (error: Exception) {
                    result.putString("deviceReadCode", (error as? com.google.firebase.firestore.FirebaseFirestoreException)?.code?.name ?: error.javaClass.simpleName)
                }
                // Completion is server-authorized and may publish presence ONLY if this
                // installation already has an approved binding. Never self-approve or reset.
                if (user != null) try {
                    FirebaseFunctions.getInstance("us-central1").getHttpsCallable("completeDeviceEnrollment")
                        .call(mapOf("deviceId" to id)).await()
                    user.getIdToken(true).await()
                    result.putString("completion", "success")
                } catch (error: Exception) {
                    result.putString("completion", SupportClient.diagnosticCode(error))
                }
                result.putString("status", "probe-complete")
            }
            instrumentation.finish(0, result)
        } catch (error: Exception) {
            result.putString("status", SupportClient.diagnosticCode(error))
            instrumentation.finish(1, result)
        }
    }
}
