package com.indybrain.indypos_Android.data.repository

import com.google.gson.Gson
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.dao.*
import com.indybrain.indypos_Android.data.local.entity.CategoryEntity
import com.indybrain.indypos_Android.data.mapper.ProductMapper
import com.indybrain.indypos_Android.data.remote.api.CreateCategoryRequestDto
import com.indybrain.indypos_Android.data.remote.api.ProductsApi
import com.indybrain.indypos_Android.data.remote.api.ToggleCategoryStatusRequestDto
import com.indybrain.indypos_Android.data.remote.api.UpdateCategoryRequestDto
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val productsApi: ProductsApi,
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val addonGroupDao: AddonGroupDao,
    private val addonDao: AddonDao,
    private val authRepository: AuthRepository,
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    private val gson: Gson
) : ProductRepository {
    
    override suspend fun syncAllProductData(): Result<Unit> {
        return try {
            // Fetch categories
            val categoriesResponse = productsApi.getCategories()
            if (categoriesResponse.status != 200 || categoriesResponse.data == null) {
                return Result.failure(Exception(categoriesResponse.message ?: "Failed to fetch categories"))
            }
            
            // Fetch products
            val productsResponse = productsApi.getMyProductsAll()
            if (productsResponse.status != 200 || productsResponse.data == null) {
                return Result.failure(Exception(productsResponse.message ?: "Failed to fetch products"))
            }
            
            // Fetch addon groups
            val addonGroupsResponse = productsApi.getAddonGroups()
            if (addonGroupsResponse.status != 200 || addonGroupsResponse.data == null) {
                return Result.failure(Exception(addonGroupsResponse.message ?: "Failed to fetch addon groups"))
            }
            
            // Fetch addons
            val addonsResponse = productsApi.getAddons()
            if (addonsResponse.status != 200 || addonsResponse.data == null) {
                return Result.failure(Exception(addonsResponse.message ?: "Failed to fetch addons"))
            }
            
            // Convert and save categories
            val categories = categoriesResponse.data.map { ProductMapper.toEntity(it) }
            categoryDao.deleteAll()
            categoryDao.insertAll(categories)
            
            // Convert and save products
            val products = productsResponse.data.map { ProductMapper.toEntity(it) }
            // Important: do NOT call deleteAll() here.
            // Deleting all products would trigger the foreign key on cart_items
            // (onDelete = SET_NULL) and clear productId on existing cart items,
            // which makes quantities disappear in the product list after refresh.
            // Using REPLACE keeps existing rows (and cart relations) while updating data.
            productDao.insertAll(products)
            
            // Convert and save addon groups
            val addonGroups = addonGroupsResponse.data.map { ProductMapper.toEntity(it) }
            addonGroupDao.deleteAll()
            addonGroupDao.insertAll(addonGroups)
            
            // Convert and save addons
            // First, collect addons from addon groups (with groupId)
            val addonsFromGroupsMap = mutableMapOf<String, com.indybrain.indypos_Android.data.local.entity.AddonEntity>()
            addonGroupsResponse.data.forEach { groupDto ->
                groupDto.addons?.forEach { addonDto ->
                    addonsFromGroupsMap[addonDto.id] = ProductMapper.toEntity(addonDto, groupDto.id)
                }
            }
            
            // Then, add standalone addons from addons endpoint (only if not already in groups)
            addonsResponse.data.forEach { addonDto ->
                if (!addonsFromGroupsMap.containsKey(addonDto.id)) {
                    addonsFromGroupsMap[addonDto.id] = ProductMapper.toEntity(addonDto, null)
                }
            }
            
            // Save all addons
            addonDao.deleteAll()
            addonDao.insertAll(addonsFromGroupsMap.values.toList())
            
            Result.success(Unit)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                else -> e.message() ?: "เกิดข้อผิดพลาดในการดึงข้อมูล"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดที่ไม่คาดคิด"))
        }
    }
    
    override fun getAllActiveProducts(): Flow<List<com.indybrain.indypos_Android.data.local.entity.ProductEntity>> {
        return productDao.getAllActiveProductsFlow()
    }
    
    override fun getProductsByCategory(categoryId: String?): Flow<List<com.indybrain.indypos_Android.data.local.entity.ProductEntity>> {
        return productDao.getProductsByCategoryFlow(categoryId)
    }
    
    override fun getAllActiveCategories(): Flow<List<com.indybrain.indypos_Android.data.local.entity.CategoryEntity>> {
        return categoryDao.getAllActiveCategoriesFlow()
    }
    
    override suspend fun fetchAndSaveProducts(): Result<Unit> {
        return try {
            // Fetch products from API
            val productsResponse = productsApi.getMyProductsAll()
            if (productsResponse.status != 200 || productsResponse.data == null) {
                return Result.failure(Exception(productsResponse.message ?: "Failed to fetch products"))
            }
            
            // Extract categories from products and save them
            val categoriesMap = mutableMapOf<String, com.indybrain.indypos_Android.data.remote.dto.CategoryDto>()
            productsResponse.data.forEach { productDto ->
                productDto.category?.let { categoryDto ->
                    categoriesMap[categoryDto.id] = categoryDto
                }
            }
            
            // Convert and save categories
            if (categoriesMap.isNotEmpty()) {
                val categories = categoriesMap.values.map { ProductMapper.toEntity(it) }
                categoryDao.insertAll(categories)
            }
            
            // Convert and save products
            val products = productsResponse.data.map { ProductMapper.toEntity(it) }
            // Important: do NOT call deleteAll() here.
            // Deleting all products would trigger the foreign key on cart_items
            // (onDelete = SET_NULL) and clear productId on existing cart items,
            // which makes quantities disappear in the product list after refresh.
            // Using REPLACE keeps existing rows (and cart relations) while updating data.
            productDao.insertAll(products)
            
            Result.success(Unit)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                else -> e.message() ?: "เกิดข้อผิดพลาดในการดึงข้อมูล"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดที่ไม่คาดคิด"))
        }
    }
    
    override suspend fun fetchAndSyncCategories(): Result<Unit> {
        return try {
            // Fetch categories from API
            val categoriesResponse = productsApi.getCategories()
            if (categoriesResponse.status != 200 || categoriesResponse.data == null) {
                return Result.failure(Exception(categoriesResponse.message ?: "Failed to fetch categories"))
            }
            
            // Get existing categories from Room
            val existingCategories = categoryDao.getAllCategories()
            val existingCategoryIds = existingCategories.map { it.id }.toSet()
            
            // Convert API categories to entities
            val apiCategories = categoriesResponse.data.map { ProductMapper.toEntity(it) }
            
            // Find new categories that don't exist in Room
            val newCategories = apiCategories.filter { it.id !in existingCategoryIds }
            
            // Insert only new categories (existing ones are already in Room)
            if (newCategories.isNotEmpty()) {
                categoryDao.insertAll(newCategories)
            }
            
            // Also update existing categories if they have changed (using REPLACE strategy)
            // This ensures data stays in sync
            categoryDao.insertAll(apiCategories)
            
            Result.success(Unit)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                else -> e.message() ?: "เกิดข้อผิดพลาดในการดึงข้อมูล"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดที่ไม่คาดคิด"))
        }
    }
    
    override suspend fun getAllCategories(): List<CategoryEntity> {
        return categoryDao.getAllCategories()
    }
    
    override fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategoriesFlow()
    }
    
    override suspend fun getCategoryById(id: String): CategoryEntity? {
        return categoryDao.getCategoryById(id)
    }
    
    override suspend fun addCategory(category: CategoryEntity): Result<Unit> {
        return try {
            categoryDao.insert(category)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการเพิ่มหมวดหมู่"))
        }
    }
    
    override suspend fun updateCategory(category: CategoryEntity): Result<Unit> {
        return try {
            categoryDao.insert(category) // Using REPLACE strategy
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการแก้ไขหมวดหมู่"))
        }
    }
    
    override suspend fun getCurrentUserId(): Int? {
        return authRepository.getCurrentUser().first()?.id
    }
    
    override suspend fun createCategory(
        name: String,
        sortOrder: Int,
        isActive: Boolean
    ): Result<CategoryEntity> {
        val userId = getCurrentUserId() ?: return Result.failure(
            Exception("ไม่พบข้อมูลผู้ใช้ กรุณาเข้าสู่ระบบใหม่")
        )
        
        return try {
            val categoryEntity: CategoryEntity
            
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val request = CreateCategoryRequestDto(
                        name = name,
                        sortOrder = sortOrder,
                        isActive = isActive
                    )
                    
                    val response = productsApi.createCategory(request)
                    
                    if (response.status == 201 && response.data != null) {
                        // API success (201 Created) - convert to entity and save to Room
                        categoryEntity = ProductMapper.toEntity(response.data)
                        categoryDao.insert(categoryEntity)
                        Result.success(categoryEntity)
                    } else {
                        // API returned error status
                        val errorMessage = response.error?.takeIf { it.isNotBlank() }
                            ?: response.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการสร้างหมวดหมู่"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    // Handle HTTP errors
                    val errorBody = e.response()?.errorBody()
                    val errorMessage = when (e.code()) {
                        400 -> {
                            // Bad Request - parse error message
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        401 -> {
                            // Unauthorized - parse specific error
                            val parsed = parseApiErrorResponse(errorBody, e.code())
                            if (parsed.contains("Unauthorized", ignoreCase = true)) {
                                "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                            } else {
                                parsed
                            }
                        }
                        403 -> {
                            // Forbidden - Free plan limit exceeded
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        409 -> {
                            // Conflict - Duplicate category name
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        500 -> {
                            // Internal Server Error
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        else -> {
                            parseApiErrorResponse(errorBody, e.code())
                        }
                    }
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network - save to Room only (for sync later)
                categoryEntity = CategoryEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    sortOrder = sortOrder,
                    isActive = isActive,
                    userId = userId,
                    productCount = 0,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                categoryDao.insert(categoryEntity)
                Result.success(categoryEntity)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการสร้างหมวดหมู่"))
        }
    }
    
    override suspend fun updateCategory(
        categoryId: String,
        name: String,
        sortOrder: Int,
        isActive: Boolean
    ): Result<CategoryEntity> {
        val userId = getCurrentUserId() ?: return Result.failure(
            Exception("ไม่พบข้อมูลผู้ใช้ กรุณาเข้าสู่ระบบใหม่")
        )
        
        return try {
            val categoryEntity: CategoryEntity
            
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val request = UpdateCategoryRequestDto(
                        name = name,
                        sortOrder = sortOrder,
                        isActive = isActive,
                        id = categoryId,
                        description = "",
                        imageUrl = ""
                    )
                    
                    val response = productsApi.updateCategory(categoryId, request)
                    
                    if (response.status == 200 && response.data != null) {
                        // API success (200 OK) - convert to entity and save to Room
                        categoryEntity = ProductMapper.toEntity(response.data)
                        categoryDao.insert(categoryEntity)
                        Result.success(categoryEntity)
                    } else {
                        // API returned error status
                        val errorMessage = response.error?.takeIf { it.isNotBlank() }
                            ?: response.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการแก้ไขหมวดหมู่"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    // Handle HTTP errors
                    val errorBody = e.response()?.errorBody()
                    val errorMessage = when (e.code()) {
                        400 -> {
                            // Bad Request
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        401 -> {
                            // Unauthorized
                            val parsed = parseApiErrorResponse(errorBody, e.code())
                            if (parsed.contains("Unauthorized", ignoreCase = true)) {
                                "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                            } else {
                                parsed
                            }
                        }
                        403 -> {
                            // Forbidden - Category ownership mismatch
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        404 -> {
                            // Not Found - Category not found
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        409 -> {
                            // Conflict - Duplicate category name
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        500 -> {
                            // Internal Server Error
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        else -> {
                            parseApiErrorResponse(errorBody, e.code())
                        }
                    }
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network - update in Room only (for sync later)
                val existingCategory = categoryDao.getCategoryById(categoryId)
                if (existingCategory == null) {
                    return Result.failure(Exception("ไม่พบหมวดหมู่ที่ต้องการแก้ไข"))
                }
                
                categoryEntity = existingCategory.copy(
                    name = name,
                    sortOrder = sortOrder,
                    isActive = isActive,
                    updatedAt = Date()
                )
                categoryDao.insert(categoryEntity)
                Result.success(categoryEntity)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการแก้ไขหมวดหมู่"))
        }
    }
    
    override suspend fun toggleCategoryStatus(
        categoryId: String,
        newStatus: Boolean
    ): Result<CategoryEntity> {
        return try {
            val categoryEntity: CategoryEntity
            
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val request = ToggleCategoryStatusRequestDto(status = newStatus)
                    val response = productsApi.toggleCategoryStatus(categoryId, request)
                    
                    if (response.status == 200 && response.data != null) {
                        // API success (200 OK) - convert to entity and save to Room
                        categoryEntity = ProductMapper.toEntity(response.data)
                        categoryDao.insert(categoryEntity)
                        Result.success(categoryEntity)
                    } else {
                        // API returned error status
                        val errorMessage = response.error?.takeIf { it.isNotBlank() }
                            ?: response.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะหมวดหมู่"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    // Handle HTTP errors
                    val errorBody = e.response()?.errorBody()
                    val errorMessage = when (e.code()) {
                        400 -> parseApiErrorResponse(errorBody, e.code())
                        401 -> {
                            val parsed = parseApiErrorResponse(errorBody, e.code())
                            if (parsed.contains("Unauthorized", ignoreCase = true)) {
                                "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                            } else {
                                parsed
                            }
                        }
                        403 -> parseApiErrorResponse(errorBody, e.code())
                        404 -> parseApiErrorResponse(errorBody, e.code())
                        500 -> parseApiErrorResponse(errorBody, e.code())
                        else -> parseApiErrorResponse(errorBody, e.code())
                    }
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network - update in Room only (for sync later)
                val existingCategory = categoryDao.getCategoryById(categoryId)
                if (existingCategory == null) {
                    return Result.failure(Exception("ไม่พบหมวดหมู่ที่ต้องการอัปเดต"))
                }
                
                categoryEntity = existingCategory.copy(
                    isActive = newStatus,
                    updatedAt = Date()
                )
                categoryDao.insert(categoryEntity)
                Result.success(categoryEntity)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะหมวดหมู่"))
        }
    }
    
    /**
     * Parse API error response body
     */
    private fun parseApiErrorResponse(errorBody: ResponseBody?, statusCode: Int): String {
        return try {
            if (errorBody == null) {
                return getDefaultErrorMessage(statusCode)
            }
            
            val errorJson = errorBody.string()
            if (errorJson.isBlank()) {
                return getDefaultErrorMessage(statusCode)
            }
            
            // Try to parse as error response
            try {
                val errorResponse = gson.fromJson(errorJson, ErrorResponse::class.java)
                val errorText = errorResponse.error?.lowercase() ?: ""
                val messageText = errorResponse.message?.lowercase() ?: ""
                
                // Handle specific status codes
                when (statusCode) {
                    400 -> {
                        // Bad Request - return message or error field
                        errorResponse.message?.takeIf { it.isNotBlank() }
                            ?: errorResponse.error?.takeIf { it.isNotBlank() }
                            ?: "ข้อมูลไม่ถูกต้อง กรุณาตรวจสอบอีกครั้ง"
                    }
                    401 -> {
                        // Unauthorized - return message or error field
                        errorResponse.message?.takeIf { it.isNotBlank() }
                            ?: errorResponse.error?.takeIf { it.isNotBlank() }
                            ?: "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                    }
                    403 -> {
                        // Forbidden - Free plan limit exceeded or ownership mismatch
                        // Return the message field which contains Thai message
                        val message = errorResponse.message?.takeIf { it.isNotBlank() }
                            ?: errorResponse.error?.takeIf { it.isNotBlank() }
                            ?: ""
                        
                        if (message.contains("own categories", ignoreCase = true) ||
                            message.contains("ownership", ignoreCase = true)) {
                            "คุณไม่มีสิทธิ์แก้ไขหมวดหมู่นี้"
                        } else if (message.contains("free_plan_limit", ignoreCase = true)) {
                            "คุณใช้หมวดหมู่ครบจำนวนที่กำหนดแล้ว กรุณาอัปเกรดแผน"
                        } else {
                            message.ifBlank { "คุณไม่มีสิทธิ์แก้ไขหมวดหมู่นี้" }
                        }
                    }
                    404 -> {
                        // Not Found - Category not found
                        errorResponse.message?.takeIf { it.isNotBlank() }
                            ?: errorResponse.error?.takeIf { it.isNotBlank() }
                            ?: "ไม่พบหมวดหมู่ที่ต้องการแก้ไข"
                    }
                    409 -> {
                        // Conflict - Duplicate category name
                        if (errorText.contains("duplicate category name") || 
                            messageText.contains("duplicate category name")) {
                            "ชื่อหมวดหมู่นี้มีอยู่แล้ว"
                        } else {
                            errorResponse.message?.takeIf { it.isNotBlank() }
                                ?: errorResponse.error?.takeIf { it.isNotBlank() }
                                ?: "ชื่อหมวดหมู่นี้มีอยู่แล้ว"
                        }
                    }
                    500 -> {
                        // Internal Server Error
                        errorResponse.message?.takeIf { it.isNotBlank() }
                            ?: errorResponse.error?.takeIf { it.isNotBlank() }
                            ?: "Server error - กรุณาลองใหม่อีกครั้ง"
                    }
                    else -> {
                        // Return error or message if available
                        errorResponse.error?.takeIf { it.isNotBlank() }
                            ?: errorResponse.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการสร้างหมวดหมู่"
                    }
                }
            } catch (e: Exception) {
                // If parsing fails, check raw string
                val errorLower = errorJson.lowercase()
                when {
                    statusCode == 409 && errorLower.contains("duplicate category name") -> {
                        "ชื่อหมวดหมู่นี้มีอยู่แล้ว"
                    }
                    statusCode == 403 && errorLower.contains("free_plan_limit_exceeded") -> {
                        "คุณใช้หมวดหมู่ครบจำนวนที่กำหนดแล้ว กรุณาอัปเกรดแผน"
                    }
                    else -> {
                        getDefaultErrorMessage(statusCode)
                    }
                }
            }
        } catch (e: Exception) {
            getDefaultErrorMessage(statusCode)
        }
    }
    
    /**
     * Get default error message for status code
     */
    private fun getDefaultErrorMessage(statusCode: Int): String {
        return when (statusCode) {
            400 -> "ข้อมูลไม่ถูกต้อง กรุณาตรวจสอบอีกครั้ง"
            401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
            403 -> "คุณไม่มีสิทธิ์แก้ไขหมวดหมู่นี้"
            404 -> "ไม่พบหมวดหมู่ที่ต้องการแก้ไข"
            409 -> "ชื่อหมวดหมู่นี้มีอยู่แล้ว"
            500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
            else -> "เกิดข้อผิดพลาดในการแก้ไขหมวดหมู่"
        }
    }
}

/**
 * Error response DTO for parsing API errors
 */
private data class ErrorResponse(
    val error: String?,
    val message: String?
)

