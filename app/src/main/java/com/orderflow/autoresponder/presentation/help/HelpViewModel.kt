package com.orderflow.autoresponder.presentation.help

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import javax.inject.Inject

interface SupportConsent {
    var enabled: Boolean
}

class DeviceSupportConsent @Inject constructor(@ApplicationContext context: Context) : SupportConsent {
    private val preferences = context.getSharedPreferences("chatzy_support_consent", Context.MODE_PRIVATE)
    override var enabled: Boolean
        get() = preferences.getBoolean("gemini_support_v1", false)
        set(value) { preferences.edit().putBoolean("gemini_support_v1", value).apply() }
}

enum class HelpDelivery { SENDING, FAILED, ANSWERED }
data class HelpMessage(
    val text: String,
    val fromUser: Boolean = false,
    val reply: HelpReply? = null,
    val id: String = java.util.UUID.randomUUID().toString(),
    val delivery: HelpDelivery? = null,
    val error: String? = null
)
data class HelpUiState(val draft: String = "", val consent: Boolean = false,
    val busy: Boolean = false, val messages: List<HelpMessage> = listOf(
        HelpMessage("Ask about Chatzy in Sinhala, Singlish or English.")
    )) {
    val canSend: Boolean get() = consent && !busy && draft.isNotBlank()
}

@HiltViewModel
class HelpViewModel @Inject constructor(private val client: SupportGateway, private val consent: SupportConsent) : ViewModel() {
    private val mutableState = MutableStateFlow(HelpUiState(consent = consent.enabled))
    val uiState = mutableState.asStateFlow()
    // Snapshot only successful AI context at acceptance; retries use exactly that context.
    private val contexts = mutableMapOf<String, List<SupportTurn>>()
    private var recent = emptyList<SupportTurn>()

    fun allowAi() {
        consent.enabled = true
        mutableState.value = mutableState.value.copy(consent = true)
    }

    fun updateDraft(value: String) {
        mutableState.value = mutableState.value.copy(draft = value.take(1000))
    }

    // Arrow and keyboard Send share this synchronous acceptance/in-flight guard.
    fun send() {
        val state = mutableState.value
        if (!state.canSend) return
        val message = HelpMessage(state.draft.trim(), fromUser = true, delivery = HelpDelivery.SENDING)
        contexts[message.id] = SupportClient.boundedTurns(recent, message.text)
        mutableState.value = state.copy(draft = "", busy = true, messages = state.messages + message)
        submit(message)
    }

    fun retry(id: String) {
        val state = mutableState.value
        if (!state.consent || state.busy) return
        val message = state.messages.firstOrNull { it.id == id && it.fromUser && it.delivery == HelpDelivery.FAILED } ?: return
        mutableState.value = state.copy(busy = true, messages = state.messages.map {
            if (it.id == id) it.copy(delivery = HelpDelivery.SENDING, error = null) else it
        })
        submit(message)
    }

    private fun submit(message: HelpMessage) {
        viewModelScope.launch {
            try {
                val turns = contexts.getValue(message.id)
                val reply = client.ask(turns)
                recent = (turns + SupportTurn("model", reply.text)).takeLast(6)
                contexts.remove(message.id)
                mutableState.value = mutableState.value.copy(busy = false,
                    messages = mutableState.value.messages.map {
                        if (it.id == message.id) it.copy(delivery = HelpDelivery.ANSWERED, error = null) else it
                    } + HelpMessage(reply.text, reply = reply))
            } catch (error: Exception) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                mutableState.value = mutableState.value.copy(busy = false,
                    messages = mutableState.value.messages.map {
                        if (it.id == message.id) it.copy(delivery = HelpDelivery.FAILED,
                            error = SupportClient.failureMessage(error)) else it
                    })
            }
        }
    }
}

@Module
@InstallIn(ViewModelComponent::class)
object HelpModule {
    @Provides fun provideSupportGateway(client: SupportClient): SupportGateway = client
    @Provides fun provideSupportConsent(consent: DeviceSupportConsent): SupportConsent = consent
}
