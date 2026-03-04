package com.indybrain.indypos_Android.data.export

import android.content.Context
import android.util.Log
import com.indybrain.indypos_Android.data.local.dao.AddonDao
import com.indybrain.indypos_Android.data.local.dao.AddonGroupAddonJunctionDao
import com.indybrain.indypos_Android.data.local.dao.AddonGroupDao
import com.indybrain.indypos_Android.data.local.dao.CategoryDao
import com.indybrain.indypos_Android.data.local.dao.ProductAddonGroupJunctionDao
import com.indybrain.indypos_Android.data.local.dao.OrderDao
import com.indybrain.indypos_Android.data.local.dao.OrderItemDao
import com.indybrain.indypos_Android.data.local.dao.ProductDao
import com.indybrain.indypos_Android.data.local.entity.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis
import org.apache.poi.xddf.usermodel.chart.XDDFChartData
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis
import org.apache.poi.xddf.usermodel.XDDFLineProperties
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties
import org.apache.poi.xddf.usermodel.XDDFShapeProperties
import org.apache.poi.xssf.usermodel.XSSFChart
import org.apache.poi.xssf.usermodel.XSSFClientAnchor
import org.apache.poi.xssf.usermodel.XSSFDrawing
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
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
    private val context: Context,
    private val gson: Gson,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val addonDao: AddonDao,
    private val addonGroupDao: AddonGroupDao,
    private val addonGroupAddonJunctionDao: AddonGroupAddonJunctionDao,
    private val productAddonGroupJunctionDao: ProductAddonGroupJunctionDao,
    private val orderDao: OrderDao,
    private val orderItemDao: OrderItemDao
) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val fileNameTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    
    suspend fun exportAllData(format: ExportFormat): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val files = mutableListOf<File>()
            
            // Export basic data
            exportProducts(format)?.let { files.add(it) }
            exportCategories(format)?.let { files.add(it) }
            exportAddons(format)?.let { files.add(it) }
            exportAddonGroups(format)?.let { files.add(it) }
            exportOrders(format)?.let { files.add(it) }
            
            // Export reports
            exportSalesReport(format)?.let { files.addAll(it) }
            exportStockReport(format)?.let { files.addAll(it) }
            
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun exportDataByType(dataType: ExportDataType, format: ExportFormat): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val files = when (dataType) {
                ExportDataType.CATEGORIES -> {
                    val file = exportCategories(format)
                    if (file != null) listOf(file) else emptyList()
                }
                ExportDataType.PRODUCTS -> {
                    val file = exportProducts(format)
                    if (file != null) listOf(file) else emptyList()
                }
                ExportDataType.ADDON_GROUPS -> {
                    val file = exportAddonGroups(format)
                    if (file != null) listOf(file) else emptyList()
                }
                ExportDataType.ADDONS -> {
                    val file = exportAddons(format)
                    if (file != null) listOf(file) else emptyList()
                }
                ExportDataType.ORDERS -> {
                    val file = exportOrders(format)
                    if (file != null) listOf(file) else emptyList()
                }
                ExportDataType.SALES_REPORT -> {
                    exportSalesReport(format) ?: emptyList()
                }
                ExportDataType.STOCK_REPORT -> {
                    exportStockReport(format) ?: emptyList()
                }
            }
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun exportProducts(format: ExportFormat): File? = withContext(Dispatchers.IO) {
        try {
            val allProducts = productDao.getAllProducts()
            val products = allProducts.filter { !it.isDeletedLocally }
            val categories = categoryDao.getAllCategories().associateBy { it.id }
            val addonGroups = addonGroupDao.getAllAddonGroups().associateBy { it.id }
            
            Log.d("ExportService", "Product export: total=${allProducts.size}, non-deleted=${products.size}")
            
            // iOS column order: ID, Name, Price, Cost Price, Product Code, Unit, SKU Code, Stock Quantity,
            // Category Name, Selected Unit, Selected Color Hex, Addon Groups, Is Active, Created At
            val headers = arrayOf(
                "ID", "Name", "Price", "Cost Price", "Product Code", "Unit",
                "SKU Code", "Stock Quantity", "Category Name", "Selected Unit", "Selected Color Hex",
                "Addon Groups", "Is Active", "Created At"
            )
            
            val rows = products.map { product ->
                val categoryName = product.categoryId?.let { categories[it]?.name } ?: ""
                val addonGroupIds = productAddonGroupJunctionDao.getAddonGroupIdsByProductIdSync(product.id)
                val addonGroupNames = addonGroupIds.mapNotNull { addonGroups[it]?.name }
                val addonGroupsStr = addonGroupNames.joinToString(", ")
                
                arrayOf(
                    product.id,
                    product.name,
                    product.price.toString(),
                    product.costPrice?.toString() ?: "",
                    product.productCode ?: "",
                    product.unit ?: "",
                    product.skuCode ?: "",
                    product.stockQuantity?.toString() ?: "",
                    categoryName,
                    product.selectedUnit ?: "",
                    product.selectedColorHex ?: "",
                    addonGroupsStr,
                    product.isActive.toString(),
                    dateFormat.format(product.createdAt)
                )
            }
            
            createFile("Products", format, headers, rows)
        } catch (e: Exception) {
            Log.e("ExportService", "Export products failed: ${e.message}", e)
            null
        }
    }
    
    suspend fun exportCategories(format: ExportFormat): File? = withContext(Dispatchers.IO) {
        try {
            val categories = categoryDao.getAllCategories()
            val headers = arrayOf(
                "ID", "Name", "Is Active", "Created At"
            )
            
            val rows = categories.map { category ->
                arrayOf(
                    category.id,
                    category.name,
                    category.isActive.toString(),
                    dateFormat.format(category.createdAt)
                )
            }
            
            createFile("Categories", format, headers, rows)
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun exportAddons(format: ExportFormat): File? = withContext(Dispatchers.IO) {
        try {
            val allAddons = addonDao.getAllAddons()
            val addons = allAddons.filter { !it.isDeletedLocally }
            
            Log.d("ExportService", "Addon export: total=${allAddons.size}, non-deleted=${addons.size}")
            
            // iOS column order: ID, Name, Price, Is Active, Created At
            val headers = arrayOf(
                "ID", "Name", "Price", "Is Active", "Created At"
            )
            
            val rows = addons.map { addon ->
                arrayOf(
                    addon.id,
                    addon.name,
                    String.format(Locale.US, "%.2f", addon.price),
                    addon.isActive.toString(),
                    dateFormat.format(addon.createdAt)
                )
            }
            
            createFile("Addons", format, headers, rows)
        } catch (e: Exception) {
            Log.e("ExportService", "Export addons failed: ${e.message}", e)
            null
        }
    }
    
    suspend fun exportAddonGroups(format: ExportFormat): File? = withContext(Dispatchers.IO) {
        try {
            val addonGroups = addonGroupDao.getAllAddonGroups()
            val allAddons = addonDao.getAllAddons().associateBy { it.id }
            
            Log.d("ExportService", "AddonGroup export: count=${addonGroups.size}")
            
            // iOS column order: ID, Name, Is Required, Is Single Selection, Max Selection, Is Active,
            // Options Count, Options ID, Options Names, Created At
            val headers = arrayOf(
                "ID", "Name", "Is Required", "Is Single Selection", "Max Selection", "Is Active",
                "Options Count", "Options ID", "Options Names", "Created At"
            )
            
            val rows = addonGroups.map { group ->
                val addonIds = addonGroupAddonJunctionDao.getAddonIdsByAddonGroupIdSync(group.id)
                val addonNames = addonIds.mapNotNull { allAddons[it]?.name }
                val optionsCount = addonIds.size
                val optionsId = addonIds.joinToString(", ")
                val optionsNames = addonNames.joinToString(", ")
                
                arrayOf(
                    group.id,
                    group.name,
                    group.isRequired.toString(),
                    group.isSingleSelection.toString(),
                    group.maxSelection?.toString() ?: "",
                    group.isActive.toString(),
                    optionsCount.toString(),
                    optionsId,
                    optionsNames,
                    dateFormat.format(group.createdAt)
                )
            }
            
            createFile("AddonGroups", format, headers, rows)
        } catch (e: Exception) {
            Log.e("ExportService", "Export addon groups failed: ${e.message}", e)
            null
        }
    }
    
    suspend fun exportOrders(format: ExportFormat): File? = withContext(Dispatchers.IO) {
        try {
            val orders = orderDao.getAllOrdersSync()
            // 7 order-level columns + 7 item-level columns (including Cost Price)
            val headers = arrayOf(
                "Order Number", "Date", "Subtotal", "Discount", "Total", "Payment Type", "Status",
                "Product Name", "Quantity", "Price", "Cost Price", "Item Total", "Addons", "Special Request"
            )
            val rows = mutableListOf<Array<String>>()
            for (order in orders) {
                val orderCells = arrayOf(
                    order.orderNumber,
                    dateFormat.format(order.orderDate),
                    String.format(Locale.US, "%.2f", order.subtotal),
                    String.format(Locale.US, "%.2f", order.discount),
                    String.format(Locale.US, "%.2f", order.total),
                    getPaymentTypeText(order.paymentTypeRaw),
                    getStatusText(order.statusRaw)
                )
                val items = orderItemDao.getOrderItemsSync(order.id)
                if (items.isEmpty()) {
                    // Order with no items: one row with order info only, item columns empty
                    rows.add(orderCells + arrayOf("", "", "", "", "", "", ""))
                } else {
                    for (item in items) {
                        val addonsDisplay = formatAddonsForExport(item.addons)
                        rows.add(
                            orderCells + arrayOf(
                                item.productName,
                                item.quantity.toString(),
                                String.format(Locale.US, "%.2f", item.productUnitPrice),
                                String.format(Locale.US, "%.2f", item.unitCost ?: 0.0),
                                String.format(Locale.US, "%.2f", item.totalPrice),
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
    
    /**
     * Parse addons JSON and return only addon names (like iOS), e.g. "นมจืด, นม x 2, น้ำผึ้ง"
     */
    private fun formatAddonsForExport(addonsJson: String?): String {
        if (addonsJson.isNullOrEmpty()) return ""
        return try {
            val listType = object : TypeToken<List<Map<String, Any?>>>() {}.type
            @Suppress("UNCHECKED_CAST")
            val list = gson.fromJson<List<Map<String, Any?>>>(addonsJson, listType) ?: return ""
            if (list.isEmpty()) return ""
            val grouped = mutableMapOf<String, Int>()
            for (item in list) {
                val name = (item["addon_name"] ?: item["addonName"])?.toString()?.takeIf { it.isNotBlank() } ?: continue
                val qty = when (val q = item["quantity"]) {
                    is Number -> q.toInt()
                    else -> 1
                }
                grouped[name] = (grouped[name] ?: 0) + qty
            }
            grouped.map { (name, qty) ->
                if (qty > 1) "$name x$qty" else name
            }.joinToString(", ")
        } catch (e: Exception) {
            addonsJson
        }
    }
    
    suspend fun exportSalesReport(format: ExportFormat): List<File>? = withContext(Dispatchers.IO) {
        try {
            val allOrders = orderDao.getAllOrdersSync()
            val orders = allOrders.filter { it.statusRaw == 1 }
            
            when (format) {
                ExportFormat.CSV -> {
                    createSalesReportCsv(orders)
                }
                ExportFormat.EXCEL -> {
                    createSalesReportExcel(orders)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun createSalesReportCsv(orders: List<OrderEntity>): List<File>? {
        val totalRevenue = orders.sumOf { it.total }
        val orderCount = orders.size
        val averageOrder = if (orderCount > 0) totalRevenue / orderCount else 0.0
        
        val productSales = buildProductSales(orders)
        val paymentMethods = buildPaymentMethods(orders)
        
        val timestamp = fileNameTimestampFormat.format(Date())
        val file = File(context.getExternalFilesDir(null), "data_type_sales_report_$timestamp.csv")
        
        return try {
            FileOutputStream(file).use { out ->
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                out.flush()
                OutputStreamWriter(out, StandardCharsets.UTF_8).use { writer ->
                    val csvWriter = CSVWriter(writer)
                    csvWriter.writeNext(arrayOf("=== Sale Report ==="))
                    csvWriter.writeNext(arrayOf("Total Revenue", String.format(Locale.US, "%.2f", totalRevenue)))
                    csvWriter.writeNext(arrayOf("Total Orders", orderCount.toString()))
                    csvWriter.writeNext(arrayOf("Average Order Value", String.format(Locale.US, "%.2f", averageOrder)))
                    csvWriter.writeNext(emptyArray())
                    csvWriter.writeNext(arrayOf("=== Order History (ประวัติสั่งซื้อทั้งหมด) ==="))
                    csvWriter.writeNext(arrayOf("Order Number", "Date", "Total", "Payment Type", "Status"))
                    orders.forEach { order ->
                        csvWriter.writeNext(arrayOf(
                            order.orderNumber,
                            dateFormat.format(order.orderDate),
                            String.format(Locale.US, "%.2f", order.total),
                            getPaymentTypeText(order.paymentTypeRaw),
                            getStatusText(order.statusRaw)
                        ))
                    }
                    csvWriter.writeNext(emptyArray())
                    csvWriter.writeNext(arrayOf("=== Product Sales ==="))
                    csvWriter.writeNext(arrayOf("Product Name", "Quantity Sold", "Total Revenue"))
                    productSales.forEach { (name, qty, revenue) ->
                        csvWriter.writeNext(arrayOf(name, qty.toString(), String.format(Locale.US, "%.2f", revenue)))
                    }
                    csvWriter.writeNext(emptyArray())
                    csvWriter.writeNext(arrayOf("=== Payment Methods ==="))
                    csvWriter.writeNext(arrayOf("Payment Method", "Number of Orders", "Total Revenue", "Percentage"))
                    paymentMethods.forEach { (method, count, revenue, pct) ->
                        csvWriter.writeNext(arrayOf(method, count.toString(), String.format(Locale.US, "%.2f", revenue), pct))
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
    
    private suspend fun createSalesReportExcel(orders: List<OrderEntity>): List<File>? {
        val workbook = XSSFWorkbook()
        val totalRevenue = orders.sumOf { it.total }
        val orderCount = orders.size
        val productSales = buildProductSales(orders)
        val paymentMethods = buildPaymentMethods(orders)
        
        createSummarySheet(workbook, totalRevenue, orderCount)
        createOrderHistorySheet(workbook, orders)
        createProductSaleSheet(workbook, productSales)
        createPaymentMethodsSheet(workbook, paymentMethods)
        
        val file = File(context.getExternalFilesDir(null), "Sales_Report_${System.currentTimeMillis()}.xlsx")
        FileOutputStream(file).use { out ->
            workbook.write(out)
        }
        workbook.close()
        return listOf(file)
    }
    
    private fun createSummarySheet(workbook: XSSFWorkbook, totalRevenue: Double, orderCount: Int) {
        val sheet = workbook.createSheet("Summary")
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont()
            font.bold = true
            setFont(font)
        }
        val row0 = sheet.createRow(0)
        row0.createCell(0).apply { setCellValue("Sales Report Summary"); cellStyle = headerStyle }
        val row1 = sheet.createRow(1)
        row1.createCell(0).setCellValue("Total Revenue:")
        row1.createCell(1).setCellValue(String.format(Locale.US, "%.2f", totalRevenue))
        val row2 = sheet.createRow(2)
        row2.createCell(0).setCellValue("Total Orders")
        row2.createCell(1).setCellValue(orderCount.toString())
    }
    
    private fun createOrderHistorySheet(workbook: XSSFWorkbook, orders: List<OrderEntity>) {
        val sheet = workbook.createSheet("Order History")
        val headers = arrayOf("Order Number", "Date", "Total", "Payment Type", "Status")
        val rows = orders.map { order ->
            arrayOf(
                order.orderNumber,
                dateFormat.format(order.orderDate),
                String.format(Locale.US, "฿%.2f", order.total),
                getPaymentTypeText(order.paymentTypeRaw),
                getStatusText(order.statusRaw)
            )
        }
        createExcelSheet(sheet, headers, rows)
    }
    
    private fun createProductSaleSheet(workbook: XSSFWorkbook, productSales: List<Triple<String, Int, Double>>) {
        val sheet = workbook.createSheet("Product Sale")
        val headers = arrayOf("Product Name", "Quantity Sold", "Total Revenue")
        val rows = productSales.map { (name, qty, revenue) ->
            arrayOf(name, qty.toString(), String.format(Locale.US, "฿%.2f", revenue))
        }
        createExcelSheet(sheet, headers, rows)
    }
    
    private fun createPaymentMethodsSheet(workbook: XSSFWorkbook, paymentMethods: List<PaymentMethodRow>) {
        val sheet = workbook.createSheet("Payment Methods")
        val headers = arrayOf("Payment Method", "Number of Orders", "Total Revenue", "Percentage")
        val rows = paymentMethods.map { (method, count, revenue, pct) ->
            arrayOf(method, count.toString(), String.format(Locale.US, "฿%.2f", revenue), pct)
        }
        createExcelSheet(sheet, headers, rows)
        
        if (paymentMethods.isNotEmpty()) {
            addPaymentMethodsBarChart(sheet, paymentMethods)
        }
    }
    
    /**
     * Creates horizontal bar chart for Payment Methods using Apache POI.
     * Chart is built programmatically: data (categories + values) -> XDDFDataSource -> ChartData -> plot.
     * Text elements: title, axis labels, legend are set via chart.setTitleText(), valueAxis.setTitle(), series.setTitle().
     */
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
            
            val legend = chart.getOrAddLegend()
            legend.setPosition(LegendPosition.RIGHT)
            
            val categoryAxis = chart.createCategoryAxis(AxisPosition.LEFT)
            categoryAxis.setTitle("ช่องทางการจ่ายเงิน")
            
            val valueAxis = chart.createValueAxis(AxisPosition.BOTTOM)
            valueAxis.setTitle("ยอดขาย (บาท)")
            valueAxis.setCrosses(AxisCrosses.AUTO_ZERO)
            valueAxis.setCrossBetween(AxisCrossBetween.BETWEEN)
            valueAxis.setNumberFormat("฿#,##0.00")
            val maxVal = values.maxOrNull() ?: 0.0
            val minVal = values.minOrNull() ?: 0.0
            val dataRange = (maxVal - minVal).coerceAtLeast(1000.0)
            val majorUnit = roundToNiceInterval(dataRange / 5)
            val axisMin = (Math.floor((minVal - dataRange * 0.05).coerceAtLeast(0.0) / majorUnit) * majorUnit).toInt()
            val axisMax = (Math.ceil((maxVal + dataRange * 0.05) / majorUnit) * majorUnit).toInt()
            valueAxis.setMinimum(axisMin.toDouble())
            valueAxis.setMaximum(axisMax.toDouble())
            valueAxis.setMajorUnit(majorUnit)
            val gridProps = valueAxis.getOrAddMajorGridProperties()
            val gridLine = XDDFLineProperties(XDDFSolidFillProperties(org.apache.poi.xddf.usermodel.XDDFColor.from(org.apache.poi.xddf.usermodel.PresetColor.LIGHT_GRAY)))
            gridProps.setLineProperties(gridLine)
            
            val catDataSource = XDDFDataSourcesFactory.fromArray(categories)
            val valDataSource = XDDFDataSourcesFactory.fromArray(values)
            
            val barData = chart.createData(ChartTypes.BAR, categoryAxis, valueAxis) as XDDFBarChartData
            barData.setBarDirection(BarDirection.BAR)
            barData.setVaryColors(false)
            val series = barData.addSeries(catDataSource, valDataSource)
            series.setTitle("ยอดขาย", null)
            chart.plot(barData)
        } catch (e: Exception) {
            Log.e("ExportService", "Error adding Payment Methods chart: ${e.message}", e)
        }
    }
    
    private fun roundToNiceInterval(value: Double): Double {
        if (value <= 0) return 1000.0
        val magnitude = Math.pow(10.0, Math.floor(Math.log10(value)))
        val normalized = value / magnitude
        val nice = when {
            normalized <= 1 -> 1.0
            normalized <= 2 -> 2.0
            normalized <= 5 -> 5.0
            else -> 10.0
        }
        return nice * magnitude
    }
    
    private data class PaymentMethodRow(val method: String, val count: Int, val revenue: Double, val percentage: String)
    
    private suspend fun buildProductSales(orders: List<OrderEntity>): List<Triple<String, Int, Double>> {
        val productMap = mutableMapOf<String, Pair<Int, Double>>()
        for (order in orders) {
            val items = orderItemDao.getOrderItemsSync(order.id)
            for (item in items) {
                val name = item.productName
                val existing = productMap[name] ?: Pair(0, 0.0)
                productMap[name] = Pair(existing.first + item.quantity, existing.second + item.totalPrice)
            }
        }
        return productMap.entries
            .map { (name, p) -> Triple(name, p.first, p.second) }
            .sortedByDescending { it.third }
    }
    
    private fun buildPaymentMethods(orders: List<OrderEntity>): List<PaymentMethodRow> {
        val totalRevenue = orders.sumOf { it.total }
        val byType = orders.groupBy { getPaymentTypeText(it.paymentTypeRaw) }
        return byType.map { (method, list) ->
            val count = list.size
            val revenue = list.sumOf { it.total }
            val pct = if (totalRevenue > 0) String.format(Locale.US, "%.1f%%", revenue / totalRevenue * 100) else "0%"
            PaymentMethodRow(method, count, revenue, pct)
        }.sortedByDescending { it.revenue }
    }
    
    suspend fun exportStockReport(format: ExportFormat): List<File>? = withContext(Dispatchers.IO) {
        try {
            val products = productDao.getAllProducts()
            val allProducts = products.filter { it.isStockEnabled == true }
            val lowStock = products.filter { 
                it.isStockEnabled == true && 
                it.stockQuantity != null && 
                it.minStockQuantity != null &&
                it.stockQuantity <= it.minStockQuantity
            }
            val outOfStock = products.filter { 
                it.isStockEnabled == true && 
                (it.stockQuantity == null || it.stockQuantity <= 0)
            }
            
            when (format) {
                ExportFormat.CSV -> {
                    val files = mutableListOf<File>()
                    
                    // Sheet 1: All Products
                    val allProductsHeaders = arrayOf(
                        "ID", "Name", "Product Code", "SKU Code", "Stock Quantity", "Min Stock", "Unit", "Price"
                    )
                    val allProductsRows = allProducts.map { product ->
                        arrayOf(
                            product.id,
                            product.name,
                            product.productCode ?: "",
                            product.skuCode ?: "",
                            product.stockQuantity?.toString() ?: "0",
                            product.minStockQuantity?.toString() ?: "",
                            product.unit ?: "",
                            product.price.toString()
                        )
                    }
                    createFile("Stock_Report_All_Products", format, allProductsHeaders, allProductsRows)?.let { files.add(it) }
                    
                    // Sheet 2: Low Stock
                    val lowStockHeaders = arrayOf(
                        "ID", "Name", "Product Code", "SKU Code", "Stock Quantity", "Min Stock", "Unit", "Price"
                    )
                    val lowStockRows = lowStock.map { product ->
                        arrayOf(
                            product.id,
                            product.name,
                            product.productCode ?: "",
                            product.skuCode ?: "",
                            product.stockQuantity?.toString() ?: "0",
                            product.minStockQuantity?.toString() ?: "",
                            product.unit ?: "",
                            product.price.toString()
                        )
                    }
                    createFile("Stock_Report_Low_Stock", format, lowStockHeaders, lowStockRows)?.let { files.add(it) }
                    
                    // Sheet 3: Out of Stock
                    val outOfStockHeaders = arrayOf(
                        "ID", "Name", "Product Code", "SKU Code", "Stock Quantity", "Min Stock", "Unit", "Price"
                    )
                    val outOfStockRows = outOfStock.map { product ->
                        arrayOf(
                            product.id,
                            product.name,
                            product.productCode ?: "",
                            product.skuCode ?: "",
                            product.stockQuantity?.toString() ?: "0",
                            product.minStockQuantity?.toString() ?: "",
                            product.unit ?: "",
                            product.price.toString()
                        )
                    }
                    createFile("Stock_Report_Out_of_Stock", format, outOfStockHeaders, outOfStockRows)?.let { files.add(it) }
                    
                    files
                }
                ExportFormat.EXCEL -> {
                    val workbook = XSSFWorkbook()
                    
                    // Sheet 1: All Products
                    val allProductsSheet = workbook.createSheet("All Products")
                    val allProductsHeaders = arrayOf(
                        "ID", "Name", "Product Code", "SKU Code", "Stock Quantity", "Min Stock", "Unit", "Price"
                    )
                    val allProductsRows = allProducts.map { product ->
                        arrayOf(
                            product.id,
                            product.name,
                            product.productCode ?: "",
                            product.skuCode ?: "",
                            product.stockQuantity?.toString() ?: "0",
                            product.minStockQuantity?.toString() ?: "",
                            product.unit ?: "",
                            product.price.toString()
                        )
                    }
                    createExcelSheet(allProductsSheet, allProductsHeaders, allProductsRows)
                    
                    // Sheet 2: Low Stock
                    val lowStockSheet = workbook.createSheet("Low Stock")
                    val lowStockHeaders = arrayOf(
                        "ID", "Name", "Product Code", "SKU Code", "Stock Quantity", "Min Stock", "Unit", "Price"
                    )
                    val lowStockRows = lowStock.map { product ->
                        arrayOf(
                            product.id,
                            product.name,
                            product.productCode ?: "",
                            product.skuCode ?: "",
                            product.stockQuantity?.toString() ?: "0",
                            product.minStockQuantity?.toString() ?: "",
                            product.unit ?: "",
                            product.price.toString()
                        )
                    }
                    createExcelSheet(lowStockSheet, lowStockHeaders, lowStockRows)
                    
                    // Sheet 3: Out of Stock
                    val outOfStockSheet = workbook.createSheet("Out of Stock")
                    val outOfStockHeaders = arrayOf(
                        "ID", "Name", "Product Code", "SKU Code", "Stock Quantity", "Min Stock", "Unit", "Price"
                    )
                    val outOfStockRows = outOfStock.map { product ->
                        arrayOf(
                            product.id,
                            product.name,
                            product.productCode ?: "",
                            product.skuCode ?: "",
                            product.stockQuantity?.toString() ?: "0",
                            product.minStockQuantity?.toString() ?: "",
                            product.unit ?: "",
                            product.price.toString()
                        )
                    }
                    createExcelSheet(outOfStockSheet, outOfStockHeaders, outOfStockRows)
                    
                    val file = File(context.getExternalFilesDir(null), "Stock_Report_${System.currentTimeMillis()}.xlsx")
                    FileOutputStream(file).use { out ->
                        workbook.write(out)
                    }
                    workbook.close()
                    
                    listOf(file)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun createFile(
        fileName: String,
        format: ExportFormat,
        headers: Array<String>,
        rows: List<Array<String>>
    ): File? {
        return try {
            val extension = if (format == ExportFormat.CSV) "csv" else "xlsx"
            val timestamp = fileNameTimestampFormat.format(Date())
            val baseName = "INDYPOS_${fileName}_$timestamp"
            val file = File(context.getExternalFilesDir(null), "$baseName.$extension")
            
            when (format) {
                ExportFormat.CSV -> {
                    FileOutputStream(file).use { out ->
                        // UTF-8 BOM is required for Excel (especially Windows) to recognize Thai/special chars
                        out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                        out.flush()
                        OutputStreamWriter(out, StandardCharsets.UTF_8).use { writer ->
                            val csvWriter = CSVWriter(writer)
                            csvWriter.writeNext(headers)
                            rows.forEach { row ->
                                csvWriter.writeNext(row)
                            }
                            csvWriter.flush()
                            csvWriter.close()
                        }
                    }
                }
                ExportFormat.EXCEL -> {
                    val workbook = XSSFWorkbook()
                    val sheet = workbook.createSheet(fileName)
                    createExcelSheet(sheet, headers, rows)
                    
                    FileOutputStream(file).use { out ->
                        workbook.write(out)
                    }
                    workbook.close()
                }
            }
            
            file
        } catch (e: Exception) {
            Log.e("ExportService", "Error creating file: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
    
    private fun createExcelSheet(
        sheet: Sheet,
        headers: Array<String>,
        rows: List<Array<String>>
    ) {
        try {
            val headerStyle = sheet.workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                val font = sheet.workbook.createFont()
                font.bold = true
                setFont(font)
            }
            
            // Create header row
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }
            
            // Create data rows
            rows.forEachIndexed { rowIndex, rowData ->
                val row = sheet.createRow(rowIndex + 1)
                rowData.forEachIndexed { colIndex, value ->
                    val cell = row.createCell(colIndex)
                    cell.setCellValue(value)
                }
            }
            
            // Note: autoSizeColumn() is not available on Android due to AWT dependencies
            // Columns will use default width, which is usually sufficient
        } catch (e: Exception) {
            Log.e("ExportService", "Error creating Excel sheet: ${e.message}", e)
            e.printStackTrace()
            throw e
        }
    }
    
    private fun getPaymentTypeText(paymentTypeRaw: Int): String {
        return when (paymentTypeRaw) {
            0 -> "Cash"
            1 -> "Transfer"
            2 -> "Card"
            3 -> "QR Code"
            else -> "Unknown"
        }
    }
    
    private fun getStatusText(statusRaw: Int): String {
        return when (statusRaw) {
            0 -> "Draft"
            1 -> "Confirmed"
            2 -> "Preparing"
            3 -> "Ready"
            4 -> "Delivered"
            5 -> "Cancelled"
            else -> "Unknown"
        }
    }
}

