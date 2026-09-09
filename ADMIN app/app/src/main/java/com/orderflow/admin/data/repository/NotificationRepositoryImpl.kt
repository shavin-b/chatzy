package com.orderflow.admin.data.repository

import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.data.model.toDomain
import com.orderflow.admin.data.model.toDto
import com.orderflow.admin.data.remote.FirestoreDataSource
import com.orderflow.admin.domain.model.NotificationItem
import com.orderflow.admin.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val firestoreSource: FirestoreDataSource
) : NotificationRepository {

    override fun getNotificationsStream(): Flow<List<NotificationItem>> {
        return firestoreSource.getNotificationsStream().map { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun sendPushNotification(notification: NotificationItem): Resource<Unit> {
        firestoreSource.sendNotification(notification.toDto())
        return Resource.Success(Unit)
    }
}
