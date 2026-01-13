package com.indybrain.indypos_Android.presentation.splash

import android.Manifest
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PrimaryButton

/**
 * Splash Screen - First screen shown when app launches
 * Checks authentication status and navigates accordingly
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onSplashReady: () -> Unit = {},
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Request notification permission for Android 13+ (API 33+)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }
    
    // Request notification permission on first launch
    LaunchedEffect(Unit) {
        onSplashReady()
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionState?.launchPermissionRequest()
        }
    }

    // Navigate based on auth status
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            if (uiState.isLoggedIn) {
                onNavigateToHome()
            } else {
                onNavigateToLogin()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BaseBackground),
        contentAlignment = Alignment.Center
    ) {
        // App Logo - Show larger to fill screen better
        Image(
            painter = painterResource(id = R.drawable.logo_appstore),
            contentDescription = null,
            modifier = Modifier.size(200.dp)
        )

        // Loading indicator at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            CircularProgressIndicator(
                color = PrimaryButton,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

