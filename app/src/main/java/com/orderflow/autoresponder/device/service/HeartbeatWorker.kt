package com.orderflow.autoresponder.device.service

import android.content.Context
import com.orderflow.autoresponder.core.security.SecureStorage
import com.orderflow.autoresponder.device.repository.DeviceRepository
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class HeartbeatWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val deviceRepository: DeviceRepository,
    private val secureStorage: SecureStorage
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Recurring watchdog: check if the notification listener is still bound
        // every 15 minutes. This recovers from silent OS-level unbinds.
        com.orderflow.autoresponder.service.NotificationServiceWatchdog.ensureListenerBound(applicationContext)

        val deviceId = secureStorage.getDeviceId() ?: return Result.success()

        // Include lock state in every heartbeat so the admin dashboard stays consistent
        // even after device reboots or missed FCM acknowledgements.
        val now = System.currentTimeMillis()
        val fcmToken = secureStorage.getFcmToken() ?: ""

        val updates = mutableMapOf<String, Any>(
            "lastSeen"    to now,
            "lastSync"    to now,
            "adminLocked" to secureStorage.isAdminLocked(),
            "lockStatus"  to if (secureStorage.isAdminLocked()) "LOCKED" else "UNLOCKED",
            "lockReason"  to secureStorage.getLockReason()
        )

        // Self-heal: include FCM token in every heartbeat so the admin console
        // always has a valid token even if the initial upload was missed.
        if (fcmToken.isNotBlank()) {
            updates["fcmToken"] = fcmToken
        }

        return try {
            deviceRepository.updateDeviceInfo(deviceId, updates)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
