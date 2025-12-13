package com.indybrain.indypos_Android.core.printer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.ReceiptSettingsEntity
import com.indybrain.indypos_Android.domain.model.PaymentType
import dagger.hilt.android.qualifiers.ApplicationContext
import net.posprinter.IDeviceConnection
import net.posprinter.POSPrinter
import net.posprinter.POSConst
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for printing receipts and opening cash drawer
 * Using POSPrinter according to Android POS Program Manual
 */
@Singleton
class PrinterService @Inject constructor(
    private val printerManager: PrinterManager,
    @ApplicationContext private val context: Context
) {
    
    companion object {
        // Alignment constants from POSConst
        private const val ALIGNMENT_LEFT = POSConst.ALIGNMENT_LEFT
        private const val ALIGNMENT_CENTER = POSConst.ALIGNMENT_CENTER
        private const val ALIGNMENT_RIGHT = POSConst.ALIGNMENT_RIGHT
        
        // Font attributes
        private const val FNT_DEFAULT = POSConst.FNT_DEFAULT
        private const val FNT_BOLD = POSConst.FNT_BOLD
        
        // Text size
        private const val TXT_1WIDTH = POSConst.TXT_1WIDTH
        private const val TXT_1HEIGHT = POSConst.TXT_1HEIGHT
        private const val TXT_2WIDTH = POSConst.TXT_2WIDTH
        private const val TXT_2HEIGHT = POSConst.TXT_2HEIGHT
    }
    
    /**
     * Open cash drawer using POSPrinter
     */
    fun openCashDrawer() {
        val connection = printerManager.currentConnection
        if (connection?.isConnect != true) {
            android.util.Log.e("PrinterService", "Printer not connected")
            return
        }
        
        try {
            val posPrinter = POSPrinter(connection)
            // Open cash drawer: pin=0, t1=30ms, t2=255ms
            posPrinter.openCashBox(0, 30, 255)
            android.util.Log.d("PrinterService", "Cash drawer opened")
        } catch (e: Exception) {
            android.util.Log.e("PrinterService", "Exception opening cash drawer", e)
        }
    }
    
    /**
     * Print order receipt using POSPrinter
     * According to Android POS Program Manual
     */
    fun printOrderReceipt(
        cartItems: List<CartItemEntity>,
        cartAddonsMap: Map<String, List<CartAddonEntity>>,
        receiptSettings: ReceiptSettingsEntity?,
        shopName: String,
        orderNumber: String?,
        subtotal: Double,
        discount: Double,
        total: Double,
        paymentType: PaymentType,
        receivedAmount: Double? = null,
        change: Double? = null
    ) {
        val connection = printerManager.currentConnection
        if (connection?.isConnect != true) {
            android.util.Log.e("PrinterService", "Printer not connected")
            return
        }
        
        try {
            val posPrinter = POSPrinter(connection)
            
            // Set character encoding for Thai language support
            // Try different encodings commonly used for Thai printers
            // According to Android POS Program Manual section 2.26
            var charsetSet = false
            val charsets = listOf("TIS-620", "Windows-874", "UTF-8", "ISO-8859-11")
            
            for (charset in charsets) {
                try {
                    posPrinter.setCharSet(charset)
                    android.util.Log.d("PrinterService", "Successfully set charset to: $charset")
                    charsetSet = true
                    break
                } catch (e: Exception) {
                    android.util.Log.w("PrinterService", "Failed to set charset $charset: ${e.message}")
                }
            }
            
            if (!charsetSet) {
                android.util.Log.w("PrinterService", "Could not set any charset, printer may use default encoding")
            }
            
            // Print shop logo if enabled
            if (receiptSettings?.printShopLogo == true && receiptSettings.shopLogoImagePath != null) {
                val logoBitmap = loadShopLogo(receiptSettings.shopLogoImagePath)
                if (logoBitmap != null) {
                    posPrinter.printBitmap(logoBitmap, ALIGNMENT_CENTER, 200)
                    posPrinter.feedLine(1)
                }
            }
            
            // Print shop name (header) - centered and bold, double size
            // According to manual section 2.3: When using alignment, data needs to end with "\n"
            if (shopName.isNotEmpty()) {
                posPrinter.printText(
                    "$shopName\n",
                    ALIGNMENT_CENTER,
                    FNT_BOLD,
                    TXT_2WIDTH or TXT_2HEIGHT
                )
            }
            
            // Print TIN if enabled
            if (receiptSettings?.taxIdentificationNumber == true && !receiptSettings.tinNumber.isNullOrEmpty()) {
                posPrinter.printText(
                    "เลขประจำตัวผู้เสียภาษี: ${receiptSettings.tinNumber}\n",
                    ALIGNMENT_LEFT,
                    FNT_DEFAULT,
                    TXT_1WIDTH or TXT_1HEIGHT
                )
            }
            
            // Print order number
            if (!orderNumber.isNullOrEmpty()) {
                posPrinter.printText(
                    "เลขที่ออเดอร์: $orderNumber\n",
                    ALIGNMENT_LEFT,
                    FNT_DEFAULT,
                    TXT_1WIDTH or TXT_1HEIGHT
                )
            }
            
            // Print date
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            posPrinter.printText(
                "วันที่: ${dateFormat.format(Date())}\n",
                ALIGNMENT_LEFT,
                FNT_DEFAULT,
                TXT_1WIDTH or TXT_1HEIGHT
            )
            
            // Separator
            posPrinter.printString("----------------------------\n")
            
            // Print items
            cartItems.forEach { cartItem ->
                val addons = cartAddonsMap[cartItem.id] ?: emptyList()
                val itemName = cartItem.productName ?: ""
                val itemPrice = (cartItem.unitPrice ?: 0.0) * cartItem.quantity +
                    addons.sumOf { it.addonPrice } * cartItem.quantity
                
                // Print item with quantity
                posPrinter.printText(
                    "${cartItem.quantity} x $itemName\n",
                    ALIGNMENT_LEFT,
                    FNT_DEFAULT,
                    TXT_1WIDTH or TXT_1HEIGHT
                )
                
                // Print price aligned to right
                posPrinter.printText(
                    "${formatCurrency(itemPrice)}\n",
                    ALIGNMENT_RIGHT,
                    FNT_DEFAULT,
                    TXT_1WIDTH or TXT_1HEIGHT
                )
                
                // Print addons
                if (addons.isNotEmpty()) {
                    // Group addons by name and count
                    val addonCounts = addons.groupBy { it.addonName }
                        .mapValues { (_, addonsList) -> addonsList.size * cartItem.quantity }
                    
                    val addonTexts = addonCounts.map { (name, count) ->
                        if (count > 1) "$name x $count" else name
                    }
                    
                    if (addonTexts.isNotEmpty()) {
                        posPrinter.printText(
                            "   ${addonTexts.joinToString(", ")}\n",
                            ALIGNMENT_LEFT,
                            FNT_DEFAULT,
                            TXT_1WIDTH or TXT_1HEIGHT
                        )
                    }
                }
                
                // Print special request
                if (!cartItem.specialRequest.isNullOrEmpty()) {
                    posPrinter.printText(
                        "   หมายเหตุ: ${cartItem.specialRequest}\n",
                        ALIGNMENT_LEFT,
                        FNT_DEFAULT,
                        TXT_1WIDTH or TXT_1HEIGHT
                    )
                }
            }
            
            // Separator
            posPrinter.printString("----------------------------\n")
            
            // Print payment type
            val paymentTypeText = when (paymentType) {
                PaymentType.CASH -> "เงินสด"
                PaymentType.TRANSFER -> "โอนเงิน"
                PaymentType.CARD -> "บัตรเครดิต"
                PaymentType.QR_CODE -> "QR Code"
            }
            posPrinter.printText(
                "วิธีการชำระเงิน: $paymentTypeText\n",
                ALIGNMENT_LEFT,
                FNT_DEFAULT,
                TXT_1WIDTH or TXT_1HEIGHT
            )
            
            // Print subtotal
            posPrinter.printText(
                "ยอดรวมราคา: ${formatCurrency(subtotal)}\n",
                ALIGNMENT_LEFT,
                FNT_DEFAULT,
                TXT_1WIDTH or TXT_1HEIGHT
            )
            
            // Print discount
            if (discount > 0) {
                posPrinter.printText(
                    "ส่วนลด: -${formatCurrency(discount)}\n",
                    ALIGNMENT_LEFT,
                    FNT_DEFAULT,
                    TXT_1WIDTH or TXT_1HEIGHT
                )
            }
            
            // Print total - bold
            posPrinter.printText(
                "รวม: ${formatCurrency(total)}\n",
                ALIGNMENT_LEFT,
                FNT_BOLD,
                TXT_1WIDTH or TXT_1HEIGHT
            )
            
            // Print cash received and change if payment type is cash
            if (paymentType == PaymentType.CASH) {
                receivedAmount?.let {
                    if (it > 0) {
                        posPrinter.printText(
                            "เงินที่รับ: ${formatCurrency(it)}\n",
                            ALIGNMENT_LEFT,
                            FNT_DEFAULT,
                            TXT_1WIDTH or TXT_1HEIGHT
                        )
                    }
                }
                
                change?.let {
                    posPrinter.printText(
                        "เงินทอน: ${formatCurrency(it)}\n",
                        ALIGNMENT_LEFT,
                        FNT_DEFAULT,
                        TXT_1WIDTH or TXT_1HEIGHT
                    )
                }
            }
            
            // Print footer
            if (!receiptSettings?.footer.isNullOrEmpty()) {
                posPrinter.printText(
                    "${receiptSettings!!.footer}\n",
                    ALIGNMENT_CENTER,
                    FNT_DEFAULT,
                    TXT_1WIDTH or TXT_1HEIGHT
                )
            }
            
            // Print QR code if enabled and payment is not cash
            if (receiptSettings?.showQRCode == true && paymentType != PaymentType.CASH) {
                // TODO: Generate PromptPay QR code
                // posPrinter.printQRCode(qrCodeData, ALIGNMENT_CENTER)
            }
            
            // Final line feeds
            posPrinter.feedLine(3)
            
            android.util.Log.d("PrinterService", "Receipt printed successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("PrinterService", "Exception printing receipt", e)
        }
    }
    
    private fun loadShopLogo(imagePath: String?): Bitmap? {
        if (imagePath.isNullOrEmpty()) return null
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("PrinterService", "Error loading shop logo", e)
            null
        }
    }
    
    private fun formatCurrency(value: Double): String {
        val formatter = java.text.DecimalFormat("#,##0.00")
        return "฿${formatter.format(value)}"
    }
}
