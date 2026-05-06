package com.indybrain.indypos_Android.presentation.cashpayment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryButton
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashPaymentScreen(
    totalAmount: Double,
    subtotal: Double,
    discount: Double,
    onBackClick: () -> Unit,
    onPaymentComplete: (Double) -> Unit,
    viewModel: CashPaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showErrorDialog by remember { mutableStateOf<String?>(null) }
    
    // Initialize view model with amounts
    LaunchedEffect(totalAmount, subtotal, discount) {
        viewModel.totalAmount = totalAmount
        viewModel.subtotal = subtotal
        viewModel.discount = discount
    }
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ชำระเงินสด",
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
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
        
        if (isLandscape) {
            // Landscape layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left side: Amount info and input
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Total amount section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ราคาที่ต้องจ่าย",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Medium
                            ),
                            color = PrimaryText
                        )
                        
                        Text(
                            text = formatNumberWithCommas(totalAmount),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Largest
                            ),
                            color = PrimaryText
                        )
                    }
                    
                    // Received amount input field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                            .background(Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.enteredAmount.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onClearClick() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "ลบ",
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(24.dp))
                            }
                            
                            Text(
                                text = if (uiState.enteredAmount.isEmpty()) {
                                    "กรุณากรอกตัวเลข"
                                } else {
                                    formatDisplayAmount(uiState.enteredAmount)
                                },
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Bold,
                                    size = FontSize.Largest
                                ),
                                color = if (uiState.enteredAmount.isEmpty()) PlaceholderText else PrimaryText,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f, fill = true))
                    
                    // Confirm button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .clickable(
                                enabled = !uiState.isProcessingOrder && uiState.receivedAmount >= totalAmount - 0.01,
                                onClick = {
                                    viewModel.onConfirmClick(
                                        onSuccess = { change ->
                                            onPaymentComplete(change)
                                        },
                                        onError = { error ->
                                            showErrorDialog = error
                                        }
                                    )
                                }
                            ),
                        color = if (uiState.isProcessingOrder || uiState.receivedAmount < totalAmount - 0.01) {
                            Color(0xFFE0E0E0)
                        } else {
                            PrimaryButton
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ตกลง",
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.SemiBold,
                                    size = FontSize.Medium
                                ),
                                color = if (uiState.isProcessingOrder || uiState.receivedAmount < totalAmount - 0.01) {
                                    SecondaryText
                                } else {
                                    Color.White
                                }
                            )
                        }
                    }
                }
                
                // Right side: Keypad
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Keypad(
                        onButtonClick = { button ->
                            viewModel.onKeypadButtonClick(button)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            // Portrait layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                // Total amount section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ราคาที่ต้องจ่าย",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText
                    )
                    
                    Text(
                        text = formatNumberWithCommas(totalAmount),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Largest
                        ),
                        color = PrimaryText
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Received amount input field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                        .background(Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.enteredAmount.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.onClearClick() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "ลบ",
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(24.dp))
                        }
                        
                        Text(
                            text = if (uiState.enteredAmount.isEmpty()) {
                                "กรุณากรอกตัวเลข"
                            } else {
                                formatDisplayAmount(uiState.enteredAmount)
                            },
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Largest
                            ),
                            color = if (uiState.enteredAmount.isEmpty()) PlaceholderText else PrimaryText,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(30.dp))
                
                // Keypad
                Keypad(
                    onButtonClick = { button ->
                        viewModel.onKeypadButtonClick(button)
                    },
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.height(30.dp))
                
                // Confirm button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .clickable(
                            enabled = !uiState.isProcessingOrder && uiState.receivedAmount >= totalAmount - 0.01,
                            onClick = {
                                viewModel.onConfirmClick(
                                    onSuccess = { change ->
                                        onPaymentComplete(change)
                                    },
                                    onError = { error ->
                                        showErrorDialog = error
                                    }
                                )
                            }
                        ),
                    color = if (uiState.isProcessingOrder || uiState.receivedAmount < totalAmount - 0.01) {
                        Color(0xFFE0E0E0)
                    } else {
                        PrimaryButton
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ตกลง",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.SemiBold,
                                size = FontSize.Medium
                            ),
                            color = if (uiState.isProcessingOrder || uiState.receivedAmount < totalAmount - 0.01) {
                                SecondaryText
                            } else {
                                Color.White
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
    
    // Error dialog
    if (showErrorDialog != null) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = null },
            title = {
                Text(
                    text = stringResource(R.string.dialog_error_title),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
            },
            text = {
                Text(
                    text = showErrorDialog ?: "",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
            },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = null }) {
                    Text(
                        text = stringResource(R.string.dialog_button_ok),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = PrimaryButton
                    )
                }
            }
        )
    }
}

@Composable
private fun Keypad(
    onButtonClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    
    Column(
        modifier = modifier,
        verticalArrangement = if (isLandscape) {
            Arrangement.spacedBy(8.dp)
        } else {
            Arrangement.spacedBy(16.dp)
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: 7, 8, 9, 1000
        KeypadRow(
            buttons = listOf(
                KeypadButton("7", KeypadButtonType.NUMBER),
                KeypadButton("8", KeypadButtonType.NUMBER),
                KeypadButton("9", KeypadButtonType.NUMBER),
                KeypadButton("1000", KeypadButtonType.PRESET)
            ),
            onButtonClick = onButtonClick
        )
        
        // Row 2: 4, 5, 6, 500
        KeypadRow(
            buttons = listOf(
                KeypadButton("4", KeypadButtonType.NUMBER),
                KeypadButton("5", KeypadButtonType.NUMBER),
                KeypadButton("6", KeypadButtonType.NUMBER),
                KeypadButton("500", KeypadButtonType.PRESET)
            ),
            onButtonClick = onButtonClick
        )
        
        // Row 3: 1, 2, 3, 100
        KeypadRow(
            buttons = listOf(
                KeypadButton("1", KeypadButtonType.NUMBER),
                KeypadButton("2", KeypadButtonType.NUMBER),
                KeypadButton("3", KeypadButtonType.NUMBER),
                KeypadButton("100", KeypadButtonType.PRESET)
            ),
            onButtonClick = onButtonClick
        )
        
        // Row 4: ., 0, spacer, exact
        KeypadRow(
            buttons = listOf(
                KeypadButton(".", KeypadButtonType.NUMBER),
                KeypadButton("0", KeypadButtonType.NUMBER),
                KeypadButton("", KeypadButtonType.SPACER),
                KeypadButton("exact", KeypadButtonType.EXACT)
            ),
            onButtonClick = onButtonClick
        )
    }
}

@Composable
private fun KeypadRow(
    buttons: List<KeypadButton>,
    onButtonClick: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 16.dp)
    ) {
        buttons.forEach { button ->
            when (button.type) {
                KeypadButtonType.SPACER -> {
                    Spacer(modifier = Modifier.weight(1f))
                }
                else -> {
                    val weight = when {
                        button.type == KeypadButtonType.EXACT -> 1.4f
                        button.type == KeypadButtonType.PRESET -> 1.4f
                        else -> 1f
                    }
                    
                    KeypadButton(
                        title = button.title,
                        type = button.type,
                        onClick = { onButtonClick(button.title) },
                        modifier = Modifier.weight(weight)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    title: String,
    type: KeypadButtonType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayText = when (title) {
        "exact" -> "เต็ม"
        else -> title
    }
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    
    Surface(
        modifier = modifier
            .then(
                if (isLandscape) {
                    // In landscape, use smaller height
                    Modifier.height(56.dp)
                } else {
                    Modifier.height(72.dp)
                }
            )
            .clip(RoundedCornerShape(25.dp))
            .clickable(onClick = onClick),
        color = SecondaryButton,
        shape = RoundedCornerShape(25.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayText,
                style = FontUtils.mainFont(
                    style = AppFontStyle.SemiBold,
                    size = FontSize.Medium
                ),
                color = when (type) {
                    KeypadButtonType.NUMBER -> PrimaryText
                    KeypadButtonType.PRESET -> PrimaryButton
                    KeypadButtonType.EXACT -> PrimaryButton
                    KeypadButtonType.SPACER -> Color.Transparent
                }
            )
        }
    }
}

private data class KeypadButton(
    val title: String,
    val type: KeypadButtonType
)

private enum class KeypadButtonType {
    NUMBER,
    PRESET,
    EXACT,
    SPACER
}

private fun formatNumberWithCommas(number: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return formatter.format(number)
}

private fun formatDisplayAmount(enteredAmount: String): String {
    return if (enteredAmount.contains(".")) {
        // If contains decimal point, show as is
        enteredAmount
    } else {
        // If no decimal point, format with commas
        val amount = enteredAmount.replace(",", "").toDoubleOrNull() ?: 0.0
        formatNumberWithCommas(amount)
    }
}

