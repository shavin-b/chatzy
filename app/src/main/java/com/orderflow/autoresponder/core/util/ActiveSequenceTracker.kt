package com.orderflow.autoresponder.core.util

import com.orderflow.autoresponder.core.logger.StructuredLogger
import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap

/**
 * Global tracker for active, in-flight auto-reply coroutine jobs.
 * Ensures that if a device is locked by the admin or subscription expires mid-sequence,
 * running delay jobs are cancelled immediately instead of continuing to dispatch messages.
 */
object ActiveSequenceTracker {
    private val activeJobs = ConcurrentHashMap<String, Job>()

    fun register(sequenceId: String, job: Job) {
        activeJobs[sequenceId] = job
        job.invokeOnCompletion {
            activeJobs.remove(sequenceId)
        }
    }

    fun cancel(sequenceId: String, reason: String = "Explicit cancellation") {
        activeJobs.remove(sequenceId)?.let { job ->
            StructuredLogger.w("ActiveSequenceTracker", "Cancelling sequence $sequenceId: $reason")
            job.cancel()
        }
    }

    fun cancelAll(reason: String = "Device locked or automation suspended") {
        val count = activeJobs.size
        if (count > 0) {
            StructuredLogger.w("ActiveSequenceTracker", "Cancelling all $count active sequences: $reason")
            for ((_, job) in activeJobs) {
                job.cancel()
            }
            activeJobs.clear()
        }
    }

    fun getActiveCount(): Int = activeJobs.size

    fun clearForTesting() {
        activeJobs.clear()
    }
}
