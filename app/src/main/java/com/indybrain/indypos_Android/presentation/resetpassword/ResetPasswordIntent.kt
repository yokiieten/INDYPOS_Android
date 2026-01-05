package com.indybrain.indypos_Android.presentation.resetpassword

/**
 * MVI Intent/Action for Reset Password screen
 * Represents user actions/events
 */
sealed class ResetPasswordIntent {
    data class UpdateNewPassword(val password: String) : ResetPasswordIntent()
    data class UpdateConfirmPassword(val password: String) : ResetPasswordIntent()
    data object ToggleNewPasswordVisibility : ResetPasswordIntent()
    data object ToggleConfirmPasswordVisibility : ResetPasswordIntent()
    data object Submit : ResetPasswordIntent()
    data object ClearError : ResetPasswordIntent()
}

