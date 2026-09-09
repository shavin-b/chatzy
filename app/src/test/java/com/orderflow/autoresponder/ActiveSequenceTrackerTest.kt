package com.orderflow.autoresponder

import com.orderflow.autoresponder.core.util.ActiveSequenceTracker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ActiveSequenceTrackerTest {

    @Before
    fun setup() {
        ActiveSequenceTracker.clearForTesting()
    }

    @Test
    fun registerAndCount() {
        val job1 = Job()
        val job2 = Job()

        ActiveSequenceTracker.register("seq_1", job1)
        ActiveSequenceTracker.register("seq_2", job2)

        assertEquals(2, ActiveSequenceTracker.getActiveCount())

        job1.cancel()
        job2.cancel()
    }

    @Test
    fun cancelSingleSequence() {
        val job = Job()
        ActiveSequenceTracker.register("seq_target", job)

        assertFalse(job.isCancelled)
        ActiveSequenceTracker.cancel("seq_target", "Test cancel")

        assertTrue("Job should be cancelled after cancel()", job.isCancelled)
        assertEquals(0, ActiveSequenceTracker.getActiveCount())
    }

    @Test
    fun cancelAllSequencesOnLock() {
        val job1 = Job()
        val job2 = Job()
        val job3 = Job()

        ActiveSequenceTracker.register("seq_1", job1)
        ActiveSequenceTracker.register("seq_2", job2)
        ActiveSequenceTracker.register("seq_3", job3)

        assertEquals(3, ActiveSequenceTracker.getActiveCount())

        ActiveSequenceTracker.cancelAll("Device locked by admin")

        assertTrue("Job 1 must be cancelled", job1.isCancelled)
        assertTrue("Job 2 must be cancelled", job2.isCancelled)
        assertTrue("Job 3 must be cancelled", job3.isCancelled)
        assertEquals(0, ActiveSequenceTracker.getActiveCount())
    }

    @Test
    fun jobCompletionCleansUpTracker() {
        val deferred = CompletableDeferred<Unit>()
        ActiveSequenceTracker.register("seq_complete", deferred)

        assertEquals(1, ActiveSequenceTracker.getActiveCount())

        deferred.complete(Unit)

        assertEquals(0, ActiveSequenceTracker.getActiveCount())
    }
}
