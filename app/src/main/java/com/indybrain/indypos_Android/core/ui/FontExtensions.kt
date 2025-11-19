package com.indybrain.indypos_Android.core.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.indybrain.indypos_Android.R

/**
 * Font size enum matching Swift FontSize
 */
enum class FontSize(val value: TextUnit) {
    Smallest(10.sp),      // 10
    Smaller(12.sp),      // 12
    Small(14.sp),        // 14
    Medium(16.sp),       // 16
    Large(18.sp),        // 18
    Larger(20.sp),       // 20
    Largest(22.sp),      // 22
    Largester(24.sp)     // 24
}

/**
 * Font style enum matching Swift FontStyle
 */
enum class AppFontStyle(val fontResource: Int) {
    Light(R.font.anuphan_light),
    Regular(R.font.anuphan_regular),
    Medium(R.font.anuphan_medium),
    SemiBold(R.font.anuphan_semibold),
    Bold(R.font.anuphan_bold)
}

/**
 * Font utility object for creating custom fonts
 * Similar to Swift UIFont.mainFont()
 */
object FontUtils {
    /**
     * Get font family for a given font style
     * Similar to Swift UIFont.mainFont(style:size:)
     */
    fun mainFont(style: AppFontStyle = AppFontStyle.Regular, size: FontSize = FontSize.Medium): androidx.compose.ui.text.TextStyle {
        return androidx.compose.ui.text.TextStyle(
            fontFamily = FontFamily(Font(style.fontResource)),
            fontSize = size.value
        )
    }
    
    /**
     * Get font family with custom size
     * Similar to Swift UIFont.mainFontCustomSize(style:customSize:)
     */
    fun mainFontCustomSize(style: AppFontStyle = AppFontStyle.Regular, customSize: TextUnit = 16.sp): androidx.compose.ui.text.TextStyle {
        return androidx.compose.ui.text.TextStyle(
            fontFamily = FontFamily(Font(style.fontResource)),
            fontSize = customSize
        )
    }
    
    /**
     * Get font family for a given font style
     */
    fun getFontFamily(style: AppFontStyle = AppFontStyle.Regular): FontFamily {
        return FontFamily(Font(style.fontResource))
    }
}

