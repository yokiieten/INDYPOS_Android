package com.indybrain.indypos_Android.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.data.local.dao.*
import com.indybrain.indypos_Android.data.local.entity.CategoryEntity
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.data.mapper.ProductMapper
import com.indybrain.indypos_Android.data.remote.api.*
import com.indybrain.indypos_Android.data.remote.dto.DeleteCategoriesResponseDto
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import com.indybrain.indypos_Android.domain.repository.CartRepository
import com.indybrain.indypos_Android.domain.repository.CategoriesPaginatedResult
import com.indybrain.indypos_Android.domain.repository.DeleteCategoriesResult
import com.indybrain.indypos_Android.domain.repository.DeleteProductsResult
import com.indybrain.indypos_Android.domain.repository.ProductDetailData
import com.indybrain.indypos_Android.domain.repository.ProductListData
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
    private val productAddonGroupJunctionDao: ProductAddonGroupJunctionDao,
    private val addonGroupAddonJunctionDao: AddonGroupAddonJunctionDao,
    private val authRepository: AuthRepository,
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    private val cartRepository: CartRepository,
    private val languageLocalDataSource: LanguageLocalDataSource,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) : ProductRepository {
    
    override suspend fun syncAllProductData(): Result<Unit> {
        return try {
            // Fetch categories (to get all categories, not just those in products)
            val categoriesResponse = productsApi.getCategories()
            if (categoriesResponse.status != 200) {
                return Result.failure(Exception(categoriesResponse.message ?: "Failed to fetch categories"))
            }
            
            // Handle null data as empty list (valid for users with no categories)
            val categoriesList = categoriesResponse.data ?: emptyList()
            
            // Fetch products with nested category and addon groups/addons
            val productsResponse = productsApi.getMyProductsAll()
            if (productsResponse.status != 200) {
                return Result.failure(Exception(productsResponse.message ?: "Failed to fetch products"))
            }
            
            // Handle null data as empty list (valid for users with no products)
            val productsList = productsResponse.data ?: emptyList()
            
            // Extract categories from products (in case there are categories not in categories endpoint)
            val categoriesFromProductsMap = mutableMapOf<String, com.indybrain.indypos_Android.data.remote.dto.CategoryDto>()
            productsList.forEach { productDto ->
                productDto.category?.let { categoryDto ->
                    categoriesFromProductsMap[categoryDto.id] = categoryDto
                }
            }
            
            // Merge categories: use categories from categories endpoint, but also include any from products
            val allCategoriesMap = mutableMapOf<String, com.indybrain.indypos_Android.data.remote.dto.CategoryDto>()
            categoriesList.forEach { categoryDto ->
                allCategoriesMap[categoryDto.id] = categoryDto
            }
            categoriesFromProductsMap.forEach { (id, categoryDto) ->
                allCategoriesMap[id] = categoryDto
            }
            
            // Convert and save categories
            val categories = allCategoriesMap.values.map { ProductMapper.toEntity(it) }
            // Important: do NOT call deleteAll() here.
            // Using REPLACE strategy keeps existing rows while updating data.
            // This preserves any local changes or offline-created categories.
            categoryDao.insertAll(categories)
            
            // Extract addon groups and addons from products
            val addonGroupsMap = mutableMapOf<String, com.indybrain.indypos_Android.data.remote.dto.AddonGroupDto>()
            val addonsMap = mutableMapOf<String, Pair<com.indybrain.indypos_Android.data.remote.dto.AddonDto, String?>>()
            
            productsList.forEach { productDto ->
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
            val products = productsList.map { ProductMapper.toEntity(it) }
            // Important: do NOT call deleteAll() here.
            // Deleting all products would trigger the foreign key on cart_items
            // (onDelete = SET_NULL) and clear productId on existing cart items,
            // which makes quantities disappear in the product list after refresh.
            // Using REPLACE keeps existing rows (and cart relations) while updating data.
            productDao.insertAll(products)
            
            // Convert and save addon groups
            val addonGroups = addonGroupsMap.values.map { ProductMapper.toEntity(it) }
            // Important: do NOT call deleteAll() here.
            // Using REPLACE strategy keeps existing rows while updating data.
            // This preserves any local changes or offline-created addon groups.
            addonGroupDao.insertAll(addonGroups)
            
            // Convert and save addons
            val addons = addonsMap.values.map { (addonDto, groupId) ->
                ProductMapper.toEntity(addonDto, groupId)
            }
            // Important: do NOT call deleteAll() here.
            // Using REPLACE strategy keeps existing rows while updating data.
            // This preserves any local changes or offline-created addons.
            addonDao.insertAll(addons)
            
            // Delete old addon group-addon junctions for all addon groups being synced
            // This ensures we remove junctions for addon groups that no longer have addons
            addonGroupsMap.values.forEach { addonGroupDto ->
                addonGroupAddonJunctionDao.deleteByAddonGroupId(addonGroupDto.id)
            }
            
            // Save addon group-addon junctions
            // Note: Use addonGroupsMap.values to avoid duplicate inserts when same addon group is used in multiple products
            addonGroupsMap.values.forEach { addonGroupDto ->
                addonGroupDto.addons?.forEachIndexed { addonIndex, addonDto ->
                    addonGroupAddonJunctionDao.insert(
                        com.indybrain.indypos_Android.data.local.entity.AddonGroupAddonJunctionEntity(
                            addonGroupId = addonGroupDto.id,
                            addonId = addonDto.id,
                            sortOrder = addonIndex + 1
                        )
                    )
                }
            }
            
            // Delete old product-addon group junctions for all products being synced
            // This ensures we remove junctions for products that no longer have addon groups
            productsList.forEach { productDto ->
                productAddonGroupJunctionDao.deleteByProductId(productDto.id)
            }
            
            // Save product-addon group junctions
            productsList.forEach { productDto ->
                val productId = productDto.id
                productDto.addonGroups?.forEach { addonGroupDto ->
                    productAddonGroupJunctionDao.insert(
                        com.indybrain.indypos_Android.data.local.entity.ProductAddonGroupJunctionEntity(
                            productId = productId,
                            addonGroupId = addonGroupDto.id
                        )
                    )
                }
            }
            
            // Remove local items that are no longer in API (e.g. deleted on another device)
            // Order: addons -> addon groups -> products -> categories (respect potential FK/cache)
            val apiAddonIds = addonsMap.keys.toSet()
            val existingAddonIds = addonDao.getAllAddons().map { it.id }.toSet()
            (existingAddonIds - apiAddonIds).forEach { id ->
                addonGroupAddonJunctionDao.deleteByAddonId(id)
                addonDao.permanentlyDeleteAddon(id)
            }
            val apiAddonGroupIds = addonGroupsMap.keys.toSet()
            val existingAddonGroupIds = addonGroupDao.getAllAddonGroups().map { it.id }.toSet()
            (existingAddonGroupIds - apiAddonGroupIds).forEach { id ->
                addonGroupAddonJunctionDao.deleteByAddonGroupId(id)
                addonGroupDao.permanentlyDeleteAddonGroup(id)
            }
            val apiProductIds = productsList.map { it.id }.toSet()
            val existingProductIds = productDao.getAllProducts().mapNotNull { it.id }.toSet()
            (existingProductIds - apiProductIds).forEach { id ->
                productAddonGroupJunctionDao.deleteByProductId(id)
                productDao.deleteProductById(id)
            }
            val apiCategoryIdsFromSync = allCategoriesMap.keys.toSet()
            val existingCategoryIdsFromSync = categoryDao.getAllCategories().map { it.id }.toSet()
            (existingCategoryIdsFromSync - apiCategoryIdsFromSync).forEach { id ->
                categoryDao.deleteCategoryById(id)
            }
            
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
            if (productsResponse.status != 200) {
                return Result.failure(Exception(productsResponse.message ?: "Failed to fetch products"))
            }
            
            // If status is 200, treat as success even if data is null or empty (new user might have no data)
            val productsList = productsResponse.data ?: emptyList()
            
            // Extract categories from products and save them
            val categoriesMap = mutableMapOf<String, com.indybrain.indypos_Android.data.remote.dto.CategoryDto>()
            productsList.forEach { productDto ->
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
            
            productsList.forEach { productDto ->
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
            
            // Delete old addon group-addon junctions for all addon groups being synced
            // This ensures we remove junctions for addon groups that no longer have addons
            addonGroupsMap.values.forEach { addonGroupDto ->
                addonGroupAddonJunctionDao.deleteByAddonGroupId(addonGroupDto.id)
            }
            
            // Save addon group-addon junctions
            // Note: Use addonGroupsMap.values to avoid duplicate inserts when same addon group is used in multiple products
            if (addonGroupsMap.isNotEmpty()) {
                addonGroupsMap.values.forEach { addonGroupDto ->
                    addonGroupDto.addons?.forEachIndexed { addonIndex, addonDto ->
                        addonGroupAddonJunctionDao.insert(
                            com.indybrain.indypos_Android.data.local.entity.AddonGroupAddonJunctionEntity(
                                addonGroupId = addonGroupDto.id,
                                addonId = addonDto.id,
                                sortOrder = addonIndex + 1
                            )
                        )
                    }
                }
            }
            
            // Convert and save products
            val products = productsList.map { ProductMapper.toEntity(it) }
            // Important: do NOT call deleteAll() here.
            // Deleting all products would trigger the foreign key on cart_items
            // (onDelete = SET_NULL) and clear productId on existing cart items,
            // which makes quantities disappear in the product list after refresh.
            // Using REPLACE keeps existing rows (and cart relations) while updating data.
            if (products.isNotEmpty()) {
                productDao.insertAll(products)
            }
            
            // Delete old product-addon group junctions for all products being synced
            // This ensures we remove junctions for products that no longer have addon groups
            productsList.forEach { productDto ->
                productAddonGroupJunctionDao.deleteByProductId(productDto.id)
            }
            
            // Save product-addon group junctions
            productsList.forEach { productDto ->
                val productId = productDto.id
                productDto.addonGroups?.forEach { addonGroupDto ->
                    productAddonGroupJunctionDao.insert(
                        com.indybrain.indypos_Android.data.local.entity.ProductAddonGroupJunctionEntity(
                            productId = productId,
                            addonGroupId = addonGroupDto.id
                        )
                    )
                }
            }
            
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
            if (categoriesResponse.status != 200) {
                return Result.failure(Exception(categoriesResponse.message ?: "Failed to fetch categories"))
            }
            
            // If status is 200, treat as success even if data is null or empty (new user might have no data)
            val categoriesList = categoriesResponse.data ?: emptyList()
            
            // Get existing categories from Room
            val existingCategories = categoryDao.getAllCategories()
            val existingCategoryIds = existingCategories.map { it.id }.toSet()
            
            // Convert API categories to entities
            val apiCategories = categoriesList.map { ProductMapper.toEntity(it) }
            
            // Find new categories that don't exist in Room
            val newCategories = apiCategories.filter { it.id !in existingCategoryIds }
            
            // Insert only new categories (existing ones are already in Room)
            if (newCategories.isNotEmpty()) {
                categoryDao.insertAll(newCategories)
            }
            
            // Also update existing categories if they have changed (using REPLACE strategy)
            // This ensures data stays in sync
            if (apiCategories.isNotEmpty()) {
                categoryDao.insertAll(apiCategories)
            }
            
            // Remove local categories that are no longer in API (e.g. deleted on another device)
            val apiCategoryIds = apiCategories.map { it.id }.toSet()
            val idsToRemove = existingCategoryIds - apiCategoryIds
            idsToRemove.forEach { categoryDao.deleteCategoryById(it) }
            
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

    override suspend fun getCategoriesPaginated(
        page: Int,
        limit: Int,
        search: String?
    ): Result<CategoriesPaginatedResult> {
        return try {
            val response = productsApi.getCategoriesPaginated(
                page = page,
                limit = limit,
                search = search?.takeIf { it.isNotBlank() }
            )
            if (response.status != 200) {
                return Result.failure(Exception(response.message ?: "Failed to fetch categories"))
            }
            val data = response.data
            val categoriesList = data?.categories ?: emptyList()
            val pagination = data?.pagination
            val categories = categoriesList.map { ProductMapper.toEntity(it) }
            Result.success(
                CategoriesPaginatedResult(
                    categories = categories,
                    currentPage = pagination?.currentPage ?: page,
                    totalCount = pagination?.totalCount ?: categories.size,
                    hasNext = pagination?.hasNext ?: false
                )
            )
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
    
    override suspend fun deleteMultipleCategories(categoryIds: List<String>): Result<DeleteCategoriesResult> {
        return try {
            if (categoryIds.isEmpty()) {
                return Result.failure(Exception("กรุณาเลือกหมวดหมู่ที่ต้องการลบ"))
            }
            
            if (networkConnectivityChecker.isConnected()) {
                try {
                    val request = DeleteCategoriesRequestDto(categoryIds = categoryIds)
                    val response = productsApi.deleteMultipleCategories(request)
                    
                    if (response.status == 200) {
                        val deletedIds = response.deletedIds.orEmpty()
                        val totalDeleted = response.data?.totalDeleted ?: response.count
                        val totalFailed = response.data?.totalFailed ?: response.failedDeletions.orEmpty().size
                        val errors = response.errors.orEmpty()
                        
                        deletedIds.forEach { id ->
                            categoryDao.deleteCategoryById(id)
                        }
                        
                        Result.success(
                            DeleteCategoriesResult(
                                deletedCount = totalDeleted,
                                failedCount = totalFailed,
                                errors = errors
                            )
                        )
                    } else {
                        val errorMessage = response.message.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการลบหมวดหมู่"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    val errorBody = e.response()?.errorBody()
                    val errorMessage = when (e.code()) {
                        400 -> parseApiErrorResponse(errorBody, e.code())
                        401 -> {
                            val parsed = parseApiErrorResponse(errorBody, e.code())
                            if (parsed.contains("Unauthorized", ignoreCase = true)) {
                                "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                            } else parsed
                        }
                        403 -> parseApiErrorResponse(errorBody, e.code())
                        404 -> parseApiErrorResponse(errorBody, e.code())
                        500 -> parseApiErrorResponse(errorBody, e.code())
                        else -> parseApiErrorResponse(errorBody, e.code())
                    }
                    Result.failure(Exception(errorMessage))
                }
            } else {
                var deletedCount = 0
                categoryIds.forEach { id ->
                    val existing = categoryDao.getCategoryById(id)
                    if (existing != null) {
                        categoryDao.markAsDeletedLocally(id)
                        deletedCount++
                    }
                }
                Result.success(DeleteCategoriesResult(deletedCount = deletedCount))
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
                
                // Check for specific error keys first
                val errorKey = errorResponse.error?.takeIf { it.isNotBlank() }
                val messageKey = errorResponse.message?.takeIf { it.isNotBlank() }
                val combinedErrorText = "$errorText $messageText"
                
                // Helper function to get localized string (respects user's selected language)
                fun getLocalizedString(resourceName: String, fallback: String): String {
                    val resourceId = context.resources.getIdentifier(
                        resourceName,
                        "string",
                        context.packageName
                    )
                    return if (resourceId != 0) {
                        val localeCode = languageLocalDataSource.getLanguageLocale()
                        val localizedContext = LocaleHelper.setLocale(context, localeCode)
                        localizedContext.getString(resourceId)
                    } else {
                        fallback
                    }
                }
                
                // Handle category_form_validation_exists error key
                when {
                    errorKey == "category_form_validation_exists" || messageKey == "category_form_validation_exists" -> {
                        return getLocalizedString("category_form_validation_exists", "หมวดหมู่นี้มีอยู่แล้ว")
                    }
                }
                
                // Category delete API errors (map en/th locale)
                when {
                    combinedErrorText.contains("category id is required") ||
                    combinedErrorText.contains("กรุณาระบุรหัสหมวดหมู่") -> {
                        return getLocalizedString("api_error_delete_category_id_required", "จำเป็นต้องระบุรหัสหมวดหมู่")
                    }
                    combinedErrorText.contains("category ids are required") ||
                    combinedErrorText.contains("at least one category id") ||
                    combinedErrorText.contains("กรุณาเลือกหมวดหมู่") -> {
                        return getLocalizedString("api_error_delete_category_ids_required", "ต้องระบุรหัสหมวดหมู่อย่างน้อย 1 รายการ")
                    }
                    combinedErrorText.contains("category not found") ||
                    combinedErrorText.contains("ไม่พบหมวดหมู่") -> {
                        return getLocalizedString("api_error_delete_category_not_found", "ไม่พบหมวดหมู่")
                    }
                    combinedErrorText.contains("only delete your own categories") ||
                    combinedErrorText.contains("own categories") ||
                    combinedErrorText.contains("หมวดหมู่ของคุณ") -> {
                        return getLocalizedString("api_error_delete_category_not_owner", "คุณสามารถลบได้เฉพาะหมวดหมู่ของคุณเท่านั้น")
                    }
                    combinedErrorText.contains("invalid request body") ||
                    combinedErrorText.contains("invalid character") ||
                    combinedErrorText.contains("ข้อมูลที่ส่งไม่ถูกต้อง") -> {
                        return getLocalizedString("api_error_delete_category_bad_request", "คำขอไม่ถูกต้อง กรุณาตรวจสอบข้อมูล")
                    }
                    combinedErrorText.contains("missing authorization") ||
                    combinedErrorText.contains("authorization") ||
                    combinedErrorText.contains("unauthorized") -> {
                        return getLocalizedString("api_error_delete_category_generic", "ไม่สามารถลบหมวดหมู่ได้ กรุณาลองใหม่อีกครั้ง")
                    }
                }
                
                // Handle product error keys
                when {
                    errorKey == "product_error_duplicate_sku" || messageKey == "product_error_duplicate_sku" -> {
                        return getLocalizedString("product_error_duplicate_sku", "รหัส SKU นี้มีอยู่แล้ว")
                    }
                    errorKey == "product_error_duplicate_name" || messageKey == "product_error_duplicate_name" -> {
                        return getLocalizedString("product_error_duplicate_name", "ชื่อนี้มีอยู่แล้ว")
                    }
                    errorKey == "product_error_duplicate_code" || messageKey == "product_error_duplicate_code" -> {
                        return getLocalizedString("product_error_duplicate_code", "รหัสสินค้านี้มีอยู่แล้ว")
                    }
                }
                
                // Handle product error messages by content (for cases where error key is not provided)
                when {
                    combinedErrorText.contains("duplicate sku") || combinedErrorText.contains("duplicate sku code") -> {
                        return getLocalizedString("product_error_duplicate_sku", "รหัส SKU นี้มีอยู่แล้ว")
                    }
                    combinedErrorText.contains("duplicate product name") || combinedErrorText.contains("duplicate name") -> {
                        return getLocalizedString("product_error_duplicate_name", "ชื่อนี้มีอยู่แล้ว")
                    }
                    combinedErrorText.contains("duplicate product code") || combinedErrorText.contains("duplicate code") -> {
                        return getLocalizedString("product_error_duplicate_code", "รหัสสินค้านี้มีอยู่แล้ว")
                    }
                    // Product delete API errors (map en/th locale)
                    combinedErrorText.contains("product id is required") ||
                    combinedErrorText.contains("กรุณาระบุรหัสสินค้า") -> {
                        return getLocalizedString("api_error_delete_product_id_required", "จำเป็นต้องระบุรหัสสินค้า")
                    }
                    combinedErrorText.contains("product ids are required") ||
                    combinedErrorText.contains("at least one product id") ||
                    combinedErrorText.contains("กรุณาเลือกสินค้า") -> {
                        return getLocalizedString("api_error_delete_product_ids_required", "ต้องระบุรหัสสินค้าอย่างน้อย 1 รายการ")
                    }
                    combinedErrorText.contains("product not found") ||
                    combinedErrorText.contains("ไม่พบสินค้า") -> {
                        return getLocalizedString("api_error_delete_product_not_found", "ไม่พบสินค้า")
                    }
                    combinedErrorText.contains("only delete your own products") ||
                    combinedErrorText.contains("own products") ||
                    combinedErrorText.contains("ลบเฉพาะสินค้าของคุณเอง") ||
                    combinedErrorText.contains("สินค้าของคุณเอง") -> {
                        return getLocalizedString("api_error_delete_not_owner", "คุณสามารถลบได้เฉพาะสินค้าของคุณเท่านั้น")
                    }
                    combinedErrorText.contains("invalid request body") ||
                    combinedErrorText.contains("invalid character") ||
                    combinedErrorText.contains("ข้อมูลที่ส่งไม่ถูกต้อง") -> {
                        return getLocalizedString("api_error_delete_invalid_request_body", "ข้อมูลที่ส่งไม่ถูกต้อง")
                    }
                    combinedErrorText.contains("missing authorization") ||
                    combinedErrorText.contains("authorization") ||
                    combinedErrorText.contains("กรุณาเข้าสู่ระบบ") -> {
                        return getLocalizedString("api_error_delete_generic", "ไม่สามารถลบสินค้าได้ กรุณาลองใหม่อีกครั้ง")
                    }
                }
                
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
                        // Check if it's a product error first
                        val errorTextValue = errorResponse.error?.takeIf { it.isNotBlank() }
                        val messageTextValue = errorResponse.message?.takeIf { it.isNotBlank() }
                        val combinedText = "$errorTextValue $messageTextValue".lowercase()
                        
                        // Check for category duplicate errors first
                        when {
                            combinedText.contains("duplicate category name") || 
                            errorTextValue?.lowercase()?.contains("duplicate category name") == true -> {
                                getLocalizedString("category_error_duplicate_name", "ชื่อหมวดหมู่นี้มีอยู่แล้ว / Duplicate category name")
                            }
                            // Check for product duplicate errors
                            combinedText.contains("duplicate sku") || combinedText.contains("duplicate sku code") -> {
                                getLocalizedString("product_error_duplicate_sku", "รหัส SKU นี้มีอยู่แล้ว")
                            }
                            combinedText.contains("duplicate product name") || combinedText.contains("duplicate name") -> {
                                getLocalizedString("product_error_duplicate_name", "ชื่อนี้มีอยู่แล้ว")
                            }
                            combinedText.contains("duplicate product code") || combinedText.contains("duplicate code") -> {
                                getLocalizedString("product_error_duplicate_code", "รหัสสินค้านี้มีอยู่แล้ว")
                            }
                            // For other duplicate errors
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
                
                // Helper function to get localized string (respects user's selected language)
                fun getLocalizedString(resourceName: String, fallback: String): String {
                    val resourceId = context.resources.getIdentifier(
                        resourceName,
                        "string",
                        context.packageName
                    )
                    return if (resourceId != 0) {
                        val localeCode = languageLocalDataSource.getLanguageLocale()
                        val localizedContext = LocaleHelper.setLocale(context, localeCode)
                        localizedContext.getString(resourceId)
                    } else {
                        fallback
                    }
                }
                
                when {
                    statusCode == 409 && errorLower.contains("duplicate category name") -> {
                        getLocalizedString("category_error_duplicate_name", "ชื่อหมวดหมู่นี้มีอยู่แล้ว / Duplicate category name")
                    }
                    statusCode == 403 && errorLower.contains("free_plan_limit_exceeded") -> {
                        "คุณใช้หมวดหมู่ครบจำนวนที่กำหนดแล้ว กรุณาอัปเกรดแผน"
                    }
                    // Product duplicate errors
                    errorLower.contains("duplicate sku") || errorLower.contains("duplicate sku code") -> {
                        getLocalizedString("product_error_duplicate_sku", "รหัส SKU นี้มีอยู่แล้ว")
                    }
                    errorLower.contains("duplicate product name") || errorLower.contains("duplicate name") -> {
                        getLocalizedString("product_error_duplicate_name", "ชื่อนี้มีอยู่แล้ว")
                    }
                    errorLower.contains("duplicate product code") || errorLower.contains("duplicate code") -> {
                        getLocalizedString("product_error_duplicate_code", "รหัสสินค้านี้มีอยู่แล้ว")
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
    
    override suspend fun getProductByCode(code: String): ProductEntity? {
        return productDao.getProductByCode(code)
    }

    override suspend fun getProductDetailFromApi(productId: String): Result<ProductDetailData> {
        if (!networkConnectivityChecker.isConnected()) {
            return Result.failure(Exception("No network connection"))
        }
        return try {
            val response = productsApi.getProductDetail(productId)
            if (response.status == 200 && response.data != null) {
                val dto = response.data
                val product = ProductMapper.toEntity(dto)
                val addonGroups = dto.addonGroups
                    ?.filter { it.isActive }
                    ?.map { ProductMapper.toEntity(it) }
                    ?.sortedBy { it.sortOrder ?: 0 }
                    ?: emptyList()
                val addonsByGroup = addonGroups.associate { group ->
                    val addons = (dto.addonGroups?.find { it.id == group.id }?.addons)
                        ?.filter { it.isActive }
                        ?.map { ProductMapper.toEntity(it, group.id) }
                        ?.sortedBy { it.sortOrder ?: 0 }
                        ?: emptyList()
                    group.id to addons
                }
                val category = dto.category?.let { ProductMapper.toEntity(it) }
                Result.success(
                    ProductDetailData(
                        product = product,
                        addonGroups = addonGroups,
                        addonsByGroup = addonsByGroup,
                        category = category
                    )
                )
            } else {
                Result.failure(Exception(response.message ?: "Unknown error"))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()
            val message = parseApiErrorResponse(errorBody, e.code())
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProductListFromApi(categoryId: String?): Result<ProductListData> {
        if (!networkConnectivityChecker.isConnected()) {
            return Result.failure(Exception("No network connection"))
        }
        return try {
            val response = productsApi.getProductList(categoryId)
            if (response.status == 200 && response.data != null) {
                val data = response.data
                val categories = data.categories.map { ProductMapper.toEntity(it) }
                val products = data.products
                    .filter { it.categoryId != null && it.categoryId.isNotBlank() }
                    .map { ProductMapper.toEntity(it) }
                    .sortedBy { it.popularityRank ?: Int.MAX_VALUE }

                Result.success(
                    ProductListData(
                        categories = categories,
                        products = products
                    )
                )
            } else {
                Result.failure(Exception(response.message ?: "Unknown error"))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()
            val message = parseApiErrorResponse(errorBody, e.code())
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun ensureProductExists(product: ProductEntity, category: CategoryEntity?) {
        if (product.categoryId != null && category != null && categoryDao.getCategoryById(category.id) == null) {
            categoryDao.insert(category)
        }
        if (productDao.getProductById(product.id) == null) {
            productDao.insertAll(listOf(product))
        }
    }
    
    override suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val response = productsApi.deleteProduct(productId)
                    
                    if (response.status == 200) {
                        // API success - clear cart items first (FK NO_ACTION จะ error ถ้ามี cart อ้างอิง)
                        cartRepository.clearCartItemsByProduct(productId)
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
                // No network - mark as deleted locally and unsynced
                val product = productDao.getProductById(productId)
                if (product == null) {
                    return Result.failure(Exception("ไม่พบสินค้าที่ต้องการลบ"))
                }
                
                if (!product.isSynced && !product.isFromServer) {
                    // Not synced and not from server - clear cart first then permanently delete
                    cartRepository.clearCartItemsByProduct(productId)
                    productDao.deleteProductById(productId)
                } else {
                    // Mark as deleted locally and unsynced for sync later
                    productDao.markAsDeletedLocallyAndUnsynced(productId)
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการลบสินค้า"))
        }
    }
    
    override suspend fun deleteMultipleProducts(productIds: List<String>): Result<DeleteProductsResult> {
        return try {
            if (productIds.isEmpty()) {
                return Result.failure(Exception("กรุณาเลือกสินค้าที่ต้องการลบ"))
            }
            
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val request = DeleteProductsRequestDto(productIds = productIds)
                    val response = productsApi.deleteMultipleProducts(request)
                    
                    if (response.status == 200) {
                        // API returns 200 for both full and partial success
                        // Use deleted_ids from response - only delete what server actually deleted
                        val deletedIds = response.deletedIds.orEmpty()
                        val totalDeleted = response.data?.totalDeleted ?: response.count
                        val totalFailed = response.data?.totalFailed ?: response.failedDeletions.orEmpty().size
                        val errors = response.errors.orEmpty()
                        
                        deletedIds.forEach { productId ->
                            cartRepository.clearCartItemsByProduct(productId)
                            productDao.deleteProductById(productId)
                        }
                        
                        Result.success(
                            DeleteProductsResult(
                                deletedCount = totalDeleted,
                                failedCount = totalFailed,
                                errors = errors
                            )
                        )
                    } else {
                        val errorMessage = response.message.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการลบสินค้า"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    val errorBody = e.response()?.errorBody()
                    val errorMessage = parseApiErrorResponse(errorBody, e.code())
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network - mark as deleted locally and unsynced
                var deletedCount = 0
                productIds.forEach { productId ->
                    val product = productDao.getProductById(productId)
                    if (product != null) {
                        if (!product.isSynced && !product.isFromServer) {
                            cartRepository.clearCartItemsByProduct(productId)
                            productDao.deleteProductById(productId)
                        } else {
                            productDao.markAsDeletedLocallyAndUnsynced(productId)
                        }
                        deletedCount++
                    }
                }
                Result.success(DeleteProductsResult(deletedCount = deletedCount))
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
            
            // Verify URI is accessible
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                if (inputStream == null) {
                    return Result.failure(Exception("ไม่สามารถอ่านไฟล์รูปภาพได้ กรุณาลองใหม่อีกครั้ง"))
                }
                inputStream.close()
            } catch (e: Exception) {
                return Result.failure(Exception("ไม่สามารถเข้าถึงไฟล์รูปภาพได้: ${e.message}"))
            }
            
            // Resize image to 150x150 before upload (balanced size for clarity and file size)
            val resizedBitmap = com.indybrain.indypos_Android.core.utils.ImageUtils.resizeImage(
                imageUri = imageUri,
                targetWidth = 150,
                targetHeight = 150,
                context = context
            )
            
            if (resizedBitmap == null) {
                return Result.failure(Exception("ไม่สามารถประมวลผลรูปภาพได้ กรุณาตรวจสอบว่าไฟล์รูปภาพถูกต้อง"))
            }
            
            // Save resized bitmap to temporary file using JPEG with high quality
            val tempFile = File(context.cacheDir, "upload_resized_${System.currentTimeMillis()}.jpg")
            val saved = com.indybrain.indypos_Android.core.utils.ImageUtils.saveBitmapToFile(
                bitmap = resizedBitmap,
                file = tempFile,
                quality = 95  // High quality JPEG (95-100) for good balance
            )
            
            if (!saved) {
                resizedBitmap.recycle()
                return Result.failure(Exception("ไม่สามารถบันทึกไฟล์รูปภาพได้"))
            }
            
            // Get MIME type for JPEG
            val mimeType = "image/jpeg"
            val mediaType = mimeType.toMediaTypeOrNull() ?: "image/jpeg".toMediaTypeOrNull()
            
            // Create request body
            val requestFile = tempFile.asRequestBody(mediaType)
            val body = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
            
            // Upload image
            val response = productsApi.uploadProductImage(body)
            
            // Clean up
            resizedBitmap.recycle()
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
                        
                        // Delete old addon group relationships before inserting new ones
                        // This ensures we remove old junctions if product was updated
                        productAddonGroupJunctionDao.deleteByProductId(productEntity.id)
                        
                        // Save addon group relationships from response
                        productDto.addonGroups?.forEach { addonGroupDto ->
                            productAddonGroupJunctionDao.insert(
                                com.indybrain.indypos_Android.data.local.entity.ProductAddonGroupJunctionEntity(
                                    productId = productEntity.id,
                                    addonGroupId = addonGroupDto.id
                                )
                            )
                        }
                        
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
                
                // Save addon group relationships locally
                addonGroupIds?.forEach { addonGroupId ->
                    productAddonGroupJunctionDao.insert(
                        com.indybrain.indypos_Android.data.local.entity.ProductAddonGroupJunctionEntity(
                            productId = productEntity.id,
                            addonGroupId = addonGroupId
                        )
                    )
                }
                Result.success(productEntity)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการสร้างสินค้า"))
        }
    }
    
    override suspend fun updateProduct(
        productId: String,
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
        return try {
            val existingProduct = productDao.getProductById(productId)
                ?: return Result.failure(Exception("ไม่พบสินค้าที่ต้องการอัปเดต"))
            
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val request = UpdateProductRequestDto(
                        name = name,
                        description = existingProduct.description,
                        price = price,
                        costPrice = costPrice,
                        imageUrl = imageUrl,
                        categoryId = categoryId,
                        popularityRank = existingProduct.popularityRank,
                        productCode = productCode,
                        unit = unit,
                        skuCode = skuCode,
                        stockQuantity = stockQuantity,
                        minStockQuantity = existingProduct.minStockQuantity,
                        selectedUnit = existingProduct.selectedUnit,
                        selectedColorHex = selectedColorHex,
                        isSkuEnabled = isSkuEnabled,
                        isStockEnabled = isStockEnabled,
                        hasAdditionalOptions = hasAdditionalOptions,
                        isActive = existingProduct.isActive,
                        addonGroupIds = addonGroupIds
                    )
                    
                    val response = productsApi.updateProduct(productId, request)
                    
                    if (response.status == 200 && response.data != null) {
                        // API success - response has nested structure: data.product
                        val productDto = response.data.product
                        val productEntity = ProductMapper.toEntity(productDto)
                        productDao.insertAll(listOf(productEntity))
                        
                        // Refresh addon group relationships using addon_group_ids from response
                        productAddonGroupJunctionDao.deleteByProductId(productId)
                        response.data.addonGroupIds?.forEach { addonGroupId ->
                            productAddonGroupJunctionDao.insert(
                                com.indybrain.indypos_Android.data.local.entity.ProductAddonGroupJunctionEntity(
                                    productId = productId,
                                    addonGroupId = addonGroupId
                                )
                            )
                        }
                        
                        Result.success(productEntity)
                    } else {
                        val errorMessage = response.error?.takeIf { it.isNotBlank() }
                            ?: response.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการอัปเดตสินค้า"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    val errorBody = e.response()?.errorBody()
                    val errorMessage = parseApiErrorResponse(errorBody, e.code())
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network - update in Room only (for sync later)
                val updatedProduct = existingProduct.copy(
                    name = name,
                    productCode = productCode,
                    price = price,
                    costPrice = costPrice,
                    unit = unit,
                    imageUrl = imageUrl,
                    selectedColorHex = selectedColorHex,
                    categoryId = categoryId,
                    skuCode = skuCode,
                    stockQuantity = stockQuantity,
                    isSkuEnabled = isSkuEnabled,
                    isStockEnabled = isStockEnabled,
                    hasAdditionalOptions = hasAdditionalOptions,
                    isSynced = false,
                    updatedAt = Date()
                )
                
                productDao.insertAll(listOf(updatedProduct))
                
                // Refresh addon group relationships locally
                productAddonGroupJunctionDao.deleteByProductId(productId)
                addonGroupIds?.forEach { addonGroupId ->
                    productAddonGroupJunctionDao.insert(
                        com.indybrain.indypos_Android.data.local.entity.ProductAddonGroupJunctionEntity(
                            productId = productId,
                            addonGroupId = addonGroupId
                        )
                    )
                }
                
                Result.success(updatedProduct)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการอัปเดตสินค้า"))
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
    
    override suspend fun updateProductStock(productId: String, delta: Int): Result<ProductEntity> {
        return try {
            val existingProduct = productDao.getProductById(productId)
            if (existingProduct == null) {
                return Result.failure(Exception("ไม่พบสินค้าที่ต้องการอัปเดต"))
            }
            
            val oldQuantity = existingProduct.stockQuantity ?: 0
            val newQuantity = oldQuantity + delta
            
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val response = productsApi.updateProductStock(
                        productId,
                        UpdateProductStockRequestDto(delta)
                    )
                    
                    if (response.status == 200) {
                        // API success - update in Room
                        // Even if data is null, status 200 means success
                        val updatedProduct = existingProduct.copy(
                            stockQuantity = newQuantity,
                            isSynced = true,
                            updatedAt = Date()
                        )
                        productDao.insertAll(listOf(updatedProduct))
                        
                        Result.success(updatedProduct)
                    } else {
                        val errorMessage = response.error?.takeIf { it.isNotBlank() }
                            ?: response.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการอัปเดตสต็อก"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    val errorBody = e.response()?.errorBody()
                    val errorMessage = parseApiErrorResponse(errorBody, e.code())
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network - update in Room only (for sync later)
                val updatedProduct = existingProduct.copy(
                    stockQuantity = newQuantity,
                    isSynced = false,
                    updatedAt = Date()
                )
                productDao.insertAll(listOf(updatedProduct))
                
                Result.success(updatedProduct)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการอัปเดตสต็อก"))
        }
    }
    
    override suspend fun clearCartItemsByProduct(productId: String) {
        cartRepository.clearCartItemsByProduct(productId)
    }
    
    /**
     * Sync categories to server
     */
    override suspend fun syncCategories(): Result<Unit> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.success(Unit) // No network, skip sync
            }
            
            // Get all unsynced categories
            val unsynced = categoryDao.getUnsyncedCategories()
            val deleted = categoryDao.getDeletedCategories()
            
            if (unsynced.isEmpty() && deleted.isEmpty()) {
                return Result.success(Unit) // Nothing to sync
            }
            
            // Date formatter for ISO string
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            
            // Convert to sync items
            val syncItems = (unsynced + deleted).map { entity ->
                SyncCategoryItemDto(
                    id = entity.id,
                    name = entity.name,
                    isActive = entity.isActive,
                    isSynced = entity.isSynced,
                    isDeletedLocally = entity.isDeletedLocally,
                    createdAt = dateFormat.format(entity.createdAt),
                    updatedAt = dateFormat.format(entity.updatedAt)
                )
            }
            
            val request = SyncCategoriesRequestDto(categories = syncItems)
            val response = productsApi.syncCategories(request)
            
            // Process sync results
            response.data?.forEach { result ->
                when {
                    result.shouldDelete -> {
                        // Delete locally
                        categoryDao.deleteCategoryById(result.id)
                    }
                    result.serverData != null -> {
                        // Update with server data
                        val serverEntity = ProductMapper.toEntity(result.serverData)
                        categoryDao.insert(serverEntity)
                    }
                    result.status.lowercase() == "success" -> {
                        // Mark as synced
                        categoryDao.markAsSynced(result.id)
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                403 -> {
                    val errorBody = e.response()?.errorBody()?.string()
                    if (errorBody?.contains("free_plan_limit_exceeded", ignoreCase = true) == true) {
                        "คุณใช้หมวดหมู่ครบจำนวนที่กำหนดแล้ว กรุณาอัปเกรดแผน"
                    } else {
                        e.message() ?: "เกิดข้อผิดพลาดในการ sync"
                    }
                }
                500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                else -> e.message() ?: "เกิดข้อผิดพลาดในการ sync"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการ sync"))
        }
    }
    
    /**
     * Sync products to server
     */
    override suspend fun syncProducts(): Result<Unit> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.success(Unit) // No network, skip sync
            }
            
            // Get all unsynced products
            val unsynced = productDao.getUnsyncedProducts()
            val deleted = productDao.getDeletedProducts()
            
            if (unsynced.isEmpty() && deleted.isEmpty()) {
                return Result.success(Unit) // Nothing to sync
            }
            
            // Date formatter for ISO string
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            
            // Convert to sync items
            val syncItems = (unsynced + deleted).mapNotNull { entity ->
                val addonGroupIds = productAddonGroupJunctionDao.getAddonGroupIdsByProductIdSync(entity.id)
                
                SyncProductItemDto(
                    id = entity.id,
                    name = entity.name ?: "",
                    description = entity.description ?: "",
                    price = entity.price,
                    costPrice = entity.costPrice ?: 0.0,
                    imageUrl = entity.imageUrl ?: "",
                    categoryId = entity.categoryId ?: "",
                    popularityRank = entity.popularityRank ?: 0,
                    productCode = entity.productCode ?: "",
                    unit = entity.unit ?: "",
                    skuCode = entity.skuCode ?: "",
                    stockQuantity = entity.stockQuantity ?: 0,
                    minStockQuantity = entity.minStockQuantity ?: 0,
                    selectedUnit = entity.selectedUnit ?: "",
                    selectedColorHex = entity.selectedColorHex ?: "",
                    isSkuEnabled = entity.isSkuEnabled ?: false,
                    isStockEnabled = entity.isStockEnabled ?: false,
                    hasAdditionalOptions = entity.hasAdditionalOptions ?: false,
                    isActive = entity.isActive,
                    isSynced = entity.isSynced,
                    isDeletedLocally = entity.isDeletedLocally,
                    createdAt = dateFormat.format(entity.createdAt),
                    updatedAt = dateFormat.format(entity.updatedAt),
                    addonGroupIds = addonGroupIds
                )
            }
            
            val request = SyncProductsRequestDto(products = syncItems)
            val response = productsApi.syncProducts(request)
            
            // Process sync results
            response.data?.forEach { result ->
                when {
                    result.shouldDelete -> {
                        // Delete locally
                        result.id?.let { id ->
                            productDao.permanentlyDelete(id)
                        }
                    }
                    result.serverData != null -> {
                        // Update with server data
                        result.id?.let { id ->
                            val serverEntity = ProductMapper.toEntity(result.serverData)
                            productDao.insertAll(listOf(serverEntity))
                            
                            // Delete old addon group relationships before inserting new ones
                            // This ensures we remove old junctions that are no longer in server data
                            productAddonGroupJunctionDao.deleteByProductId(id)
                            
                            // Update addon group relationships
                            result.serverData.addonGroups?.forEach { addonGroupDto ->
                                productAddonGroupJunctionDao.insert(
                                    com.indybrain.indypos_Android.data.local.entity.ProductAddonGroupJunctionEntity(
                                        productId = id,
                                        addonGroupId = addonGroupDto.id
                                    )
                                )
                            }
                        }
                    }
                    result.status.lowercase() == "success" -> {
                        // Mark as synced
                        result.id?.let { id ->
                            productDao.markAsSynced(id)
                        }
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                403 -> {
                    val errorBody = e.response()?.errorBody()?.string()
                    if (errorBody?.contains("free_plan_limit_exceeded", ignoreCase = true) == true) {
                        "คุณใช้สินค้าครบจำนวนที่กำหนดแล้ว กรุณาอัปเกรดแผน"
                    } else {
                        e.message() ?: "เกิดข้อผิดพลาดในการ sync"
                    }
                }
                500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                else -> e.message() ?: "เกิดข้อผิดพลาดในการ sync"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการ sync"))
        }
    }
    
    /**
     * Clear all products, categories, addon groups, addons and their junctions from Room database
     * This is used before fetching fresh data from API
     * Note: This will trigger foreign key constraints on cart_items (productId will be set to NULL)
     * Cart items should be restored after fetching new products
     */
    override suspend fun clearAllProductsAndCategories() {
        // Delete junctions first (to avoid foreign key constraint issues)
        productAddonGroupJunctionDao.deleteAll()
        addonGroupAddonJunctionDao.deleteAll()
        
        // Delete products (this will set productId to NULL in cart_items due to foreign key)
        productDao.deleteAll()
        
        // Delete categories
        categoryDao.deleteAll()
        
        // Delete addon groups and addons
        addonGroupDao.deleteAll()
        addonDao.deleteAll()
    }
}

/**
 * Error response DTO for parsing API errors
 */
private data class ErrorResponse(
    val error: String?,
    val message: String?
)

