package com.indybrain.indypos_Android.presentation.forgotpassword

/**
 * MVI Intent/Action for Forgot Password screen
 * Represents user actions/events
 */
sealed class ForgotPasswordIntent {
    data class UpdateEmail(val email: String) : ForgotPasswordIntent()
    data object Submit : ForgotPasswordIntent()
    data object ClearError : ForgotPasswordIntent()
}

