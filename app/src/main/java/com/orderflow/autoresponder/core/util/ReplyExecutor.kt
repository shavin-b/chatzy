package com.orderflow.autoresponder.core.util

import android.app.PendingIntent
import android.app.RemoteInput

/**
 * Interface responsible for executing direct notification replies via PendingIntent + RemoteInput.
 */
interface ReplyExecutor {
    fun sendDirectReply(
        pendingIntent: PendingIntent,
        remoteInput: RemoteInput,
        replyText: String
    ): Boolean
}
