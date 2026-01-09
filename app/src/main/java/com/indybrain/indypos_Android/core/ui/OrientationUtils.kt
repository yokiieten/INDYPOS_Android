package com.indybrain.indypos_Android.core.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Utility functions for detecting device orientation and screen dimensions
 */

/**
 * Returns true if the device is in landscape orientation
 */
@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

/**
 * Returns true if the device is in portrait orientation
 */
@Composable
fun isPortrait(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_PORTRAIT
}

/**
 * Returns the current screen width in Dp
 */
@Composable
fun getScreenWidthDp(): Dp {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp.dp
}

/**
 * Returns the current screen height in Dp
 */
@Composable
fun getScreenHeightDp(): Dp {
    val configuration = LocalConfiguration.current
    return configuration.screenHeightDp.dp
}

/**
 * Returns true if the device is a tablet
 * 
 * Criteria:
 * - Smallest width >= 600dp (standard tablet threshold)
 * - Or screen width >= 600dp in current orientation
 */
@Composable
fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.smallestScreenWidthDp >= 600
}

/**
 * Returns true if the device is a tablet in landscape mode
 * This is useful for showing tablet-optimized layouts only in landscape
 */
@Composable
fun isTabletLandscape(): Boolean {
    return isTablet() && isLandscape()
}

/**
 * Returns true if the device is a mobile phone (not tablet) in landscape mode
 */
@Composable
fun isMobileLandscape(): Boolean {
    return !isTablet() && isLandscape()
}

/**
 * Returns the appropriate number of columns for product grid based on device and orientation
 * - Mobile portrait: 2 columns
 * - Mobile landscape: 4-6 columns (depends on screen width)
 * - Tablet portrait: 2-3 columns (depends on screen width)
 * - Tablet landscape: 3 columns (in split screen with cart)
 */
@Composable
fun getProductGridColumns(): Int {
    val screenWidth = getScreenWidthDp()
    val isTabletDevice = isTablet()
    val isLandscapeMode = isLandscape()
    
    return when {
        // Tablet landscape: 3 columns (in split screen)
        isTabletDevice && isLandscapeMode -> 3
        
        // Mobile landscape: 4-6 columns based on width
        !isTabletDevice && isLandscapeMode -> {
            when {
                screenWidth >= 900.dp -> 6  // Extra large phones (7"+)
                screenWidth >= 800.dp -> 5  // Large phones (6.5-7")
                screenWidth >= 700.dp -> 5  // Large phones (6-6.5")
                screenWidth >= 600.dp -> 4  // Medium phones (5.5-6")
                else -> 4                   // Small phones (< 5.5")
            }
        }
        
        // Tablet portrait: 2-3 columns based on width
        isTabletDevice && !isLandscapeMode -> {
            when {
                screenWidth >= 700.dp -> 3  // Large tablets
                else -> 2                   // Medium tablets
            }
        }
        
        // Mobile portrait: 2 columns
        else -> 2
    }
}

/**
 * Returns the appropriate split ratio for landscape mode based on screen width
 * - Large tablets (>= 10"): 0.65f (65% products, 35% cart)
 * - Medium tablets (7-9"): 0.60f (60% products, 40% cart)
 */
@Composable
fun getLandscapeSplitRatio(): Float {
    val screenWidth = getScreenWidthDp()
    return when {
        screenWidth >= 900.dp -> 0.65f // Large tablets
        else -> 0.60f // Medium tablets
    }
}
