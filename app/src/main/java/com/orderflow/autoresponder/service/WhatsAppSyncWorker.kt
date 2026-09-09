package com.orderflow.autoresponder.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.orderflow.autoresponder.core.logger.StructuredLogger
import com.orderflow.autoresponder.domain.usecase.SyncMessagesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WhatsAppSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncMessagesUseCase: SyncMessagesUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        StructuredLogger.i("WhatsAppSyncWorker", "Executing scheduled WhatsApp sync background job")
        return when (syncMessagesUseCase()) {
            is com.orderflow.autoresponder.core.util.Result.Success -> Result.success()
            else -> Result.retry()
        }
    }
}
