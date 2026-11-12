package com.orderflow.autoresponder

import com.orderflow.autoresponder.core.util.ConversationLockManager
import com.orderflow.autoresponder.core.util.MessageFingerprintGenerator
import com.orderflow.autoresponder.domain.util.VariableResolver
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ReliabilityAuditTest {

    @Test
    fun conversationLockManager_serializesSameConversation() = runBlocking {
        val lockManager = ConversationLockManager()
        val executionOrder = mutableListOf<Int>()

        val job1 = launch {
            lockManager.withLock("conv_101") {
                delay(100)
                executionOrder.add(1)
            }
        }

        val job2 = launch {
            delay(10) // Ensure job1 starts first
            lockManager.withLock("conv_101") {
                executionOrder.add(2)
            }
        }

        job1.join()
        job2.join()

        assertEquals("Same conversation tasks must execute sequentially in order", listOf(1, 2), executionOrder)
        assertEquals("Active lock map must return to 0 when no coroutines hold locks", 0, lockManager.getActiveLockCount())
    }

    @Test
    fun conversationLockManager_allowsConcurrentDifferentConversations() = runBlocking {
        val lockManager = ConversationLockManager()
        val finishedFirst = AtomicBoolean(false)

        val job1 = launch {
            lockManager.withLock("conv_A") {
                delay(150)
                finishedFirst.set(true)
            }
        }

        val job2 = async {
            delay(10)
            lockManager.withLock("conv_B") {
                // Should run concurrently without waiting for conv_A to finish
                finishedFirst.get()
            }
        }

        val conv2FinishedBeforeConv1 = job2.await()
        job1.join()

        assertFalse("conv_B must execute concurrently without waiting for conv_A delay", conv2FinishedBeforeConv1)
        assertEquals("Active lock map must clean up all locks when done", 0, lockManager.getActiveLockCount())
    }

    @Test
    fun coroutineScope_supervisorJob_isolatesFailures() = runBlocking {
        val uncaughtExceptionCount = AtomicInteger(0)
        val handler = CoroutineExceptionHandler { _, _ ->
            uncaughtExceptionCount.incrementAndGet()
        }

        val supervisorJob = SupervisorJob()
        val testScope = CoroutineScope(supervisorJob + Dispatchers.Default + handler)

        val failedTask = testScope.launch {
            throw RuntimeException("Simulated processing error in message 1")
        }

        val successfulTaskCompleted = AtomicBoolean(false)
        val secondTask = testScope.launch {
            delay(50)
            successfulTaskCompleted.set(true)
        }

        failedTask.join()
        secondTask.join()

        assertTrue("Failure in message 1 must NOT cancel execution of subsequent message 2", successfulTaskCompleted.get())
        assertEquals("Uncaught exception must be reported to CoroutineExceptionHandler", 1, uncaughtExceptionCount.get())
    }

    @Test
    fun variableResolver_handlesNullsAndTemplatesSafely() {
        val resolvedName = VariableResolver.resolve(
            template = "Hello {sender}, welcome!",
            senderName = "John Doe",
            incomingMessage = "Hi",
            chatName = null
        )
        assertEquals("Hello John Doe, welcome!", resolvedName)

        val resolvedFallback = VariableResolver.resolve(
            template = "Hello {sender}, chat: {chat}",
            senderName = "John",
            incomingMessage = "Test",
            chatName = null
        )
        assertEquals("Hello John, chat: John", resolvedFallback)
    }

    @Test
    fun messageFingerprint_normalizesVariationsConsistently() {
        val text1 = "  Hello  World!!! 😊  "
        val text2 = "hello world!!! 😊"

        val fp1 = MessageFingerprintGenerator.normalize(text1)
        val fp2 = MessageFingerprintGenerator.normalize(text2)

        assertEquals("Whitespace collapsing and lowercasing must normalize text consistently for deduplication", fp2, fp1)
    }
}
