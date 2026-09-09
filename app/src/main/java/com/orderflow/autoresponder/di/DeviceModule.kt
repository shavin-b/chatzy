package com.orderflow.autoresponder.di

import com.orderflow.autoresponder.device.repository.DeviceRepository
import com.orderflow.autoresponder.device.repository.FirebaseDeviceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceModule {

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(
        impl: FirebaseDeviceRepository
    ): DeviceRepository
}
