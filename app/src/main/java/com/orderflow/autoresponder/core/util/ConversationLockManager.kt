package com.orderflow.autoresponder.core.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationLockManager @Inject constructor() {

    private class RefCountedLock {
        val mutex = Mutex()
        var refCount = 0
    }

    private val locks = ConcurrentHashMap<String, RefCountedLock>()
    private val globalLock = Mutex()

    /**
     * Executes the block with a race-safe reference-counted mutex lock for the specific conversation ID.
     * Prevents concurrent message processing for the same conversation while safely removing idle locks
     * from memory when no coroutines hold or await them.
     */
    suspend fun <T> withLock(conversationId: String, block: suspend () -> T): T {
        val refLock = globalLock.withLock {
            locks.getOrPut(conversationId) { RefCountedLock() }.also {
                it.refCount++
            }
        }

        try {
            return refLock.mutex.withLock {
                block()
            }
        } finally {
            globalLock.withLock {
                refLock.refCount--
                if (refLock.refCount == 0) {
                    locks.remove(conversationId, refLock)
                }
            }
        }
    }

    /**
     * Returns the current number of active conversation lock entries in memory.
     * Useful for diagnostics and testing.
     */
    fun getActiveLockCount(): Int = locks.size
}
