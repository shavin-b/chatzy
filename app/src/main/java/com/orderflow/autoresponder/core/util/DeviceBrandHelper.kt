package com.orderflow.autoresponder.core.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.orderflow.autoresponder.core.logger.StructuredLogger

/**
 * Universal Brand & OEM OS Helper.
 * Handles background task killer exceptions, autostart managers, and device-specific settings
 * across Samsung, Xiaomi, Oppo, Realme, OnePlus, Vivo, Huawei, Honor, Transsion (Tecno/Infinix), etc.
 */
object DeviceBrandHelper {

    enum class Brand {
        SAMSUNG,
        XIAOMI,
        OPPO,
        REALME,
        ONEPLUS,
        VIVO,
        HUAWEI,
        HONOR,
        TRANSSION, // Tecno, Infinix, Itel
        ASUS,
        OTHER
    }

    fun getBrand(): Brand {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("samsung") -> Brand.SAMSUNG
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> Brand.XIAOMI
            manufacturer.contains("oppo") -> Brand.OPPO
            manufacturer.contains("realme") -> Brand.REALME
            manufacturer.contains("oneplus") -> Brand.ONEPLUS
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> Brand.VIVO
            manufacturer.contains("huawei") -> Brand.HUAWEI
            manufacturer.contains("honor") -> Brand.HONOR
            manufacturer.contains("tecno") || manufacturer.contains("infinix") || manufacturer.contains("itel") -> Brand.TRANSSION
            manufacturer.contains("asus") -> Brand.ASUS
            else -> Brand.OTHER
        }
    }

    fun getBrandDisplayName(): String {
        return when (getBrand()) {
            Brand.SAMSUNG -> "Samsung (One UI)"
            Brand.XIAOMI -> "Xiaomi / Redmi / POCO (MIUI / HyperOS)"
            Brand.OPPO -> "Oppo (ColorOS)"
            Brand.REALME -> "Realme (Realme UI)"
            Brand.ONEPLUS -> "OnePlus (OxygenOS)"
            Brand.VIVO -> "Vivo / iQOO (Funtouch OS)"
            Brand.HUAWEI -> "Huawei (EMUI / HarmonyOS)"
            Brand.HONOR -> "Honor (MagicOS)"
            Brand.TRANSSION -> "Tecno / Infinix / Itel (HiOS / XOS)"
            Brand.ASUS -> "Asus (ZenUI)"
            Brand.OTHER -> "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} (Android ${Build.VERSION.RELEASE})"
        }
    }

    fun hasSpecialAutostartSettings(): Boolean {
        return getBrand() != Brand.OTHER
    }

    fun getBrandGuidanceSteps(): List<String> {
        return when (getBrand()) {
            Brand.XIAOMI -> listOf(
                "Enable 'Autostart' toggle for OrderFlow.",
                "Set Battery Saver to 'No restrictions'.",
                "Lock OrderFlow in Recent Apps screen."
            )
            Brand.SAMSUNG -> listOf(
                "Go to Battery -> Background usage limits.",
                "Ensure OrderFlow is NOT in 'Sleeping' or 'Deep sleeping' apps list.",
                "Set Battery usage to 'Unrestricted'."
            )
            Brand.OPPO, Brand.REALME -> listOf(
                "Enable 'Allow auto-launch' and 'Allow secondary launch'.",
                "In Battery settings, set App Battery Management to 'Don't optimize'."
            )
            Brand.ONEPLUS -> listOf(
                "Enable 'Auto-launch' for OrderFlow.",
                "Set Battery optimization to 'Don't optimize'."
            )
            Brand.VIVO -> listOf(
                "Enable 'Autostart' permission.",
                "In Battery settings, enable 'High background power consumption'."
            )
            Brand.HUAWEI, Brand.HONOR -> listOf(
                "Go to App Launch settings.",
                "Turn OFF 'Manage automatically' for OrderFlow.",
                "Turn ON 'Auto-launch', 'Secondary launch', and 'Run in background'."
            )
            Brand.TRANSSION -> listOf(
                "Open Phone Master -> Auto-start Management.",
                "Allow OrderFlow to auto-start and run in background."
            )
            Brand.ASUS -> listOf(
                "Open Mobile Manager -> Auto-start Manager.",
                "Enable auto-start for OrderFlow."
            )
            Brand.OTHER -> listOf(
                "Set Battery usage to 'Unrestricted' in App Info.",
                "Ensure Notification Access is enabled."
            )
        }
    }

    /**
     * Attempts to open the manufacturer's hidden Autostart / Battery manager screen.
     * Falls back to general App Info if the vendor-specific intent cannot be resolved.
     */
    fun openBrandSettings(context: Context): Boolean {
        val intents = getBrandIntents(context)
        for (intent in intents) {
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    StructuredLogger.i("DeviceBrandHelper", "Successfully launched OEM intent: ${intent.component?.className ?: intent.action}")
                    return true
                }
            } catch (e: Exception) {
                StructuredLogger.w("DeviceBrandHelper", "Failed to launch intent: ${e.message}")
            }
        }

        // Fallback: Open standard App Details Settings
        return try {
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getBrandIntents(context: Context): List<Intent> {
        val brand = getBrand()
        val list = mutableListOf<Intent>()

        when (brand) {
            Brand.XIAOMI -> {
                list.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
                list.add(Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT))
                list.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")))
            }
            Brand.SAMSUNG -> {
                list.add(Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")))
                list.add(Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")))
                list.add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
            Brand.OPPO, Brand.REALME -> {
                list.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")))
                list.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")))
                list.add(Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")))
            }
            Brand.ONEPLUS -> {
                list.add(Intent().setComponent(ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")))
            }
            Brand.VIVO -> {
                list.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")))
                list.add(Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")))
            }
            Brand.HUAWEI, Brand.HONOR -> {
                list.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")))
                list.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")))
            }
            Brand.TRANSSION -> {
                list.add(Intent().setComponent(ComponentName("com.transsion.phonemanager", "com.itel.autostart.AutoStartManageActivity")))
                list.add(Intent().setComponent(ComponentName("com.transsion.phonemanager", "com.transsion.phonemanager.settings.AutoStartManageActivity")))
            }
            Brand.ASUS -> {
                list.add(Intent().setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity")))
            }
            else -> {}
        }

        return list
    }
}
