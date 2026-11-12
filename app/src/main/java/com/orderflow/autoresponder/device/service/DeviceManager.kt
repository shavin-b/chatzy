package com.orderflow.autoresponder.device.service

import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessaging
import com.orderflow.autoresponder.core.logger.StructuredLogger
import com.orderflow.autoresponder.core.security.SecureStorage
import com.orderflow.autoresponder.device.repository.DeviceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage,
    private val deviceRepository: DeviceRepository,
    private val firebaseMessaging: FirebaseMessaging,
    private val enrollment: com.orderflow.autoresponder.device.auth.DeviceEnrollment
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize() {
        val firebaseApp = com.google.firebase.FirebaseApp.getInstance()
        val projectId = firebaseApp.options.projectId
        val appId = firebaseApp.options.applicationId
        StructuredLogger.i("DeviceManager", "Initializing DeviceManager...")
        StructuredLogger.i("DeviceManager", "[FIREBASE CONFIG] Project ID: $projectId")
        StructuredLogger.i("DeviceManager", "[FIREBASE CONFIG] App ID: $appId")
        
        // Ensure app starts unlocked on first-ever install
        secureStorage.forceUnlockOnFirstRun()

        // Log current persistent state for diagnostics
        StructuredLogger.i("DeviceManager", "[DeviceControl] Current Lock State: ${if (secureStorage.isAdminLocked()) "LOCKED" else "UNLOCKED"}")
        StructuredLogger.i("DeviceManager", "[DeviceControl] Current Subscription: ${secureStorage.getSubscriptionStatus()}")

        serviceScope.launch {
            val deviceId = getOrCreateDeviceId()
            StructuredLogger.i("DeviceManager", "Device ID: $deviceId")

            if (enrollment.enabled) {
                // Keep background recovery scheduled while physical approval is pending.
                scheduleHeartbeat()
                while (true) {
                    try { enrollment.ensureAuthorized(deviceId); break }
                    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (_: Exception) { kotlinx.coroutines.delay(30_000) }
                }
            }
            // 1. Start observer FIRST — reads authoritative lock/subscription state from Firestore
            //    BEFORE we write anything. This ensures any admin-set lock is applied locally
            //    before registerOrUpdateDevice reads and re-writes the local state.
            observeDeviceState(deviceId)

            // 2. Register/Update device identity fields (model, version, profile, etc.)
            //    Lock fields are intentionally excluded — they are managed exclusively by
            //    the observer (Firestore → device) and FCM commands.
            registerOrUpdateDevice(deviceId)

            // 3. Force-upload FCM token — document now guaranteed to exist from step 2
            synchronizeFcmToken(deviceId)

            // 4. Handle pending profile sync
            if (secureStorage.isProfileSyncPending()) {
                syncProfile()
            }

            // 5. Schedule periodic heartbeat
            scheduleHeartbeat()
        }
    }

    /**
     * Explicitly retrieves the current FCM token and updates Firestore if it changed.
     */
    suspend fun synchronizeFcmToken(deviceId: String) {
        try {
            val token = firebaseMessaging.token.await()
            if (token.isNullOrBlank()) {
                StructuredLogger.w("DeviceManager", "FCM token is null or blank — skipping sync")
                return
            }
            val lastSavedToken = secureStorage.getFcmToken()
            // Always save locally
            secureStorage.saveFcmToken(token)
            // Always upload to Firestore unconditionally — the Firestore document may not
            // have the token yet (e.g. fresh install, or update() failed on a missing doc).
            StructuredLogger.i("DeviceManager", "FCM token uploading to Firestore (changed=${token != lastSavedToken})")
            deviceRepository.updateDeviceInfo(deviceId, mapOf("fcmToken" to token))
        } catch (e: Exception) {
            StructuredLogger.e("DeviceManager", "Failed to synchronize FCM token", e)
        }
    }

    private fun scheduleHeartbeat() {
        StructuredLogger.i("DeviceManager", "Scheduling heartbeat...")
        val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(15, TimeUnit.MINUTES)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "device_heartbeat",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun getOrCreateDeviceId(): String {
        val existingId = secureStorage.getDeviceId()
        if (!existingId.isNullOrBlank()) return existingId

        // Use Android ID for persistence across re-installs
        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: UUID.randomUUID().toString()
        
        val newId = "OF-${androidId.take(16)}"
        secureStorage.saveDeviceId(newId)
        StructuredLogger.i("DeviceManager", "Persistent Device ID created: $newId")
        return newId
    }

    private suspend fun registerOrUpdateDevice(deviceId: String) {
        StructuredLogger.i("DeviceManager", "Syncing device identity fields with Firestore...")
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

        val appBuildNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }

        val businessName = secureStorage.getBusinessName()
        val whatsappNumber = secureStorage.getWhatsAppNumber()
        val currentToken = secureStorage.getFcmToken() ?: ""
        val now = System.currentTimeMillis()

        // IMPORTANT: Only write device identity and telemetry fields here.
        // Lock fields (adminLocked, lockStatus, isLocked, adminLock, lockReason, status) are
        // intentionally EXCLUDED from this write. They are managed exclusively by:
        //   - observeDeviceState()  : Firestore → local (applies remote admin changes)
        //   - FCM processCommand()  : FCM command → local + Firestore (applies LOCK/UNLOCK)
        //   - HeartbeatWorker       : local → Firestore (reports current lock state periodically)
        // Writing local lock state here would overwrite the admin's intent on every app startup.
        val identityFields = mapOf(
            "deviceId"         to deviceId,
            "generatedUuid"    to deviceId,
            "phoneModel"       to Build.MODEL,
            "manufacturer"     to Build.MANUFACTURER,
            "androidVersion"   to Build.VERSION.RELEASE,
            "appVersion"       to (packageInfo.versionName ?: "1.0.0"),
            "appBuildNumber"   to appBuildNumber,
            "installationDate" to packageInfo.firstInstallTime,
            "userName"         to businessName.ifBlank { "User ${deviceId.takeLast(4)}" },
            "businessName"     to businessName.ifBlank { "Chatzy User" },
            "phoneNumber"      to whatsappNumber,
            "lastSeen"         to now,
            "lastSync"         to now,
            "fcmToken"         to currentToken,
            "subscriptionStatus" to secureStorage.getSubscriptionStatus()
        )

        val result = deviceRepository.updateDeviceInfo(deviceId, identityFields)
        if (result.isSuccess) {
            StructuredLogger.i("DeviceManager", "Device identity synced successfully")
            if (secureStorage.isProfileSyncPending()) {
                secureStorage.setProfileSyncPending(false)
            }
        } else {
            StructuredLogger.e("DeviceManager", "Device identity sync failed: ${result.exceptionOrNull()?.message}")
        }
    }

    /**
     * Idempotent synchronization of the customer profile.
     */
    suspend fun syncProfile() {
        val deviceId = secureStorage.getDeviceId() ?: return
        val businessName = secureStorage.getBusinessName()
        val whatsappNumber = secureStorage.getWhatsAppNumber()

        if (businessName.isBlank() && whatsappNumber.isBlank()) return

        StructuredLogger.i("DeviceManager", "[DeviceControl] Synchronizing profile for $deviceId")
        
        val updates = mapOf(
            "businessName" to businessName,
            "userName" to businessName, // Map to userName for dashboard display
            "phoneNumber" to whatsappNumber,
            "lastSync" to System.currentTimeMillis()
        )

        val result = deviceRepository.updateDeviceInfo(deviceId, updates)
        if (result.isSuccess) {
            StructuredLogger.i("DeviceManager", "[DeviceControl] Profile synchronized successfully")
            secureStorage.setProfileSyncPending(false)
        } else {
            StructuredLogger.e("DeviceManager", "[DeviceControl] Profile synchronization failed")
            secureStorage.setProfileSyncPending(true)
        }
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun observeDeviceState(deviceId: String) {
        StructuredLogger.i("DeviceManager", "Starting device state observer for $deviceId...")
        serviceScope.launch {
            deviceRepository.getDeviceRawFlow(deviceId)
                // Debounce: a single LOCK/UNLOCK command causes 3 Firestore writes
                // (processCommand, acknowledgeCommand, lastCommandId) each firing a snapshot
                // within ~800ms. 600ms coalesces them into one processing call, eliminating
                // redundant SharedPreferences writes without adding any perceivable delay.
                .debounce(600)
                .catch { e ->
                    StructuredLogger.e("DeviceManager", "Device state flow error", e)
                }
                .collect { data ->
                    if (data != null) {
                        StructuredLogger.w("DeviceManager", "[DeviceControl] REMOTE SNAPSHOT RECEIVED")
                        // Never log the raw device document: it contains the FCM token.

                        // 1. Handle Admin Lock State (Independent)
                        // The website might use different fields, so we check ALL of them.
                        val lockStatusField = data["lockStatus"]?.toString() ?: ""
                        val adminLockField = data["adminLock"]
                        val adminLockedField = data["adminLocked"]
                        val isLockedField = data["isLocked"]
                        val lockedField = data["locked"]

                        // Priority 1: Explicit "LOCKED" string in lockStatus field.
                        // CRITICAL: Use equals(), NOT contains() — "UNLOCKED".contains("LOCKED") = true,
                        // which would re-lock the device immediately after every unlock command.
                        val hasLockString = lockStatusField.equals("LOCKED", ignoreCase = true) ||
                                           lockStatusField.equals("ADMIN_LOCKED", ignoreCase = true) ||
                                           lockStatusField.equals("Admin Locked", ignoreCase = true)

                        // Priority 2: Boolean flags — these are the source of truth for admin lock
                        val isLockFlagTrue = (adminLockField == true) ||
                                            (adminLockedField == true) ||
                                            (isLockedField == true) ||
                                            (lockedField == true) ||
                                            (adminLockedField?.toString()?.lowercase() == "true") ||
                                            (lockedField?.toString()?.lowercase() == "true")

                        // Lock is determined ONLY by explicit lock fields.
                        // The 'status' field is a display string used by the dashboard UI — it is NOT
                        // used for lock decisions because the admin console always keeps status="Active"
                        // even when the device is admin-locked. Using it as a guard would block all locks.
                        val shouldBeLocked = hasLockString || isLockFlagTrue

                        StructuredLogger.i("DeviceManager", "[DeviceControl] Lock check for $deviceId: hasLockString=$hasLockString, isLockFlagTrue=$isLockFlagTrue -> FINAL=$shouldBeLocked")
                        
                        if (secureStorage.isAdminLocked() != shouldBeLocked) {
                            StructuredLogger.e("DeviceManager", "[DeviceControl] APPLYING REMOTE LOCK CHANGE -> $shouldBeLocked")
                            secureStorage.setAdminLocked(shouldBeLocked)
                            val reason = data["lockReason"]?.toString() ?: if (shouldBeLocked) "ADMIN" else "NONE"
                            secureStorage.setLockReason(reason)
                        }

                        // 2. Handle Subscription Status
                        // Website might use "subscriptionStatus" or just "status"
                        val remoteEnd = when (val value = data["subscriptionEnd"]) {
                            is Number -> value.toLong()
                            is com.google.firebase.Timestamp -> value.toDate().time
                            else -> null
                        }
                        val subStatus = if (enrollment.enabled) {
                            secureStorage.saveSubscriptionEnd(remoteEnd ?: 0L)
                            com.orderflow.autoresponder.device.auth.DeviceSubscriptionState.resolve(
                                data["status"]?.toString(), data["subscriptionStatus"]?.toString(), remoteEnd, System.currentTimeMillis())
                        } else data["subscriptionStatus"]?.toString() ?: "ACTIVE"
                        
                        if (secureStorage.getSubscriptionStatus() != subStatus) {
                            StructuredLogger.i("DeviceManager", "[DeviceControl] REMOTE CHANGE DETECTED: Subscription -> $subStatus")
                            secureStorage.setSubscriptionStatus(subStatus)
                            
                            // Handle Long timestamp for subscription end
                            val subEnd = data["subscriptionEnd"]
                            if (subEnd is Long) {
                                secureStorage.saveSubscriptionEnd(subEnd)
                            } else if (subEnd is com.google.firebase.Timestamp) {
                                secureStorage.saveSubscriptionEnd(subEnd.toDate().time)
                            }
                        }
                    } else {
                        StructuredLogger.i("DeviceManager", "[DeviceControl] No backend document found for $deviceId")
                    }
                }
        }
    }
}
