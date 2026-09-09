package com.orderflow.autoresponder

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationGuardTest {

    private fun isSubscriptionActive(status: String): Boolean {
        val s = status.trim()
        return s.isEmpty() ||
               s.equals("ACTIVE", ignoreCase = true) || 
               s.equals("EXPIRING_SOON", ignoreCase = true) ||
               s.equals("Active", ignoreCase = true) ||
               s.equals("Trial", ignoreCase = true) ||
               s.equals("none", ignoreCase = true) ||
               s.equals("n/a", ignoreCase = true)
    }

    private fun isEffectivelyBlocked(adminLocked: Boolean, subscriptionStatus: String): Boolean {
        return adminLocked || !isSubscriptionActive(subscriptionStatus)
    }

    @Test
    fun unlockedAndActive_isNotBlocked() {
        assertFalse(
            "Device must NOT be blocked when adminLocked=false and sub=ACTIVE",
            isEffectivelyBlocked(adminLocked = false, subscriptionStatus = "ACTIVE")
        )
        assertFalse(
            "Device must NOT be blocked when sub=EXPIRING_SOON",
            isEffectivelyBlocked(adminLocked = false, subscriptionStatus = "EXPIRING_SOON")
        )
        assertFalse(
            "Device must NOT be blocked when sub=Trial",
            isEffectivelyBlocked(adminLocked = false, subscriptionStatus = "Trial")
        )
    }

    @Test
    fun adminLocked_isAlwaysBlocked() {
        assertTrue(
            "Device must be blocked when adminLocked=true even if sub=ACTIVE",
            isEffectivelyBlocked(adminLocked = true, subscriptionStatus = "ACTIVE")
        )
        assertTrue(
            "Device must be blocked when adminLocked=true and sub=EXPIRED",
            isEffectivelyBlocked(adminLocked = true, subscriptionStatus = "EXPIRED")
        )
    }

    @Test
    fun expiredOrSuspended_isBlockedEvenWhenNotAdminLocked() {
        assertTrue(
            "Device must be blocked when subscription is EXPIRED",
            isEffectivelyBlocked(adminLocked = false, subscriptionStatus = "EXPIRED")
        )
        assertTrue(
            "Device must be blocked when subscription is SUSPENDED",
            isEffectivelyBlocked(adminLocked = false, subscriptionStatus = "SUSPENDED")
        )
        assertTrue(
            "Device must be blocked when subscription is CANCELLED",
            isEffectivelyBlocked(adminLocked = false, subscriptionStatus = "CANCELLED")
        )
    }
}
