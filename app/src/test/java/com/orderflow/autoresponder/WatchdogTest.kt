package com.orderflow.autoresponder

import com.orderflow.autoresponder.service.NotificationServiceWatchdog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchdogTest {

    @Test
    fun isConnectedTracksState() {
        NotificationServiceWatchdog.isConnected = false
        assertFalse(NotificationServiceWatchdog.isConnected)

        NotificationServiceWatchdog.isConnected = true
        assertTrue(NotificationServiceWatchdog.isConnected)

        NotificationServiceWatchdog.isConnected = false
        assertFalse(NotificationServiceWatchdog.isConnected)
    }
}
