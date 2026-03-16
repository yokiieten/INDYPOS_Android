package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.indybrain.indypos_Android.data.local.converter.DateConverter
import java.util.Date

@Entity(
    tableName = "cart_items",
    foreignKeys = [
        // ไม่ใช้ onDelete = SET_NULL เพราะ productDao.insertAll(REPLACE) จะทำ DELETE+INSERT
        // ตอนอัปเดตสินค้า → SET_NULL จะทำให้ cart productId เป็น null และ badge หาย
        // ใช้ default NO_ACTION → cart ยังอ้างอิง productId ได้หลังแก้ไขสินค้า
        // หมายเหตุ: ก่อนลบสินค้าต้อง clearCartItemsByProduct() ก่อนเสมอ
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"]
        )
    ]
)
@TypeConverters(DateConverter::class)
data class CartItemEntity(
    @PrimaryKey
    val id: String, // Changed from Long to String to match Core Data
    val productId: String?, // Many-to-One with ProductEntity
    val quantity: Int,
    val specialRequest: String?,
    val createdAt: Date,
    // Extra fields (snapshot data for display, not in Core Data relationships)
    val productName: String? = null,
    val productImageUrl: String? = null,
    val productColorHex: String? = null,
    val unitPrice: Double? = null
)

