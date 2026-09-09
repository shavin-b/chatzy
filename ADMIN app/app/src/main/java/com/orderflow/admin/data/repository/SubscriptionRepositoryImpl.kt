package com.orderflow.admin.data.repository

import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.data.model.toDomain
import com.orderflow.admin.data.model.toDto
import com.orderflow.admin.data.remote.FirestoreDataSource
import com.orderflow.admin.domain.model.Subscription
import com.orderflow.admin.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val firestoreSource: FirestoreDataSource
) : SubscriptionRepository {

    override fun getSubscriptionsStream(): Flow<List<Subscription>> {
        return firestoreSource.getSubscriptionsStream().map { dtos ->
            dtos.map { it.toDomain() }
        }
    }

    override suspend fun recordSubscriptionUpdate(subscription: Subscription): Resource<Unit> {
        firestoreSource.recordSubscription(subscription.toDto())
        return Resource.Success(Unit)
    }
}
