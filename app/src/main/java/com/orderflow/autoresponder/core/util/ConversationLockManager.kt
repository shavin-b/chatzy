package com.orderflow.autoresponder.core.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationLockManager @Inject constructor() {
    private val locks = ConcurrentHashMap<String, Mutex>()

    /**
     * Executes the block with a mutex lock for the specific conversation ID.
     */
    suspend fun <T> withLock(conversationId: String, block: suspend () -> T): T {
        val mutex = locks.getOrPut(conversationId) { Mutex() }
        return mutex.withLock {
            block()
        }
    }
}
