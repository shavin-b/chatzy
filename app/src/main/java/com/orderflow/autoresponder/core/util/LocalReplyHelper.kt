package com.orderflow.autoresponder.core.util

import android.app.ActivityOptions
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.orderflow.autoresponder.core.logger.StructuredLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalReplyHelper @Inject constructor(
    @ApplicationContext private val context: Context
) : ReplyExecutor {
    /**
     * Executes a direct reply using the provided notification action metadata.
     * Compatible with all Android versions from API 23 to API 36 (Android 16).
     */
    @Suppress("DEPRECATION")
    override fun sendDirectReply(
        pendingIntent: PendingIntent,
        remoteInput: RemoteInput,
        replyText: String
    ): Boolean {
        if (replyText.isBlank()) {
            StructuredLogger.w("LocalReplyHelper", "Reply text is blank, skipping.")
            return false
        }

        return try {
            StructuredLogger.i(
                "LocalReplyHelper",
                "Preparing local reply: '$replyText' to package ${pendingIntent.creatorPackage} (API ${Build.VERSION.SDK_INT})"
            )

            // 1. Build the results Bundle with the expected RemoteInput resultKey
            val results = Bundle().apply {
                putCharSequence(remoteInput.resultKey, replyText)
            }

            // 2. Prepare the reply Intent with foreground receiver flag
            val intent = Intent().apply {
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }

            // 3. Attach results to the Intent via platform RemoteInput
            RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, results)

            // 4. Android 14+ (API 34+) and Android 15 (API 35+):
            // Pass background activity launch permission so the OS does not drop the reply action
            val options: Bundle? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    ActivityOptions.makeBasic().apply {
                        setPendingIntentBackgroundActivityStartMode(
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                        )
                    }.toBundle()
                } catch (e: Throwable) {
                    null
                }
            } else null

            // 5. Dispatch the PendingIntent
            if (options != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                pendingIntent.send(context, 0, intent, null, null, null, options)
            } else {
                pendingIntent.send(context, 0, intent)
            }

            // 6. Record sent reply to prevent echo loop
            EchoReplyTracker.recordSentReply(replyText)

            StructuredLogger.i("LocalReplyHelper", "Successfully dispatched WhatsApp notification reply via PendingIntent")
            true
        } catch (e: PendingIntent.CanceledException) {
            StructuredLogger.e("LocalReplyHelper", "PendingIntent was canceled (notification dismissed)", e)
            false
        } catch (e: Exception) {
            StructuredLogger.e("LocalReplyHelper", "Failed to send local reply on API ${Build.VERSION.SDK_INT}", e)
            false
        }
    }
}


