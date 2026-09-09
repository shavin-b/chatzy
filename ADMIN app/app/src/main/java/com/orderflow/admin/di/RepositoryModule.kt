package com.orderflow.admin.di

import com.orderflow.admin.data.repository.AuthRepositoryImpl
import com.orderflow.admin.data.repository.DeviceRepositoryImpl
import com.orderflow.admin.data.repository.LogRepositoryImpl
import com.orderflow.admin.data.repository.NotificationRepositoryImpl
import com.orderflow.admin.data.repository.SettingsRepositoryImpl
import com.orderflow.admin.data.repository.SubscriptionRepositoryImpl
import com.orderflow.admin.domain.repository.AuthRepository
import com.orderflow.admin.domain.repository.DeviceRepository
import com.orderflow.admin.domain.repository.LogRepository
import com.orderflow.admin.domain.repository.NotificationRepository
import com.orderflow.admin.domain.repository.SettingsRepository
import com.orderflow.admin.domain.repository.SubscriptionRepository
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindLogRepository(impl: LogRepositoryImpl): LogRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
