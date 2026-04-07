package com.indybrain.indypos_Android.data.export

import android.content.Context
import android.util.Log
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.data.remote.dto.*
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xddf.usermodel.chart.AxisCrossBetween
import org.apache.poi.xddf.usermodel.chart.AxisCrosses
import org.apache.poi.xddf.usermodel.chart.AxisPosition
import org.apache.poi.xddf.usermodel.chart.BarDirection
import org.apache.poi.xddf.usermodel.chart.ChartTypes
import org.apache.poi.xddf.usermodel.chart.LegendPosition
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory
import org.apache.poi.xddf.usermodel.XDDFLineProperties
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

enum class ExportFormat {
    CSV,
    EXCEL
}

@Singleton
class ExportService @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext
    private val context: Context
) {
    
    private val fileNameTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private val spaceDateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)

    /**
     * Timestamps in exported cell values and file names use the **device default timezone**
     * ([TimeZone.getDefault] / [ZoneId.systemDefault]), not a fixed zone such as `Asia/Bangkok`.
     */
    private fun deviceTimeZone(): TimeZone = TimeZone.getDefault()

    /** `yyyyMMdd_HHmmss` in the device zone (refreshed each call if the user changes zone). */
    private fun formatExportFileNameTimestamp(date: Date = Date()): String {
        fileNameTimestampFormat.timeZone = deviceTimeZone()
        return fileNameTimestampFormat.format(date)
    }

    /**
     * Parses API date strings into an instant. Supports ISO-8601 with/without offset,
     * fractional seconds, minute-only precision (no :ss), date-only, and "yyyy-MM-dd HH:mm:ss".
     * Strings without an offset are interpreted as **local wall time on this device**
     * ([ZoneId.systemDefault]), not UTC or Thailand.
     */
    private fun parseApiDateToInstant(raw: String): Instant? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        try {
            return OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
        } catch (_: DateTimeException) { /* try next */ }
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        } catch (_: DateTimeException) { /* try next */ }
        try {
            return LocalDateTime.parse(s, spaceDateTimeFormatter)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        } catch (_: DateTimeException) { /* try next */ }
        try {
            return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
        } catch (_: DateTimeException) { /* try next */ }
        return null
    }

    private fun formatDate(isoDate: String): String {
        val trimmed = isoDate.trim()
        if (trimmed.isEmpty()) return ""
        val instant = parseApiDateToInstant(trimmed) ?: return isoDate
        val isThai = Locale.getDefault().language == "th"
        val displayLocale = if (isThai) Locale("th", "TH") else Locale.US
        return SimpleDateFormat("dd/MM/yyyy HH:mm:ss", displayLocale).apply {
            timeZone = deviceTimeZone()
        }.format(Date.from(instant))
    }

    /** Stock report export: หมด ≤0, ใกล้หมด 1…5, พอเพียง >5 (localized). */
    private fun stockReportStatusLabel(currentStock: Int): String = when {
        currentStock <= 0 -> context.getString(R.string.export_stock_status_out)
        currentStock <= 5 -> context.getString(R.string.export_stock_status_low)
        else -> context.getString(R.string.export_stock_status_sufficient)
    }

    // ──────────────────────── Products ────────────────────────

    suspend fun exportProducts(
        products: List<ProductDto>,
        categories: List<CategoryDto>,
        addonGroups: List<AddonGroupDto>,
        format: ExportFormat
    ): File? = withContext(Dispatchers.IO) {
        try {
            val categoryMap = categories.associateBy { it.id }

            val headers = arrayOf(
                "ID", "Name", "Price", "Cost Price", "Product Code", "Unit",
                "SKU Code", "Stock Quantity", "Category Name", "Selected Unit", "Selected Color Hex",
                "Addon Groups", "Is Active", "Created At"
            )

            val baseRows = products.map { product ->
                val categoryName = (product.categoryId?.let { categoryMap[it]?.name }
                    ?: product.category?.name) ?: ""
                val addonGroupNames = product.addonGroups?.map { it.name } ?: emptyList()
                val addonGroupsStr = addonGroupNames.joinToString("; ")

                arrayOf(
                    product.id,
                    product.name,
                    String.format(Locale.US, "฿%,.2f", product.price),
                    product.costPrice?.let { String.format(Locale.US, "฿%,.2f", it) } ?: "",
                    product.productCode ?: "",
                    product.unit ?: "",
                    product.skuCode ?: "",
                    product.stockQuantity?.toString() ?: "",
                    categoryName,
                    product.selectedUnit ?: "",
                    product.selectedColorHex ?: "",
                    addonGroupsStr,
                    product.isActive.toString(),
                    formatDate(product.createdAt)
                )
            }

            val rowsForFormat = if (format == ExportFormat.CSV) {
                baseRows.map { row ->
                    row.copyOf().also { copy ->
                        if (copy[4].isNotEmpty()) { copy[4] = "'${copy[4]}'" }
                    }
                }
            } else {
                baseRows
            }

            createFile("Products", format, headers, rowsForFormat)
        } catch (e: Exception) {
            Log.e("ExportService", "Export products failed: ${e.message}", e)
            null
        }
    }

    // ──────────────────────── Categories ────────────────────────

    suspend fun exportCategories(
        categories: List<CategoryDto>,
        format: ExportFormat
    ): File? = withContext(Dispatchers.IO) {
        try {
            val headers = arrayOf("ID", "Name", "Is Active", "Created At")
            val rows = categories.map { cat ->
                arrayOf(cat.id, cat.name, cat.isActive.toString(), formatDate(cat.createdAt))
            }
            createFile("Categories", format, headers, rows)
        } catch (e: Exception) {
            null
        }
    }

    // ──────────────────────── Addons ────────────────────────

    suspend fun exportAddons(
        addons: List<AddonDto>,
        format: ExportFormat
    ): File? = withContext(Dispatchers.IO) {
        try {
            val headers = arrayOf("ID", "Name", "Price", "Is Active", "Created At")
            val rows = addons.map { addon ->
                arrayOf(
                    addon.id,
                    addon.name,
                    String.format(Locale.US, "฿%,.2f", addon.price),
                    addon.isActive.toString(),
                    formatDate(addon.createdAt)
                )
            }
            createFile("Addons", format, headers, rows)
        } catch (e: Exception) {
            Log.e("ExportService", "Export addons failed: ${e.message}", e)
            null
        }
    }

    // ──────────────────────── Addon Groups ────────────────────────

    suspend fun exportAddonGroups(
        addonGroups: List<AddonGroupDto>,
        format: ExportFormat
    ): File? = withContext(Dispatchers.IO) {
        try {
            val headers = arrayOf(
                "ID", "Name", "Is Required", "Max Selection", "Is Active",
                "Options Count", "Options ID", "Options Names", "Created At"
            )

            val rows = addonGroups.map { group ->
                val addons = group.addons ?: emptyList()
                arrayOf(
                    group.id,
                    group.name,
                    group.isRequired.toString(),
                    group.maxSelection?.toString() ?: "",
                    group.isActive.toString(),
                    addons.size.toString(),
                    addons.joinToString(", ") { it.id },
                    addons.joinToString("; ") { it.name },
                    formatDate(group.createdAt)
                )
            }
            createFile("AddonGroups", format, headers, rows)
        } catch (e: Exception) {
            Log.e("ExportService", "Export addon groups failed: ${e.message}", e)
            null
        }
    }

    // ──────────────────────── Orders ────────────────────────

    suspend fun exportOrders(
        orders: List<OrderDto>,
        format: ExportFormat
    ): File? = withContext(Dispatchers.IO) {
        try {
            val headers = arrayOf(
                "Order Number", "Date", "Subtotal", "Discount", "Total", "Payment Type", "Status",
                "Product Name", "Quantity", "Price", "Cost Price", "Item Total", "Addons", "Special Request"
            )
            val rows = mutableListOf<Array<String>>()
            for (order in orders) {
                val orderCells = arrayOf(
                    order.orderNumber,
                    formatDate(order.orderDate),
                    String.format(Locale.US, "฿%,.2f", order.subtotal),
                    String.format(Locale.US, "฿%,.2f", order.discountAmount),
                    String.format(Locale.US, "฿%,.2f", order.total),
                    getPaymentTypeText(order.paymentType),
                    getStatusText(order.orderStatus)
                )
                val items = order.items ?: emptyList()
                if (items.isEmpty()) {
                    rows.add(orderCells + arrayOf("", "", "", "", "", "", ""))
                } else {
                    for (item in items) {
                        val addonsDisplay = formatAddonsFromDto(item.addons)
                        rows.add(
                            orderCells + arrayOf(
                                item.productName,
                                item.quantity.toString(),
                                String.format(Locale.US, "฿%,.2f", item.unitPrice),
                                String.format(Locale.US, "฿%,.2f", item.unitCost),
                                String.format(Locale.US, "฿%,.2f", item.totalPrice),
                                addonsDisplay,
                                item.specialRequest ?: ""
                            )
                        )
                    }
                }
            }
            createFile("Orders", format, headers, rows)
        } catch (e: Exception) {
            Log.e("ExportService", "Export orders failed: ${e.message}", e)
            null
        }
    }

    private fun formatAddonsFromDto(addons: List<OrderAddonDto>?): String {
        if (addons.isNullOrEmpty()) return ""
        val grouped = mutableMapOf<String, Int>()
        for (addon in addons) {
            val name = addon.addonName.takeIf { it.isNotBlank() } ?: continue
            grouped[name] = (grouped[name] ?: 0) + addon.quantity
        }
        return grouped.map { (name, qty) -> if (qty > 1) "$name x$qty" else name }.joinToString(", ")
    }

    // ──────────────────────── Sales Report ────────────────────────

    suspend fun exportSalesReport(
        orders: List<OrderDto>,
        format: ExportFormat
    ): List<File>? = withContext(Dispatchers.IO) {
        try {
            val confirmed = orders.filter { it.orderStatus == 1 }
            when (format) {
                ExportFormat.CSV -> createSalesReportCsv(confirmed)
                ExportFormat.EXCEL -> createSalesReportExcel(confirmed)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun createSalesReportCsv(orders: List<OrderDto>): List<File>? {
        val totalRevenue = orders.sumOf { it.total }
        val orderCount = orders.size
        val averageOrder = if (orderCount > 0) totalRevenue / orderCount else 0.0
        val productSales = buildProductSalesFromDto(orders)
        val paymentMethods = buildPaymentMethodsFromDto(orders)
        val timestamp = formatExportFileNameTimestamp()
        val file = File(context.cacheDir, "data_type_sales_report_$timestamp.csv")

        return try {
            FileOutputStream(file).use { out ->
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                out.flush()
                OutputStreamWriter(out, StandardCharsets.UTF_8).use { writer ->
                    val csvWriter = CSVWriter(writer)
                    csvWriter.writeNext(arrayOf("=== Sale Report ==="))
                    csvWriter.writeNext(arrayOf("Total Revenue", String.format(Locale.US, "฿%,.2f", totalRevenue)))
                    csvWriter.writeNext(arrayOf("Total Orders", orderCount.toString()))
                    csvWriter.writeNext(arrayOf("Average Order Value", String.format(Locale.US, "฿%,.2f", averageOrder)))
                    csvWriter.writeNext(emptyArray())
                    csvWriter.writeNext(arrayOf("=== Order History (ประวัติสั่งซื้อทั้งหมด) ==="))
                    csvWriter.writeNext(arrayOf("Order Number", "Date", "Total", "Payment Type", "Status"))
                    orders.forEach { order ->
                        csvWriter.writeNext(arrayOf(
                            order.orderNumber,
                            formatDate(order.orderDate),
                            String.format(Locale.US, "฿%,.2f", order.total),
                            getPaymentTypeText(order.paymentType),
                            getStatusText(order.orderStatus)
                        ))
                    }
                    csvWriter.writeNext(emptyArray())
                    csvWriter.writeNext(arrayOf("=== Product Sales ==="))
                    csvWriter.writeNext(arrayOf("Product Name", "Quantity Sold", "Total Revenue"))
                    productSales.forEach { (name, qty, revenue) ->
                        csvWriter.writeNext(arrayOf(name, qty.toString(), String.format(Locale.US, "฿%,.2f", revenue)))
                    }
                    csvWriter.writeNext(emptyArray())
                    csvWriter.writeNext(arrayOf("=== Payment Methods ==="))
                    csvWriter.writeNext(arrayOf("Payment Method", "Number of Orders", "Total Revenue", "Percentage"))
                    paymentMethods.forEach { (method, count, revenue, pct) ->
                        csvWriter.writeNext(arrayOf(method, count.toString(), String.format(Locale.US, "฿%,.2f", revenue), pct))
                    }
                    csvWriter.flush()
                    csvWriter.close()
                }
            }
            listOf(file)
        } catch (e: Exception) {
            Log.e("ExportService", "Error creating sales CSV: ${e.message}", e)
            null
        }
    }

    private fun createSalesReportExcel(orders: List<OrderDto>): List<File>? {
        val workbook = XSSFWorkbook()
        val totalRevenue = orders.sumOf { it.total }
        val orderCount = orders.size
        val averageOrder = if (orderCount > 0) totalRevenue / orderCount else 0.0
        val productSales = buildProductSalesFromDto(orders)
        val paymentMethods = buildPaymentMethodsFromDto(orders)

        createSummarySheet(workbook, totalRevenue, orderCount, averageOrder)
        createOrderHistorySheet(workbook, orders)
        createProductSaleSheet(workbook, productSales)
        createPaymentMethodsSheet(workbook, paymentMethods)

        val file = File(context.cacheDir, "Sales_Report_${System.currentTimeMillis()}.xlsx")
        FileOutputStream(file).use { out -> workbook.write(out) }
        workbook.close()
        return listOf(file)
    }

    // ──────────────────────── Stock Report ────────────────────────

    suspend fun exportStockReport(
        products: List<ProductDto>,
        categories: List<CategoryDto>,
        format: ExportFormat
    ): List<File>? = withContext(Dispatchers.IO) {
        try {
            val categoryMap = categories.associateBy { it.id }
            val stockProducts = products.filter { it.isStockEnabled == true }

            val headers = arrayOf("Product Name", "Product Code", "Current Stock", "Status", "Category")
            val rows = stockProducts.map { product ->
                val currentStock = product.stockQuantity ?: 0
                val status = stockReportStatusLabel(currentStock)
                val categoryName = (product.categoryId?.let { categoryMap[it]?.name }
                    ?: product.category?.name) ?: ""
                arrayOf(
                    product.name,
                    product.productCode ?: "",
                    String.format(Locale.US, "%,d", currentStock),
                    status,
                    categoryName
                )
            }

            when (format) {
                ExportFormat.CSV -> {
                    val timestamp = formatExportFileNameTimestamp()
                    val file = File(context.cacheDir, "data_type_stock_report_$timestamp.csv")
                    FileOutputStream(file).use { out ->
                        out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                        out.flush()
                        OutputStreamWriter(out, StandardCharsets.UTF_8).use { writer ->
                            val csvWriter = CSVWriter(writer)
                            csvWriter.writeNext(headers)
                            rows.forEach { csvWriter.writeNext(it) }
                            csvWriter.flush()
                            csvWriter.close()
                        }
                    }
                    listOf(file)
                }
                ExportFormat.EXCEL -> {
                    val workbook = XSSFWorkbook()
                    val sheet = workbook.createSheet("Stock Report")
                    createExcelSheet(sheet, headers, rows)
                    val file = File(context.cacheDir, "data_type_stock_report_${System.currentTimeMillis()}.xlsx")
                    FileOutputStream(file).use { out -> workbook.write(out) }
                    workbook.close()
                    listOf(file)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    // ──────────────────────── Helpers (Sales Report) ────────────────────────

    private fun buildProductSalesFromDto(orders: List<OrderDto>): List<Triple<String, Int, Double>> {
        val map = mutableMapOf<String, Pair<Int, Double>>()
        for (order in orders) {
            for (item in order.items ?: emptyList()) {
                val existing = map[item.productName] ?: Pair(0, 0.0)
                map[item.productName] = Pair(existing.first + item.quantity, existing.second + item.totalPrice)
            }
        }
        return map.entries.map { (n, p) -> Triple(n, p.first, p.second) }.sortedByDescending { it.third }
    }

    private data class PaymentMethodRow(val method: String, val count: Int, val revenue: Double, val percentage: String)

    private fun buildPaymentMethodsFromDto(orders: List<OrderDto>): List<PaymentMethodRow> {
        val totalRevenue = orders.sumOf { it.total }
        val byType = orders.groupBy { getPaymentTypeText(it.paymentType) }
        return byType.map { (method, list) ->
            val count = list.size
            val revenue = list.sumOf { it.total }
            val pct = if (totalRevenue > 0) String.format(Locale.US, "%.1f%%", revenue / totalRevenue * 100) else "0%"
            PaymentMethodRow(method, count, revenue, pct)
        }.sortedByDescending { it.revenue }
    }

    // ──────────────────────── Excel helper sheets ────────────────────────

    private fun createSummarySheet(workbook: XSSFWorkbook, totalRevenue: Double, orderCount: Int, averageOrder: Double) {
        val sheet = workbook.createSheet("Summary")
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont(); font.bold = true; setFont(font)
        }
        sheet.createRow(0).createCell(0).apply { setCellValue("Sales Report Summary"); cellStyle = headerStyle }
        sheet.createRow(1).also { it.createCell(0).setCellValue("Total Revenue:"); it.createCell(1).setCellValue(String.format(Locale.US, "฿%,.2f", totalRevenue)) }
        sheet.createRow(2).also { it.createCell(0).setCellValue("Total Orders"); it.createCell(1).setCellValue(orderCount.toString()) }
        sheet.createRow(3).also { it.createCell(0).setCellValue("Average Order Value:"); it.createCell(1).setCellValue(String.format(Locale.US, "฿%,.2f", averageOrder)) }
    }

    private fun createOrderHistorySheet(workbook: XSSFWorkbook, orders: List<OrderDto>) {
        val sheet = workbook.createSheet("Order History")
        val headers = arrayOf("Order Number", "Date", "Total", "Payment Type", "Status")
        val rows = orders.map { o ->
            arrayOf(o.orderNumber, formatDate(o.orderDate), String.format(Locale.US, "฿%,.2f", o.total), getPaymentTypeText(o.paymentType), getStatusText(o.orderStatus))
        }
        createExcelSheet(sheet, headers, rows)
    }

    private fun createProductSaleSheet(workbook: XSSFWorkbook, productSales: List<Triple<String, Int, Double>>) {
        val sheet = workbook.createSheet("Product Sale")
        val headers = arrayOf("Product Name", "Quantity Sold", "Total Revenue")
        val rows = productSales.map { (name, qty, revenue) -> arrayOf(name, qty.toString(), String.format(Locale.US, "฿%,.2f", revenue)) }
        createExcelSheet(sheet, headers, rows)
    }

    private fun createPaymentMethodsSheet(workbook: XSSFWorkbook, paymentMethods: List<PaymentMethodRow>) {
        val sheet = workbook.createSheet("Payment Methods")
        val headers = arrayOf("Payment Method", "Number of Orders", "Total Revenue", "Percentage")
        val rows = paymentMethods.map { (method, count, revenue, pct) -> arrayOf(method, count.toString(), String.format(Locale.US, "฿%,.2f", revenue), pct) }
        createExcelSheet(sheet, headers, rows)
        if (paymentMethods.isNotEmpty()) addPaymentMethodsBarChart(sheet, paymentMethods)
    }

    private fun addPaymentMethodsBarChart(sheet: Sheet, paymentMethods: List<PaymentMethodRow>) {
        try {
            val sorted = paymentMethods.sortedByDescending { it.revenue }
            val categories = sorted.map { it.method }.toTypedArray()
            val values = sorted.map { it.revenue }.toTypedArray()
            val drawing = (sheet as org.apache.poi.xssf.usermodel.XSSFSheet).createDrawingPatriarch()
            val anchor = drawing.createAnchor(0, 0, 0, 0, 7, 0, 12, 14)
            val chart = drawing.createChart(anchor)
            chart.setTitleText("ยอดขายตามช่องทางการจ่ายเงิน")
            chart.setTitleOverlay(false)
            chart.getOrAddLegend().setPosition(LegendPosition.RIGHT)
            val categoryAxis = chart.createCategoryAxis(AxisPosition.LEFT); categoryAxis.setTitle("ช่องทางการจ่ายเงิน")
            val valueAxis = chart.createValueAxis(AxisPosition.BOTTOM); valueAxis.setTitle("ยอดขาย (บาท)")
            valueAxis.setCrosses(AxisCrosses.AUTO_ZERO); valueAxis.setCrossBetween(AxisCrossBetween.BETWEEN)
            valueAxis.setNumberFormat("฿#,##0.00")
            val maxVal = values.maxOrNull() ?: 0.0; val minVal = values.minOrNull() ?: 0.0
            val dataRange = (maxVal - minVal).coerceAtLeast(1000.0)
            val majorUnit = roundToNiceInterval(dataRange / 5)
            valueAxis.setMinimum((Math.floor((minVal - dataRange * 0.05).coerceAtLeast(0.0) / majorUnit) * majorUnit))
            valueAxis.setMaximum((Math.ceil((maxVal + dataRange * 0.05) / majorUnit) * majorUnit))
            valueAxis.setMajorUnit(majorUnit)
            val gridLine = XDDFLineProperties(XDDFSolidFillProperties(org.apache.poi.xddf.usermodel.XDDFColor.from(org.apache.poi.xddf.usermodel.PresetColor.LIGHT_GRAY)))
            valueAxis.getOrAddMajorGridProperties().setLineProperties(gridLine)
            val barData = chart.createData(ChartTypes.BAR, categoryAxis, valueAxis) as XDDFBarChartData
            barData.setBarDirection(BarDirection.BAR); barData.setVaryColors(false)
            barData.addSeries(XDDFDataSourcesFactory.fromArray(categories), XDDFDataSourcesFactory.fromArray(values)).setTitle("ยอดขาย", null)
            chart.plot(barData)
        } catch (e: Exception) {
            Log.e("ExportService", "Error adding Payment Methods chart: ${e.message}", e)
        }
    }

    private fun roundToNiceInterval(value: Double): Double {
        if (value <= 0) return 1000.0
        val magnitude = Math.pow(10.0, Math.floor(Math.log10(value)))
        val normalized = value / magnitude
        val nice = when { normalized <= 1 -> 1.0; normalized <= 2 -> 2.0; normalized <= 5 -> 5.0; else -> 10.0 }
        return nice * magnitude
    }

    // ──────────────────────── File creation ────────────────────────

    private fun createFile(fileName: String, format: ExportFormat, headers: Array<String>, rows: List<Array<String>>): File? {
        return try {
            val extension = if (format == ExportFormat.CSV) "csv" else "xlsx"
            val timestamp = formatExportFileNameTimestamp()
            val file = File(context.cacheDir, "INDYPOS_${fileName}_$timestamp.$extension")
            when (format) {
                ExportFormat.CSV -> {
                    FileOutputStream(file).use { out ->
                        out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())); out.flush()
                        OutputStreamWriter(out, StandardCharsets.UTF_8).use { writer ->
                            val csvWriter = CSVWriter(writer)
                            csvWriter.writeNext(headers); rows.forEach { csvWriter.writeNext(it) }
                            csvWriter.flush(); csvWriter.close()
                        }
                    }
                }
                ExportFormat.EXCEL -> {
                    val workbook = XSSFWorkbook()
                    createExcelSheet(workbook.createSheet(fileName), headers, rows)
                    FileOutputStream(file).use { out -> workbook.write(out) }; workbook.close()
                }
            }
            file
        } catch (e: Exception) {
            Log.e("ExportService", "Error creating file: ${e.message}", e); null
        }
    }

    private fun createExcelSheet(sheet: Sheet, headers: Array<String>, rows: List<Array<String>>) {
        val headerStyle = sheet.workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = sheet.workbook.createFont(); font.bold = true; setFont(font)
        }
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h -> headerRow.createCell(i).apply { setCellValue(h); cellStyle = headerStyle } }
        rows.forEachIndexed { ri, rd -> val row = sheet.createRow(ri + 1); rd.forEachIndexed { ci, v -> row.createCell(ci).setCellValue(v) } }
    }

    private fun getPaymentTypeText(paymentTypeRaw: Int): String = when (paymentTypeRaw) {
        0 -> "Cash"; 1 -> "Transfer"; 2 -> "Card"; 3 -> "QR Code"; else -> "Unknown"
    }

    private fun getStatusText(statusRaw: Int): String = when (statusRaw) {
        0 -> "Draft"; 1 -> "Confirmed"; 2 -> "Preparing"; 3 -> "Ready"; 4 -> "Delivered"; 5 -> "Cancelled"; else -> "Unknown"
    }
}
