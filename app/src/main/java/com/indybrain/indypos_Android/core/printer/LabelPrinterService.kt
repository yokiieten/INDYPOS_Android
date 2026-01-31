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
import net.posprinter.TSPLPrinter
import net.posprinter.TSPLConst
import net.posprinter.model.AlgorithmType
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
        private const val LABEL_WIDTH = 40.0  // 40mm width
        private const val LABEL_HEIGHT = 30.0 // 30mm height
        
        // DPI for XP 420B and similar TSPL printers (203 DPI)
        private const val DPI = 203
        
        // Gap between labels (in mm)
        private const val LABEL_GAP = 3.0
        
        // Printer settings
        private const val PRINT_DENSITY = 8  // 0-15, 8 is medium (default)
        private const val PRINT_SPEED = 4.0  // Speed in inches per second
        
        // Convert mm to dots (1 inch = 25.4mm)
        private fun mmToDots(mm: Double): Int = (mm * DPI / 25.4).toInt()
    }
    
    /**
     * Print label for a single cart item
     * Uses TSPLPrinter API with BITMAP method to support Thai language
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
            android.util.Log.e("LabelPrinter", "❌ Printer not connected")
            return false
        }
        
        return try {
            android.util.Log.d("LabelPrinter", "📄 Preparing label: ${cartItem.productName}")
            
            // Use TSPLPrinter API from SDK
            val tsplPrinter = TSPLPrinter(connection)
            
            // Create bitmap with Thai text
            val bitmap = createLabelBitmap(cartItem, addons, shopName)
            
            android.util.Log.d("LabelPrinter", "  Bitmap: ${bitmap.width}x${bitmap.height}px")
            android.util.Log.d("LabelPrinter", "  Addons: ${addons.size}, Special: ${!cartItem.specialRequest.isNullOrEmpty()}")
            
            // Configure and print using TSPLPrinter API
            tsplPrinter
                .sizeMm(LABEL_WIDTH, LABEL_HEIGHT)  // Set label size to 40x30mm
                .gapMm(LABEL_GAP, 0.0)              // Set gap between labels (3mm gap, 0mm offset)
                .direction(TSPLConst.DIRECTION_FORWARD, false)  // Direction: forward, no mirror
                .reference(0, 0)                    // Set reference point to (0,0)
                .cls()                              // Clear image buffer
                .density(PRINT_DENSITY)             // Set print darkness (0-15, 8 is medium)
                .speed(PRINT_SPEED)                 // Set print speed (4 ips)
                .bitmap(0, 0, TSPLConst.BMP_MODE_OVERWRITE, mmToDots(LABEL_WIDTH), bitmap, AlgorithmType.Threshold)
                .print(1)                           // Print 1 copy
            
            android.util.Log.d("LabelPrinter", "  ✓ Print command sent successfully")
            
            // Clean up
            bitmap.recycle()
            
            true
        } catch (e: Exception) {
            android.util.Log.e("LabelPrinter", "❌ Error printing label: ${cartItem.productName}", e)
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Create bitmap for label with Thai language support
     * Uses Android Canvas to render Thai text properly
     * Size: 40x30mm (approximately 315x236 pixels at 203 DPI)
     */
    private fun createLabelBitmap(
        cartItem: CartItemEntity,
        addons: List<CartAddonEntity>,
        shopName: String
    ): Bitmap {
        val widthPixels = mmToDots(LABEL_WIDTH)
        val heightPixels = mmToDots(LABEL_HEIGHT)
        
        android.util.Log.d("LabelPrinter", "Creating label bitmap ${widthPixels}x${heightPixels}px (40x30mm)")
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
            flags = 0  // Clear all flags
            isUnderlineText = false  // Explicitly disable underline
            isStrikeThruText = false  // Explicitly disable strikethrough
            style = Paint.Style.FILL  // Use fill style for text
        }
        
        val margin = 10f
        var yPosition = 22f
        val maxWidth = widthPixels - (margin * 2)
        
        // Shop name (small)
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.flags = 0  // Reset flags
        paint.isUnderlineText = false
        paint.isStrikeThruText = false
        paint.style = Paint.Style.FILL  // Ensure fill style
        drawTextWithWrap(canvas, shopName, margin, yPosition, maxWidth, paint)
        yPosition += 30f  // ลบเส้นแบ่งออก เพิ่มระยะห่างแทน
        
        // Product name (large, bold) - รองรับภาษาไทย
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.flags = 0  // Reset all paint flags
        paint.isUnderlineText = false  // Explicitly disable underline
        paint.isStrikeThruText = false  // Explicitly disable strikethrough
        paint.style = Paint.Style.FILL  // Ensure fill style
        val productName = cartItem.productName ?: "Unknown"
        val productHeight = drawTextWithWrap(canvas, productName, margin, yPosition, maxWidth, paint)
        yPosition += productHeight + 8f
        
        // Quantity (medium)
        paint.textSize = 26f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.flags = 0  // Reset flags
        paint.isUnderlineText = false
        paint.isStrikeThruText = false
        paint.style = Paint.Style.FILL
        canvas.drawText("จำนวน: ${cartItem.quantity}", margin, yPosition, paint)
        yPosition += 32f
        
        // Addons (small) - รองรับภาษาไทย
        if (addons.isNotEmpty()) {
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            paint.flags = 0  // Reset flags
            paint.isUnderlineText = false
            paint.isStrikeThruText = false
            paint.style = Paint.Style.FILL
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
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            paint.flags = 0  // Reset flags
            paint.isUnderlineText = false
            paint.isStrikeThruText = false
            paint.style = Paint.Style.FILL
            val noteText = "หมายเหตุ: ${cartItem.specialRequest}"
            drawTextWithWrap(canvas, noteText, margin, yPosition, maxWidth, paint)
        }
        
        return bitmap
    }
    
    /**
     * Draw text with word wrap support for long text
     * Supports Thai language and mixed Thai-English text
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
        if (text.isEmpty()) return 0f
        
        var currentY = y
        val lineSpacing = 5f
        
        // Split by spaces first (for English words)
        val segments = text.split(" ")
        var currentLine = ""
        
        for (segment in segments) {
            val testLine = if (currentLine.isEmpty()) segment else "$currentLine $segment"
            val textWidth = paint.measureText(testLine)
            
            if (textWidth > maxWidth) {
                if (currentLine.isNotEmpty()) {
                    // Draw current line
                    canvas.drawText(currentLine, x, currentY, paint)
                    currentY += paint.textSize + lineSpacing
                    currentLine = segment
                    
                    // Check if even the single segment is too long
                    if (paint.measureText(segment) > maxWidth) {
                        // Break segment character by character
                        currentLine = ""
                        for (char in segment) {
                            val testChar = currentLine + char
                            if (paint.measureText(testChar) > maxWidth && currentLine.isNotEmpty()) {
                                canvas.drawText(currentLine, x, currentY, paint)
                                currentY += paint.textSize + lineSpacing
                                currentLine = char.toString()
                            } else {
                                currentLine = testChar
                            }
                        }
                    }
                } else {
                    // First segment is too long, break it character by character
                    for (char in segment) {
                        val testChar = currentLine + char
                        if (paint.measureText(testChar) > maxWidth && currentLine.isNotEmpty()) {
                            canvas.drawText(currentLine, x, currentY, paint)
                            currentY += paint.textSize + lineSpacing
                            currentLine = char.toString()
                        } else {
                            currentLine = testChar
                        }
                    }
                }
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
     * Uses TSPLPrinter API for reliable printing
     */
    fun printLabels(
        cartItems: List<CartItemEntity>,
        cartAddonsMap: Map<String, List<CartAddonEntity>>,
        shopName: String
    ): Boolean {
        android.util.Log.d("LabelPrinter", "=== Starting batch label printing ===")
        android.util.Log.d("LabelPrinter", "Total cart items: ${cartItems.size}")
        android.util.Log.d("LabelPrinter", "Shop name: $shopName")
        
        var allSuccess = true
        var totalLabels = 0
        var successCount = 0
        
        cartItems.forEach { cartItem ->
            val addons = cartAddonsMap[cartItem.id] ?: emptyList()
            val quantity = cartItem.quantity
            totalLabels += quantity
            
            android.util.Log.d("LabelPrinter", "Printing ${quantity}x labels for: ${cartItem.productName}")
            
            // Print label for each quantity
            repeat(quantity) { index ->
                val success = printLabel(cartItem, addons, shopName)
                if (success) {
                    successCount++
                    android.util.Log.d("LabelPrinter", "  ✓ Label ${index + 1}/${quantity} printed")
                } else {
                    allSuccess = false
                    android.util.Log.e("LabelPrinter", "  ✗ Label ${index + 1}/${quantity} failed")
                }
            }
        }
        
        android.util.Log.d("LabelPrinter", "=== Batch printing completed: $successCount/$totalLabels successful ===")
        
        return allSuccess
    }
    
    
    /**
     * Test print - print a test label with Thai language support
     * Uses TSPLPrinter API with BITMAP method to ensure Thai text displays correctly
     */
    fun testPrint(shopName: String): Boolean {
        val connection = printerManager.getCurrentConnection(PrinterType.LABEL)
        if (connection?.isConnect != true) {
            android.util.Log.e("LabelPrinter", "Printer not connected for test print")
            return false
        }
        
        return try {
            // Use TSPLPrinter API from SDK instead of manual commands
            val tsplPrinter = TSPLPrinter(connection)
            
            // Create test label bitmap with Thai text
            val bitmap = createTestLabelBitmap(shopName)
            
            android.util.Log.d("LabelPrinter", "Sending test print...")
            android.util.Log.d("LabelPrinter", "Bitmap size: ${bitmap.width}x${bitmap.height}")
            
            // Configure and print using TSPLPrinter API
            tsplPrinter
                .sizeMm(LABEL_WIDTH, LABEL_HEIGHT)  // Set label size to 40x30mm
                .gapMm(LABEL_GAP, 0.0)              // Set gap between labels (3mm gap, 0mm offset)
                .direction(TSPLConst.DIRECTION_FORWARD, false)  // Direction: forward, no mirror
                .reference(0, 0)                    // Set reference point to (0,0)
                .cls()                              // Clear image buffer
                .density(PRINT_DENSITY)             // Set print darkness (0-15, 8 is medium)
                .speed(PRINT_SPEED)                 // Set print speed (4 ips)
                .bitmap(0, 0, TSPLConst.BMP_MODE_OVERWRITE, mmToDots(LABEL_WIDTH), bitmap, AlgorithmType.Threshold)
                .print(1)                           // Print 1 copy
            
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
     * Create test label bitmap with Thai language
     * Size: 40x30mm (approximately 315x236 pixels at 203 DPI)
     */
    private fun createTestLabelBitmap(shopName: String): Bitmap {
        val widthPixels = mmToDots(LABEL_WIDTH)
        val heightPixels = mmToDots(LABEL_HEIGHT)
        
        android.util.Log.d("LabelPrinter", "Creating test bitmap: ${widthPixels}x${heightPixels}px for 40x30mm label")
        android.util.Log.d("LabelPrinter", "DPI: $DPI, Label: ${LABEL_WIDTH}x${LABEL_HEIGHT}mm")
        
        // Create bitmap with ARGB_8888 for best quality
        val bitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Fill with white background
        canvas.drawColor(Color.WHITE)
        
        val paint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER  // Center align for better appearance
            color = Color.BLACK
            // Use SANS_SERIF for better Thai language support
            typeface = Typeface.SANS_SERIF
            flags = 0  // Clear all flags
            isUnderlineText = false  // Explicitly disable underline
            isStrikeThruText = false  // Explicitly disable strikethrough
            style = Paint.Style.FILL  // Use fill style for text
        }
        
        val centerX = widthPixels / 2f
        var yPosition = 35f
        
        // Shop name (top)
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.flags = 0  // Reset flags
        paint.isUnderlineText = false
        paint.isStrikeThruText = false
        paint.style = Paint.Style.FILL  // Ensure fill style
        canvas.drawText(shopName, centerX, yPosition, paint)
        yPosition += 45f  // ลบเส้นแบ่งออก เพิ่มระยะห่างแทน
        
        // Main Thai text (large and bold)
        paint.textSize = 40f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.flags = 0  // Reset all paint flags
        paint.isUnderlineText = false
        paint.isStrikeThruText = false
        paint.style = Paint.Style.FILL  // Ensure fill style
        canvas.drawText("ทดสอบพิมพ์", centerX, yPosition, paint)
        yPosition += 50f
        
        // Thai text example
        paint.textSize = 32f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.flags = 0  // Reset flags
        paint.isUnderlineText = false
        paint.isStrikeThruText = false
        paint.style = Paint.Style.FILL  // Ensure fill style
        canvas.drawText("กาแฟร้อน", centerX, yPosition, paint)
        yPosition += 40f
        
        // Label size info
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.flags = 0  // Reset flags
        paint.isUnderlineText = false
        paint.isStrikeThruText = false
        paint.style = Paint.Style.FILL  // Ensure fill style
        canvas.drawText("40 x 30 mm", centerX, yPosition, paint)
        
        android.util.Log.d("LabelPrinter", "Test bitmap created successfully")
        
        return bitmap
    }
}
