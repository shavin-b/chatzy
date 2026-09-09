package com.orderflow.admin.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orderflow.admin.core.common.Constants
import com.orderflow.admin.core.designsystem.theme.BadgeActive
import com.orderflow.admin.core.designsystem.theme.BadgeExpired
import com.orderflow.admin.core.designsystem.theme.BadgeExpiring
import com.orderflow.admin.core.designsystem.theme.BadgeOffline
import com.orderflow.admin.core.designsystem.theme.BadgeSuspended
import com.orderflow.admin.core.designsystem.theme.BadgeUninstalled

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        Constants.STATUS_ACTIVE -> BadgeActive to Color.White
        Constants.STATUS_EXPIRING_SOON -> BadgeExpiring to Color.Black
        Constants.STATUS_EXPIRED -> BadgeExpired to Color.White
        Constants.STATUS_SUSPENDED -> BadgeSuspended to Color.White
        Constants.STATUS_OFFLINE -> BadgeOffline to Color.White
        Constants.STATUS_UNINSTALLED -> BadgeUninstalled to Color.White
        else -> BadgeOffline to Color.White
    }

    Box(
        modifier = modifier
            .background(bgColor.copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
