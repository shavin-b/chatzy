package com.orderflow.autoresponder.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SuspendedScreen(
    isAdminLocked: Boolean,
    subscriptionStatus: String,
    deviceId: String = "Unknown",
    lockReason: String = "NONE"
) {
    // Intercept back button to keep user on this screen
    BackHandler(enabled = true) {
        // Do nothing, effectively blocking the back button
    }

    val isExpired = subscriptionStatus == "EXPIRED" || subscriptionStatus == "SUSPENDED"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Darker background to look more "locked"
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isAdminLocked) Icons.Default.Lock else Icons.Default.Warning,
            contentDescription = "Locked",
            modifier = Modifier.size(80.dp),
            tint = Color.Red
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isAdminLocked) {
            // Manual Admin Lock
            LockMessage(
                english = "Your application has been locked by the administrator.",
                sinhala = "පරිපාලක විසින් ඔබගේ යෙදුම අගුළු දමා ඇත."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Reason: $lockReason",
                color = Color.Gray,
                fontSize = 14.sp
            )
        } else if (isExpired) {
            // Subscription Expiry
            LockMessage(
                english = "Your subscription period has expired. Please subscribe again to continue using this service.",
                sinhala = "ඔබගේ subscription කාලය අවසන් වී ඇත. නැවත subscribe කර මෙම සේවාව ලබා ගන්න."
            )
        } else {
            // Generic fallback
            LockMessage(
                english = "Your access is restricted. Please contact support.",
                sinhala = "ඔබගේ ප්‍රවේශය සීමා කර ඇත. කරුණාකර සහාය අමතන්න."
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
        
        // Help the user find their ID for the website
        Text(
            text = "Device ID: $deviceId",
            color = Color.DarkGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LockMessage(english: String, sinhala: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Locked / සීමා කර ඇත",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Red,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = english,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = sinhala,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
    }
}
