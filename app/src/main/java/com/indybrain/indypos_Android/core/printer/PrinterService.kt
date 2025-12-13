package com.indybrain.indypos_Android.core.printer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.content.Context
import androidx.core.content.res.ResourcesCompat
import com.indybrain.indypos_Android.R
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
        
        // Receipt width in pixels (typical 58mm thermal printer = 384 pixels at 203 DPI)
        private const val RECEIPT_WIDTH = 384
        private const val FONT_SIZE_NORMAL = 24
        private const val FONT_SIZE_LARGE = 32
        private const val FONT_SIZE_HEADER = 40
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
            
            // Set character encoding to UTF-8 for Thai language support
            // According to Android POS Program Manual section 2.26
            try {
                posPrinter.setCharSet("UTF-8")
            } catch (e: Exception) {
                // If setCharSet is not available, try alternative method
                android.util.Log.w("PrinterService", "setCharSet not available, trying alternative")
                // Some printers may need UTF-8 encoding set via sendData
                // We'll use printString/printText which should handle UTF-8 by default
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
            if (shopName.isNotEmpty()) {
                val headerBitmap = createTextBitmap(shopName, FONT_SIZE_HEADER, true, ALIGNMENT_CENTER)
                if (headerBitmap != null) {
                    posPrinter.printBitmap(headerBitmap, ALIGNMENT_CENTER, RECEIPT_WIDTH)
                    posPrinter.feedLine(1)
                }
            }
            
            // Print TIN if enabled
            if (receiptSettings?.taxIdentificationNumber == true && !receiptSettings.tinNumber.isNullOrEmpty()) {
                val tinText = "เลขประจำตัวผู้เสียภาษี: ${receiptSettings.tinNumber}"
                val tinBitmap = createTextBitmap(tinText, FONT_SIZE_NORMAL, false, ALIGNMENT_LEFT)
                if (tinBitmap != null) {
                    posPrinter.printBitmap(tinBitmap, ALIGNMENT_LEFT, RECEIPT_WIDTH)
                    posPrinter.feedLine(1)
                }
            }
            
            // Print order number
            if (!orderNumber.isNullOrEmpty()) {
                val orderText = "เลขที่ออเดอร์: $orderNumber"
                val orderBitmap = createTextBitmap(orderText, FONT_SIZE_NORMAL, false, ALIGNMENT_LEFT)
                if (orderBitmap != null) {
                    posPrinter.printBitmap(orderBitmap, ALIGNMENT_LEFT, RECEIPT_WIDTH)
                    posPrinter.feedLine(1)
                }
            }
            
            // Print date
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateText = "วันที่: ${dateFormat.format(Date())}"
            val dateBitmap = createTextBitmap(dateText, FONT_SIZE_NORMAL, false, ALIGNMENT_LEFT)
            if (dateBitmap != null) {
                posPrinter.printBitmap(dateBitmap, ALIGNMENT_LEFT, RECEIPT_WIDTH)
                posPrinter.feedLine(1)
            }
            
            // Separator
            posPrinter.printString("----------------------------\n")
            
            // Print items
            cartItems.forEach { cartItem ->
                val addons = cartAddonsMap[cartItem.id] ?: emptyList()
                val itemName = cartItem.productName ?: ""
                val itemPrice = (cartItem.unitPrice ?: 0.0) * cartItem.quantity +
                    addons.sumOf { it.addonPrice } * cartItem.quantity
                
                // Print item with quantity
                val itemText = "${cartItem.quantity} x $itemName"
                val itemBitmap = createTextBitmap(itemText, FONT_SIZE_NORMAL, false, ALIGNMENT_LEFT)
                if (itemBitmap != null) {
                    posPrinter.printBitmap(itemBitmap, ALIGNMENT_LEFT, RECEIPT_WIDTH)
                    posPrinter.feedLine(1)
                }
                
                // Print price aligned to right
                val priceText = formatCurrency(itemPrice)
                val priceBitmap = createTextBitmap(priceText, FONT_SIZE_NORMAL, false, ALIGNMENT_RIGHT)
                if (priceBitmap != null) {
                    posPrinter.printBitmap(priceBitmap, ALIGNMENT_RIGHT, RECEIPT_WIDTH)
                    posPrinter.feedLine(1)
                }
                
                // Print addons
                if (addons.isNotEmpty()) {
                    // Group addons by name and count
                    val addonCounts = addons.groupBy { it.addonName }
                        .mapValues { (_, addonsList) -> addonsList.size * cartItem.quantity }
                    
                    val addonTexts = addonCounts.map { (name, count) ->
                        if (count > 1) "$name x $count" else name
                    }
                    
                    if (addonTexts.isNotEmpty()) {
                        val addonLine = "   ${addonTexts.joinToString(", ")}"
                        val addonBitmap = createTextBitmap(addonLine, FONT_SIZE_NORMAL, false, ALIGNMENT_LEFT)
                        if (addonBitmap != null) {
                            posPrinter.printBitmap(addonBitmap, ALIGNMENT_LEFT, RECEIPT_WIDTH)
                            posPrinter.feedLine(1)
                        }
                    }
                }
                
                // Print special request
                if (!cartItem.specialRequest.isNullOrEmpty()) {
                    val requestText = "   หมายเหตุ: ${cartItem.specialRequest}"
                    val requestBitmap = createTextBitmap(requestText, FONT_SIZE_NORMAL, false, ALIGNMENT_LEFT)
                    if (requestBitmap != null) {
                        posPrinter.printBitmap(requestBitmap, ALIGNMENT_LEFT, RECEIPT_WIDTH)
                        posPrinter.feedLine(1)
                    }
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
            val paymentText = "วิธีการชำระเงิน: $paymentTypeText"
            val paymentBitmap = createTextBitmap(paymentText, FONT_SIZE_NORMAL, false, ALIGNMENT_LEFT)
            if (paymentBitmap != null) {
                posPrinter.printBitmap(paymentBitmap, ALIGNMENT_LEFT, RECEIPT_WIDTH)
                posPrinter.feedLine(1)
            }
            
            // Print subtotal
            val subtotalText = "ยอดรวมราคา: ${formatCurrency(subtotal)}"
            val subtotalBitmap = createTextBitmap(subtotalText, FONT_SIZE_NORMAL, false, ALIGNMENT_LEFT)
            if (subtotalBitmap != null) {
                posPrinter.printBitmap(subtotalBitmap, ALIGNMENT_LEFT, RECEIPT_WIDTH)
                posPrinter.feedLine(1)
            }
            
            // Print discount
            if (discount > 0) {
                val discountText = "ส่วนลด: -${formatCurrency(discount)}"
                val discountBitmap = createTextBitmap(discountText, FONT_SIZE_NORMAL, false, ALIGNMENT_LEFT)
                if (discountBitmap != null) {
                    posPrinter.printBitmap(discountBitmap, ALIGNMENT_LEFT, RECEIPT_WIDTH)
                    posPrinter.feedLine(1)
                }
            }
            
            // Print total - bold
            val totalText = "รวม: ${formatCurrency(total)}"
            val totalBitmap = createTextBitmap(totalText, FONT_SIZE_NORMAL, true, ALIGNMENT_LEFT)
            if (totalBitmap != null) {
                posPrinter.printBitmap(totalBitmap, ALIGNMENT_LEFT, RECEIPT_WIDTH)
                posPrinter.feedLine(1)
            }
            
            // Print cash received and change if payment type is cash
            if (paymentType == PaymentType.CASH) {
                receivedAmount?.let {
                    if (it > 0) {
                        val receivedText = "เงินที่รับ: ${formatCurrency(it)}"
                        val receivedBitmap = createTextBitmap(receivedText, FONT_SIZE_NORMAL, false, ALIGNMENT_LEFT)
                        if (receivedBitmap != null) {
                            posPrinter.printBitmap(receivedBitmap, ALIGNMENT_LEFT, RECEIPT_WIDTH)
                            posPrinter.feedLine(1)
                        }
                    }
                }
                
                change?.let {
                    val changeText = "เงินทอน: ${formatCurrency(it)}"
                    val changeBitmap = createTextBitmap(changeText, FONT_SIZE_NORMAL, false, ALIGNMENT_LEFT)
                    if (changeBitmap != null) {
                        posPrinter.printBitmap(changeBitmap, ALIGNMENT_LEFT, RECEIPT_WIDTH)
                        posPrinter.feedLine(1)
                    }
                }
            }
            
            // Print footer
            if (!receiptSettings?.footer.isNullOrEmpty()) {
                val footerBitmap = createTextBitmap(receiptSettings!!.footer, FONT_SIZE_NORMAL, false, ALIGNMENT_CENTER)
                if (footerBitmap != null) {
                    posPrinter.printBitmap(footerBitmap, ALIGNMENT_CENTER, RECEIPT_WIDTH)
                    posPrinter.feedLine(1)
                }
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
    
    /**
     * Create a bitmap from text with Thai language support
     * This ensures proper rendering of Thai characters before printing
     */
    private fun createTextBitmap(
        text: String,
        fontSize: Int,
        isBold: Boolean = false,
        alignment: Int = ALIGNMENT_LEFT
    ): Bitmap? {
        return try {
            // Load Thai font
            val typeface = if (isBold) {
                ResourcesCompat.getFont(context, R.font.anuphan_bold)
            } else {
                ResourcesCompat.getFont(context, R.font.anuphan_regular)
            }
            
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                this.textSize = fontSize.toFloat()
                this.color = android.graphics.Color.BLACK
                this.isFakeBoldText = isBold
            }
            
            // Measure text width
            val textBounds = android.graphics.Rect()
            paint.getTextBounds(text, 0, text.length, textBounds)
            val textWidth = paint.measureText(text).toInt()
            val textHeight = textBounds.height()
            
            // Create bitmap with padding
            val padding = 10
            val bitmapWidth = RECEIPT_WIDTH.coerceAtLeast(textWidth + padding * 2)
            val bitmapHeight = textHeight + padding * 2
            
            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            
            // Calculate x position based on alignment
            val x = when (alignment) {
                ALIGNMENT_CENTER -> (bitmapWidth - textWidth) / 2f
                ALIGNMENT_RIGHT -> (bitmapWidth - textWidth - padding).toFloat()
                else -> padding.toFloat()
            }
            
            // Draw text (y position accounts for baseline)
            val y = (bitmapHeight - padding - textBounds.bottom).toFloat()
            canvas.drawText(text, x, y, paint)
            
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("PrinterService", "Error creating text bitmap", e)
            null
        }
    }
}
