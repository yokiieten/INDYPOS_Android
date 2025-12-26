package com.indybrain.indypos_Android.presentation.datamanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.data.export.ExportDataType
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText

@Composable
fun ExportDataTypeDialog(
    onDismiss: () -> Unit,
    onDataTypeSelected: (ExportDataType) -> Unit
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
                    text = stringResource(R.string.export_data_type_dialog_title),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    ),
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Data Type Options
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // หมวดหมู่สินค้า
                    DataTypeOptionItem(
                        text = stringResource(R.string.export_data_type_categories),
                        onClick = {
                            onDataTypeSelected(ExportDataType.CATEGORIES)
                            onDismiss()
                        }
                    )
                    
                    // สินค้า
                    DataTypeOptionItem(
                        text = stringResource(R.string.export_data_type_products),
                        onClick = {
                            onDataTypeSelected(ExportDataType.PRODUCTS)
                            onDismiss()
                        }
                    )
                    
                    // กลุ่มตัวเลือกเพิ่มเติม
                    DataTypeOptionItem(
                        text = stringResource(R.string.export_data_type_addon_groups),
                        onClick = {
                            onDataTypeSelected(ExportDataType.ADDON_GROUPS)
                            onDismiss()
                        }
                    )
                    
                    // ตัวเลือกเพิ่มเติม
                    DataTypeOptionItem(
                        text = stringResource(R.string.export_data_type_addons),
                        onClick = {
                            onDataTypeSelected(ExportDataType.ADDONS)
                            onDismiss()
                        }
                    )
                    
                    // ประวัติการสั่งซื้อ
                    DataTypeOptionItem(
                        text = stringResource(R.string.export_data_type_orders),
                        onClick = {
                            onDataTypeSelected(ExportDataType.ORDERS)
                            onDismiss()
                        }
                    )
                    
                    // รายงานยอดขาย
                    DataTypeOptionItem(
                        text = stringResource(R.string.export_data_type_sales_report),
                        onClick = {
                            onDataTypeSelected(ExportDataType.SALES_REPORT)
                            onDismiss()
                        }
                    )
                    
                    // รายงานสต็อก
                    DataTypeOptionItem(
                        text = stringResource(R.string.export_data_type_stock_report),
                        onClick = {
                            onDataTypeSelected(ExportDataType.STOCK_REPORT)
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
private fun DataTypeOptionItem(
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


