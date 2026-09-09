package com.orderflow.autoresponder.core.util

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory tracker for recently sent auto-replies.
 *
 * Prevents echo loops across all WhatsApp versions and Android API levels:
 * When an auto-reply is dispatched via RemoteInput, WhatsApp updates the active
 * notification with the sent text. By checking whether an incoming notification's text
 * matches a reply dispatched within the last 15 seconds, we reliably ignore self-messages
 * without depending on fragile OS-specific or localized "You:" / Person parsing.
 */
object EchoReplyTracker {

    private val recentlySentReplies = ConcurrentHashMap<String, Long>()
    private const val TTL_MS = 15_000L // 15 seconds window

    fun recordSentReply(replyText: String) {
        val key = normalize(replyText)
        if (key.isNotEmpty()) {
            recentlySentReplies[key] = System.currentTimeMillis()
            pruneStale()
        }
    }

    fun isEcho(messageText: String): Boolean {
        val key = normalize(messageText)
        val sentAt = recentlySentReplies[key] ?: return false
        val isRecent = (System.currentTimeMillis() - sentAt) < TTL_MS
        if (!isRecent) {
            recentlySentReplies.remove(key)
        }
        return isRecent
    }

    private fun normalize(text: String): String = text.trim().lowercase()

    private fun pruneStale() {
        val cutoff = System.currentTimeMillis() - TTL_MS
        val iterator = recentlySentReplies.entries.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().value < cutoff) {
                iterator.remove()
            }
        }
    }
}
