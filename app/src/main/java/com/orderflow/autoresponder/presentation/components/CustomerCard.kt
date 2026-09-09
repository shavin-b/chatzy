package com.orderflow.autoresponder.presentation.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orderflow.autoresponder.core.util.DateUtils
import com.orderflow.autoresponder.domain.model.Customer
import com.orderflow.autoresponder.presentation.theme.BrandAccent
import com.orderflow.autoresponder.presentation.theme.BrandCardDark
import com.orderflow.autoresponder.presentation.theme.BrandTextPrimary
import com.orderflow.autoresponder.presentation.theme.BrandTextSecondary

@Composable
fun CustomerCard(customer: Customer) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = BrandCardDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name.ifBlank { customer.phone },
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = customer.phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandAccent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Last seen: ${DateUtils.formatDateTime(customer.lastInteractionTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandTextSecondary,
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${customer.totalMessages}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BrandAccent
                )
                Text(
                    text = "Messages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
