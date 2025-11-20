package com.indybrain.indypos_Android.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText

/**
 * Reusable TopAppBar component for shop name display
 * 
 * Best Practices:
 * 1. Keep composables stateless - pass data via parameters
 * 2. Use @Composable functions for reusable UI components
 * 3. Place shared components in core/ui/components/ for easy access
 * 4. Use Material3 TopAppBar for consistent behavior (scrolling, elevation, etc.)
 * 
 * Usage:
 * ```
 * Scaffold(
 *     topBar = {
 *         ShopTopAppBar(
 *             shopName = "My Shop",
 *             onEditClick = { /* handle edit */ }
 *         )
 *     }
 * ) { padding ->
 *     // Content
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopTopAppBar(
    shopName: String,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // ชื่อร้านอยู่ตรงกลาง
                Text(
                    text = shopName.ifBlank { 
                        stringResource(id = R.string.home_default_shop_name) 
                    },
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    ),
                    color = PrimaryText,
                    modifier = Modifier.align(Alignment.Center)
                )
                
                // ปุ่ม edit เป็นวงกลมสีขาวอยู่ขวา
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onEditClick),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.home_edit_shop_cd),
                            tint = SecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BaseBackground,
            titleContentColor = PrimaryText
        ),
        modifier = modifier
    )
}

