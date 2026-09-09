package com.orderflow.autoresponder.core.security

import android.content.Context
import android.content.SharedPreferences
import com.orderflow.autoresponder.domain.model.MetaCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveMetaCredentials(credentials: MetaCredentials) {
        prefs.edit()
            .putString(KEY_PHONE_ID, credentials.phoneNumberId)
            .putString(KEY_ACCESS_TOKEN, credentials.accessToken)
            .putString(KEY_BUSINESS_ID, credentials.businessAccountId)
            .putString(KEY_WEBHOOK_TOKEN, credentials.webhookVerifyToken)
            .apply()
    }

    fun getMetaCredentials(): MetaCredentials {
        return MetaCredentials(
            phoneNumberId = prefs.getString(KEY_PHONE_ID, "") ?: "",
            accessToken = prefs.getString(KEY_ACCESS_TOKEN, "") ?: "",
            businessAccountId = prefs.getString(KEY_BUSINESS_ID, "") ?: "",
            webhookVerifyToken = prefs.getString(KEY_WEBHOOK_TOKEN, "") ?: ""
        )
    }

    fun setAutoResponderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RESPONDER_ENABLED, enabled).apply()
    }

    fun isAutoResponderEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_RESPONDER_ENABLED, true)
    }

    fun setUseCloudApi(useCloud: Boolean) {
        prefs.edit().putBoolean(KEY_USE_CLOUD_API, useCloud).apply()
    }

    fun useCloudApi(): Boolean {
        return prefs.getBoolean(KEY_USE_CLOUD_API, false)
    }

    // --- Subscription Management ---
    
    fun setSubscriptionStatus(status: String) {
        prefs.edit().putString(KEY_SUBSCRIPTION_STATUS, status).apply()
        if (!isSubscriptionActive()) {
            com.orderflow.autoresponder.core.util.ActiveSequenceTracker.cancelAll("Subscription not active: $status")
        }
    }

    fun getSubscriptionStatus(): String {
        // DEFAULT: Always ACTIVE for new installations until synced
        return prefs.getString(KEY_SUBSCRIPTION_STATUS, "ACTIVE") ?: "ACTIVE"
    }

    fun isSubscriptionActive(): Boolean {
        val status = getSubscriptionStatus().trim()
        val isActive = status.isEmpty() || // Allow empty as active for safety
                       status.equals("ACTIVE", ignoreCase = true) || 
                       status.equals("EXPIRING_SOON", ignoreCase = true) ||
                       status.equals("Active", ignoreCase = true) ||
                       status.equals("Trial", ignoreCase = true) ||
                       status.equals("none", ignoreCase = true) ||
                       status.equals("n/a", ignoreCase = true)
        
        android.util.Log.d("OrderFlow", "[SecureStorage] isSubscriptionActive: status='$status', result=$isActive")
        return isActive
    }

    // --- Admin Lock Management ---

    fun setAdminLocked(locked: Boolean) {
        android.util.Log.w("OrderFlow", "[SecureStorage] setAdminLocked: $locked")
        prefs.edit().putBoolean(KEY_ADMIN_LOCKED, locked).apply()
        if (locked) {
            com.orderflow.autoresponder.core.util.ActiveSequenceTracker.cancelAll("Device locked by admin")
        }
    }

    fun isAdminLocked(): Boolean {
        val isLocked = prefs.getBoolean(KEY_ADMIN_LOCKED, false)
        android.util.Log.d("OrderFlow", "[SecureStorage] isAdminLocked: $isLocked")
        return isLocked
    }

    fun setLockReason(reason: String) {
        prefs.edit().putString(KEY_LOCK_REASON, reason).apply()
    }

    fun getLockReason(): String {
        return prefs.getString(KEY_LOCK_REASON, "NONE") ?: "NONE"
    }

    // --- Customer Profile Management ---

    fun setBusinessName(name: String) {
        prefs.edit().putString(KEY_BUSINESS_NAME, name).apply()
    }

    fun getBusinessName(): String {
        return prefs.getString(KEY_BUSINESS_NAME, "") ?: ""
    }

    fun setWhatsAppNumber(number: String) {
        prefs.edit().putString(KEY_WHATSAPP_NUMBER, number).apply()
    }

    fun getWhatsAppNumber(): String {
        return prefs.getString(KEY_WHATSAPP_NUMBER, "") ?: ""
    }

    fun setProfileCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_PROFILE_COMPLETED, completed).apply()
    }

    fun isProfileCompleted(): Boolean {
        return prefs.getBoolean(KEY_PROFILE_COMPLETED, false)
    }

    fun setProfileSyncPending(pending: Boolean) {
        prefs.edit().putBoolean(KEY_PROFILE_SYNC_PENDING, pending).apply()
    }

    fun isProfileSyncPending(): Boolean {
        return prefs.getBoolean(KEY_PROFILE_SYNC_PENDING, false)
    }

    /**
     * Observable flow for profile completion status.
     */
    fun profileCompletionFlow(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == KEY_PROFILE_COMPLETED) {
                trySend(sharedPreferences.getBoolean(KEY_PROFILE_COMPLETED, false))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.onStart { emit(isProfileCompleted()) }

    // --- Effective Application State ---

    fun isEffectivelyBlocked(): Boolean {
        val admin = isAdminLocked()
        val subActive = isSubscriptionActive()
        val blocked = admin || !subActive
        android.util.Log.w("OrderFlow", "[SecureStorage] isEffectivelyBlocked: adminLocked=$admin, subActive=$subActive -> result=$blocked")
        return blocked
    }

    /**
     * Combined flow for the UI to observe effectively blocked state.
     * Emits true if Admin Locked OR Subscription is NOT Active.
     */
    fun applicationBlockFlow(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == KEY_ADMIN_LOCKED || key == KEY_SUBSCRIPTION_STATUS) {
                trySend(isEffectivelyBlocked())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.onStart { emit(isEffectivelyBlocked()) }

    // --- Identity ---

    fun getDeviceId(): String? {
        return prefs.getString(KEY_DEVICE_ID, null)
    }

    fun forceUnlockOnFirstRun() {
        val deviceIdExists = prefs.contains(KEY_DEVICE_ID)
        val markerExists = prefs.contains(KEY_FIRST_LAUNCH_MARKER)
        
        if (!deviceIdExists || !markerExists) {
            android.util.Log.e("OrderFlow", "[SecureStorage] Fresh installation or missing ID detected. Forcing clean state (UNLOCKED + ACTIVE).")
            setAdminLocked(false)
            setSubscriptionStatus("ACTIVE")
            setLockReason("NONE")
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH_MARKER, true).apply()
        }
    }

    fun saveDeviceId(deviceId: String) {
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }

    fun getFcmToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    fun saveFcmToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getSubscriptionEnd(): Long {
        return prefs.getLong(KEY_SUBSCRIPTION_END, 0L)
    }

    fun saveSubscriptionEnd(timestamp: Long) {
        prefs.edit().putLong(KEY_SUBSCRIPTION_END, timestamp).apply()
    }

    // --- Command Sync ---

    fun isCommandProcessed(commandId: String): Boolean {
        val processed = prefs.getStringSet(KEY_PROCESSED_COMMANDS, emptySet()) ?: emptySet()
        return processed.contains(commandId)
    }

    fun markCommandAsProcessed(commandId: String) {
        val processed = prefs.getStringSet(KEY_PROCESSED_COMMANDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        processed.add(commandId)
        // Keep only last 100 commands to prevent unlimited growth
        val trimmed = if (processed.size > 100) processed.toList().takeLast(100).toSet() else processed
        prefs.edit().putStringSet(KEY_PROCESSED_COMMANDS, trimmed).apply()
    }

    // --- Legacy / Compatibility (To be removed after UI update) ---

    @Deprecated("Use isSubscriptionActive()", replaceWith = ReplaceWith("!isSubscriptionActive()"))
    fun isAppSuspended(): Boolean = !isSubscriptionActive()

    @Deprecated("Use setSubscriptionStatus()", replaceWith = ReplaceWith("setSubscriptionStatus(if (suspended) \"EXPIRED\" else \"ACTIVE\")"))
    fun setAppSuspended(suspended: Boolean) {
        setSubscriptionStatus(if (suspended) "EXPIRED" else "ACTIVE")
    }

    fun isAppSuspendedFlow(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == KEY_SUBSCRIPTION_STATUS) {
                trySend(!isSubscriptionActive())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.onStart { emit(!isSubscriptionActive()) }

    fun isAdminLockedFlow(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == KEY_ADMIN_LOCKED) {
                trySend(isAdminLocked())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.onStart { emit(isAdminLocked()) }

    companion object {
        private const val PREFS_NAME = "orderflow_secure_prefs"
        private const val KEY_PHONE_ID = "phone_number_id"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_BUSINESS_ID = "business_account_id"
        private const val KEY_WEBHOOK_TOKEN = "webhook_verify_token"
        private const val KEY_AUTO_RESPONDER_ENABLED = "auto_responder_enabled"
        private const val KEY_USE_CLOUD_API = "use_cloud_api"
        private const val KEY_ADMIN_LOCKED = "admin_locked"
        private const val KEY_LOCK_REASON = "lock_reason"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_SUBSCRIPTION_END = "subscription_end"
        private const val KEY_SUBSCRIPTION_STATUS = "subscription_status"
        private const val KEY_PROCESSED_COMMANDS = "processed_command_ids"
        
        private const val KEY_BUSINESS_NAME = "business_name"
        private const val KEY_WHATSAPP_NUMBER = "whatsapp_number"
        private const val KEY_PROFILE_COMPLETED = "profile_completed"
        private const val KEY_PROFILE_SYNC_PENDING = "profile_sync_pending"

        private const val KEY_FIRST_LAUNCH_MARKER = "first_launch_marker"
        private const val KEY_APP_SUSPENDED = "app_suspended" // Kept for legacy key
    }
}
