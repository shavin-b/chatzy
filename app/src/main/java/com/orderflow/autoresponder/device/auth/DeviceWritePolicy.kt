package com.orderflow.autoresponder.device.auth

/** Enrollment mode has no client authority over identity, lock or subscription fields. */
object DeviceWritePolicy {
    private val telemetry = setOf("phoneModel", "manufacturer", "androidVersion", "appVersion", "appBuildNumber",
        "installationDate", "userName", "businessName", "phoneNumber", "lastSeen", "lastSync", "fcmToken",
        "lastCommandId", "lastCommandStatus")
    fun telemetryOnly(updates: Map<String, Any>): Map<String, Any> = updates.filterKeys { it in telemetry }

    /** Keep lastSync compatible with existing consumers; lastSeen is always server time. */
    fun serverLastSeen(updates: Map<String, Any>): Map<String, Any> =
        if ("lastSeen" in updates) updates + ("lastSeen" to com.google.firebase.firestore.FieldValue.serverTimestamp())
        else updates
}
