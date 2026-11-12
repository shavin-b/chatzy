package com.orderflow.autoresponder.device.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.orderflow.autoresponder.device.model.DeviceCommand
import com.orderflow.autoresponder.device.model.DeviceInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDeviceRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val enrollment: com.orderflow.autoresponder.device.auth.DeviceEnrollment
) : DeviceRepository {

    override suspend fun registerDevice(deviceInfo: DeviceInfo): Result<Unit> {
        if (enrollment.enabled) return updateDeviceInfo(deviceInfo.deviceId, mapOf(
            "phoneModel" to deviceInfo.phoneModel, "manufacturer" to deviceInfo.manufacturer,
            "androidVersion" to deviceInfo.androidVersion, "appVersion" to deviceInfo.appVersion,
            "appBuildNumber" to deviceInfo.appBuildNumber, "userName" to deviceInfo.userName,
            "businessName" to deviceInfo.businessName, "phoneNumber" to deviceInfo.phoneNumber,
            "lastSeen" to System.currentTimeMillis(), "lastSync" to System.currentTimeMillis(), "fcmToken" to deviceInfo.fcmToken))
        return try {
            firestore.collection("devices")
                .document(deviceInfo.deviceId)
                .set(deviceInfo, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDeviceInfo(deviceId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            if (enrollment.enabled) {
                enrollment.ensureAuthorized(deviceId)
                val telemetry = com.orderflow.autoresponder.device.auth.DeviceWritePolicy.telemetryOnly(updates)
                if (telemetry.isNotEmpty()) firestore.collection("devices").document(deviceId).update(telemetry).await()
                return Result.success(Unit)
            }
            // Use set+merge instead of update() so this succeeds even if the document
            // doesn't exist yet (fresh install, cache cleared, Firestore offline recovery).
            // update() throws "NOT_FOUND" on missing docs; set+merge creates or patches safely.
            firestore.collection("devices")
                .document(deviceId)
                .set(updates, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acknowledgeCommand(command: DeviceCommand): Result<Unit> {
        return try {
            if (enrollment.enabled) {
                enrollment.ensureAuthorized(command.deviceId)
                val ref = firestore.collection("deviceCommands").document(command.commandId)
                firestore.runTransaction { tx ->
                    val record = tx.get(ref)
                    check(record.getString("deviceId") == command.deviceId && record.getString("command") == command.command) { "Acknowledgement target mismatch" }
                    val fields = mutableMapOf<String, Any>("status" to command.status, "processedAt" to (command.processedAt ?: System.currentTimeMillis()))
                    command.errorMessage?.let { fields["errorMessage"] = it }
                    tx.update(ref, fields)
                }.await()
                return Result.success(Unit)
            }
            // Write acknowledgement to deviceCommands/{commandId}.
            // Use set+merge — the document may or may not exist depending on whether
            // the web backend created it before sending FCM. This is safe either way.
            val ackFields = mutableMapOf<String, Any>(
                "status"      to command.status,
                "processedAt" to (command.processedAt ?: System.currentTimeMillis()),
                "commandId"   to command.commandId,
                "deviceId"    to command.deviceId,
                "command"     to command.command
            )
            command.errorMessage?.let { ackFields["errorMessage"] = it }

            firestore.collection("deviceCommands")
                .document(command.commandId)
                .set(ackFields, SetOptions.merge())
                .await()

            // Also reflect the current lock state on devices/{deviceId} so the admin dashboard's
            // subscribeToDevice() listener updates the adminLocked badge immediately.
            val isNowLocked   = command.command == "LOCK"   && command.status == "SUCCESS"
            val isNowUnlocked = command.command == "UNLOCK" && command.status == "SUCCESS"
            if (isNowLocked || isNowUnlocked) {
                val locked = isNowLocked
                firestore.collection("devices")
                    .document(command.deviceId)
                    .set(
                        mapOf(
                            "adminLocked" to locked,
                            "lockStatus"  to if (locked) "LOCKED" else "UNLOCKED",
                            "lockReason"  to if (locked) "ADMIN" else "NONE",
                            "lastSync"    to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                    .await()
            }
            Result.success(Unit)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDeviceSubscriptionFlow(deviceId: String): Flow<DeviceInfo?> = callbackFlow {
        enrollment.ensureAuthorized(deviceId)
        val listener = firestore.collection("devices")
            .document(deviceId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Do not close the channel on transient network glitches (e.g. UNAVAILABLE, UnknownHostException).
                    // Firestore auto-retries with backoff. Closing the channel would permanently kill this listener.
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(DeviceInfo::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getDeviceRawFlow(deviceId: String): Flow<Map<String, Any>?> = callbackFlow {
        enrollment.ensureAuthorized(deviceId)
        val listener = firestore.collection("devices")
            .document(deviceId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Do not close the channel on transient network glitches (e.g. UNAVAILABLE, UnknownHostException).
                    // Firestore auto-retries with backoff. Closing the channel would permanently kill this listener.
                    return@addSnapshotListener
                }
                trySend(snapshot?.data)
            }
        awaitClose { listener.remove() }
    }
}
