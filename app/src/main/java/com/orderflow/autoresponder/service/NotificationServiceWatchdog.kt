package com.orderflow.autoresponder.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import com.orderflow.autoresponder.core.logger.StructuredLogger

/**
 * Watchdog and self-healing engine for WhatsAppNotificationListener.
 *
 * Solves the infamous OEM bug (especially on Samsung One UI, Xiaomi MIUI/HyperOS, OnePlus)
 * where NotificationListenerService IPC connection is randomly severed by the OS task killer
 * and the system fails to rebind it automatically.
 */
object NotificationServiceWatchdog {

    @Volatile
    var isConnected: Boolean = false
        internal set

    private var lastRecoveryAttempt = 0L
    private const val MIN_RECOVERY_INTERVAL_MS = 5_000L // 5 seconds rate-limit

    fun isNotificationAccessGranted(context: Context): Boolean {
        val cn = ComponentName(context, WhatsAppNotificationListener::class.java)
        val cnFlat = cn.flattenToString()
        val listenersRaw = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: ""
        return listenersRaw.split(":").map { it.trim() }.any { it == cnFlat }
    }

    /**
     * Dispatches official requestRebind() (API 24+) to re-establish the IPC binder connection.
     * Safe to call during lifecycle transitions (disconnection, task removal, boot).
     */
    fun requestRebind(context: Context): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastRecoveryAttempt < MIN_RECOVERY_INTERVAL_MS) {
            StructuredLogger.w("NotificationServiceWatchdog", "requestRebind throttled (last attempt was ${now - lastRecoveryAttempt}ms ago)")
            return false
        }
        lastRecoveryAttempt = now

        val componentName = ComponentName(context, WhatsAppNotificationListener::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return try {
                StructuredLogger.i("NotificationServiceWatchdog", "[NLS] requestRebind requested")
                NotificationListenerService.requestRebind(componentName)
                true
            } catch (e: Exception) {
                StructuredLogger.w("NotificationServiceWatchdog", "[NLS] requestRebind failed: ${e.message}")
                false
            }
        }
        return false
    }

    /**
     * Executes the recovery sequence (requestRebind + component toggle).
     * Intended for explicit manual user recovery from Settings UI when service is disconnected.
     */
    fun recoverService(context: Context): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastRecoveryAttempt < MIN_RECOVERY_INTERVAL_MS) {
            StructuredLogger.w("NotificationServiceWatchdog", "Recovery throttled (last attempt was ${now - lastRecoveryAttempt}ms ago)")
            return false
        }
        lastRecoveryAttempt = now

        val componentName = ComponentName(context, WhatsAppNotificationListener::class.java)
        StructuredLogger.i("NotificationServiceWatchdog", "Initiating recovery for WhatsAppNotificationListener...")

        var recovered = false

        // 1. API requestRebind
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                NotificationListenerService.requestRebind(componentName)
                StructuredLogger.i("NotificationServiceWatchdog", "[NLS] requestRebind requested")
                recovered = true
            } catch (e: Exception) {
                StructuredLogger.w("NotificationServiceWatchdog", "requestRebind() failed: ${e.message}")
            }
        }

        // 2. Component toggle trick
        try {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            StructuredLogger.i("NotificationServiceWatchdog", "Component toggle recovery executed successfully")
            recovered = true
        } catch (e: Exception) {
            StructuredLogger.e("NotificationServiceWatchdog", "Component toggle recovery failed", e)
        }

        return recovered
    }

    /**
     * Checks if the listener is bound; if permission is granted but the service is
     * not connected, triggers requestRebind.
     */
    fun ensureListenerBound(context: Context) {
        if (isNotificationAccessGranted(context) && !isConnected) {
            StructuredLogger.w("NotificationServiceWatchdog", "Listener permission is granted but service is disconnected! Triggering requestRebind...")
            requestRebind(context)
        }
    }
}
