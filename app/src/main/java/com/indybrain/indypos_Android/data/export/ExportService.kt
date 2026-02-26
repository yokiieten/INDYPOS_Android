package com.indybrain.indypos_Android.data.export

import android.content.Context
import android.util.Log
import com.indybrain.indypos_Android.data.local.dao.AddonDao
import com.indybrain.indypos_Android.data.local.dao.AddonGroupDao
import com.indybrain.indypos_Android.data.local.dao.CategoryDao
import com.indybrain.indypos_Android.data.local.dao.ProductAddonGroupJunctionDao
import com.indybrain.indypos_Android.data.local.dao.OrderDao
import com.indybrain.indypos_Android.data.local.dao.OrderItemDao
import com.indybrain.indypos_Android.data.local.dao.ProductDao
import com.indybrain.indypos_Android.data.local.entity.*
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
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
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val addonDao: AddonDao,
    private val addonGroupDao: AddonGroupDao,
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
            val addons = addonDao.getAllAddons()
            val headers = arrayOf(
                "ID", "Name", "Price", "Addon Group ID", "Is Active", "Sort Order", "Created At", "Updated At"
            )
            
            val rows = addons.map { addon ->
                arrayOf(
                    addon.id,
                    addon.name,
                    addon.price.toString(),
                    addon.addonGroupId ?: "",
                    addon.isActive.toString(),
                    addon.sortOrder?.toString() ?: "",
                    dateFormat.format(addon.createdAt),
                    dateFormat.format(addon.updatedAt)
                )
            }
            
            createFile("Addons", format, headers, rows)
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun exportAddonGroups(format: ExportFormat): File? = withContext(Dispatchers.IO) {
        try {
            val addonGroups = addonGroupDao.getAllAddonGroups()
            val headers = arrayOf(
                "ID", "Name", "Is Required", "Is Single Selection", "Min Selection", "Max Selection",
                "Is Active", "Sort Order", "Created At", "Updated At"
            )
            
            val rows = addonGroups.map { group ->
                arrayOf(
                    group.id,
                    group.name,
                    group.isRequired.toString(),
                    group.isSingleSelection.toString(),
                    group.minSelection?.toString() ?: "",
                    group.maxSelection?.toString() ?: "",
                    group.isActive.toString(),
                    group.sortOrder?.toString() ?: "",
                    dateFormat.format(group.createdAt),
                    dateFormat.format(group.updatedAt)
                )
            }
            
            createFile("AddonGroups", format, headers, rows)
        } catch (e: Exception) {
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
                    order.subtotal.toString(),
                    order.discount.toString(),
                    order.total.toString(),
                    getPaymentTypeText(order.paymentTypeRaw),
                    getStatusText(order.statusRaw)
                )
                val items = orderItemDao.getOrderItemsSync(order.id)
                if (items.isEmpty()) {
                    // Order with no items: one row with order info only, item columns empty
                    rows.add(orderCells + arrayOf("", "", "", "", "", "", ""))
                } else {
                    for (item in items) {
                        rows.add(
                            orderCells + arrayOf(
                                item.productName,
                                item.quantity.toString(),
                                item.productUnitPrice.toString(),
                                item.unitCost?.toString() ?: "",
                                item.totalPrice.toString(),
                                item.addons ?: "",
                                item.specialRequest ?: ""
                            )
                        )
                    }
                }
            }
            createFile("Orders", format, headers, rows)
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun exportSalesReport(format: ExportFormat): List<File>? = withContext(Dispatchers.IO) {
        try {
            val orders = orderDao.getAllOrdersSync()
            
            when (format) {
                ExportFormat.CSV -> {
                    // For CSV, create separate files for each sheet
                    val files = mutableListOf<File>()
                    
                    // Sheet 1: Order List
                    val orderListHeaders = arrayOf(
                        "Order ID", "Order Number", "Order Date", "Customer Name", "Customer Phone",
                        "Subtotal", "Discount", "Total", "Payment Type", "Status"
                    )
                    val orderListRows = orders.map { order ->
                        arrayOf(
                            order.id,
                            order.orderNumber,
                            dateFormat.format(order.orderDate),
                            order.customerName ?: "",
                            order.customerPhone ?: "",
                            order.subtotal.toString(),
                            order.discount.toString(),
                            order.total.toString(),
                            getPaymentTypeText(order.paymentTypeRaw),
                            getStatusText(order.statusRaw)
                        )
                    }
                    createFile("Sales_Report_Order_List", format, orderListHeaders, orderListRows)?.let { files.add(it) }
                    
                    // Sheet 2: Sales Summary
                    val totalSales = orders.sumOf { it.total }
                    val orderCount = orders.size
                    val averageOrder = if (orderCount > 0) totalSales / orderCount else 0.0
                    val salesSummaryHeaders = arrayOf("Metric", "Value")
                    val salesSummaryRows = listOf(
                        arrayOf("Total Sales", totalSales.toString()),
                        arrayOf("Total Orders", orderCount.toString()),
                        arrayOf("Average Order Value", averageOrder.toString())
                    )
                    createFile("Sales_Report_Summary", format, salesSummaryHeaders, salesSummaryRows)?.let { files.add(it) }
                    
                    // Sheet 3: Sales By Date
                    val salesByDate = orders.groupBy { dateOnlyFormat.format(it.orderDate) }
                        .map { (date, dateOrders) ->
                            val dateTotal = dateOrders.sumOf { it.total }
                            arrayOf(date, dateOrders.size.toString(), dateTotal.toString())
                        }
                    val salesByDateHeaders = arrayOf("Date", "Order Count", "Total Sales")
                    createFile("Sales_Report_By_Date", format, salesByDateHeaders, salesByDate)?.let { files.add(it) }
                    
                    files
                }
                ExportFormat.EXCEL -> {
                    // For Excel, create one file with multiple sheets
                    val workbook = XSSFWorkbook()
                    
                    // Sheet 1: Order List
                    val orderListSheet = workbook.createSheet("Order List")
                    val orderListHeaders = arrayOf(
                        "Order ID", "Order Number", "Order Date", "Customer Name", "Customer Phone",
                        "Subtotal", "Discount", "Total", "Payment Type", "Status"
                    )
                    createExcelSheet(orderListSheet, orderListHeaders, orders.map { order ->
                        arrayOf(
                            order.id,
                            order.orderNumber,
                            dateFormat.format(order.orderDate),
                            order.customerName ?: "",
                            order.customerPhone ?: "",
                            order.subtotal.toString(),
                            order.discount.toString(),
                            order.total.toString(),
                            getPaymentTypeText(order.paymentTypeRaw),
                            getStatusText(order.statusRaw)
                        )
                    })
                    
                    // Sheet 2: Sales Summary
                    val summarySheet = workbook.createSheet("Sales Summary")
                    val totalSales = orders.sumOf { it.total }
                    val orderCount = orders.size
                    val averageOrder = if (orderCount > 0) totalSales / orderCount else 0.0
                    val salesSummaryHeaders = arrayOf("Metric", "Value")
                    val salesSummaryRows = listOf(
                        arrayOf("Total Sales", totalSales.toString()),
                        arrayOf("Total Orders", orderCount.toString()),
                        arrayOf("Average Order Value", averageOrder.toString())
                    )
                    createExcelSheet(summarySheet, salesSummaryHeaders, salesSummaryRows)
                    
                    // Sheet 3: Sales By Date
                    val salesByDateSheet = workbook.createSheet("Sales By Date")
                    val salesByDate = orders.groupBy { dateOnlyFormat.format(it.orderDate) }
                        .map { (date, dateOrders) ->
                            val dateTotal = dateOrders.sumOf { it.total }
                            arrayOf(date, dateOrders.size.toString(), dateTotal.toString())
                        }
                    val salesByDateHeaders = arrayOf("Date", "Order Count", "Total Sales")
                    createExcelSheet(salesByDateSheet, salesByDateHeaders, salesByDate)
                    
                    val file = File(context.getExternalFilesDir(null), "Sales_Report_${System.currentTimeMillis()}.xlsx")
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
                        // UTF-8 BOM helps Excel on Windows recognize the file as UTF-8 (fixes Thai text showing as garbled)
                        out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                        OutputStreamWriter(out, StandardCharsets.UTF_8).use { writer ->
                            val csvWriter = CSVWriter(writer)
                            csvWriter.writeNext(headers)
                            rows.forEach { row ->
                                csvWriter.writeNext(row)
                            }
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

