package com.indybrain.indypos_Android.data.mapper

import com.google.gson.Gson
import com.indybrain.indypos_Android.data.local.entity.OrderAddonEntity
import com.indybrain.indypos_Android.data.local.entity.OrderEntity
import com.indybrain.indypos_Android.data.local.entity.OrderItemEntity
import com.indybrain.indypos_Android.data.remote.dto.OrderAddonDto
import com.indybrain.indypos_Android.data.remote.dto.OrderDto
import com.indybrain.indypos_Android.data.remote.dto.OrderItemDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object OrderMapper {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    private val gson = Gson()
    
    fun toEntity(dto: OrderDto): OrderEntity {
        return OrderEntity(
            id = dto.id,
            orderNumber = dto.orderNumber,
            orderDate = parseDate(dto.orderDate),
            subtotal = dto.subtotal,
            discount = dto.discountAmount, // Core Data uses "discount", map from discountAmount
            total = dto.total,
            paymentTypeRaw = dto.paymentType, // Changed from paymentType to paymentTypeRaw
            statusRaw = dto.orderStatus, // Changed from orderStatus to statusRaw
            updatedAt = parseDate(dto.updatedAt),
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
            createdAt = parseDate(dto.createdAt)
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
    
    private fun parseDate(dateString: String): Date {
        return try {
            dateFormat.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
}

