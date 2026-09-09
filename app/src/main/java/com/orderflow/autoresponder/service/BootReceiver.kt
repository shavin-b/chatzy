package com.orderflow.autoresponder.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.orderflow.autoresponder.core.logger.StructuredLogger

/**
 * BroadcastReceiver triggered on system reboot (ACTION_BOOT_COMPLETED)
 * and app update (ACTION_MY_PACKAGE_REPLACED).
 *
 * Ensures NotificationServiceWatchdog immediately verifies that
 * WhatsAppNotificationListener is rebound by the system on all device brands.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action
        StructuredLogger.i("BootReceiver", "Boot / Package broadcast received: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" || // HTC / MTK fastboot
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            NotificationServiceWatchdog.ensureListenerBound(context)
        }
    }
}
