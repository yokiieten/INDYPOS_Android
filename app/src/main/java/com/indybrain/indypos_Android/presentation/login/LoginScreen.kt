package com.indybrain.indypos_Android.presentation.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.ui.theme.*

/**
 * Login Screen implementing MVI pattern
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit = {}
) {
    // Collect UI state
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Collect MVI state
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    // Handle success state
    LaunchedEffect(state) {
        if (state is LoginState.Success) {
            onLoginSuccess()
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BaseBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "Login",
                style = MaterialTheme.typography.headlineLarge,
                color = PrimaryText,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Email field
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { email ->
                    viewModel.handleIntent(LoginIntent.UpdateEmail(email))
                },
                label = { Text("Email", color = SecondaryText) },
                placeholder = { Text("Enter your email", color = PlaceholderText) },
                singleLine = true,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PrimaryText,
                    unfocusedTextColor = PrimaryText,
                    focusedBorderColor = PrimaryButton,
                    unfocusedBorderColor = SecondaryButton
                )
            )
            
            // Password field
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { password ->
                    viewModel.handleIntent(LoginIntent.UpdatePassword(password))
                },
                label = { Text("Password", color = SecondaryText) },
                placeholder = { Text("Enter your password", color = PlaceholderText) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PrimaryText,
                    unfocusedTextColor = PrimaryText,
                    focusedBorderColor = PrimaryButton,
                    unfocusedBorderColor = SecondaryButton
                )
            )
            
            // Error message
            uiState.errorMessage?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    color = RedFailure,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
            
            // Login button
            Button(
                onClick = {
                    viewModel.handleIntent(LoginIntent.Login)
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryButton,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = SecondaryButton
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Login",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            
            // Success indicator
            if (state is LoginState.Success) {
                Text(
                    text = "Login successful!",
                    color = GreenComplete,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

