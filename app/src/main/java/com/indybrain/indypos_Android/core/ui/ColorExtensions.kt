package com.indybrain.indypos_Android.core.ui

import androidx.compose.ui.graphics.Color

/**
 * Extension functions for Color utilities similar to Swift UIColor
 */

/**
 * Convert hex string to Color
 * Supports formats: "#RRGGBB" or "RRGGBB"
 * Usage: Color.fromHex("#FFFFFF")
 */
fun Color.Companion.fromHex(hex: String): Color {
    var hexSanitized = hex.trim()
    hexSanitized = hexSanitized.replace("#", "")
    
    if (hexSanitized.length != 6) {
        throw IllegalArgumentException("Invalid hex color format: $hex")
    }
    
    val rgb = hexSanitized.toLong(16)
    
    val red = ((rgb and 0xFF0000) shr 16) / 255.0f
    val green = ((rgb and 0x00FF00) shr 8) / 255.0f
    val blue = (rgb and 0x0000FF) / 255.0f
    
    return Color(red, green, blue, 1.0f)
}

/**
 * Extension function to check if two colors are equal (with tolerance)
 */
fun Color.isEqual(other: Color, tolerance: Float = 0.01f): Boolean {
    return kotlin.math.abs(this.red - other.red) < tolerance &&
            kotlin.math.abs(this.green - other.green) < tolerance &&
            kotlin.math.abs(this.blue - other.blue) < tolerance &&
            kotlin.math.abs(this.alpha - other.alpha) < tolerance
}

/**
 * Extension function to convert Color to hex string
 */
fun Color.toHexString(): String {
    val r = (this.red * 255).toInt()
    val g = (this.green * 255).toInt()
    val b = (this.blue * 255).toInt()
    return String.format("#%02X%02X%02X", r, g, b)
}

/**
 * App Colors (Light Mode Only)
 * Matching Swift UIColor constants
 */
object AppColors {
    // Background
    val BaseBackground = Color.fromHex("#FFFFFF")
    
    // Buttons
    val PrimaryButton = Color.fromHex("#5EA6ED")
    val SecondaryButton = Color.fromHex("#DEE8F2")
    
    // Text
    val PrimaryText = Color.fromHex("#0A141F")
    val SecondaryText = Color.fromHex("#3B70A6")
    val PlaceholderText = Color.fromHex("#C7C7CD")
    
    // Accents
    val AccentSuccess = Color.fromHex("#A8E6CF")
    val AccentWarning = Color.fromHex("#FFBE98")
    val GreenComplete = Color.fromHex("#088738")
    val RedFailure = Color.fromHex("#E83808")
}

