package com.orderflow.autoresponder.device.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import com.orderflow.autoresponder.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceEnrollment @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {
    val enabled: Boolean get() = BuildConfig.DEVICE_ENROLLMENT_ENABLED
    private val mutex = Mutex()
    private val mutableStatus = MutableStateFlow("Device approval has not been requested.")
    val status = mutableStatus.asStateFlow()

    private suspend fun installation(): com.google.firebase.auth.FirebaseUser {
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
        check(user != null && user.isAnonymous) { "A dedicated installation identity is required." }
        return user
    }

    suspend fun request(deviceId: String) = mutex.withLock {
        check(enabled) { "Secure enrollment is not enabled in this build." }
        installation()
        val result = functions.getHttpsCallable("requestDeviceEnrollment").call(mapOf("deviceId" to deviceId)).await().data as? Map<*, *>
        // Pairing request ID is for physical comparison, never an Auth token or credential.
        mutableStatus.value = "Device: $deviceId\nPairing ID: ${result?.get("requestId")}\nShow this screen to your Super Admin. Then check approval."
    }

    suspend fun ensureAuthorized(deviceId: String) = mutex.withLock {
        if (!enabled) return@withLock
        val user = installation()
        var token = user.getIdToken(false).await()
        if (token.claims["device_id"] != deviceId) {
            functions.getHttpsCallable("completeDeviceEnrollment").call(mapOf("deviceId" to deviceId)).await()
            token = user.getIdToken(true).await()
        }
        check(token.claims["device_id"] == deviceId) { "Device identity mismatch." }
        val binding = firestore.collection("deviceBindings").document(deviceId).get(Source.SERVER).await()
        check(binding.getString("authUid") == user.uid && binding.getBoolean("active") == true) { "Device approval missing or revoked." }
        mutableStatus.value = "Approved device: $deviceId"
    }

    suspend fun verifyCommand(deviceId: String, commandId: String, command: String) {
        if (!enabled) return
        ensureAuthorized(deviceId)
        val snapshot = firestore.collection("deviceCommands").document(commandId).get(Source.SERVER).await()
        check(snapshot.exists() && snapshot.getString("deviceId") == deviceId && snapshot.getString("command") == command
            && snapshot.getString("status") in listOf("PENDING", "SENT", "RECEIVED")) { "Untrusted or completed remote command." }
        // Reject superseded LOCK/UNLOCK commands instead of undoing a newer server decision.
        if (command == "LOCK" || command == "UNLOCK") {
            val device = firestore.collection("devices").document(deviceId).get(Source.SERVER).await()
            check(device.getString("desiredCommandId") == commandId) { "Superseded remote command." }
        }
    }
}
