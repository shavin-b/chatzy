package com.orderflow.autoresponder
import com.orderflow.autoresponder.device.auth.DeviceSubscriptionState
import org.junit.Assert.assertEquals
import org.junit.Test
class DeviceSubscriptionStateTest {
    @Test fun dashboardSuspensionOverridesCachedActiveStatus() {
        assertEquals("SUSPENDED", DeviceSubscriptionState.resolve("Suspended", "ACTIVE", 2000L, 1000L))
    }
    @Test fun approvedRenewalUsesDashboardAuthorityAndPreservedFutureExpiry() {
        assertEquals("ACTIVE", DeviceSubscriptionState.resolve("Active", "SUSPENDED", 2000L, 1000L))
    }
    @Test fun expiredSubscriptionsDoNotBecomeActiveFromAnOldStatus() {
        assertEquals("EXPIRED", DeviceSubscriptionState.resolve("Active", "ACTIVE", 500L, 1000L))
    }
    @Test fun healthStatusDoesNotEraseSubscriptionAuthority() {
        assertEquals("ACTIVE", DeviceSubscriptionState.resolve("Offline", "ACTIVE", 2000L, 1000L))
    }
    @Test fun unknownServerStatusFailsClosed() {
        assertEquals("SUSPENDED", DeviceSubscriptionState.resolve(null, null, null, 1000L))
    }
}
