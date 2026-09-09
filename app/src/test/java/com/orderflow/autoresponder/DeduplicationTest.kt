package com.orderflow.autoresponder

import com.orderflow.autoresponder.core.util.EchoReplyTracker
import com.orderflow.autoresponder.core.util.MessageFingerprintGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeduplicationTest {

    @Test
    fun sameMessageGeneratesIdenticalFingerprint() {
        val fp1 = MessageFingerprintGenerator.generate(
            packageName = "com.whatsapp.w4b",
            conversationTitle = "Customer Alice",
            senderName = "Alice",
            text = "Hello OrderFlow",
            timestamp = 1700000000500L
        )

        val fp2 = MessageFingerprintGenerator.generate(
            packageName = "com.whatsapp.w4b",
            conversationTitle = "Customer Alice",
            senderName = "Alice",
            text = "  hello orderflow  ", // normalized
            timestamp = 1700000000800L  // same 1-second bucket
        )

        assertEquals("Identical message within same second bucket must have identical fingerprint", fp1, fp2)
    }

    @Test
    fun differentMessageGeneratesDifferentFingerprint() {
        val fp1 = MessageFingerprintGenerator.generate(
            packageName = "com.whatsapp.w4b",
            conversationTitle = "Customer Alice",
            senderName = "Alice",
            text = "Hello OrderFlow",
            timestamp = 1700000000500L
        )

        val fp2 = MessageFingerprintGenerator.generate(
            packageName = "com.whatsapp.w4b",
            conversationTitle = "Customer Alice",
            senderName = "Alice",
            text = "Different message",
            timestamp = 1700000000500L
        )

        assertNotEquals("Different messages must generate different fingerprints", fp1, fp2)
    }

    @Test
    fun differentTimeBucketGeneratesDifferentFingerprint() {
        val fp1 = MessageFingerprintGenerator.generate(
            packageName = "com.whatsapp.w4b",
            conversationTitle = "Customer Alice",
            senderName = "Alice",
            text = "Hello OrderFlow",
            timestamp = 1700000000000L
        )

        val fp2 = MessageFingerprintGenerator.generate(
            packageName = "com.whatsapp.w4b",
            conversationTitle = "Customer Alice",
            senderName = "Alice",
            text = "Hello OrderFlow",
            timestamp = 1700000005000L // 5 seconds later
        )

        assertNotEquals("Messages in different time buckets must have distinct fingerprints", fp1, fp2)
    }

    @Test
    fun echoReplyTracker_detectsEcho() {
        val replyText = "Thank you for contacting us! We will get back to you shortly."

        // Before recording, it should not be considered an echo
        assertFalse(EchoReplyTracker.isEcho(replyText))

        // Record the sent reply
        EchoReplyTracker.recordSentReply(replyText)

        // Immediately afterwards, incoming notification with this text is an echo
        assertTrue("Sent reply must be detected as an echo", EchoReplyTracker.isEcho(replyText))
        assertTrue("Echo check must be case and whitespace insensitive", EchoReplyTracker.isEcho("  thank you for contacting us! We will get back to you shortly.  "))

        // Unrelated message should not be an echo
        assertFalse(EchoReplyTracker.isEcho("A completely different incoming customer inquiry"))
    }
}
