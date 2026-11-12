package com.orderflow.autoresponder.presentation.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orderflow.autoresponder.R
import com.orderflow.autoresponder.presentation.theme.BrandAccent
import com.orderflow.autoresponder.presentation.theme.BrandGreen
import com.orderflow.autoresponder.presentation.theme.BrandTextPrimary
import com.orderflow.autoresponder.presentation.theme.BrandTextSecondary

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
    val state by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F0F0F) // Matching app dark background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            Image(
                painter = painterResource(id = R.drawable.app_icon_provided),
                contentDescription = "Chatzy logo",
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome to Chatzy",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = BrandTextPrimary
            )

            Text(
                text = "Set up your information to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Business Name Input
            OutlinedTextField(
                value = state.businessName,
                onValueChange = { viewModel.onBusinessNameChange(it) },
                label = { Text("Business Name") },
                placeholder = { Text("e.g. Shavin Electronics") },
                modifier = Modifier.fillMaxWidth(),
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                singleLine = true,
                colors = onboardingTextFieldColors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // WhatsApp Number Input
            OutlinedTextField(
                value = state.whatsappNumber,
                onValueChange = { viewModel.onWhatsAppNumberChange(it) },
                label = { Text("WhatsApp Business Number") },
                placeholder = { Text("e.g. +94771234567") },
                modifier = Modifier.fillMaxWidth(),
                isError = state.phoneError != null,
                supportingText = state.phoneError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = onboardingTextFieldColors()
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { viewModel.submitProfile() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                shape = RoundedCornerShape(12.dp),
                enabled = !state.isSubmitting
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                } else {
                    Text(
                        text = "Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun onboardingTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandAccent,
    unfocusedBorderColor = BrandTextSecondary.copy(alpha = 0.5f),
    focusedLabelColor = BrandAccent,
    unfocusedLabelColor = BrandTextSecondary,
    focusedTextColor = BrandTextPrimary,
    unfocusedTextColor = BrandTextPrimary,
    cursorColor = BrandAccent
)
