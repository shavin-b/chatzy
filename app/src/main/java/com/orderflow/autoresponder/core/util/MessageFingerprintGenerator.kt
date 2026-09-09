package com.orderflow.autoresponder.core.util

import java.security.MessageDigest

object MessageFingerprintGenerator {

    /**
     * Generates a unique SHA-256 fingerprint for a message.
     * 
     * @param packageName Package of the source app (e.g. com.whatsapp)
     * @param conversationTitle Title of the chat (Contact name or Group name)
     * @param senderName Name of the person who sent the message
     * @param text The actual message content
     * @param timestamp The time the message was received/posted
     * @param bucketSizeMs Time window to bucket similar messages (default 1 second)
     */
    fun generate(
        packageName: String,
        conversationTitle: String?,
        senderName: String?,
        text: String,
        timestamp: Long,
        bucketSizeMs: Long = 1_000L
    ): String {
        val timestampBucket = timestamp / bucketSizeMs
        
        // Normalize text: trim and lower case to handle minor variations
        val normalizedText = text.trim().lowercase()
        
        val rawString = buildString {
            append(packageName)
            append("|")
            append(conversationTitle ?: "")
            append("|")
            append(senderName ?: "")
            append("|")
            append(normalizedText)
            append("|")
            append(timestampBucket)
        }
        
        return sha256(rawString)
    }

    private fun sha256(input: String): String {
        return try {
            val bytes = input.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            // Fallback to hashCode if SHA-256 fails (shouldn't happen)
            input.hashCode().toString()
        }
    }
}
