package com.indybrain.indypos_Android.presentation.datamanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.data.export.ExportFormat
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText

@Composable
fun ExportFormatDialog(
    onDismiss: () -> Unit,
    onFormatSelected: (ExportFormat) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = stringResource(R.string.export_format_dialog_title),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    ),
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Subtitle
                Text(
                    text = stringResource(R.string.export_format_dialog_subtitle),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = SecondaryText,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Format Options
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Excel Option
                    FormatOptionItem(
                        text = stringResource(R.string.export_format_excel),
                        onClick = {
                            onFormatSelected(ExportFormat.EXCEL)
                            onDismiss()
                        }
                    )
                    
                    // CSV Option
                    FormatOptionItem(
                        text = stringResource(R.string.export_format_csv),
                        onClick = {
                            onFormatSelected(ExportFormat.CSV)
                            onDismiss()
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cancel Button
                Text(
                    text = stringResource(R.string.home_cancel),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = PrimaryButton,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FormatOptionItem(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        style = FontUtils.mainFont(
            style = AppFontStyle.Regular,
            size = FontSize.Medium
        ),
        color = PrimaryButton,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        textAlign = TextAlign.Center
    )
}

