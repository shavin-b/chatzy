package com.orderflow.admin.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.orderflow.admin.core.common.DateUtils
import com.orderflow.admin.core.designsystem.components.AdminTopAppBar
import com.orderflow.admin.core.designsystem.components.GlassCard
import com.orderflow.admin.core.designsystem.theme.AccentBlue
import com.orderflow.admin.core.designsystem.theme.AccentIndigo
import com.orderflow.admin.presentation.devicedetails.DetailRow

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.adminUser.collectAsState()

    Scaffold(
        topBar = { AdminTopAppBar(title = "Administrator Profile", onBackClick = onBackClick) }
    ) { padding ->
        if (user != null) {
            val u = user!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(AccentIndigo.copy(alpha = 0.2f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(56.dp))
                }

                Text(
                    text = u.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .background(AccentIndigo, shape = CircleShape)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(text = u.role, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Account Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow("Admin ID", u.adminId.ifEmpty { "ADMIN-001" })
                    DetailRow("Email", u.email)
                    DetailRow("Role", u.role)
                    DetailRow("Last Security Login", DateUtils.formatTimestamp(u.lastLogin))
                }
            }
        }
    }
}
