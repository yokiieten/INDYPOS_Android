package com.indybrain.indypos_Android.presentation.register

/**
 * MVI Intent/Action for Register screen
 * Represents user actions/events
 */
sealed class RegisterIntent {
    data class UpdateUsername(val username: String) : RegisterIntent()
    data class UpdatePassword(val password: String) : RegisterIntent()
    data class UpdateConfirmPassword(val confirmPassword: String) : RegisterIntent()
    data class UpdateFirstName(val firstName: String) : RegisterIntent()
    data class UpdateLastName(val lastName: String) : RegisterIntent()
    data class UpdateEmail(val email: String) : RegisterIntent()
    data class UpdatePhone(val phone: String) : RegisterIntent()
    data class UpdateGender(val gender: String) : RegisterIntent()
    data class UpdateBirthDate(val birthDate: String) : RegisterIntent()
    data class UpdateShopName(val shopName: String) : RegisterIntent()
    data class UpdateShopDescription(val shopDescription: String) : RegisterIntent()
    data class UpdateTermsAccepted(val accepted: Boolean) : RegisterIntent()
    data class UpdatePrivacyAccepted(val accepted: Boolean) : RegisterIntent()
    data object TogglePasswordVisibility : RegisterIntent()
    data object ToggleConfirmPasswordVisibility : RegisterIntent()
    data object Register : RegisterIntent()
    data object ClearError : RegisterIntent()
}

