package com.indybrain.indypos_Android.presentation.discount

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.core.ui.isLandscape
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscountScreen(
    subtotal: Double,
    onBackClick: () -> Unit = {},
    onDiscountSelected: (DiscountModel) -> Unit = {},
    onCancel: () -> Unit = {},
    viewModel: DiscountViewModel = hiltViewModel()
) {
    val discount by viewModel.discount.collectAsStateWithLifecycle()
    val isValid by viewModel.isValid.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val isLandscapeMode = isLandscape()
    
    var valueText by remember { mutableStateOf("") }
    
    LaunchedEffect(discount) {
        if (discount.value > 0) {
            valueText = discount.value.toInt().toString()
        }
    }
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ส่วนลด",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Large
                        ),
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "กลับ",
                            tint = PrimaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BaseBackground,
                    titleContentColor = PrimaryText
                )
            )
        }
    ) { padding ->
        if (isLandscapeMode) {
            // Landscape Layout - Split screen
            DiscountScreenLandscape(
                subtotal = subtotal,
                discount = discount,
                isValid = isValid,
                errorMessage = errorMessage,
                valueText = valueText,
                quickOptions = viewModel.getQuickDiscountOptions(),
                onValueTextChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() }
                    valueText = filtered
                    val doubleValue = filtered.toDoubleOrNull() ?: 0.0
                    viewModel.setDiscountValue(doubleValue)
                },
                onDiscountTypeChange = { type ->
                    viewModel.setDiscountType(type)
                    valueText = ""
                    keyboardController?.hide()
                },
                onQuickOptionClick = { option ->
                    viewModel.setDiscountType(option.type)
                    viewModel.setDiscountValue(option.value)
                    valueText = option.value.toInt().toString()
                    keyboardController?.hide()
                },
                onCancel = onCancel,
                onApply = {
                    onDiscountSelected(viewModel.getDiscountModel())
                },
                modifier = Modifier.padding(padding)
            )
        } else {
            // Portrait Layout - Original vertical layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Title and Subtotal
                item {
                    Column {
                        Text(
                            text = "เพิ่มส่วนลด",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Largester
                            ),
                            color = PrimaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ยอดรวม ${formatCurrency(subtotal)}",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Medium
                            ),
                            color = SecondaryText
                        )
                    }
                }
                
                // Discount Type Selection
                item {
                    Column {
                        Text(
                            text = "ประเภทส่วนลด",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Large
                            ),
                            color = PrimaryText,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            DiscountType.values().forEachIndexed { index, type ->
                                val isSelected = discount.type == type
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clickable {
                                            viewModel.setDiscountType(type)
                                            valueText = ""
                                            keyboardController?.hide()
                                        },
                                    shape = when {
                                        index == 0 -> RoundedCornerShape(
                                            topStart = 8.dp,
                                            bottomStart = 8.dp,
                                            topEnd = 0.dp,
                                            bottomEnd = 0.dp
                                        )
                                        index == DiscountType.values().size - 1 -> RoundedCornerShape(
                                            topStart = 0.dp,
                                            bottomStart = 0.dp,
                                            topEnd = 8.dp,
                                            bottomEnd = 8.dp
                                        )
                                        else -> RoundedCornerShape(0.dp)
                                    },
                                    color = if (isSelected) PrimaryButton else Color(0xFFF5F5F5),
                                    border = if (!isSelected && index > 0) {
                                        androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                                    } else null
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = type.title,
                                            style = FontUtils.mainFont(
                                                style = if (isSelected) AppFontStyle.SemiBold else AppFontStyle.Medium,
                                                size = FontSize.Medium
                                            ),
                                            color = if (isSelected) Color.White else Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Value Input Section
                item {
                    Column {
                        Text(
                            text = "จำนวนส่วนลด",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Large
                            ),
                            color = PrimaryText,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 2.dp,
                                    color = if (errorMessage != null) Color(0xFFFF5252) else Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = valueText,
                                    onValueChange = { newValue ->
                                        val filtered = newValue.filter { it.isDigit() }
                                        valueText = filtered
                                        val doubleValue = filtered.toDoubleOrNull() ?: 0.0
                                        viewModel.setDiscountValue(doubleValue)
                                    },
                                    modifier = Modifier.weight(1f),
                                    textStyle = FontUtils.mainFont(
                                        style = AppFontStyle.SemiBold,
                                        size = FontSize.Large
                                    ).copy(textAlign = TextAlign.Center),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        if (valueText.isEmpty()) {
                                            Text(
                                                text = discount.type.placeholder,
                                                style = FontUtils.mainFont(
                                                    style = AppFontStyle.SemiBold,
                                                    size = FontSize.Large
                                                ),
                                                color = PlaceholderText,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                
                                Text(
                                    text = discount.type.symbol,
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Bold,
                                        size = FontSize.Large
                                    ),
                                    color = PrimaryButton
                                )
                            }
                        }
                        
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Medium,
                                    size = FontSize.Small
                                ),
                                color = Color(0xFFFF5252)
                            )
                        }
                    }
                }
                
                // Quick Options Section
                item {
                    Column {
                        Text(
                            text = "ตัวเลือกด่วน",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Large
                            ),
                            color = PrimaryText,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        val quickOptions = viewModel.getQuickDiscountOptions()
                        
                        // Display in 2 columns without scrolling
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            quickOptions.chunked(2).forEach { rowOptions ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowOptions.forEach { option ->
                                        QuickDiscountOption(
                                            discount = option,
                                            isSelected = discount.type == option.type && discount.value == option.value,
                                            onClick = {
                                                viewModel.setDiscountType(option.type)
                                                viewModel.setDiscountValue(option.value)
                                                valueText = option.value.toInt().toString()
                                                keyboardController?.hide()
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    // Fill empty space if odd number of items
                                    if (rowOptions.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Preview Section
                item {
                    val discountAmount = calculateDiscountAmount(subtotal, discount)
                    val finalPrice = subtotal - discountAmount
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "ตัวอย่างการคำนวณ",
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Bold,
                                    size = FontSize.Medium
                                ),
                                color = PrimaryText
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ราคาเดิม",
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Medium,
                                        size = FontSize.Medium
                                    ),
                                    color = PrimaryButton
                                )
                                Text(
                                    text = formatCurrency(subtotal),
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Medium,
                                        size = FontSize.Medium
                                    ),
                                    color = PrimaryButton
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ส่วนลด",
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Medium,
                                        size = FontSize.Medium
                                    ),
                                    color = Color(0xFFFF5252)
                                )
                                Text(
                                    text = "-${formatCurrency(discountAmount)} (${discount.previewText})",
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Medium,
                                        size = FontSize.Medium
                                    ),
                                    color = Color(0xFFFF5252)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ราคาสุทธิ",
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Bold,
                                        size = FontSize.Large
                                    ),
                                    color = PrimaryButton
                                )
                                Text(
                                    text = formatCurrency(finalPrice),
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Bold,
                                        size = FontSize.Large
                                    ),
                                    color = PrimaryButton
                                )
                            }
                        }
                    }
                }
            }
            
            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        containerColor = Color(0xFFE0E0E0)
                    )
                ) {
                    Text(
                        text = "ยกเลิก",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.SemiBold,
                            size = FontSize.Large
                        ),
                        color = SecondaryText
                    )
                }
                
                TextButton(
                    onClick = {
                        onDiscountSelected(viewModel.getDiscountModel())
                    },
                    enabled = isValid && discount.value > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        containerColor = PrimaryButton,
                        disabledContainerColor = PrimaryButton.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = "ใช้ส่วนลด",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.SemiBold,
                            size = FontSize.Large
                        ),
                        color = Color.White
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun QuickDiscountOption(
    discount: DiscountModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PrimaryButton else Color.White,
        border = if (!isSelected) {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
        } else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = discount.displayText,
                style = FontUtils.mainFont(
                    style = AppFontStyle.SemiBold,
                    size = FontSize.Medium
                ),
                color = if (isSelected) Color.White else PrimaryText,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return "฿${formatter.format(value)}"
}

private fun calculateDiscountAmount(subtotal: Double, discount: DiscountModel): Double {
    return when (discount.type) {
        DiscountType.PERCENTAGE -> (subtotal * discount.value / 100.0).coerceAtMost(subtotal)
        DiscountType.FIXED_AMOUNT -> discount.value.coerceAtMost(subtotal)
    }
}

