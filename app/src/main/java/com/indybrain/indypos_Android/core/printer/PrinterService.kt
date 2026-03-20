package com.indybrain.indypos_Android.core.printer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.content.Context
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.ReceiptSettingsEntity
import com.indybrain.indypos_Android.domain.model.PaymentType
import com.indybrain.indypos_Android.domain.model.PromptPayType
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import net.posprinter.IDeviceConnection
import net.posprinter.POSPrinter
import net.posprinter.POSConst
import java.io.File
import java.util.Date
import java.util.Hashtable
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
        
        // Receipt bitmap width for 58mm thermal paper (384 dots at 203 DPI)
        private const val RECEIPT_BITMAP_WIDTH = 384
        
        // Addon - ชิดซ้ายเหมือนเดิม (ไม่มี indent)
        private const val ADDON_INDENT = ""
        
        // Addon separator - คั่น addon ด้วยคอมม่าเหมือนในภาพ
        private const val ADDON_SEPARATOR = ", "
    }
    
    /** ความสูงแต่ละบรรทัด - ชิดกันมากที่สุด (เท่าความสูงตัวอักษร) */
    private fun getReceiptLineHeight(paint: Paint): Float {
        val fm = paint.fontMetrics
        return (fm.descent - fm.ascent)
    }
    
    /** Paint สำหรับ receipt - ชัดคม ไม่ blur (ไม่มี anti-alias สำหรับ thermal printer) */
    private fun createReceiptPaint(textSize: Float, isBold: Boolean = true): Paint {
        return Paint(0).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.MONOSPACE, if (isBold) Typeface.BOLD else Typeface.NORMAL)
            this.textSize = textSize
            isAntiAlias = false
            isFilterBitmap = false
            setHinting(Paint.HINTING_ON)
        }
    }
    
    /** แสดงข้อความบรรทัดเดียว ไม่ปัด - สำหรับ separator, order number */
    private fun textToBitmapSingleLine(text: String, alignment: Int): Bitmap {
        val paint = createReceiptPaint(textSize = 30f)
        var drawText = text
        val textWidth = paint.measureText(text)
        if (textWidth > RECEIPT_BITMAP_WIDTH) {
            val scale = RECEIPT_BITMAP_WIDTH / textWidth
            paint.textSize = 30f * scale
            drawText = text
        }
        val lineHeight = getReceiptLineHeight(paint)
        val totalHeight = lineHeight.toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(RECEIPT_BITMAP_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val baselineOffset = -paint.fontMetrics.ascent
        val x = when (alignment) {
            ALIGNMENT_CENTER -> ((RECEIPT_BITMAP_WIDTH - paint.measureText(drawText)) / 2f).coerceAtLeast(0f)
            ALIGNMENT_RIGHT -> (RECEIPT_BITMAP_WIDTH - paint.measureText(drawText)).coerceAtLeast(0f)
            else -> 0f
        }
        canvas.drawText(drawText, x, baselineOffset, paint)
        return bitmap
    }
    
    /**
     * Open cash drawer using POSPrinter
     */
    fun openCashDrawer() {
        val connection = printerManager.currentConnection
        if (connection?.isConnect != true) {
            return
        }
        
        try {
            val posPrinter = POSPrinter(connection)
            // Open cash drawer: pin=0, t1=30ms, t2=255ms
            posPrinter.openCashBox(0, 30, 255)
        } catch (e: Exception) {
            // Exception opening cash drawer
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
            return
        }
        
        try {
            val posPrinter = POSPrinter(connection)
            
            // Print shop logo if enabled
            if (receiptSettings?.printShopLogo == true && receiptSettings.shopLogoImagePath != null) {
                val logoBitmap = loadShopLogo(receiptSettings.shopLogoImagePath)
                if (logoBitmap != null) {
                    posPrinter.printBitmap(logoBitmap, ALIGNMENT_CENTER, 200)
                    posPrinter.feedLine(0)
                }
            }
            
            // Print shop name (header) - centered and bold, double size
            if (shopName.isNotEmpty()) {
                val bitmap = textToBitmap(shopName, ALIGNMENT_CENTER, isBold = true, isDoubleSize = true)
                posPrinter.printBitmap(bitmap, ALIGNMENT_CENTER, RECEIPT_BITMAP_WIDTH)
                posPrinter.feedLine(0)
            }
            
            // Print TIN if enabled
            if (receiptSettings?.taxIdentificationNumber == true && !receiptSettings.tinNumber.isNullOrEmpty()) {
                val bitmap = textToBitmap("เลขประจำตัวผู้เสียภาษี: ${receiptSettings.tinNumber}", ALIGNMENT_LEFT)
                posPrinter.printBitmap(bitmap, ALIGNMENT_LEFT, RECEIPT_BITMAP_WIDTH)
                posPrinter.feedLine(0)
            }
            
            // Print order number - label บรรทัดหนึ่ง, value บรรทัดสองชิดขวา
            if (!orderNumber.isNullOrEmpty()) {
                val labelBitmap = textToBitmapSingleLine("เลขที่คำสั่งซื้อ:", ALIGNMENT_LEFT)
                posPrinter.printBitmap(labelBitmap, ALIGNMENT_LEFT, RECEIPT_BITMAP_WIDTH)
                posPrinter.feedLine(0)
                val valueBitmap = textToBitmapSingleLine(orderNumber, ALIGNMENT_RIGHT)
                posPrinter.printBitmap(valueBitmap, ALIGNMENT_RIGHT, RECEIPT_BITMAP_WIDTH)
                posPrinter.feedLine(0)
            }
            
            // Print date (พ.ศ. = ค.ศ. + 543)
            val calendar = java.util.Calendar.getInstance()
            val now = Date()
            calendar.time = now
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            val month = calendar.get(java.util.Calendar.MONTH) + 1
            val yearBuddhist = calendar.get(java.util.Calendar.YEAR) + 543
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = calendar.get(java.util.Calendar.MINUTE)
            val dateStr = String.format(Locale.getDefault(), "%02d/%02d/%d %02d:%02d", day, month, yearBuddhist, hour, minute)
            val dateBitmap = textToBitmap("วันที่: $dateStr", ALIGNMENT_LEFT)
            posPrinter.printBitmap(dateBitmap, ALIGNMENT_LEFT, RECEIPT_BITMAP_WIDTH)
            posPrinter.feedLine(0)
            
            // Separator - บรรทัดเดียว ไม่ปัด
            val separatorBitmap = textToBitmapSingleLine("--------------------------------", ALIGNMENT_CENTER)
            posPrinter.printBitmap(separatorBitmap, ALIGNMENT_CENTER, RECEIPT_BITMAP_WIDTH)
            posPrinter.feedLine(0)
            
            // Print items
            // Note: unitPrice in CartItemEntity already includes addon prices when item was added to cart
            cartItems.forEach { cartItem ->
                val addons = cartAddonsMap[cartItem.id] ?: emptyList()
                val itemName = cartItem.productName ?: ""
                val itemPrice = (cartItem.unitPrice ?: 0.0) * cartItem.quantity
                
                // Print item - support multi-line for long product names, price ชิดขวาแน่นอน
                val label = "${cartItem.quantity} x $itemName"
                val priceStr = formatCurrencyWithoutSymbol(itemPrice)
                val itemLines = formatItemLinesWithWrap(label = label, price = priceStr)
                itemLines.forEach { (lineLabel, linePrice) ->
                    val lineBitmap = if (linePrice != null) {
                        textToBitmapLabelPrice(lineLabel, linePrice)
                    } else {
                        textToBitmap(lineLabel, ALIGNMENT_LEFT)
                    }
                    posPrinter.printBitmap(lineBitmap, ALIGNMENT_LEFT, RECEIPT_BITMAP_WIDTH)
                    posPrinter.feedLine(0)
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
                        val addonBitmap = textToBitmap(
                            "${ADDON_INDENT}${addonTexts.joinToString(separator = ADDON_SEPARATOR)}",
                            ALIGNMENT_LEFT,
                            continuationIndent = ADDON_INDENT
                        )
                        posPrinter.printBitmap(addonBitmap, ALIGNMENT_LEFT, RECEIPT_BITMAP_WIDTH)
                        posPrinter.feedLine(0)
                    }
                }
                
                // Print special request (indent เท่ากับ addon)
                if (!cartItem.specialRequest.isNullOrEmpty()) {
                    val noteBitmap = textToBitmap(
                        "${ADDON_INDENT}หมายเหตุ: ${cartItem.specialRequest}",
                        ALIGNMENT_LEFT,
                        continuationIndent = ADDON_INDENT
                    )
                    posPrinter.printBitmap(noteBitmap, ALIGNMENT_LEFT, RECEIPT_BITMAP_WIDTH)
                    posPrinter.feedLine(0)
                }
            }
            
            // Separator - บรรทัดเดียว ไม่ปัด
            val sepBitmap = textToBitmapSingleLine("--------------------------------", ALIGNMENT_CENTER)
            posPrinter.printBitmap(sepBitmap, ALIGNMENT_CENTER, RECEIPT_BITMAP_WIDTH)
            posPrinter.feedLine(0)
            
            // Print payment section (รูปแบบเหมือน iOS สำหรับเงินสด)
            val paymentTypeText = when (paymentType) {
                PaymentType.CASH -> "จ่ายเงินสด"
                PaymentType.TRANSFER -> "โอนเงิน"
                PaymentType.CARD -> "บัตรเครดิต"
                PaymentType.QR_CODE -> "QR Code"
            }
            // ส่วนสรุป: หัวข้อชิดซ้าย ค่าชิดขวา (pixel-perfect)
            val payBitmap = textToBitmapLabelPrice("วิธีการชำระเงิน:", paymentTypeText)
            posPrinter.printBitmap(payBitmap, ALIGNMENT_LEFT, RECEIPT_BITMAP_WIDTH)
            posPrinter.feedLine(0)
            
            val subBitmap = textToBitmapLabelPrice("ยอดรวมราคา:", formatCurrencyWithoutSymbol(subtotal))
            posPrinter.printBitmap(subBitmap, ALIGNMENT_LEFT, RECEIPT_BITMAP_WIDTH)
            posPrinter.feedLine(0)
            
            val discountPriceStr = if (discount > 0) "-${formatCurrencyWithoutSymbol(discount)}" else formatCurrencyWithoutSymbol(0.0)
            val discBitmap = textToBitmapLabelPrice("ส่วนลด:", discountPriceStr)
            posPrinter.printBitmap(discBitmap, ALIGNMENT_LEFT, RECEIPT_BITMAP_WIDTH)
            posPrinter.feedLine(0)
            
            val totalLabel = if (paymentType == PaymentType.CASH) "ยอดรวมทั้งหมด:" else "รวม:"
            val totalBitmap = textToBitmapLabelPrice(totalLabel, formatCurrencyWithoutSymbol(total), isBold = true)
            posPrinter.printBitmap(totalBitmap, ALIGNMENT_LEFT, RECEIPT_BITMAP_WIDTH)
            posPrinter.feedLine(0)
            
            if (paymentType == PaymentType.CASH) {
                receivedAmount?.let {
                    if (it > 0) {
                        val receivedBitmap = textToBitmapLabelPrice("เงินสด:", formatCurrencyWithoutSymbol(it))
                        posPrinter.printBitmap(receivedBitmap, ALIGNMENT_LEFT, RECEIPT_BITMAP_WIDTH)
                        posPrinter.feedLine(0)
                    }
                }
                change?.let {
                    val changeBitmap = textToBitmapLabelPrice("เงินทอน:", formatCurrencyWithoutSymbol(it))
                    posPrinter.printBitmap(changeBitmap, ALIGNMENT_LEFT, RECEIPT_BITMAP_WIDTH)
                    posPrinter.feedLine(0)
                }
            }
            
            // Print footer
            if (!receiptSettings?.footer.isNullOrEmpty()) {
                val footerBitmap = textToBitmap(receiptSettings!!.footer!!, ALIGNMENT_CENTER)
                posPrinter.printBitmap(footerBitmap, ALIGNMENT_CENTER, RECEIPT_BITMAP_WIDTH)
                posPrinter.feedLine(0)
            }
            
            // Print QR code if enabled and payment is not cash
            if (receiptSettings?.showQRCode == true && paymentType != PaymentType.CASH) {
                val promptPayType = receiptSettings.promptPayType?.let { 
                    PromptPayType.fromString(it) 
                }
                val promptPayIdentifier = receiptSettings.promptPayIdentifier
                
                if (promptPayType != null && !promptPayIdentifier.isNullOrEmpty()) {
                    // Calculate final total to match what's shown on receipt (subtotal - discount)
                    // This ensures QR code amount matches the displayed total
                    val finalTotal = (subtotal - discount).coerceAtLeast(0.0)
                    val qrData = generatePromptPayQRDataWithAmount(
                        type = promptPayType,
                        identifier = promptPayIdentifier,
                        amount = finalTotal
                    )
                    val qrBitmap = generateQRCodeBitmap(qrData, 200)
                    if (qrBitmap != null) {
                        posPrinter.printBitmap(qrBitmap, ALIGNMENT_CENTER, 200)
                        posPrinter.feedLine(0)
                    }
                }
            }
            
            // Final line feeds
            posPrinter.feedLine(3)
            
        } catch (e: Exception) {
            // Exception printing receipt
        }
    }
    
    private fun loadShopLogo(imagePath: String?): Bitmap? {
        if (imagePath.isNullOrEmpty()) return null
        return try {
            // imagePath is a relative path like "shop_logos/shop_logo_xxx.jpg"
            // Need to combine with context.filesDir
            val file = File(context.filesDir, imagePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Convert text to bitmap for thermal printer (bitmap printing)
     * Uses MONOSPACE font so character-based alignment (spaces) works correctly
     * @param continuationIndent ถ้ามี (เช่น addon) บรรทัดต่อเนื่องจะเติม indent
     */
    private fun textToBitmap(
        text: String,
        alignment: Int,
        isBold: Boolean = false,
        isDoubleSize: Boolean = false,
        continuationIndent: String = ""
    ): Bitmap {
        val lines = text.split("\n").filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            return Bitmap.createBitmap(RECEIPT_BITMAP_WIDTH, 24, Bitmap.Config.ARGB_8888).apply {
                Canvas(this).drawColor(Color.WHITE)
            }
        }
        val paint = createReceiptPaint(
            textSize = if (isDoubleSize) 52f else 30f,
            isBold = isBold
        )
        val lineHeight = getReceiptLineHeight(paint)
        val allLines = lines.flatMap { wrapTextByPixel(it, paint, RECEIPT_BITMAP_WIDTH.toFloat(), continuationIndent) }
        val totalHeight = (lineHeight * allLines.size).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(RECEIPT_BITMAP_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val baselineOffset = -paint.fontMetrics.ascent
        allLines.forEachIndexed { index, drawLine ->
            val textWidth = paint.measureText(drawLine)
            val x = when (alignment) {
                ALIGNMENT_CENTER -> ((RECEIPT_BITMAP_WIDTH - textWidth) / 2f).coerceAtLeast(0f)
                ALIGNMENT_RIGHT -> (RECEIPT_BITMAP_WIDTH - textWidth).coerceAtLeast(0f)
                ALIGNMENT_LEFT -> 0f
                else -> 0f
            }
            val y = baselineOffset + index * lineHeight
            canvas.drawText(drawLine, x, y, paint)
        }
        return bitmap
    }
    
    /**
     * แบ่งข้อความตามความกว้างจริง (พิกเซล) - ใช้พื้นที่เต็มก่อนปัดบรรทัด
     * พยายามตัดที่ space หรือ comma
     */
    private fun wrapTextByPixel(
        text: String,
        paint: Paint,
        maxWidthPx: Float,
        continuationIndent: String = ""
    ): List<String> {
        if (paint.measureText(text) <= maxWidthPx) return listOf(text)
        val indentWidth = paint.measureText(continuationIndent)
        val firstLineMaxWidth = maxWidthPx
        val continuationMaxWidth = maxWidthPx - indentWidth
        val result = mutableListOf<String>()
        var remaining = text
        var isFirst = true
        while (remaining.isNotEmpty()) {
            val lineMaxWidth = if (isFirst) firstLineMaxWidth else continuationMaxWidth
            if (paint.measureText(remaining) <= lineMaxWidth) {
                result.add(if (isFirst) remaining else continuationIndent + remaining)
                break
            }
            var fitLength = remaining.length
            while (fitLength > 0 && paint.measureText(remaining.take(fitLength)) > lineMaxWidth) {
                fitLength--
            }
            if (fitLength <= 0) fitLength = 1
            val chunk = remaining.take(fitLength)
            val breakAt = listOf(chunk.lastIndexOf(' '), chunk.lastIndexOf(',')).filter { it > 0 }.maxOrNull() ?: (fitLength - 1)
            val splitPoint = (breakAt + 1).coerceAtLeast(1)
            val firstPart = remaining.take(splitPoint).trimEnd()
            result.add(if (isFirst) firstPart else continuationIndent + firstPart)
            remaining = remaining.drop(splitPoint).trimStart()
            isFirst = false
        }
        return result
    }
    
    /**
     * Draw label (left) + price (right) - ไม่ใช้ ... แบบปัดบรรทัดแทน
     */
    private fun textToBitmapLabelPrice(
        label: String,
        price: String,
        isBold: Boolean = false
    ): Bitmap {
        val paint = createReceiptPaint(textSize = 30f, isBold = isBold)
        val lineHeight = getReceiptLineHeight(paint)
        val priceWidth = paint.measureText(price)
        val labelMaxWidth = RECEIPT_BITMAP_WIDTH - priceWidth
        if (paint.measureText(label) <= labelMaxWidth) {
            val totalHeight = lineHeight.toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(RECEIPT_BITMAP_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            val baselineOffset = -paint.fontMetrics.ascent
            canvas.drawText(label, 0f, baselineOffset, paint)
            canvas.drawText(price, RECEIPT_BITMAP_WIDTH - priceWidth, baselineOffset, paint)
            return bitmap
        }
        val labelLines = wrapTextByPixel(label, paint, labelMaxWidth, "")
        val totalHeight = (lineHeight * labelLines.size).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(RECEIPT_BITMAP_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val baselineOffset = -paint.fontMetrics.ascent
        labelLines.dropLast(1).forEachIndexed { index, line ->
            canvas.drawText(line, 0f, baselineOffset + index * lineHeight, paint)
        }
        val lastLine = labelLines.last()
        canvas.drawText(lastLine, 0f, baselineOffset + (labelLines.size - 1) * lineHeight, paint)
        canvas.drawText(price, RECEIPT_BITMAP_WIDTH - priceWidth, baselineOffset + (labelLines.size - 1) * lineHeight, paint)
        return bitmap
    }
    
    private fun formatCurrency(value: Double): String {
        val formatter = java.text.DecimalFormat("#,##0.00")
        return "฿${formatter.format(value)}"
    }
    
    /**
     * Format currency without symbol (for item prices in receipt)
     * Always formats with 2 decimal places and thousand separators for consistent alignment
     */
    private fun formatCurrencyWithoutSymbol(value: Double): String {
        val formatter = java.text.DecimalFormat("#,##0.00")
        return formatter.format(value)
    }
    
    
    /**
     * Format item with long name - บรรทัดแรกมีราคาชิดขวาคู่กับต้นชื่อ; บรรทัดต่อใช้ความกว้างเต็ม
     * ใช้ pixel-based measurement จริง (ไทย/อังกฤษกว้างไม่เท่ากัน)
     */
    private fun formatItemLinesWithWrap(label: String, price: String): List<Pair<String, String?>> {
        val paint = createReceiptPaint(textSize = 30f)
        val priceWidth = paint.measureText(price)
        val firstLineLabelMax = RECEIPT_BITMAP_WIDTH - priceWidth
        if (paint.measureText(label) <= firstLineLabelMax) {
            return listOf(label to price)
        }
        val lines = mutableListOf<Pair<String, String?>>()
        var remaining = label

        fun takeOneLine(text: String, maxWidthPx: Float): Pair<String, String> {
            if (text.isEmpty()) return "" to ""
            if (paint.measureText(text) <= maxWidthPx) return text to ""
            var fitLength = text.length
            while (fitLength > 0 && paint.measureText(text.take(fitLength)) > maxWidthPx) {
                fitLength--
            }
            if (fitLength <= 0) fitLength = 1
            val chunk = text.take(fitLength)
            val breakAt = listOf(chunk.lastIndexOf(' '), chunk.lastIndexOf(',')).filter { it > 0 }.maxOrNull() ?: (fitLength - 1)
            val splitPoint = (breakAt + 1).coerceAtLeast(1)
            val part = text.take(splitPoint).trimEnd()
            val rest = text.drop(splitPoint).trimStart()
            return part to rest
        }

        val (firstPart, afterFirst) = takeOneLine(remaining, firstLineLabelMax)
        lines.add(firstPart to price)
        remaining = afterFirst
        val continuationMax = RECEIPT_BITMAP_WIDTH.toFloat()
        while (remaining.isNotEmpty()) {
            if (paint.measureText(remaining) <= continuationMax) {
                lines.add(remaining to null)
                break
            }
            val (line, rest) = takeOneLine(remaining, continuationMax)
            lines.add(line to null)
            remaining = rest
        }
        return lines
    }
    
    /**
     * Format item line with label on left and price on right
     * Matches the format shown in receipt image: "1 x Item Name                    10.00"
     * - Never truncate item name, always show full name and price
     * - Use fixed width to align prices to the right edge consistently
     * - If price is empty, just return label without extra spacing
     * - Ensures all prices align to the same right edge position
     */
    private fun formatItemLine(label: String, price: String): String {
        // If price is empty, just return label
        if (price.isEmpty()) {
            return label
        }
        
        // Receipt width for 58mm + monospace font
        val maxWidth = 28
        
        // Calculate string length (Thai characters count as 1 character in monospace fonts)
        val labelLength = label.length
        val priceLength = price.length
        val totalLength = labelLength + priceLength
        
        // Always show full label and price, never truncate
        // Add spaces between label and price to align price to right edge
        val spaces = if (totalLength < maxWidth) {
            maxWidth - totalLength
        } else {
            // If total length exceeds maxWidth, use minimum spacing (1 space)
            // This ensures price is always visible even for very long item names
            1
        }
        
        return "$label${" ".repeat(spaces)}$price"
    }
    
    /**
     * Generate PromptPay QR Code data with amount (Dynamic QR)
     * Format: [00]01[01]12[29][30]A000000677010111[01-03][identifier][54][amount][52]0000[53]764[58]TH[63][CRC]
     */
    private fun generatePromptPayQRDataWithAmount(
        type: PromptPayType,
        identifier: String,
        amount: Double
    ): String {
        // Clean identifier (remove dashes and spaces)
        var cleanIdentifier = identifier.replace(Regex("[^0-9]"), "")
        
        // Format identifier according to PromptPay standard
        when (type) {
            PromptPayType.PHONE_NUMBER -> {
                // Phone number format: 0066[phone without leading 0]
                if (cleanIdentifier.startsWith("0") && cleanIdentifier.length == 10) {
                    cleanIdentifier = "0066" + cleanIdentifier.substring(1)
                } else if (!cleanIdentifier.startsWith("0066")) {
                    if (cleanIdentifier.startsWith("66")) {
                        cleanIdentifier = "00$cleanIdentifier"
                    } else {
                        cleanIdentifier = "0066$cleanIdentifier"
                    }
                }
                cleanIdentifier = cleanIdentifier.padStart(13, '0').take(13)
            }
            PromptPayType.NATIONAL_ID -> {
                cleanIdentifier = cleanIdentifier.padStart(13, '0').take(13)
            }
            PromptPayType.E_WALLET -> {
                if (cleanIdentifier.length < 13) {
                    cleanIdentifier = cleanIdentifier.padStart(13, '0')
                }
            }
        }
        
        // Format amount: Fix for PromptPay QR Code amount display issue
        // Problem: When using satang (24300), scanning app shows as 24300 baht instead of 243 baht
        // Root cause: The scanning app doesn't divide by 100 to convert satang to baht
        // 
        // Solution: Use amount in baht as integer (rounded to nearest integer)
        // This matches what the scanning app expects and displays
        // Example: 243.00 baht -> "243", 243.50 baht -> "244" (rounded), 243.25 baht -> "243" (rounded)
        val amountRounded = java.math.BigDecimal(amount).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
        val amountString = amountRounded.toString()
        
        // Build EMV QR Code payload
        val payload = buildString {
            // Payload Format Indicator (00)
            append("00")
            append("02") // Length
            append("01") // Value
            
            // Point of Initiation Method (01) - 12 = Dynamic (with amount)
            append("01")
            append("02") // Length
            append("12") // Dynamic QR (with amount)
            
            // Merchant Account Information (29-51)
            append("29") // Tag
            val merchantAccountInfo = buildString {
                // AID (00)
                append("00")
                append("16") // Length
                append("A000000677010111") // PromptPay AID
                
                // Account Identifier (01-03)
                when (type) {
                    PromptPayType.PHONE_NUMBER -> append("01")
                    PromptPayType.NATIONAL_ID -> append("02")
                    PromptPayType.E_WALLET -> append("03")
                }
                val identifierLength = String.format("%02d", cleanIdentifier.length)
                append(identifierLength)
                append(cleanIdentifier)
            }
            val merchantAccountInfoLength = String.format("%02d", merchantAccountInfo.length)
            append(merchantAccountInfoLength)
            append(merchantAccountInfo)
            
            // Transaction Amount (54) - only for Dynamic QR
            append("54")
            val amountLength = String.format("%02d", amountString.length)
            append(amountLength)
            append(amountString)
            
            // Merchant Category Code (52)
            append("52")
            append("04") // Length
            append("0000") // General
            
            // Transaction Currency (53)
            append("53")
            append("03") // Length
            append("764") // THB (Thai Baht)
            
            // Country Code (58)
            append("58")
            append("02") // Length
            append("TH") // Thailand
        }
        
        // Calculate CRC16-CCITT
        val payloadWithoutCRC = payload.toString()
        val payloadForCRC = payloadWithoutCRC + "6304"
        val crc = calculateCRC16(payloadForCRC)
        val crcHex = String.format("%04X", crc)
        
        // Append CRC to complete the payload
        return payloadWithoutCRC + "63" + "04" + crcHex
    }
    
    /**
     * Calculate CRC16-CCITT checksum for EMV QR Code
     * Uses CRC-16-CCITT (polynomial 0x1021, initial value 0xFFFF)
     */
    private fun calculateCRC16(data: String): Int {
        var crc = 0xFFFF
        val polynomial = 0x1021
        
        for (byte in data.toByteArray(Charsets.ISO_8859_1)) {
            val unsignedByte = byte.toInt() and 0xFF
            crc = crc xor (unsignedByte shl 8)
            for (i in 0 until 8) {
                if ((crc and 0x8000) != 0) {
                    crc = ((crc shl 1) xor polynomial) and 0xFFFF
                } else {
                    crc = (crc shl 1) and 0xFFFF
                }
            }
        }
        
        return crc and 0xFFFF
    }
    
    /**
     * Generate QR code bitmap from text
     */
    private fun generateQRCodeBitmap(text: String, size: Int): Bitmap? {
        return try {
            val hints = Hashtable<EncodeHintType, Any>()
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.MARGIN] = 1

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }

            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
