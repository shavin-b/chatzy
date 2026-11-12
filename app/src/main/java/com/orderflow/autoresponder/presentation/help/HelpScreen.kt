package com.orderflow.autoresponder.presentation.help

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.orderflow.autoresponder.presentation.components.AppTopBar
import com.orderflow.autoresponder.presentation.theme.BrandGreen

@Composable
fun HelpScreen(viewModel: HelpViewModel, onNavigateBack: () -> Unit, onAction: (HelpAction) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.busy) {
        listState.animateScrollToItem(state.messages.lastIndex)
    }
    Scaffold(
        // IME padding owns the bottom inset; don't reserve the navigation bar twice.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar("Chatzy Help", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack)
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
            .navigationBarsPadding().imePadding()) {
            if (!state.consent) {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.Center) {
                    Text("AI support chat", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(16.dp))
                    Text("Your support question and up to 6 recent AI chat turns are sent to Google Gemini with verified Chatzy help content. WhatsApp messages, customer records, tokens and logs are not attached. Please do not paste private information.")
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = viewModel::allowAi, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = Color.Black)) {
                        Text("Enable AI chat")
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState,
                    contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.messages, key = { it.id }) { message ->
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = if (message.fromUser) Alignment.End else Alignment.Start) {
                            Surface(shape = RoundedCornerShape(16.dp),
                                color = if (message.fromUser) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.widthIn(max = 560.dp)) {
                                Text(message.text, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge)
                            }
                            if (message.delivery == HelpDelivery.SENDING) {
                                Text("Sending…", Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                            }
                            message.error?.let { error ->
                                Text(error, Modifier.widthIn(max = 560.dp).padding(top = 8.dp),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                OutlinedButton(onClick = { viewModel.retry(message.id) }, enabled = !state.busy) {
                                    Text("Retry")
                                }
                            }
                            message.reply?.actions?.forEach { action ->
                                OutlinedButton(onClick = { onAction(action) }) { Text(action.label) }
                            }
                        }
                    }
                    if (state.busy) item { Text("Asking Chatzy Help…", style = MaterialTheme.typography.bodySmall) }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = state.draft, onValueChange = viewModel::updateDraft,
                        modifier = Modifier.weight(1f), label = { Text("Ask Chatzy Help") }, maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { viewModel.send() }))
                    FilledIconButton(onClick = viewModel::send, enabled = state.canSend,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = BrandGreen, contentColor = Color.Black),
                        modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}
