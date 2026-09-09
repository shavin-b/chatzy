package com.orderflow.admin.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.orderflow.admin.core.common.Constants
import com.orderflow.admin.data.model.DeviceDto
import com.orderflow.admin.data.model.LogDto
import com.orderflow.admin.data.model.NotificationDto
import com.orderflow.admin.data.model.SettingsDto
import com.orderflow.admin.data.model.SubscriptionDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val sampleDevices = listOf(
        DeviceDto(
            deviceId = "DEV-8821-X9",
            phoneModel = "Galaxy S23 Ultra",
            manufacturer = "Samsung",
            androidVersion = "Android 14 (API 34)",
            appVersion = "v2.4.1",
            userName = "Sarah Jenkins",
            businessName = "Apex Retail Mart",
            phoneNumber = "+1 555-0147",
            installationDate = System.currentTimeMillis() - (60 * 86400000L),
            activationDate = System.currentTimeMillis() - (58 * 86400000L),
            subscriptionStart = System.currentTimeMillis() - (58 * 86400000L),
            subscriptionEnd = System.currentTimeMillis() + (120 * 86400000L),
            daysRemaining = 120,
            status = Constants.STATUS_ACTIVE,
            lastSeen = System.currentTimeMillis() - 120000L, // 2 mins ago
            lastSync = System.currentTimeMillis() - 120000L,
            fcmToken = "sample_fcm_token_1",
            isOnline = true,
            generatedUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        ),
        DeviceDto(
            deviceId = "DEV-4092-A2",
            phoneModel = "Pixel 8 Pro",
            manufacturer = "Google",
            androidVersion = "Android 14 (API 34)",
            appVersion = "v2.4.1",
            userName = "Michael Chang",
            businessName = "Metro Logistics & Supply",
            phoneNumber = "+1 555-0188",
            installationDate = System.currentTimeMillis() - (25 * 86400000L),
            activationDate = System.currentTimeMillis() - (24 * 86400000L),
            subscriptionStart = System.currentTimeMillis() - (24 * 86400000L),
            subscriptionEnd = System.currentTimeMillis() + (4 * 86400000L), // 4 days remaining
            daysRemaining = 4,
            status = Constants.STATUS_EXPIRING_SOON,
            lastSeen = System.currentTimeMillis() - 300000L, // 5 mins ago
            lastSync = System.currentTimeMillis() - 300000L,
            fcmToken = "sample_fcm_token_2",
            isOnline = true,
            generatedUuid = "b2c3d4e5-f6a7-8901-bcde-f23456789012"
        ),
        DeviceDto(
            deviceId = "DEV-1093-K7",
            phoneModel = "Xiaomi 13 Pro",
            manufacturer = "Xiaomi",
            androidVersion = "Android 13 (API 33)",
            appVersion = "v2.3.0",
            userName = "David Ross",
            businessName = "Sunset Bistro & Cafe",
            phoneNumber = "+1 555-0922",
            installationDate = System.currentTimeMillis() - (90 * 86400000L),
            activationDate = System.currentTimeMillis() - (90 * 86400000L),
            subscriptionStart = System.currentTimeMillis() - (90 * 86400000L),
            subscriptionEnd = System.currentTimeMillis() - (2 * 86400000L), // Expired 2 days ago
            daysRemaining = 0,
            status = Constants.STATUS_EXPIRED,
            lastSeen = System.currentTimeMillis() - (36 * 3600000L), // 36 hours ago
            lastSync = System.currentTimeMillis() - (36 * 3600000L),
            fcmToken = "sample_fcm_token_3",
            isOnline = false,
            generatedUuid = "c3d4e5f6-a7b8-9012-cdef-345678901234"
        ),
        DeviceDto(
            deviceId = "DEV-7761-M4",
            phoneModel = "OnePlus 11",
            manufacturer = "OnePlus",
            androidVersion = "Android 14 (API 34)",
            appVersion = "v2.4.0",
            userName = "Elena Rostova",
            businessName = "Global Trade Hub",
            phoneNumber = "+1 555-0331",
            installationDate = System.currentTimeMillis() - (15 * 86400000L),
            activationDate = System.currentTimeMillis() - (15 * 86400000L),
            subscriptionStart = System.currentTimeMillis() - (15 * 86400000L),
            subscriptionEnd = System.currentTimeMillis() + (165 * 86400000L),
            daysRemaining = 165,
            status = Constants.STATUS_ACTIVE,
            lastSeen = System.currentTimeMillis() - 450000L,
            lastSync = System.currentTimeMillis() - 450000L,
            fcmToken = "sample_fcm_token_4",
            isOnline = true,
            generatedUuid = "d4e5f6a7-b8c9-0123-def4-456789012345"
        )
    )

    fun getDevicesStream(): Flow<List<DeviceDto>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_DEVICES)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    trySend(sampleDevices)
                } else {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(DeviceDto::class.java)?.copy(deviceId = doc.id)
                    }
                    trySend(if (list.isEmpty()) sampleDevices else list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getDeviceByIdStream(deviceId: String): Flow<DeviceDto?> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_DEVICES).document(deviceId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    val fallback = sampleDevices.find { it.deviceId == deviceId } ?: sampleDevices.first()
                    trySend(fallback)
                } else {
                    val dto = snapshot.toObject(DeviceDto::class.java)?.copy(deviceId = snapshot.id)
                    trySend(dto)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateDeviceSubscription(
        deviceId: String,
        newExpiry: Long,
        actionType: String,
        daysAdded: Int
    ) {
        try {
            val status = if (newExpiry <= System.currentTimeMillis()) Constants.STATUS_EXPIRED else Constants.STATUS_ACTIVE
            firestore.collection(Constants.COLLECTION_DEVICES).document(deviceId).update(
                mapOf(
                    "subscriptionEnd" to newExpiry,
                    "status" to status,
                    "lastSync" to System.currentTimeMillis()
                )
            ).await()
        } catch (_: Exception) {
            // Silently fallback if offline/demo
        }
    }

    suspend fun updateDeviceStatus(deviceId: String, status: String) {
        try {
            firestore.collection(Constants.COLLECTION_DEVICES).document(deviceId).update(
                mapOf(
                    "status" to status,
                    "lastSync" to System.currentTimeMillis()
                )
            ).await()
        } catch (_: Exception) {}
    }

    fun getSubscriptionsStream(): Flow<List<SubscriptionDto>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_SUBSCRIPTIONS)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { it.toObject(SubscriptionDto::class.java) }
                    trySend(list)
                } else {
                    trySend(
                        listOf(
                            SubscriptionDto(
                                subscriptionId = "sub_1",
                                deviceId = "DEV-8821-X9",
                                businessName = "Apex Retail Mart",
                                updatedBy = "Super Admin",
                                updatedAt = System.currentTimeMillis() - 3600000L,
                                oldExpiryDate = System.currentTimeMillis() + 90 * 86400000L,
                                newExpiryDate = System.currentTimeMillis() + 120 * 86400000L,
                                daysAdded = 30,
                                actionType = "Extend"
                            )
                        )
                    )
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun recordSubscription(subscription: SubscriptionDto) {
        try {
            firestore.collection(Constants.COLLECTION_SUBSCRIPTIONS)
                .document(subscription.subscriptionId)
                .set(subscription)
                .await()
        } catch (_: Exception) {}
    }

    fun getLogsStream(): Flow<List<LogDto>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_LOGS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { it.toObject(LogDto::class.java) }
                    trySend(list)
                } else {
                    trySend(
                        listOf(
                            LogDto(
                                logId = "log_1",
                                timestamp = System.currentTimeMillis() - 1200000L,
                                title = "Subscription Extended",
                                description = "Extended subscription for Apex Retail Mart by +30 Days.",
                                category = "Subscription",
                                performedBy = "Super Admin",
                                deviceId = "DEV-8821-X9"
                            ),
                            LogDto(
                                logId = "log_2",
                                timestamp = System.currentTimeMillis() - 7200000L,
                                title = "Device Activated",
                                description = "Metro Logistics & Supply activated app version v2.4.1.",
                                category = "Device Registered",
                                performedBy = "System",
                                deviceId = "DEV-4092-A2"
                            )
                        )
                    )
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addLog(log: LogDto) {
        try {
            firestore.collection(Constants.COLLECTION_LOGS)
                .document(log.logId)
                .set(log)
                .await()
        } catch (_: Exception) {}
    }

    fun getNotificationsStream(): Flow<List<NotificationDto>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { it.toObject(NotificationDto::class.java) }
                    trySend(list)
                } else {
                    trySend(
                        listOf(
                            NotificationDto(
                                notificationId = "notif_1",
                                title = "System Upgrade Notice",
                                body = "OrderFlow cloud infrastructure will undergo maintenance tonight at 02:00 UTC.",
                                type = "Maintenance Notice",
                                targetDeviceId = null,
                                sentAt = System.currentTimeMillis() - 86400000L,
                                sentBy = "Super Admin",
                                status = "Sent"
                            )
                        )
                    )
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendNotification(notification: NotificationDto) {
        try {
            firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
                .document(notification.notificationId)
                .set(notification)
                .await()
        } catch (_: Exception) {}
    }

    fun getSettingsStream(): Flow<SettingsDto> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_SETTINGS).document("global")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val dto = snapshot.toObject(SettingsDto::class.java)
                    trySend(dto ?: SettingsDto())
                } else {
                    trySend(SettingsDto())
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateSettings(settings: SettingsDto) {
        try {
            firestore.collection(Constants.COLLECTION_SETTINGS)
                .document("global")
                .set(settings)
                .await()
        } catch (_: Exception) {}
    }
}
