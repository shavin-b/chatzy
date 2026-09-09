package com.orderflow.autoresponder

import com.orderflow.autoresponder.core.util.WhatsAppConstants
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies strict package filtering for WhatsApp Business exclusively.
 */
class PackageFilterTest {

    @Test
    fun targetPackageIsWhatsAppBusiness() {
        assertTrue(
            "WhatsApp Business package must be com.whatsapp.w4b",
            WhatsAppConstants.TARGET_PACKAGE == "com.whatsapp.w4b"
        )
    }

    @Test
    fun acceptsOnlyWhatsAppBusinessPackage() {
        assertTrue(
            "Must accept com.whatsapp.w4b",
            WhatsAppConstants.isWhatsApp("com.whatsapp.w4b")
        )
    }

    @Test
    fun rejectsRegularWhatsAppPackage() {
        assertFalse(
            "Must reject com.whatsapp (regular consumer WhatsApp)",
            WhatsAppConstants.isWhatsApp("com.whatsapp")
        )
    }

    @Test
    fun rejectsThirdPartyPackages() {
        assertFalse("Must reject Telegram", WhatsAppConstants.isWhatsApp("org.telegram.messenger"))
        assertFalse("Must reject Facebook Messenger", WhatsAppConstants.isWhatsApp("com.facebook.orca"))
        assertFalse("Must reject Signal", WhatsAppConstants.isWhatsApp("org.thoughtcrime.securesms"))
        assertFalse("Must reject SMS", WhatsAppConstants.isWhatsApp("com.google.android.apps.messaging"))
    }

    @Test
    fun rejectsNullAndEmptyPackage() {
        assertFalse("Must reject null package", WhatsAppConstants.isWhatsApp(null))
        assertFalse("Must reject blank package", WhatsAppConstants.isWhatsApp(""))
        assertFalse("Must reject whitespace package", WhatsAppConstants.isWhatsApp("   "))
    }
}
