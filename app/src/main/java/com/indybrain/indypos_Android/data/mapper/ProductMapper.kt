package com.indybrain.indypos_Android.data.mapper

import com.indybrain.indypos_Android.data.local.entity.*
import com.indybrain.indypos_Android.data.remote.dto.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ProductMapper {
    
    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    )
    
    fun toEntity(dto: CategoryDto): CategoryEntity {
        return CategoryEntity(
            id = dto.id,
            name = dto.name,
            sortOrder = dto.sortOrder,
            isActive = dto.isActive,
            userId = dto.userId,
            productCount = dto.productCount,
            createdAt = parseDate(dto.createdAt),
            updatedAt = parseDate(dto.updatedAt)
        )
    }
    
    fun toEntity(dto: ProductDto): ProductEntity {
        return ProductEntity(
            id = dto.id,
            name = dto.name,
            description = dto.description,
            price = dto.price,
            costPrice = dto.costPrice,
            imageUrl = dto.imageUrl,
            categoryId = dto.categoryId,
            userId = dto.userId,
            popularityRank = dto.popularityRank,
            productCode = dto.productCode,
            unit = dto.unit,
            skuCode = dto.skuCode,
            stockQuantity = dto.stockQuantity,
            minStockQuantity = dto.minStockQuantity,
            selectedUnit = dto.selectedUnit,
            selectedColorHex = dto.selectedColorHex,
            isSkuEnabled = dto.isSkuEnabled,
            isStockEnabled = dto.isStockEnabled,
            hasAdditionalOptions = dto.hasAdditionalOptions,
            isActive = dto.isActive,
            createdAt = parseDate(dto.createdAt),
            updatedAt = parseDate(dto.updatedAt)
        )
    }
    
    fun toEntity(dto: AddonGroupDto): AddonGroupEntity {
        return AddonGroupEntity(
            id = dto.id,
            name = dto.name,
            isRequired = dto.isRequired,
            isSingleSelection = dto.isSingleSelection,
            maxSelection = dto.maxSelection,
            minSelection = dto.minSelection,
            sortOrder = dto.sortOrder,
            isActive = dto.isActive,
            userId = dto.userId,
            createdAt = parseDate(dto.createdAt),
            updatedAt = parseDate(dto.updatedAt)
        )
    }
    
    fun toEntity(dto: AddonDto, addonGroupId: String? = null): AddonEntity {
        return AddonEntity(
            id = dto.id,
            name = dto.name,
            price = dto.price,
            sortOrder = dto.sortOrder,
            isActive = dto.isActive,
            userId = dto.userId,
            addonGroupId = addonGroupId,
            createdAt = parseDate(dto.createdAt),
            updatedAt = parseDate(dto.updatedAt)
        )
    }
    
    private fun parseDate(dateString: String): Date {
        return try {
            for (format in dateFormats) {
                try {
                    val parsed = format.parse(dateString)
                    if (parsed != null) return parsed
                } catch (e: Exception) {
                    // Try next format
                }
            }
            Date() // Fallback to current date
        } catch (e: Exception) {
            Date() // Fallback to current date
        }
    }
}

