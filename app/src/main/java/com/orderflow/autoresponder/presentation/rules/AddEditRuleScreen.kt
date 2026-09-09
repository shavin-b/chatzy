package com.orderflow.autoresponder.presentation.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orderflow.autoresponder.domain.model.AutoReplyMessage
import com.orderflow.autoresponder.domain.model.AutoReplyRule
import com.orderflow.autoresponder.domain.model.MatchOption
import com.orderflow.autoresponder.presentation.components.AppTopBar
import com.orderflow.autoresponder.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRuleScreen(
    editingRule: AutoReplyRule? = null,
    viewModel: RulesViewModel,
    onNavigateBack: () -> Unit
) {
    var ruleName by remember { mutableStateOf(editingRule?.ruleName ?: "") }
    var keywordsCsv by remember { mutableStateOf(editingRule?.keywordsCsv ?: "") }
    var selectedMatchOption by remember { mutableStateOf(editingRule?.matchOption ?: MatchOption.EXACT) }
    
    var initialDelayText by remember { mutableStateOf(editingRule?.initialDelaySeconds?.toString() ?: "0") }
    var replyDelayText by remember { mutableStateOf(editingRule?.delaySeconds?.toString() ?: "0") }
    
    var priorityText by remember { mutableStateOf(editingRule?.priority?.toString() ?: "0") }
    var caseSensitive by remember { mutableStateOf(editingRule?.caseSensitive ?: false) }
    var enabledForGroups by remember { mutableStateOf(editingRule?.enabledForGroups ?: false) }

    // Use a snapshots state list for messages
    val messagesList = remember { 
        val initialList = editingRule?.messages?.sortedBy { it.position } ?: listOf(AutoReplyMessage(message = ""))
        initialList.toMutableStateList() 
    }

    var expandedDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (editingRule == null) "Create New Rule" else "Edit Rule",
                navigationIcon = Icons.Default.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        containerColor = BrandDarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Rule Configuration", color = BrandAccent, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = ruleName,
                onValueChange = { ruleName = it },
                label = { Text("Rule Name") },
                placeholder = { Text("e.g. Welcome Message") },
                modifier = Modifier.fillMaxWidth(),
                colors = customTextFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = expandedDropdown,
                onExpandedChange = { expandedDropdown = !expandedDropdown }
            ) {
                OutlinedTextField(
                    value = selectedMatchOption.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Matching Option") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = customTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    MatchOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option.name) },
                            onClick = {
                                selectedMatchOption = option
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = keywordsCsv,
                onValueChange = { keywordsCsv = it },
                label = { Text("Keywords (comma separated)") },
                placeholder = { Text("e.g. price, cost, rate") },
                modifier = Modifier.fillMaxWidth(),
                colors = customTextFieldColors()
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Automated Replies", color = BrandAccent, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            messagesList.forEachIndexed { index, replyMsg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = replyMsg.message,
                            onValueChange = { 
                                messagesList[index] = messagesList[index].copy(message = it)
                            },
                            label = { Text("Message ${index + 1}") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            colors = customTextFieldColors()
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = {
                            if (index > 0) {
                                val item = messagesList.removeAt(index)
                                messagesList.add(index - 1, item)
                            }
                        }) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = BrandTextSecondary)
                        }
                        IconButton(onClick = {
                            if (messagesList.size > 1) {
                                messagesList.removeAt(index)
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusFailed)
                        }
                        IconButton(onClick = {
                            if (index < messagesList.size - 1) {
                                val item = messagesList.removeAt(index)
                                messagesList.add(index + 1, item)
                            }
                        }) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = BrandTextSecondary)
                        }
                    }
                }
            }

            Button(
                onClick = { messagesList.add(AutoReplyMessage(message = "")) },
                modifier = Modifier.padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandAccent.copy(alpha = 0.2f), contentColor = BrandAccent)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Another Reply")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Sequence Timing", color = BrandAccent, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = initialDelayText,
                    onValueChange = { initialDelayText = it },
                    label = { Text("Initial Delay (s)") },
                    modifier = Modifier.weight(1f),
                    colors = customTextFieldColors()
                )
                OutlinedTextField(
                    value = replyDelayText,
                    onValueChange = { replyDelayText = it },
                    label = { Text("Between Replies (s)") },
                    modifier = Modifier.weight(1f),
                    colors = customTextFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Advanced Options", color = BrandAccent, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = priorityText,
                onValueChange = { priorityText = it },
                label = { Text("Priority (Higher matches first)") },
                modifier = Modifier.fillMaxWidth(),
                colors = customTextFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = caseSensitive,
                    onCheckedChange = { caseSensitive = it },
                    colors = CheckboxDefaults.colors(checkedColor = BrandGreen)
                )
                Text(text = "Case Sensitive Matching", color = BrandTextPrimary)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = enabledForGroups,
                    onCheckedChange = { enabledForGroups = it },
                    colors = CheckboxDefaults.colors(checkedColor = BrandGreen)
                )
                Text(text = "Enable for Group Chats", color = BrandTextPrimary)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val initialDelay = initialDelayText.toIntOrNull() ?: 0
                    val replyDelay = replyDelayText.toIntOrNull() ?: 0
                    val priority = priorityText.toIntOrNull() ?: 0
                    
                    // Final position update
                    val finalMessages = messagesList.mapIndexed { i, msg -> 
                        msg.copy(position = i) 
                    }.filter { it.message.isNotBlank() }

                    if (finalMessages.isNotEmpty()) {
                        viewModel.saveRule(
                            id = editingRule?.id ?: 0L,
                            ruleName = ruleName,
                            keywordsCsv = keywordsCsv,
                            messages = finalMessages,
                            matchOption = selectedMatchOption,
                            initialDelaySeconds = initialDelay,
                            delaySeconds = replyDelay,
                            priority = priority,
                            caseSensitive = caseSensitive,
                            enabledForGroups = enabledForGroups
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (editingRule == null) "Save Rule" else "Update Rule",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandAccent,
    unfocusedBorderColor = BrandTextSecondary,
    focusedLabelColor = BrandAccent,
    unfocusedLabelColor = BrandTextSecondary,
    focusedTextColor = BrandTextPrimary,
    unfocusedTextColor = BrandTextPrimary,
    cursorColor = BrandAccent
)
