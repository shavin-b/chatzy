package com.orderflow.admin.domain.repository

import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun getSubscriptionsStream(): Flow<List<Subscription>>
    suspend fun recordSubscriptionUpdate(subscription: Subscription): Resource<Unit>
}
