package com.orderflow.autoresponder

import com.orderflow.autoresponder.device.auth.DeviceWritePolicy
import org.junit.Assert.*
import org.junit.Test

class DeviceWritePolicyTest {
    @Test fun telemetrySurvivesButAuthorityAndIdentityNeverLeaveTheDevice() {
        val input = mapOf<String, Any>("lastSeen" to 123L, "fcmToken" to "test-only-token", "businessName" to "Shop",
            "adminLocked" to false, "lockStatus" to "UNLOCKED", "lockReason" to "NONE", "subscriptionEnd" to 999L,
            "subscriptionStatus" to "ACTIVE", "status" to "Active", "deviceId" to "OF-OTHER", "generatedUuid" to "OF-OTHER", "desiredCommandId" to "forged")
        val actual = DeviceWritePolicy.telemetryOnly(input)
        assertEquals(setOf("lastSeen", "fcmToken", "businessName"), actual.keys)
        assertEquals(123L, actual["lastSeen"])
    }
    @Test fun fullHeartbeatFiltersLocalLockReportsWithoutChangingTimestampOrToken() {
        val actual = DeviceWritePolicy.telemetryOnly(mapOf("lastSeen" to 100L, "lastSync" to 100L, "adminLocked" to true, "lockStatus" to "LOCKED", "lockReason" to "ADMIN", "fcmToken" to "test-only-token"))
        assertEquals(setOf("lastSeen", "lastSync", "fcmToken"), actual.keys)
    }
    @Test fun commandMetadataRemainsTelemetry() {
        val update = mapOf<String, Any>("lastCommandId" to "command", "lastCommandStatus" to "SUCCESS", "lastSync" to 123L)
        assertEquals(update, DeviceWritePolicy.telemetryOnly(update))
    }
}
