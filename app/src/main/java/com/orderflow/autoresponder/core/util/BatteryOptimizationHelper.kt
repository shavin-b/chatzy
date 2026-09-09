package com.orderflow.autoresponder.core.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.orderflow.autoresponder.core.logger.StructuredLogger

object BatteryOptimizationHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    /**
     * Requests the system direct dialog: "Let app always run in background? [Allow] [Deny]"
     * using Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS with package URI.
     * Falls back to the general battery settings if the direct action is not allowed.
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            StructuredLogger.i("BatteryOptimizationHelper", "Requested direct ignore battery optimization prompt")
            true
        } catch (e: Exception) {
            StructuredLogger.w("BatteryOptimizationHelper", "Direct request failed (${e.message}), falling back to general list")
            try {
                val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                true
            } catch (e2: Exception) {
                StructuredLogger.e("BatteryOptimizationHelper", "All battery optimization intents failed", e2)
                false
            }
        }
    }
}
