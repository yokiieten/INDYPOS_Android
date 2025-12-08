package com.indybrain.indypos_Android.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.dao.*
import com.indybrain.indypos_Android.data.local.entity.CategoryEntity
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.data.mapper.ProductMapper
import com.indybrain.indypos_Android.data.remote.api.CreateCategoryRequestDto
import com.indybrain.indypos_Android.data.remote.api.CreateProductRequestDto
import com.indybrain.indypos_Android.data.remote.api.DeleteProductsRequestDto
import com.indybrain.indypos_Android.data.remote.api.ProductsApi
import com.indybrain.indypos_Android.data.remote.api.ToggleCategoryStatusRequestDto
import com.indybrain.indypos_Android.data.remote.api.ToggleProductStatusRequestDto
import com.indybrain.indypos_Android.data.remote.api.UpdateCategoryRequestDto
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import com.indybrain.indypos_Android.domain.repository.CartRepository
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import com.indybrain.indypos_Android.domain.repository.ProductSyncStatistics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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
    private val cartRepository: CartRepository,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) : ProductRepository {
    
    override suspend fun syncAllProductData(): Result<Unit> {
        return try {
            // Fetch categories (to get all categories, not just those in products)
            val categoriesResponse = productsApi.getCategories()
            if (categoriesResponse.status != 200 || categoriesResponse.data == null) {
                return Result.failure(Exception(categoriesResponse.message ?: "Failed to fetch categories"))
            }
            
            // Fetch products with nested category and addon groups/addons
            val productsResponse = productsApi.getMyProductsAll()
            if (productsResponse.status != 200 || productsResponse.data == null) {
                return Result.failure(Exception(productsResponse.message ?: "Failed to fetch products"))
            }
            
            // Extract categories from products (in case there are categories not in categories endpoint)
            val categoriesFromProductsMap = mutableMapOf<String, com.indybrain.indypos_Android.data.remote.dto.CategoryDto>()
            productsResponse.data.forEach { productDto ->
                productDto.category?.let { categoryDto ->
                    categoriesFromProductsMap[categoryDto.id] = categoryDto
                }
            }
            
            // Merge categories: use categories from categories endpoint, but also include any from products
            val allCategoriesMap = mutableMapOf<String, com.indybrain.indypos_Android.data.remote.dto.CategoryDto>()
            categoriesResponse.data.forEach { categoryDto ->
                allCategoriesMap[categoryDto.id] = categoryDto
            }
            categoriesFromProductsMap.forEach { (id, categoryDto) ->
                allCategoriesMap[id] = categoryDto
            }
            
            // Convert and save categories
            val categories = allCategoriesMap.values.map { ProductMapper.toEntity(it) }
            categoryDao.deleteAll()
            categoryDao.insertAll(categories)
            
            // Extract addon groups and addons from products
            val addonGroupsMap = mutableMapOf<String, com.indybrain.indypos_Android.data.remote.dto.AddonGroupDto>()
            val addonsMap = mutableMapOf<String, Pair<com.indybrain.indypos_Android.data.remote.dto.AddonDto, String?>>()
            
            productsResponse.data.forEach { productDto ->
                productDto.addonGroups?.forEach { addonGroupDto ->
                    // Add addon group
                    addonGroupsMap[addonGroupDto.id] = addonGroupDto
                    
                    // Add addons from this group
                    addonGroupDto.addons?.forEach { addonDto ->
                        addonsMap[addonDto.id] = Pair(addonDto, addonGroupDto.id)
                    }
                }
            }
            
            // Convert and save products
            val products = productsResponse.data.map { ProductMapper.toEntity(it) }
            // Important: do NOT call deleteAll() here.
            // Deleting all products would trigger the foreign key on cart_items
            // (onDelete = SET_NULL) and clear productId on existing cart items,
            // which makes quantities disappear in the product list after refresh.
            // Using REPLACE keeps existing rows (and cart relations) while updating data.
            productDao.insertAll(products)
            
            // Convert and save addon groups
            val addonGroups = addonGroupsMap.values.map { ProductMapper.toEntity(it) }
            addonGroupDao.deleteAll()
            addonGroupDao.insertAll(addonGroups)
            
            // Convert and save addons
            val addons = addonsMap.values.map { (addonDto, groupId) ->
                ProductMapper.toEntity(addonDto, groupId)
            }
            addonDao.deleteAll()
            addonDao.insertAll(addons)
            
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
            // Fetch products from API (includes category and addon groups/addons)
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
            
            // Extract addon groups and addons from products
            val addonGroupsMap = mutableMapOf<String, com.indybrain.indypos_Android.data.remote.dto.AddonGroupDto>()
            val addonsMap = mutableMapOf<String, Pair<com.indybrain.indypos_Android.data.remote.dto.AddonDto, String?>>()
            
            productsResponse.data.forEach { productDto ->
                productDto.addonGroups?.forEach { addonGroupDto ->
                    // Add addon group
                    addonGroupsMap[addonGroupDto.id] = addonGroupDto
                    
                    // Add addons from this group
                    addonGroupDto.addons?.forEach { addonDto ->
                        addonsMap[addonDto.id] = Pair(addonDto, addonGroupDto.id)
                    }
                }
            }
            
            // Convert and save addon groups
            if (addonGroupsMap.isNotEmpty()) {
                val addonGroups = addonGroupsMap.values.map { ProductMapper.toEntity(it) }
                addonGroupDao.insertAll(addonGroups)
            }
            
            // Convert and save addons
            if (addonsMap.isNotEmpty()) {
                val addons = addonsMap.values.map { (addonDto, groupId) ->
                    ProductMapper.toEntity(addonDto, groupId)
                }
                addonDao.insertAll(addons)
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
                    updatedAt = Date(),
                    isDeletedLocally = false,
                    isFromServer = false,
                    isSynced = false
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
                    updatedAt = Date(),
                    isSynced = false
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
                    updatedAt = Date(),
                    isSynced = false
                )
                categoryDao.insert(categoryEntity)
                Result.success(categoryEntity)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะหมวดหมู่"))
        }
    }
    
    override suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val response = productsApi.deleteCategory(categoryId)
                    
                    if (response.status == 200) {
                        // API success (200 OK) - delete from Room
                        categoryDao.deleteCategoryById(categoryId)
                        Result.success(Unit)
                    } else {
                        // API returned error status
                        val errorMessage = response.error?.takeIf { it.isNotBlank() }
                            ?: response.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการลบหมวดหมู่"
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
                // No network - mark as deleted locally (for sync later)
                val existingCategory = categoryDao.getCategoryById(categoryId)
                if (existingCategory == null) {
                    return Result.failure(Exception("ไม่พบหมวดหมู่ที่ต้องการลบ"))
                }
                
                categoryDao.markAsDeletedLocally(categoryId)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการลบหมวดหมู่"))
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
                        // Conflict - Duplicate name or code
                        // Show both error and message if available
                        val errorTextValue = errorResponse.error?.takeIf { it.isNotBlank() }
                        val messageTextValue = errorResponse.message?.takeIf { it.isNotBlank() }
                        when {
                            errorTextValue != null && messageTextValue != null -> "$errorTextValue ($messageTextValue)"
                            errorTextValue != null -> errorTextValue
                            messageTextValue != null -> messageTextValue
                            else -> "ข้อมูลซ้ำกัน กรุณาตรวจสอบอีกครั้ง"
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
    
    override fun getAllProductsForManagement(): Flow<List<ProductEntity>> {
        return productDao.getAllProductsForManagementFlow()
    }
    
    override fun searchProducts(query: String, categoryId: String?): Flow<List<ProductEntity>> {
        return productDao.searchProductsFlow(query, categoryId)
    }
    
    override suspend fun getProductById(id: String): ProductEntity? {
        return productDao.getProductById(id)
    }
    
    override suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val response = productsApi.deleteProduct(productId)
                    
                    if (response.status == 200) {
                        // API success - permanently delete from Room
                        productDao.deleteProductById(productId)
                        Result.success(Unit)
                    } else {
                        val errorMessage = response.error?.takeIf { it.isNotBlank() }
                            ?: response.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการลบสินค้า"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    val errorBody = e.response()?.errorBody()
                    val errorMessage = parseApiErrorResponse(errorBody, e.code())
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network - check if product can be permanently deleted
                val product = productDao.getProductById(productId)
                if (product == null) {
                    return Result.failure(Exception("ไม่พบสินค้าที่ต้องการลบ"))
                }
                
                if (!product.isSynced && !product.isFromServer) {
                    // Not synced and not from server - permanently delete
                    productDao.deleteProductById(productId)
                } else {
                    // Mark as deleted locally for sync later
                    productDao.markAsDeletedLocally(productId)
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการลบสินค้า"))
        }
    }
    
    override suspend fun deleteMultipleProducts(productIds: List<String>): Result<Unit> {
        return try {
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val response = productsApi.deleteMultipleProducts(DeleteProductsRequestDto(productIds))
                    
                    if (response.status == 200) {
                        // API success - permanently delete from Room
                        productIds.forEach { productId ->
                            productDao.deleteProductById(productId)
                        }
                        Result.success(Unit)
                    } else {
                        val errorMessage = response.error?.takeIf { it.isNotBlank() }
                            ?: response.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการลบสินค้า"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    val errorBody = e.response()?.errorBody()
                    val errorMessage = parseApiErrorResponse(errorBody, e.code())
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network - handle each product individually
                productIds.forEach { productId ->
                    val product = productDao.getProductById(productId)
                    if (product != null) {
                        if (!product.isSynced && !product.isFromServer) {
                            productDao.deleteProductById(productId)
                        } else {
                            productDao.markAsDeletedLocally(productId)
                        }
                    }
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการลบสินค้า"))
        }
    }
    
    override suspend fun toggleProductStatus(productId: String, newStatus: Boolean): Result<ProductEntity> {
        return try {
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val response = productsApi.toggleProductStatus(
                        productId,
                        ToggleProductStatusRequestDto(newStatus)
                    )
                    
                    if (response.status == 200 && response.data != null) {
                        // API success - convert to entity and save to Room
                        val productEntity = ProductMapper.toEntity(response.data)
                        productDao.insertAll(listOf(productEntity))
                        
                        // If deactivated, clear cart items for this product
                        if (!newStatus) {
                            cartRepository.clearCartItemsByProduct(productId)
                        }
                        
                        Result.success(productEntity)
                    } else {
                        val errorMessage = response.error?.takeIf { it.isNotBlank() }
                            ?: response.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะสินค้า"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    val errorBody = e.response()?.errorBody()
                    val errorMessage = parseApiErrorResponse(errorBody, e.code())
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network - update in Room only (for sync later)
                val existingProduct = productDao.getProductById(productId)
                if (existingProduct == null) {
                    return Result.failure(Exception("ไม่พบสินค้าที่ต้องการอัปเดต"))
                }
                
                val updatedProduct = existingProduct.copy(
                    isActive = newStatus,
                    isSynced = false,
                    updatedAt = Date()
                )
                productDao.insertAll(listOf(updatedProduct))
                
                // If deactivated, clear cart items for this product
                if (!newStatus) {
                    cartRepository.clearCartItemsByProduct(productId)
                }
                
                Result.success(updatedProduct)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะสินค้า"))
        }
    }
    
    override suspend fun uploadProductImage(imageUri: Uri): Result<String> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception("กรุณาเชื่อมต่ออินเทอร์เน็ต"))
            }
            
            // Read file from URI
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                return Result.failure(Exception("ไม่สามารถอ่านไฟล์รูปภาพได้"))
            }
            
            // Create temporary file
            val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)
            
            try {
                inputStream.copyTo(outputStream)
            } finally {
                inputStream.close()
                outputStream.close()
            }
            
            // Get file extension and MIME type
            val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
            val mediaType = mimeType.toMediaTypeOrNull() ?: "image/jpeg".toMediaTypeOrNull()
            
            // Create request body
            val requestFile = tempFile.asRequestBody(mediaType)
            val body = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
            
            // Upload image
            val response = productsApi.uploadProductImage(body)
            
            // Clean up temp file
            tempFile.delete()
            
            Result.success(response.url)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()
            val errorMessage = when (e.code()) {
                400 -> {
                    // Bad Request - parse error message
                    try {
                        errorBody?.string() ?: "ไม่สามารถอัปโหลดรูปภาพได้"
                    } catch (ex: Exception) {
                        "ไม่สามารถอัปโหลดรูปภาพได้"
                    }
                }
                401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                else -> e.message() ?: "เกิดข้อผิดพลาดในการอัปโหลดรูปภาพ"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการอัปโหลดรูปภาพ"))
        }
    }
    
    override suspend fun createProduct(
        name: String,
        productCode: String?,
        price: Double,
        costPrice: Double?,
        unit: String?,
        imageUrl: String?,
        selectedColorHex: String?,
        categoryId: String?,
        skuCode: String?,
        stockQuantity: Int?,
        isSkuEnabled: Boolean?,
        isStockEnabled: Boolean?,
        hasAdditionalOptions: Boolean?,
        addonGroupIds: List<String>?
    ): Result<ProductEntity> {
        val userId = getCurrentUserId() ?: return Result.failure(
            Exception("ไม่พบข้อมูลผู้ใช้ กรุณาเข้าสู่ระบบใหม่")
        )
        
        return try {
            val productEntity: ProductEntity
            
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val request = CreateProductRequestDto(
                        name = name,
                        description = null,
                        price = price,
                        costPrice = costPrice,
                        imageUrl = imageUrl,
                        categoryId = categoryId,
                        productCode = productCode,
                        unit = unit,
                        skuCode = skuCode,
                        stockQuantity = stockQuantity,
                        minStockQuantity = null,
                        selectedUnit = null,
                        selectedColorHex = selectedColorHex,
                        isSkuEnabled = isSkuEnabled,
                        isStockEnabled = isStockEnabled,
                        hasAdditionalOptions = hasAdditionalOptions,
                        isActive = true,
                        addonGroupIds = addonGroupIds
                    )
                    
                    val response = productsApi.createProduct(request)
                    
                    if (response.status == 201 && response.data != null) {
                        // API success (201 Created) - convert to entity and save to Room
                        // Response has nested structure: data.product
                        val productDto = response.data.product
                        productEntity = ProductMapper.toEntity(productDto)
                        productDao.insertAll(listOf(productEntity))
                        Result.success(productEntity)
                    } else {
                        // API returned error status
                        val errorMessage = response.error?.takeIf { it.isNotBlank() }
                            ?: response.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการสร้างสินค้า"
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
                            // Forbidden
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        409 -> {
                            // Conflict - Duplicate product code or name
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
                productEntity = ProductEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    description = null,
                    price = price,
                    costPrice = costPrice,
                    imageUrl = imageUrl,
                    categoryId = categoryId,
                    userId = userId,
                    popularityRank = null,
                    productCode = productCode,
                    unit = unit,
                    skuCode = skuCode,
                    stockQuantity = stockQuantity,
                    minStockQuantity = null,
                    selectedUnit = null,
                    selectedColorHex = selectedColorHex,
                    isSkuEnabled = isSkuEnabled,
                    isStockEnabled = isStockEnabled,
                    hasAdditionalOptions = hasAdditionalOptions,
                    isActive = true,
                    createdAt = Date(),
                    updatedAt = Date(),
                    isDeletedLocally = false,
                    isFromServer = false,
                    isSynced = false
                )
                productDao.insertAll(listOf(productEntity))
                Result.success(productEntity)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการสร้างสินค้า"))
        }
    }
    
    override suspend fun getSyncStatistics(): ProductSyncStatistics {
        val totalProducts = productDao.getTotalProductCount()
        val syncedCount = productDao.getSyncedProductCount()
        val pendingSyncCount = productDao.getPendingSyncProductCount()
        val deletedCount = productDao.getDeletedProductCount()
        return ProductSyncStatistics(
            totalProducts = totalProducts,
            syncedCount = syncedCount,
            pendingSyncCount = pendingSyncCount,
            deletedCount = deletedCount
        )
    }
    
    override suspend fun clearCartItemsByProduct(productId: String) {
        cartRepository.clearCartItemsByProduct(productId)
    }
}

/**
 * Error response DTO for parsing API errors
 */
private data class ErrorResponse(
    val error: String?,
    val message: String?
)

