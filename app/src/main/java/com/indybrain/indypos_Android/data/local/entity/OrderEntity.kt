package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.indybrain.indypos_Android.data.local.converter.DateConverter
import java.util.Date

@Entity(tableName = "orders")
@TypeConverters(DateConverter::class)
data class OrderEntity(
    @PrimaryKey
    val id: String,
    val userId: Int,
    val orderNumber: String,
    val orderDate: Date,
    val customerName: String?,
    val customerPhone: String?,
    val customerEmail: String?,
    val subtotal: Double,
    val discountAmount: Double,
    val discountPercentage: Double,
    val taxAmount: Double,
    val taxPercentage: Double,
    val total: Double,
    val paymentType: Int,
    val paymentStatus: Int,
    val orderStatus: Int,
    val notes: String?,
    val createdAt: Date,
    val updatedAt: Date
)

