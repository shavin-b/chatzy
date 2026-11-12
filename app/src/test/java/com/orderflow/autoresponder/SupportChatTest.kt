package com.orderflow.autoresponder

import com.google.firebase.functions.FirebaseFunctionsException
import com.orderflow.autoresponder.presentation.help.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SupportChatTest {
    private class Consent(override var enabled: Boolean = false) : SupportConsent
    private fun functionsError(code: FirebaseFunctionsException.Code): FirebaseFunctionsException =
        FirebaseFunctionsException::class.java.getDeclaredConstructor(String::class.java,
            FirebaseFunctionsException.Code::class.java, Any::class.java).apply { isAccessible = true }
            .newInstance("Simulated support failure", code, null)

    @Test fun sinhalaAndSinglishFollowupsSendOnlyRecentSupportTurns() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val requests = mutableListOf<List<SupportTurn>>()
            val vm = HelpViewModel(SupportGateway { turns ->
                requests += turns
                HelpReply("Notification access බලන්න", actions = listOf(HelpAction.SERVICE_STATUS))
            }, Consent())
            vm.updateDraft("පිළිතුරු එන්නේ නැහැ")
            vm.send(); advanceUntilIdle()
            assertTrue(requests.isEmpty())
            vm.allowAi(); vm.send(); advanceUntilIdle()
            assertEquals("පිළිතුරු එන්නේ නැහැ", requests[0].single().text)
            // Merely receiving a button leaves the current screen and draft state alone.
            assertEquals(HelpAction.SERVICE_STATUS, vm.uiState.value.messages.last().reply?.actions?.single())
            vm.updateDraft("ow enabled"); vm.send(); advanceUntilIdle()
            assertEquals(listOf("user", "model", "user"), requests.last().map { it.role })
            vm.updateDraft("eka kohomada karanne?"); vm.send(); advanceUntilIdle()
            assertEquals("ow enabled", requests.last()[2].text)
            repeat(5) { vm.updateDraft("next step"); vm.send(); advanceUntilIdle() }
            assertEquals(7, requests.last().size)
            assertEquals("", vm.uiState.value.draft)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun repeatedFailuresAndRetriesUpdateOneMessageInPlace() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val failures = listOf(
                com.google.firebase.auth.FirebaseAuthException("ERROR_ADMIN_RESTRICTED_OPERATION", "Private authentication response") to "account creation is restricted",
                com.google.firebase.auth.FirebaseAuthException("ERROR_OPERATION_NOT_ALLOWED", "Private authentication response") to "Anonymous sign-in is disabled",
                com.google.firebase.auth.FirebaseAuthException("ERROR_CONFIGURATION_NOT_FOUND", "Private authentication response") to "Authentication is not configured",
                java.io.IOException("offline") to "Connection failed",
                java.net.SocketTimeoutException("timeout") to "timed out",
                functionsError(FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED) to "quota",
                functionsError(FirebaseFunctionsException.Code.DEADLINE_EXCEEDED) to "timed out",
                functionsError(FirebaseFunctionsException.Code.FAILED_PRECONDITION) to "not configured",
                functionsError(FirebaseFunctionsException.Code.NOT_FOUND) to "endpoint is not deployed",
                functionsError(FirebaseFunctionsException.Code.UNAVAILABLE) to "unavailable")
            for ((failure, expected) in failures) {
                val requests = mutableListOf<List<SupportTurn>>()
                val vm = HelpViewModel(SupportGateway {
                    requests += it
                    kotlinx.coroutines.delay(100)
                    if (requests.size <= 3) throw failure
                    HelpReply("Check Service status")
                }, Consent(true))
                vm.updateDraft("auto reply wada nahane"); vm.send(); vm.send()
                assertEquals("", vm.uiState.value.draft) // Accepted text lives on the stable message.
                val id = vm.uiState.value.messages.last().id
                assertEquals(HelpDelivery.SENDING, vm.uiState.value.messages.last().delivery)
                advanceUntilIdle()
                repeat(2) {
                    assertEquals(2, vm.uiState.value.messages.size) // Intro + one user, no error bubbles.
                    assertEquals(1, vm.uiState.value.messages.count { it.error != null })
                    assertTrue(vm.uiState.value.messages.last().error!!.contains(expected))
                    vm.retry(id); vm.retry(id)
                    assertNull(vm.uiState.value.messages.last().error)
                    advanceUntilIdle()
                }
                assertEquals(3, requests.size)
                assertEquals(HelpDelivery.FAILED, vm.uiState.value.messages.last().delivery)
                vm.updateDraft("next draft")
                vm.retry(id); advanceUntilIdle()
                assertEquals(4, requests.size)
                assertTrue(requests.all { it == requests.first() })
                assertEquals(3, vm.uiState.value.messages.size)
                assertEquals(1, vm.uiState.value.messages.count { it.fromUser })
                assertEquals(HelpDelivery.ANSWERED, vm.uiState.value.messages[1].delivery)
                assertNull(vm.uiState.value.messages[1].error)
                assertEquals(id, vm.uiState.value.messages[1].id)
                assertEquals("auto reply wada nahane", vm.uiState.value.messages[1].text)
                assertEquals("next draft", vm.uiState.value.draft)
                vm.retry(id); advanceUntilIdle()
                assertEquals(4, requests.size) // An answered message cannot be retried.
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun failedMessageRetriesItsOriginalContextAfterOtherConversation() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val requests = mutableListOf<List<SupportTurn>>()
            val vm = HelpViewModel(SupportGateway {
                requests += it
                if (requests.size == 1) throw java.io.IOException()
                HelpReply("Open Rules")
            }, Consent(true))
            vm.updateDraft("first question"); vm.send(); advanceUntilIdle()
            val firstId = vm.uiState.value.messages.last().id
            vm.updateDraft("another question"); vm.send(); advanceUntilIdle()
            vm.retry(firstId); advanceUntilIdle()
            assertEquals(requests.first(), requests.last())
            assertEquals(2, vm.uiState.value.messages.count { it.fromUser })
            assertEquals(2, vm.uiState.value.messages.count { it.reply != null })
        } finally { Dispatchers.resetMain() }
    }

    @Test fun unicodeContextFitsServerUtf8BudgetAndPreservesPairs() {
        val recent = (0..2).flatMap { listOf(SupportTurn("user", "\u0dc3\u0dd2".repeat(500)), SupportTurn("model", "\u0dc3\u0dd2".repeat(1000))) }
        val turns = SupportClient.boundedTurns(recent, "\u0dc3\u0dd2".repeat(500))
        assertTrue(turns.size < 7)
        assertEquals("user", turns.first().role)
        assertTrue(turns.size % 2 == 1)
        assertEquals("\u0dc3\u0dd2".repeat(500), turns.last().text)
    }

    @Test fun consentSurvivesReopeningAndPreservesExistingDraft() {
        val consent = Consent()
        val gateway = SupportGateway { error("Consent does not make a request") }
        val vm = HelpViewModel(gateway, consent)
        vm.updateDraft("auto reply weda naha")
        assertFalse(vm.uiState.value.canSend)
        vm.send()
        assertEquals(1, vm.uiState.value.messages.size)
        vm.allowAi()
        assertEquals("auto reply weda naha", vm.uiState.value.draft)
        assertTrue(vm.uiState.value.canSend)
        assertTrue(HelpViewModel(gateway, consent).uiState.value.consent)
    }

    @Test fun arrowAndKeyboardHandlerIgnoreBlankAndDuplicateSubmissions() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var calls = 0
            val vm = HelpViewModel(SupportGateway {
                calls++
                kotlinx.coroutines.delay(100)
                HelpReply("Check your rule")
            }, Consent(true))
            for (source in listOf("arrow", "keyboard")) {
                vm.updateDraft("  "); vm.send(); advanceUntilIdle()
                val before = calls
                assertFalse(vm.uiState.value.canSend)
                vm.updateDraft("auto reply weda naha")
                assertTrue(source, vm.uiState.value.canSend)
                vm.send(); vm.send(); vm.send()
                assertTrue(vm.uiState.value.busy)
                assertFalse(vm.uiState.value.canSend)
                advanceUntilIdle()
                assertEquals(source, before + 1, calls)
                assertEquals("", vm.uiState.value.draft)
            }
            assertEquals(2, vm.uiState.value.messages.count { it.fromUser })
        } finally { Dispatchers.resetMain() }
    }

    @Test fun onlyAllowlistedActionIdsBecomeButtons() {
        val reply = SupportClient.decode(mapOf("text" to "Open Rules", "actions" to listOf("javascript:bad", "rules", "unknown")))
        assertEquals(listOf(HelpAction.RULES), reply.actions)
        assertTrue(SupportClient.decode(mapOf("text" to "Help", "actions" to listOf("arbitrary_route"))).actions.isEmpty())
    }
}
