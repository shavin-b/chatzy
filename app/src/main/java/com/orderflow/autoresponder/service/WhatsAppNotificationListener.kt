package com.orderflow.autoresponder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.orderflow.autoresponder.core.logger.StructuredLogger
import com.orderflow.autoresponder.core.util.WhatsAppConstants
import com.orderflow.autoresponder.domain.usecase.ProcessIncomingMessageUseCase
import com.orderflow.autoresponder.service.parser.WhatsAppNotificationParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import javax.inject.Inject

@AndroidEntryPoint
class WhatsAppNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var processIncomingMessageUseCase: ProcessIncomingMessageUseCase

    @Inject
    lateinit var secureStorage: com.orderflow.autoresponder.core.security.SecureStorage

    private var serviceJob = SupervisorJob()
    private var serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    companion object {
        private const val CHANNEL_ID = "orderflow_engine_channel"
        private const val NOTIFICATION_ID = 8877
    }

    override fun onCreate() {
        super.onCreate()
        if (serviceJob.isCancelled) {
            serviceJob = SupervisorJob()
            serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)
        }
        StructuredLogger.i("WhatsAppNotificationListener", "[NLS] onCreate")
        startPersistentForegroundService()
    }

    private fun startPersistentForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OrderFlow Engine Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps OrderFlow auto-reply active in the background"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OrderFlow Engine Active")
            .setContentText("Monitoring WhatsApp Business notifications...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            StructuredLogger.i("WhatsAppNotificationListener", "[NLS] Foreground service started successfully")
        } catch (e: Exception) {
            StructuredLogger.e("WhatsAppNotificationListener", "[NLS] Failed to start foreground service", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        StructuredLogger.d("WhatsAppNotificationListener", "[NLS] onStartCommand")
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        StructuredLogger.i("WhatsAppNotificationListener", "[NLS] onTaskRemoved")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                StructuredLogger.i("WhatsAppNotificationListener", "[NLS] requestRebind requested")
                requestRebind(ComponentName(this, WhatsAppNotificationListener::class.java))
            } catch (e: Exception) {
                StructuredLogger.w("WhatsAppNotificationListener", "[NLS] requestRebind in onTaskRemoved failed: ${e.message}")
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        StructuredLogger.i("AutoReply", "[NLS] Notification received from $packageName")

        if (packageName != WhatsAppConstants.WHATSAPP_W4B_PKG) {
            StructuredLogger.d("AutoReply", "[NLS] Ignored package: $packageName")
            return
        }

        StructuredLogger.i("AutoReply", "[NLS] Accepted package: com.whatsapp.w4b")

        val parsedMessage = WhatsAppNotificationParser.parse(sbn)
        if (parsedMessage == null) {
            StructuredLogger.w("AutoReply", "[AutoReply] Message parsing failed or ignored (summary/system notification)")
            return
        }

        StructuredLogger.i("AutoReply", "[AutoReply] Message parsed successfully. Sender: ${parsedMessage.senderName}, Group: ${parsedMessage.isGroup}")
        StructuredLogger.d("AutoReply", "[AutoReply] Content: ${parsedMessage.messageText}")

        // Magic Keyword Dismissal
        val text = parsedMessage.messageText
        val isMagicKeyword = text.contains("A3&^\$\$JSJXGHhgshjzx6586+95") || 
                             (text.contains("suyf%") && text.contains("&#44646871HytYTFH"))
        
        if (isMagicKeyword) {
            cancelNotification(sbn.key)
            StructuredLogger.i("AutoReply", "[AutoReply] Control notification dismissed.")
        }

        // Early Automation Guard: Block normal replies immediately if admin locked or subscription inactive
        if (secureStorage.isEffectivelyBlocked() && !isMagicKeyword) {
            StructuredLogger.w("AutoReply", "[NLS] Automation blocked: Device is locked or subscription is inactive")
            return
        }

        // Extract Direct Reply Action
        val (pendingIntent, remoteInput) = findReplyAction(sbn.notification)
        StructuredLogger.i("AutoReply", "[AutoReply] Reply action found: ${pendingIntent != null}, RemoteInput: ${remoteInput?.resultKey}")

        // Extract Phone Number
        val extras = sbn.notification.extras ?: android.os.Bundle.EMPTY
        var senderPhone = ""
        val waId = extras.getString("key_remote_jid")?.split("@")?.firstOrNull() 
            ?: extras.getString("sender_contact_id")
            ?: extras.getString("contact_id")
        
        if (!waId.isNullOrBlank() && waId.all { it.isDigit() }) {
            senderPhone = waId
        }

        val finalPhone = senderPhone.ifBlank { parsedMessage.senderName ?: "Unknown" }

        // Acquire a short partial wake lock (5s safety timeout) to guarantee that OEM Doze / App Standby
        // does not suspend the CPU while we are evaluating rules and dispatching the reply.
        val powerManager = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        val wakeLock = powerManager?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "OrderFlow:NotificationWakeLock")
        try {
            wakeLock?.acquire(5_000L)
        } catch (e: Exception) {
            // Ignore if wake lock cannot be acquired
        }

        serviceScope.launch {
            try {
                processIncomingMessageUseCase(
                    senderPhone = finalPhone,
                    senderName = parsedMessage.senderName ?: "Unknown",
                    messageText = parsedMessage.messageText,
                    isGroup = parsedMessage.isGroup,
                    conversationTitle = parsedMessage.conversationTitle,
                    timestamp = parsedMessage.timestamp,
                    packageName = parsedMessage.packageName,
                    pendingIntent = pendingIntent,
                    remoteInput = remoteInput
                )
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (e: Exception) {
                    // Ignore release errors
                }
            }
        }
    }

    private fun findReplyAction(notification: Notification): Pair<PendingIntent?, RemoteInput?> {
        // Strategy 1: Check standard platform notification.actions
        val standardActions = notification.actions
        if (standardActions != null) {
            val result = searchActionsForReply(standardActions)
            if (result != null) return result
        }

        // Strategy 2: Check androidx NotificationCompat (extracts compat & invisible actions)
        try {
            val count = androidx.core.app.NotificationCompat.getActionCount(notification)
            // Pass 1: Prioritize SEMANTIC_ACTION_REPLY
            for (i in 0 until count) {
                val compatAction = androidx.core.app.NotificationCompat.getAction(notification, i) ?: continue
                if (compatAction.semanticAction == androidx.core.app.NotificationCompat.Action.SEMANTIC_ACTION_REPLY) {
                    val pi = compatAction.actionIntent ?: continue
                    val remoteInputs = compatAction.remoteInputs ?: continue
                    for (remoteInput in remoteInputs) {
                        if (remoteInput.allowFreeFormInput && !remoteInput.isDataOnly) {
                            val platformInput = RemoteInput.Builder(remoteInput.resultKey).apply {
                                setLabel(remoteInput.label)
                                setChoices(remoteInput.choices)
                                setAllowFreeFormInput(remoteInput.allowFreeFormInput)
                                addExtras(remoteInput.extras)
                            }.build()
                            StructuredLogger.i("AutoReply", "[AutoReply] Found reply action via NotificationCompat (SEMANTIC_REPLY): '${compatAction.title}'")
                            return Pair(pi, platformInput)
                        }
                    }
                }
            }

            // Pass 2: Fallback to any action with valid RemoteInput
            for (i in 0 until count) {
                val compatAction = androidx.core.app.NotificationCompat.getAction(notification, i) ?: continue
                val pi = compatAction.actionIntent ?: continue
                val remoteInputs = compatAction.remoteInputs ?: continue
                for (remoteInput in remoteInputs) {
                    if (remoteInput.allowFreeFormInput && !remoteInput.isDataOnly) {
                        val platformInput = RemoteInput.Builder(remoteInput.resultKey).apply {
                            setLabel(remoteInput.label)
                            setChoices(remoteInput.choices)
                            setAllowFreeFormInput(remoteInput.allowFreeFormInput)
                            addExtras(remoteInput.extras)
                        }.build()
                        StructuredLogger.i("AutoReply", "[AutoReply] Found reply action via NotificationCompat: '${compatAction.title}'")
                        return Pair(pi, platformInput)
                    }
                }
            }
        } catch (e: Exception) {
            StructuredLogger.w("AutoReply", "[AutoReply] NotificationCompat action extraction error: ${e.message}")
        }

        // Strategy 3: WearableExtender (Platform)
        try {
            val wearable = Notification.WearableExtender(notification)
            val wearableActions = wearable.actions
            if (wearableActions.isNotEmpty()) {
                val result = searchActionsForReply(wearableActions.toTypedArray())
                if (result != null) return result
            }
        } catch (e: Exception) {
            // ignore
        }

        StructuredLogger.w("AutoReply", "[AutoReply] No reply action found in standard, compat, or wearable actions")
        return Pair(null, null)
    }

    private fun isValidRemoteInput(remoteInput: RemoteInput): Boolean {
        if (!remoteInput.allowFreeFormInput) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && remoteInput.isDataOnly) return false
        return true
    }

    private fun searchActionsForReply(actions: Array<Notification.Action>): Pair<PendingIntent, RemoteInput>? {
        // Pass 1: Prioritize SEMANTIC_ACTION_REPLY (API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            for (action in actions) {
                if (action.semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY) {
                    val pi = action.actionIntent ?: continue
                    val remoteInputs = action.remoteInputs ?: continue
                    for (remoteInput in remoteInputs) {
                        if (isValidRemoteInput(remoteInput)) {
                            StructuredLogger.i("AutoReply", "[AutoReply] Found reply action via SEMANTIC_ACTION_REPLY: '${action.title}', key='${remoteInput.resultKey}'")
                            return Pair(pi, remoteInput)
                        }
                    }
                }
            }
        }

        // Pass 2: Fallback to any action with valid freeform RemoteInput
        for (action in actions) {
            val pi = action.actionIntent ?: continue
            val remoteInputs = action.remoteInputs ?: continue
            for (remoteInput in remoteInputs) {
                if (isValidRemoteInput(remoteInput)) {
                    StructuredLogger.i("AutoReply", "[AutoReply] Found reply action: '${action.title}', key='${remoteInput.resultKey}'")
                    return Pair(pi, remoteInput)
                }
            }
        }
        return null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationServiceWatchdog.isConnected = true
        if (serviceJob.isCancelled) {
            serviceJob = SupervisorJob()
            serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)
        }
        StructuredLogger.i("WhatsAppNotificationListener", "[NLS] onListenerConnected")
        StructuredLogger.i("WhatsAppNotificationListener", "[NLS] Listener CONNECTED")
        StructuredLogger.i(
            "WhatsAppNotificationListener",
            "[NLS] Listener CONNECTED — device=${Build.MANUFACTURER} ${Build.MODEL}, " +
            "api=${Build.VERSION.SDK_INT}, fingerprint=${Build.FINGERPRINT.take(60)}"
        )
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationServiceWatchdog.isConnected = false
        StructuredLogger.w("WhatsAppNotificationListener", "[NLS] onListenerDisconnected")
        StructuredLogger.w(
            "WhatsAppNotificationListener",
            "[NLS] Listener DISCONNECTED (API ${Build.VERSION.SDK_INT}) on ${Build.MANUFACTURER}. Requesting rebind..."
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                StructuredLogger.i("WhatsAppNotificationListener", "[NLS] requestRebind requested")
                requestRebind(ComponentName(this, WhatsAppNotificationListener::class.java))
            } catch (e: Exception) {
                StructuredLogger.e("WhatsAppNotificationListener", "[NLS] requestRebind failed: ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        StructuredLogger.w("WhatsAppNotificationListener", "[NLS] onDestroy")
        NotificationServiceWatchdog.isConnected = false
        serviceJob.cancel()
        super.onDestroy()
    }
}
