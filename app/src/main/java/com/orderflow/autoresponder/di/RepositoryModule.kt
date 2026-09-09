package com.orderflow.autoresponder.di

import com.orderflow.autoresponder.data.repository.CustomerRepositoryImpl
import com.orderflow.autoresponder.data.repository.MessageLogRepositoryImpl
import com.orderflow.autoresponder.data.repository.ProcessedNotificationRepositoryImpl
import com.orderflow.autoresponder.data.repository.RuleRepositoryImpl
import com.orderflow.autoresponder.data.repository.WhatsAppCloudApiRepositoryImpl
import com.orderflow.autoresponder.domain.repository.CustomerRepository
import com.orderflow.autoresponder.domain.repository.MessageLogRepository
import com.orderflow.autoresponder.domain.repository.ProcessedNotificationRepository
import com.orderflow.autoresponder.domain.repository.RuleRepository
import com.orderflow.autoresponder.domain.repository.WhatsAppCloudApiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRuleRepository(
        impl: RuleRepositoryImpl
    ): RuleRepository

    @Binds
    @Singleton
    abstract fun bindMessageLogRepository(
        impl: MessageLogRepositoryImpl
    ): MessageLogRepository

    @Binds
    @Singleton
    abstract fun bindCustomerRepository(
        impl: CustomerRepositoryImpl
    ): CustomerRepository

    @Binds
    @Singleton
    abstract fun bindWhatsAppCloudApiRepository(
        impl: WhatsAppCloudApiRepositoryImpl
    ): WhatsAppCloudApiRepository

    @Binds
    @Singleton
    abstract fun bindProcessedNotificationRepository(
        impl: ProcessedNotificationRepositoryImpl
    ): ProcessedNotificationRepository
}
