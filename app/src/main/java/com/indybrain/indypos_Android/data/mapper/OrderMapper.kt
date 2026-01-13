package com.indybrain.indypos_Android.data.mapper

import com.google.gson.Gson
import com.indybrain.indypos_Android.data.local.entity.OrderAddonEntity
import com.indybrain.indypos_Android.data.local.entity.OrderEntity
import com.indybrain.indypos_Android.data.local.entity.OrderItemEntity
import com.indybrain.indypos_Android.data.remote.dto.OrderAddonDto
import com.indybrain.indypos_Android.data.remote.dto.OrderDto
import com.indybrain.indypos_Android.data.remote.dto.OrderItemDto
import com.indybrain.indypos_Android.data.remote.dto.OrderListAddon
import com.indybrain.indypos_Android.data.remote.dto.OrderListData
import com.indybrain.indypos_Android.data.remote.dto.OrderListItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object OrderMapper {
    
    // Support multiple date formats from API
    // Note: ISO 8601 formats with 'Z' should use 'X' or 'XXX' for timezone, not quoted 'Z'
    private val dateFormats = listOf(
        // ISO 8601 with timezone offset (e.g., "2024-01-01T22:47:00+00:00" or "2024-01-01T22:47:00Z")
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US),
        // ISO 8601 with literal Z (UTC indicator)
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        // ISO 8601 without timezone (assume UTC)
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        // Standard format without timezone (assume UTC)
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    )
    
    private val gson = Gson()
    
    fun toEntity(dto: OrderDto): OrderEntity {
        val orderDate = parseDate(dto.orderDate) ?: Date() // Fallback to current date only if orderDate is invalid
        val updatedAt = parseDate(dto.updatedAt) ?: orderDate // Fallback to orderDate if updatedAt is invalid
        val createdAt = parseDate(dto.createdAt) // Can be null
        
        return OrderEntity(
            id = dto.id,
            orderNumber = dto.orderNumber,
            orderDate = orderDate,
            subtotal = dto.subtotal,
            discount = dto.discountAmount, // Core Data uses "discount", map from discountAmount
            total = dto.total,
            paymentTypeRaw = dto.paymentType, // Changed from paymentType to paymentTypeRaw
            statusRaw = dto.orderStatus, // Changed from orderStatus to statusRaw
            updatedAt = updatedAt,
            // Extra fields (optional)
            userId = dto.userId,
            customerName = dto.customerName,
            customerPhone = dto.customerPhone,
            customerEmail = dto.customerEmail,
            discountAmount = dto.discountAmount, // Keep as optional for compatibility
            discountPercentage = dto.discountPercentage,
            taxAmount = dto.taxAmount,
            taxPercentage = dto.taxPercentage,
            paymentStatus = dto.paymentStatus,
            notes = dto.notes,
            createdAt = createdAt
        )
    }
    
    fun toEntity(dto: OrderItemDto, orderId: String): OrderItemEntity {
        // Convert addons list to JSON string
        val addonsJson = if (dto.addons != null && dto.addons.isNotEmpty()) {
            gson.toJson(dto.addons)
        } else {
            null
        }
        
        return OrderItemEntity(
            id = dto.id,
            orderId = orderId,
            productName = dto.productName,
            productPrice = dto.unitPrice, // Map unitPrice to productPrice (snapshot)
            productUnitPrice = dto.unitPrice, // Map unitPrice to productUnitPrice (snapshot)
            quantity = dto.quantity,
            totalPrice = dto.totalPrice,
            addons = addonsJson, // JSON string from addons list
            specialRequest = dto.specialRequest,
            // Extra fields (optional)
            productId = dto.productId,
            productCode = dto.productCode,
            unitCost = dto.unitCost,
            notes = dto.notes,
            createdAt = parseDate(dto.createdAt)
        )
    }
    
    fun toEntity(dto: OrderAddonDto, orderItemId: String): OrderAddonEntity {
        return OrderAddonEntity(
            orderItemId = orderItemId,
            addonId = dto.addonId,
            addonName = dto.addonName,
            addonPrice = dto.addonPrice,
            quantity = dto.quantity
        )
    }
    
    // Mapper functions for OrderListData (used by HomeScreen)
    fun toEntity(dto: OrderListData): OrderEntity? {
        // Skip if required fields are missing
        if (dto.id.isNullOrBlank() || dto.orderNumber.isNullOrBlank()) {
            return null
        }
        
        val orderDate = parseDate(dto.orderDate) ?: Date()
        val updatedAt = parseDate(dto.updatedAt) ?: orderDate
        val createdAt = parseDate(dto.createdAt)
        
        return OrderEntity(
            id = dto.id,
            orderNumber = dto.orderNumber,
            orderDate = orderDate,
            subtotal = dto.subtotal ?: 0.0,
            discount = dto.discountAmount ?: 0.0,
            total = dto.total ?: 0.0,
            paymentTypeRaw = dto.paymentType ?: 0,
            statusRaw = dto.orderStatus ?: 0,
            updatedAt = updatedAt,
            userId = dto.userId,
            customerName = dto.customerName,
            customerPhone = dto.customerPhone,
            customerEmail = dto.customerEmail,
            discountAmount = dto.discountAmount,
            discountPercentage = dto.discountPercentage,
            taxAmount = dto.taxAmount,
            taxPercentage = dto.taxPercentage,
            paymentStatus = dto.paymentStatus,
            notes = dto.notes,
            createdAt = createdAt
        )
    }
    
    fun toEntity(dto: OrderListItem, orderId: String): OrderItemEntity? {
        // Skip if required fields are missing
        if (dto.id.isNullOrBlank() || dto.productName.isNullOrBlank()) {
            return null
        }
        
        val addonsJson = if (dto.addons != null && dto.addons.isNotEmpty()) {
            gson.toJson(dto.addons)
        } else {
            null
        }
        
        return OrderItemEntity(
            id = dto.id,
            orderId = orderId,
            productName = dto.productName,
            productPrice = dto.unitPrice ?: 0.0,
            productUnitPrice = dto.unitPrice ?: 0.0,
            quantity = dto.quantity ?: 0,
            totalPrice = dto.totalPrice ?: 0.0,
            addons = addonsJson,
            specialRequest = dto.specialRequest,
            productId = dto.productId,
            productCode = dto.productCode,
            unitCost = dto.unitCost,
            notes = dto.notes,
            createdAt = parseDate(dto.createdAt)
        )
    }
    
    fun toEntity(dto: OrderListAddon, orderItemId: String): OrderAddonEntity? {
        // Skip if required fields are missing
        if (dto.addonId.isNullOrBlank() || dto.addonName.isNullOrBlank()) {
            return null
        }
        
        return OrderAddonEntity(
            orderItemId = orderItemId,
            addonId = dto.addonId,
            addonName = dto.addonName,
            addonPrice = dto.unitPrice ?: 0.0, // Map unitPrice to addonPrice
            quantity = dto.quantity ?: 0
        )
    }
    
    private fun parseDate(dateString: String?): Date? {
        if (dateString.isNullOrBlank()) {
            return null
        }
        
        // Try each date format
        for (format in dateFormats) {
            try {
                val parsed = format.parse(dateString)
                if (parsed != null) {
                    return parsed
                }
            } catch (e: Exception) {
                // Try next format
                continue
            }
        }
        
        // If all formats fail, return null instead of current date
        // This will help identify parsing issues
        return null
    }
}

