package com.indybrain.indypos_Android.data.mapper

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
    
    fun toEntity(dto: OrderDto): OrderEntity {
        return OrderEntity(
            id = dto.id,
            userId = dto.userId,
            orderNumber = dto.orderNumber,
            orderDate = parseDate(dto.orderDate),
            customerName = dto.customerName,
            customerPhone = dto.customerPhone,
            customerEmail = dto.customerEmail,
            subtotal = dto.subtotal,
            discountAmount = dto.discountAmount,
            discountPercentage = dto.discountPercentage,
            taxAmount = dto.taxAmount,
            taxPercentage = dto.taxPercentage,
            total = dto.total,
            paymentType = dto.paymentType,
            paymentStatus = dto.paymentStatus,
            orderStatus = dto.orderStatus,
            notes = dto.notes,
            createdAt = parseDate(dto.createdAt),
            updatedAt = parseDate(dto.updatedAt)
        )
    }
    
    fun toEntity(dto: OrderItemDto, orderId: String): OrderItemEntity {
        return OrderItemEntity(
            id = dto.id,
            orderId = orderId,
            productId = dto.productId,
            productName = dto.productName,
            productCode = dto.productCode,
            unitPrice = dto.unitPrice,
            unitCost = dto.unitCost,
            quantity = dto.quantity,
            totalPrice = dto.totalPrice,
            specialRequest = dto.specialRequest,
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

