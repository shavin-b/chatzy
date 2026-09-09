package com.orderflow.autoresponder.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.orderflow.autoresponder.core.security.SecureStorage
import com.orderflow.autoresponder.presentation.components.SuspendedScreen
import com.orderflow.autoresponder.presentation.navigation.NavGraph
import com.orderflow.autoresponder.presentation.onboarding.OnboardingScreen
import com.orderflow.autoresponder.presentation.onboarding.OnboardingViewModel
import com.orderflow.autoresponder.presentation.theme.OrderFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var secureStorage: SecureStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            OrderFlowTheme {
                val isBlocked by secureStorage.applicationBlockFlow().collectAsState(initial = secureStorage.isEffectivelyBlocked())
                val isProfileComplete by secureStorage.profileCompletionFlow().collectAsState(initial = secureStorage.isProfileCompleted())
                
                val context = androidx.compose.ui.platform.LocalContext.current

                // CRITICAL LOGGING
                val adminLocked = secureStorage.isAdminLocked()
                val subStatus = secureStorage.getSubscriptionStatus()
                val subActive = secureStorage.isSubscriptionActive()
                
                android.util.Log.e("OrderFlow", "[MainActivity] STATE CHECK: adminLocked=$adminLocked, subStatus='$subStatus', subActive=$subActive, isBlocked=$isBlocked, isProfileComplete=$isProfileComplete")

                LaunchedEffect(isBlocked) {
                    if (isBlocked) {
                        android.widget.Toast.makeText(context, "Access Restricted by Administrator", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
                
                android.util.Log.d("OrderFlow", "[MainActivity] Navigation State: isBlocked=$isBlocked, isProfileComplete=$isProfileComplete")

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        isBlocked -> {
                            SuspendedScreen(
                                isAdminLocked = secureStorage.isAdminLocked(),
                                subscriptionStatus = secureStorage.getSubscriptionStatus(),
                                deviceId = secureStorage.getDeviceId() ?: "Unknown",
                                lockReason = secureStorage.getLockReason()
                            )
                        }
                        !isProfileComplete -> {
                            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
                            OnboardingScreen(viewModel = onboardingViewModel)
                        }
                        else -> {
                            NavGraph()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.orderflow.autoresponder.service.NotificationServiceWatchdog.ensureListenerBound(this)
    }
}
