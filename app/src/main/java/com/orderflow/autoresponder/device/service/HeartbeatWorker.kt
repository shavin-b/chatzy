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

        // Heartbeats report presence, never overwrite administrator lock/subscription state.
        val now = System.currentTimeMillis()
        val fcmToken = secureStorage.getFcmToken() ?: ""

        val updates = mutableMapOf<String, Any>(
            "lastSeen"    to now,
            "lastSync"    to now
        )

        // Self-heal: include FCM token in every heartbeat so the admin console
        // always has a valid token even if the initial upload was missed.
        if (fcmToken.isNotBlank()) {
            updates["fcmToken"] = fcmToken
        }

        return try {
            val result = deviceRepository.updateDeviceInfo(deviceId, updates)
            if (result.isSuccess) Result.success() else Result.retry()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            com.orderflow.autoresponder.device.auth.DeviceRegistrationDiagnostics.failure("heartbeat", deviceId, e)
            Result.retry()
        }
    }
}
