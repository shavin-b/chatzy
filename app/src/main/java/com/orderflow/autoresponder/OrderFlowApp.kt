package com.orderflow.autoresponder

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.orderflow.autoresponder.core.logger.StructuredLogger
import com.orderflow.autoresponder.device.service.DeviceManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class OrderFlowApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var deviceManager: DeviceManager

    override fun onCreate() {
        super.onCreate()
        StructuredLogger.i("OrderFlowApp", "Application initialized successfully")
        deviceManager.initialize()

        // Global watchdog: ensure listener is bound whenever the app process starts
        // (including system rebind, FCM message receipt, or Worker execution).
        com.orderflow.autoresponder.service.NotificationServiceWatchdog.ensureListenerBound(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
