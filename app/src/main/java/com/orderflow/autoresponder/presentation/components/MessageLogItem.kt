package com.orderflow.autoresponder.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orderflow.autoresponder.core.util.DateUtils
import com.orderflow.autoresponder.domain.model.MessageLog
import com.orderflow.autoresponder.domain.model.MessageStatus
import com.orderflow.autoresponder.presentation.theme.BrandAccent
import com.orderflow.autoresponder.presentation.theme.BrandCardDark
import com.orderflow.autoresponder.presentation.theme.BrandTextPrimary
import com.orderflow.autoresponder.presentation.theme.BrandTextSecondary
import com.orderflow.autoresponder.presentation.theme.StatusFailed
import com.orderflow.autoresponder.presentation.theme.StatusSuccess
import com.orderflow.autoresponder.presentation.theme.StatusWarning

@Composable
fun MessageLogItem(log: MessageLog) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = BrandCardDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.senderName.ifBlank { log.senderPhone },
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandTextPrimary,
                    modifier = Modifier.weight(1f)
                )

                val (statusText, statusColor) = when (log.status) {
                    MessageStatus.RECEIVED -> Pair("RECEIVED", BrandTextPrimary)
                    MessageStatus.SENT -> Pair("SENT", StatusSuccess)
                    MessageStatus.FAILED -> Pair("FAILED", StatusFailed)
                    MessageStatus.QUEUED -> Pair("QUEUED", StatusWarning)
                    MessageStatus.IGNORED -> Pair("IGNORED", BrandTextSecondary)
                    MessageStatus.DUPLICATE -> Pair("DUPLICATE", BrandTextSecondary)
                    MessageStatus.SKIPPED -> Pair("SKIPPED", StatusWarning)
                }

                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "In: \"${log.incomingMessage}\"",
                style = MaterialTheme.typography.bodyLarge,
                color = BrandTextPrimary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Out: \"${log.replyMessage}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandAccent,
                fontSize = 13.sp
            )

            if (!log.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Error: ${log.errorMessage}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusFailed,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = DateUtils.formatDateTime(log.timestamp),
                style = MaterialTheme.typography.bodyMedium,
                color = BrandTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
