package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.indybrain.indypos_Android.data.local.converter.DateConverter
import java.util.Date

@Entity(tableName = "categories")
@TypeConverters(DateConverter::class)
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val sortOrder: Int,
    val isActive: Boolean,
    val userId: Int,
    val productCount: Int?,
    val createdAt: Date,
    val updatedAt: Date
)

