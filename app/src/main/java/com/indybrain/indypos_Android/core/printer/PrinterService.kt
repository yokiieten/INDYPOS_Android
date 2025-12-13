package com.indybrain.indypos_Android.core.printer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.text.SimpleDateFormat
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
            
            // Set character encoding for Thai language support
            // Try different encodings commonly used for Thai printers
            // According to Android POS Program Manual section 2.26
            var charsetSet = false
            val charsets = listOf("TIS-620", "Windows-874", "UTF-8", "ISO-8859-11")
            
            for (charset in charsets) {
                try {
                    posPrinter.setCharSet(charset)
                    charsetSet = true
                    break
                } catch (e: Exception) {
                    // Failed to set charset, try next one
                }
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
                
                // Print item with quantity and price on the same line
                // Format: "1 x น้ำเปล่า                    10.00"
                val itemLine = formatItemLine(
                    label = "${cartItem.quantity} x $itemName",
                    price = formatCurrencyWithoutSymbol(itemPrice)
                )
                posPrinter.printText(
                    "$itemLine\n",
                    ALIGNMENT_LEFT,
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
            
            // Print payment type (no price, but format for consistency)
            val paymentTypeText = when (paymentType) {
                PaymentType.CASH -> "เงินสด"
                PaymentType.TRANSFER -> "โอนเงิน"
                PaymentType.CARD -> "บัตรเครดิต"
                PaymentType.QR_CODE -> "QR Code"
            }
            val paymentLine = formatItemLine(
                label = "วิธีการชำระเงิน: $paymentTypeText",
                price = ""
            )
            posPrinter.printText(
                "$paymentLine\n",
                ALIGNMENT_LEFT,
                FNT_DEFAULT,
                TXT_1WIDTH or TXT_1HEIGHT
            )
            
            // Print subtotal
            val subtotalLine = formatItemLine(
                label = "ยอดรวมราคา:",
                price = formatCurrencyWithoutSymbol(subtotal)
            )
            posPrinter.printText(
                "$subtotalLine\n",
                ALIGNMENT_LEFT,
                FNT_DEFAULT,
                TXT_1WIDTH or TXT_1HEIGHT
            )
            
            // Print discount
            if (discount > 0) {
                val discountLine = formatItemLine(
                    label = "ส่วนลด:",
                    price = "-${formatCurrencyWithoutSymbol(discount)}"
                )
                posPrinter.printText(
                    "$discountLine\n",
                    ALIGNMENT_LEFT,
                    FNT_DEFAULT,
                    TXT_1WIDTH or TXT_1HEIGHT
                )
            }
            
            // Print total - bold
            val totalLine = formatItemLine(
                label = "รวม:",
                price = formatCurrencyWithoutSymbol(total)
            )
            posPrinter.printText(
                "$totalLine\n",
                ALIGNMENT_LEFT,
                FNT_BOLD,
                TXT_1WIDTH or TXT_1HEIGHT
            )
            
            // Print cash received and change if payment type is cash
            if (paymentType == PaymentType.CASH) {
                receivedAmount?.let {
                    if (it > 0) {
                        val receivedLine = formatItemLine(
                            label = "เงินที่รับ:",
                            price = formatCurrencyWithoutSymbol(it)
                        )
                        posPrinter.printText(
                            "$receivedLine\n",
                            ALIGNMENT_LEFT,
                            FNT_DEFAULT,
                            TXT_1WIDTH or TXT_1HEIGHT
                        )
                    }
                }
                
                change?.let {
                    val changeLine = formatItemLine(
                        label = "เงินทอน:",
                        price = formatCurrencyWithoutSymbol(it)
                    )
                    posPrinter.printText(
                        "$changeLine\n",
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
                        posPrinter.feedLine(1)
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
     * Format price with fixed width for alignment
     * Ensures all prices have the same display width
     */
    private fun formatPriceFixedWidth(value: Double): String {
        val formatted = formatCurrencyWithoutSymbol(value)
        // Ensure consistent width by padding if needed (though DecimalFormat should handle this)
        return formatted
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
        
        // Receipt width for 58mm paper is typically 32 characters
        // Use a consistent maxWidth for all items to ensure prices align
        // This width should match the actual printable width of the receipt
        // Using 32 characters as standard width for 58mm thermal paper
        val maxWidth = 32
        
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
