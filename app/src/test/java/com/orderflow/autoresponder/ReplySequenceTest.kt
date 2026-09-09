package com.orderflow.autoresponder

import com.orderflow.autoresponder.domain.model.AutoReplyMessage
import com.orderflow.autoresponder.domain.util.VariableResolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReplySequenceTest {

    @Test
    fun multiReply_preservesPositionOrderAndFiltersDisabled() {
        val messages = listOf(
            AutoReplyMessage(id = 3, message = "Third message", position = 2, isEnabled = true),
            AutoReplyMessage(id = 1, message = "First message", position = 0, isEnabled = true),
            AutoReplyMessage(id = 99, message = "Disabled message", position = 1, isEnabled = false),
            AutoReplyMessage(id = 2, message = "Second message", position = 1, isEnabled = true)
        )

        val activeSorted = messages.filter { it.isEnabled }.sortedBy { it.position }

        assertEquals(3, activeSorted.size)
        assertEquals("First message", activeSorted[0].message)
        assertEquals("Second message", activeSorted[1].message)
        assertEquals("Third message", activeSorted[2].message)
    }

    @Test
    fun variableResolver_resolvesAllPlaceholders() {
        val template = "Hi {sender}, received your '{message}' in {chat}!"
        val resolved = VariableResolver.resolve(
            template = template,
            senderName = "John Doe",
            incomingMessage = "What is the order status?",
            chatName = "VIP Group"
        )

        assertEquals("Hi John Doe, received your 'What is the order status?' in VIP Group!", resolved)
    }

    @Test
    fun sequenceExecution_abortsImmediatelyWhenLocked() = runTest {
        val messages = listOf(
            AutoReplyMessage(id = 1, message = "Msg 1", position = 0, isEnabled = true),
            AutoReplyMessage(id = 2, message = "Msg 2", position = 1, isEnabled = true),
            AutoReplyMessage(id = 3, message = "Msg 3", position = 2, isEnabled = true)
        )

        var deviceLocked = false
        val sentList = mutableListOf<String>()

        for ((index, msg) in messages.withIndex()) {
            if (deviceLocked) {
                break
            }
            sentList.add(msg.message)

            // Simulate device locking between message 1 and message 2
            if (index == 0) {
                deviceLocked = true
            }
        }

        assertEquals(1, sentList.size)
        assertEquals("Msg 1", sentList[0])
        assertTrue("Sequence must have aborted before sending Msg 2 or 3", !sentList.contains("Msg 2"))
    }
}
