package com.orderflow.autoresponder.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orderflow.autoresponder.core.util.DateUtils
import com.orderflow.autoresponder.domain.model.Customer

@Composable
fun CustomerCard(customer: Customer) {
    val colors = MaterialTheme.colorScheme
    val name = customer.name.ifBlank { customer.phone }
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(50.dp).clip(CircleShape).background(colors.secondaryContainer),
                contentAlignment = Alignment.Center) {
                Text(name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleLarge, color = colors.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(customer.phone, style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant)
                Text(DateUtils.formatDateTime(customer.lastInteractionTime),
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            Surface(shape = CircleShape, color = colors.primary) {
                Text("${customer.totalMessages}", Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = colors.onPrimary, style = MaterialTheme.typography.labelMedium)
            }
        }
        HorizontalDivider(Modifier.padding(start = 64.dp), color = colors.outlineVariant)
    }
}
