package com.indybrain.indypos_Android.core.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import net.posprinter.POSPrinter
import net.posprinter.POSConst
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for printing labels using TSPL (TSC Printer Language)
 * Supports XP 420B and other TSPL-compatible label printers
 * According to Android TSPL Program Manual
 */
@Singleton
class LabelPrinterService @Inject constructor(
    private val printerManager: PrinterManager,
    @ApplicationContext private val context: Context
) {
    
    companion object {
        // Label size constants (in mm)
        private const val LABEL_WIDTH = 40  // 40mm width
        private const val LABEL_HEIGHT = 30 // 30mm height
        
        // DPI for XP 420B (203 DPI)
        private const val DPI = 203
        
        // Convert mm to dots
        private fun mmToDots(mm: Int): Int = (mm * DPI) / 25
    }
    
    /**
     * Print label for a single cart item
     * Uses BITMAP method to support Thai language
     * Format:
     * - Shop name
     * - Product name (bold, large)
     * - Quantity
     * - Addons (if any)
     * - Special request (if any)
     */
    fun printLabel(
        cartItem: CartItemEntity,
        addons: List<CartAddonEntity>,
        shopName: String
    ): Boolean {
        val connection = printerManager.getCurrentConnection(PrinterType.LABEL)
        if (connection?.isConnect != true) {
            android.util.Log.e("LabelPrinter", "Printer not connected")
            return false
        }
        
        return try {
            val posPrinter = POSPrinter(connection)
            
            // Create bitmap with Thai text
            val bitmap = createLabelBitmap(cartItem, addons, shopName)
            
            android.util.Log.d("LabelPrinter", "Printing label for: ${cartItem.productName}")
            android.util.Log.d("LabelPrinter", "Bitmap size: ${bitmap.width}x${bitmap.height}")
            
            // Convert bitmap to TSPL BITMAP command manually
            val tsplCommands = buildTSPLCommandsWithBitmap(bitmap)
            
            android.util.Log.d("LabelPrinter", "TSPL commands length: ${tsplCommands.length} bytes")
            
            // Send all TSPL commands as one string
            posPrinter.printString(tsplCommands)
            
            // Clean up
            bitmap.recycle()
            
            true
        } catch (e: Exception) {
            android.util.Log.e("LabelPrinter", "Error printing label", e)
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Create bitmap for label with Thai language support
     * Uses Android Canvas to render Thai text properly
     */
    private fun createLabelBitmap(
        cartItem: CartItemEntity,
        addons: List<CartAddonEntity>,
        shopName: String
    ): Bitmap {
        val widthPixels = mmToDots(LABEL_WIDTH)
        val heightPixels = mmToDots(LABEL_HEIGHT)
        
        android.util.Log.d("LabelPrinter", "Creating label bitmap ${widthPixels}x${heightPixels}px")
        android.util.Log.d("LabelPrinter", "Product: ${cartItem.productName}, Qty: ${cartItem.quantity}")
        
        val bitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Fill with white background
        canvas.drawColor(Color.WHITE)
        
        val paint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
            color = Color.BLACK
            // Use SANS_SERIF for better Thai font support
            typeface = Typeface.SANS_SERIF
        }
        
        val margin = 15f
        var yPosition = 25f
        val maxWidth = widthPixels - (margin * 2)
        
        // Shop name (small)
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        drawTextWithWrap(canvas, shopName, margin, yPosition, maxWidth, paint)
        yPosition += 30f
        
        // Separator line
        paint.strokeWidth = 2f
        canvas.drawLine(margin, yPosition, widthPixels - margin, yPosition, paint)
        yPosition += 15f
        
        // Product name (large, bold) - รองรับภาษาไทย
        paint.textSize = 36f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val productName = cartItem.productName ?: "Unknown"
        val productHeight = drawTextWithWrap(canvas, productName, margin, yPosition, maxWidth, paint)
        yPosition += productHeight + 10f
        
        // Quantity (medium)
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        canvas.drawText("จำนวน: ${cartItem.quantity}", margin, yPosition, paint)
        yPosition += 35f
        
        // Addons (small) - รองรับภาษาไทย
        if (addons.isNotEmpty()) {
            paint.textSize = 22f
            val addonCounts = addons.groupBy { it.addonName }
                .mapValues { (_, list) -> list.size }
            val addonTexts = addonCounts.map { (name, count) ->
                if (count > 1) "$name x$count" else name
            }
            val addonText = "+ " + addonTexts.joinToString(", ")
            val addonHeight = drawTextWithWrap(canvas, addonText, margin, yPosition, maxWidth, paint)
            yPosition += addonHeight + 5f
        }
        
        // Special request - รองรับภาษาไทย
        if (!cartItem.specialRequest.isNullOrEmpty()) {
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            val noteText = "หมายเหตุ: ${cartItem.specialRequest}"
            drawTextWithWrap(canvas, noteText, margin, yPosition, maxWidth, paint)
        }
        
        return bitmap
    }
    
    /**
     * Draw text with word wrap support for long text
     * Returns the total height used
     */
    private fun drawTextWithWrap(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint
    ): Float {
        var currentY = y
        val words = text.split(" ")
        var currentLine = ""
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val textWidth = paint.measureText(testLine)
            
            if (textWidth > maxWidth && currentLine.isNotEmpty()) {
                // Draw current line and move to next
                canvas.drawText(currentLine, x, currentY, paint)
                currentY += paint.textSize + 5f
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        
        // Draw remaining text
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, paint)
            currentY += paint.textSize
        }
        
        return currentY - y
    }
    
    
    /**
     * Print labels for multiple cart items
     * Prints one label per item based on quantity
     */
    fun printLabels(
        cartItems: List<CartItemEntity>,
        cartAddonsMap: Map<String, List<CartAddonEntity>>,
        shopName: String
    ): Boolean {
        var allSuccess = true
        
        cartItems.forEach { cartItem ->
            val addons = cartAddonsMap[cartItem.id] ?: emptyList()
            
            // Print label for each quantity
            repeat(cartItem.quantity) {
                val success = printLabel(cartItem, addons, shopName)
                if (!success) {
                    allSuccess = false
                }
            }
        }
        
        return allSuccess
    }
    
    
    /**
     * Test print - print a test label with Thai language support
     * Uses BITMAP method to ensure Thai text displays correctly
     */
    fun testPrint(shopName: String): Boolean {
        val connection = printerManager.getCurrentConnection(PrinterType.LABEL)
        if (connection?.isConnect != true) {
            android.util.Log.e("LabelPrinter", "Printer not connected for test print")
            return false
        }
        
        return try {
            val posPrinter = POSPrinter(connection)
            
            // Create test label bitmap with Thai text
            val bitmap = createTestLabelBitmap(shopName)
            
            android.util.Log.d("LabelPrinter", "Sending test print...")
            android.util.Log.d("LabelPrinter", "Bitmap size: ${bitmap.width}x${bitmap.height}")
            
            // Convert bitmap to complete TSPL commands
            val tsplCommands = buildTSPLCommandsWithBitmap(bitmap)
            
            android.util.Log.d("LabelPrinter", "TSPL commands length: ${tsplCommands.length} bytes")
            
            // Send all TSPL commands as one string
            posPrinter.printString(tsplCommands)
            
            android.util.Log.d("LabelPrinter", "Test print completed successfully")
            
            // Clean up
            bitmap.recycle()
            
            true
        } catch (e: Exception) {
            android.util.Log.e("LabelPrinter", "Error in test print", e)
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Build complete TSPL commands with embedded bitmap
     * This is the most basic and reliable method
     */
    private fun buildTSPLCommandsWithBitmap(bitmap: Bitmap): String {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8 // Round up to nearest byte
        
        android.util.Log.d("LabelPrinter", "Converting bitmap: ${width}x${height}, widthBytes=$widthBytes")
        
        // Convert bitmap to monochrome byte array
        val bitmapData = StringBuilder()
        
        for (y in 0 until height) {
            for (xByte in 0 until widthBytes) {
                var byteValue = 0
                for (bit in 0 until 8) {
                    val x = xByte * 8 + bit
                    if (x < width) {
                        val pixel = bitmap.getPixel(x, y)
                        val r = Color.red(pixel)
                        val g = Color.green(pixel)
                        val b = Color.blue(pixel)
                        
                        // Calculate brightness (0-255)
                        val brightness = (r + g + b) / 3
                        
                        // If dark (less than 128), set bit to 1
                        if (brightness < 128) {
                            byteValue = byteValue or (0x80 shr bit)
                        }
                    }
                }
                bitmapData.append(String.format("%02X", byteValue))
            }
        }
        
        android.util.Log.d("LabelPrinter", "Bitmap data hex length: ${bitmapData.length}")
        
        // Build complete TSPL command
        return buildString {
            // Setup commands
            append("SIZE $LABEL_WIDTH mm,$LABEL_HEIGHT mm\r\n")
            append("GAP 3 mm,0 mm\r\n")
            append("DIRECTION 1,0\r\n")
            append("REFERENCE 0,0\r\n")
            append("OFFSET 0 mm\r\n")
            append("SET TEAR ON\r\n")
            append("CLS\r\n")
            
            // BITMAP command
            // Format: BITMAP x, y, width_bytes, height, mode, bitmap_data
            // mode 0 = OVERWRITE, mode 1 = OR, mode 2 = XOR
            append("BITMAP 0,0,$widthBytes,$height,0,")
            append(bitmapData)
            append("\r\n")
            
            // Print command
            append("PRINT 1,1\r\n")
        }
    }
    
    /**
     * Create test label bitmap with Thai language
     */
    private fun createTestLabelBitmap(shopName: String): Bitmap {
        val widthPixels = mmToDots(LABEL_WIDTH)
        val heightPixels = mmToDots(LABEL_HEIGHT)
        
        val bitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Fill with white background
        canvas.drawColor(Color.WHITE)
        
        val paint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
            color = Color.BLACK
            typeface = Typeface.DEFAULT
        }
        
        var yPosition = 30f
        
        // Shop name
        paint.textSize = 24f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(shopName, 10f, yPosition, paint)
        yPosition += 40f
        
        // Test label title
        paint.textSize = 36f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("TEST LABEL", 10f, yPosition, paint)
        yPosition += 50f
        
        // Thai test text
        paint.textSize = 28f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("ทดสอบภาษาไทย", 10f, yPosition, paint)
        yPosition += 40f
        
        // Additional Thai text
        paint.textSize = 20f
        canvas.drawText("กาแฟร้อน + นมสด", 10f, yPosition, paint)
        
        return bitmap
    }
}
