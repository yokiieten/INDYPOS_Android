package com.indybrain.indypos_Android.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.data.local.entity.CategoryEntity
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.data.mapper.ProductMapper
import com.indybrain.indypos_Android.data.remote.api.*
import java.util.Locale
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import com.indybrain.indypos_Android.domain.repository.CartRepository
import com.indybrain.indypos_Android.domain.repository.CategoriesPaginatedResult
import com.indybrain.indypos_Android.domain.repository.ProductsPaginatedResult
import com.indybrain.indypos_Android.domain.repository.DeleteCategoriesResult
import com.indybrain.indypos_Android.domain.repository.DeleteProductsResult
import com.indybrain.indypos_Android.domain.repository.ProductDetailData
import com.indybrain.indypos_Android.domain.repository.ProductListData
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import com.indybrain.indypos_Android.domain.repository.ProductSyncStatistics
import com.indybrain.indypos_Android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Date
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val productsApi: ProductsApi,
    private val authRepository: AuthRepository,
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    private val cartRepository: CartRepository,
    private val languageLocalDataSource: LanguageLocalDataSource,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) : ProductRepository {
    
    override suspend fun syncAllProductData(): Result<Unit> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception(context.getString(R.string.logout_no_internet_title)))
            }
            val categoriesResponse = productsApi.getCategories()
            if (categoriesResponse.status != 200) {
                return Result.failure(Exception(categoriesResponse.message ?: "Failed to fetch categories"))
            }
            val productsResponse = productsApi.getMyProductsAll()
            if (productsResponse.status != 200) {
                return Result.failure(Exception(productsResponse.message ?: "Failed to fetch products"))
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
    
    override fun getAllActiveProducts(): Flow<List<ProductEntity>> {
        return flowOf(emptyList())
    }
    
    override fun getProductsByCategory(categoryId: String?): Flow<List<ProductEntity>> {
        return flowOf(emptyList())
    }
    
    override fun getAllActiveCategories(): Flow<List<CategoryEntity>> {
        return flowOf(emptyList())
    }
    
    override suspend fun fetchAndSaveProducts(): Result<Unit> {
        return try {
            val productsResponse = productsApi.getMyProductsAll()
            if (productsResponse.status != 200) {
                return Result.failure(Exception(productsResponse.message ?: "Failed to fetch products"))
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
            val categoriesResponse = productsApi.getCategories()
            if (categoriesResponse.status != 200) {
                return Result.failure(Exception(categoriesResponse.message ?: "Failed to fetch categories"))
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
        return emptyList()
    }
    
    override fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> {
        return flowOf(emptyList())
    }
    
    override suspend fun getCategoryById(id: String): CategoryEntity? {
        return null
    }
    
    private suspend fun fetchCategoriesListFromApi(): Result<List<CategoryEntity>> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception(context.getString(R.string.logout_no_internet_title)))
            }
            val response = productsApi.getCategories()
            if (response.status != 200) {
                return Result.failure(Exception(response.message ?: "Failed to fetch categories"))
            }
            val list = (response.data ?: emptyList()).map { ProductMapper.toEntity(it) }
            Result.success(list)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                else -> e.message() ?: "เกิดข้อผิดพลาดในการดึงข้อมูลหมวดหมู่"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดที่ไม่คาดคิด"))
        }
    }
    
    override suspend fun getAllCategoriesFromApi(): Result<List<CategoryEntity>> {
        return fetchCategoriesListFromApi()
    }
    
    override suspend fun getCategoryByIdFromApi(id: String): Result<CategoryEntity> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception(context.getString(R.string.logout_no_internet_title)))
            }
            val response = productsApi.getCategoryDetail(id)
            if (response.status == 200 && response.data != null) {
                val categoryEntity = ProductMapper.toEntity(response.data)
                Result.success(categoryEntity)
            } else {
                Result.failure(
                    Exception(
                        response.message?.takeIf { it.isNotBlank() }
                            ?: context.getString(R.string.category_form_validation_edit_failed)
                    )
                )
            }
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                403 -> "Access denied to this category"
                404 -> context.getString(R.string.category_form_validation_edit_failed)
                500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                else -> e.message() ?: "เกิดข้อผิดพลาดในการดึงข้อมูลหมวดหมู่"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดที่ไม่คาดคิด"))
        }
    }
    
    override suspend fun addCategory(category: CategoryEntity): Result<Unit> {
        return Result.failure(Exception("Local category cache is not used"))
    }
    
    override suspend fun updateCategory(category: CategoryEntity): Result<Unit> {
        return Result.failure(Exception("Local category cache is not used"))
    }
    
    override suspend fun getCurrentUserId(): Int? {
        return authRepository.getCurrentUser().first()?.id
    }
    
    override suspend fun createCategory(
        name: String,
        sortOrder: Int,
        isActive: Boolean
    ): Result<CategoryEntity> {
        getCurrentUserId() ?: return Result.failure(
            Exception("ไม่พบข้อมูลผู้ใช้ กรุณาเข้าสู่ระบบใหม่")
        )
        if (!networkConnectivityChecker.isConnected()) {
            return Result.failure(Exception(context.getString(R.string.logout_no_internet_title)))
        }
        
        return try {
            try {
                val request = CreateCategoryRequestDto(
                    name = name,
                    sortOrder = sortOrder,
                    isActive = isActive
                )
                
                val response = productsApi.createCategory(request)
                
                if (response.status == 201 && response.data != null) {
                    val categoryEntity = ProductMapper.toEntity(response.data)
                    Result.success(categoryEntity)
                } else {
                    val errorMessage = mapApiErrorFromResponseFields(
                        statusCode = response.status,
                        error = response.error,
                        message = response.message
                    )
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
                        } else {
                            parsed
                        }
                    }
                    403 -> parseApiErrorResponse(errorBody, e.code())
                    409 -> parseApiErrorResponse(errorBody, e.code())
                    500 -> parseApiErrorResponse(errorBody, e.code())
                    else -> parseApiErrorResponse(errorBody, e.code())
                }
                Result.failure(Exception(errorMessage))
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
        getCurrentUserId() ?: return Result.failure(
            Exception("ไม่พบข้อมูลผู้ใช้ กรุณาเข้าสู่ระบบใหม่")
        )
        if (!networkConnectivityChecker.isConnected()) {
            return Result.failure(Exception(context.getString(R.string.logout_no_internet_title)))
        }
        
        return try {
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
                    val categoryEntity = ProductMapper.toEntity(response.data)
                    Result.success(categoryEntity)
                } else {
                    val errorMessage = mapApiErrorFromResponseFields(
                        statusCode = response.status,
                        error = response.error,
                        message = response.message
                    )
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
                        } else {
                            parsed
                        }
                    }
                    403 -> parseApiErrorResponse(errorBody, e.code())
                    404 -> parseApiErrorResponse(errorBody, e.code())
                    409 -> parseApiErrorResponse(errorBody, e.code())
                    500 -> parseApiErrorResponse(errorBody, e.code())
                    else -> parseApiErrorResponse(errorBody, e.code())
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการแก้ไขหมวดหมู่"))
        }
    }
    
    override suspend fun toggleCategoryStatus(
        categoryId: String,
        newStatus: Boolean
    ): Result<CategoryEntity> {
        if (!networkConnectivityChecker.isConnected()) {
            return Result.failure(Exception(context.getString(R.string.logout_no_internet_title)))
        }
        return try {
            try {
                val request = ToggleCategoryStatusRequestDto(status = newStatus)
                val response = productsApi.toggleCategoryStatus(categoryId, request)
                
                if (response.status == 200 && response.data != null) {
                    val categoryEntity = ProductMapper.toEntity(response.data)
                    Result.success(categoryEntity)
                } else {
                    val errorMessage = response.error?.takeIf { it.isNotBlank() }
                        ?: response.message?.takeIf { it.isNotBlank() }
                        ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะหมวดหมู่"
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
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะหมวดหมู่"))
        }
    }
    
    override suspend fun deleteCategory(categoryId: String): Result<Unit> {
        if (!networkConnectivityChecker.isConnected()) {
            return Result.failure(Exception(context.getString(R.string.logout_no_internet_title)))
        }
        return try {
            try {
                val response = productsApi.deleteCategory(categoryId)
                
                if (response.status == 200) {
                    Result.success(Unit)
                } else {
                    val errorMessage = response.error?.takeIf { it.isNotBlank() }
                        ?: response.message?.takeIf { it.isNotBlank() }
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
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการลบหมวดหมู่"))
        }
    }
    
    override suspend fun deleteMultipleCategories(categoryIds: List<String>): Result<DeleteCategoriesResult> {
        return try {
            if (categoryIds.isEmpty()) {
                return Result.failure(Exception("กรุณาเลือกหมวดหมู่ที่ต้องการลบ"))
            }
            
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception(context.getString(R.string.logout_no_internet_title)))
            }
            try {
                val request = DeleteCategoriesRequestDto(categoryIds = categoryIds)
                val response = productsApi.deleteMultipleCategories(request)
                
                if (response.status == 200) {
                    val deletedIds = response.deletedIds.orEmpty()
                    val totalDeleted = response.data?.totalDeleted ?: response.count
                    val totalFailed = response.data?.totalFailed ?: response.failedDeletions.orEmpty().size
                    val errors = response.errors.orEmpty()
                    
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
     * Map API error fields from response body (status/error/message) to localized UI text.
     * This is used when API returns HTTP 200 but business status is non-success (e.g. 409 in body).
     */
    private fun mapApiErrorFromResponseFields(
        statusCode: Int,
        error: String?,
        message: String?
    ): String {
        val errorText = error?.lowercase() ?: ""
        val messageText = message?.lowercase() ?: ""
        val combined = "$errorText $messageText"

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

        return when {
            combined.contains("duplicate sku") || combined.contains("duplicate sku code") ->
                getLocalizedString("product_error_duplicate_sku", "รหัส SKU นี้มีอยู่แล้ว")

            combined.contains("duplicate product name") || combined.contains("duplicate name") ->
                getLocalizedString("product_error_duplicate_name", "ชื่อนี้มีอยู่แล้ว")

            combined.contains("duplicate product code") || combined.contains("duplicate code") ->
                getLocalizedString("product_error_duplicate_code", "รหัสสินค้านี้มีอยู่แล้ว")

            combined.contains("duplicate category name") ->
                getLocalizedString("category_error_duplicate_name", "ชื่อหมวดหมู่นี้มีอยู่แล้ว")

            else -> {
                // Do not return raw server messages here; they may not match the user's selected locale.
                getDefaultErrorMessage(statusCode)
            }
        }
    }
    
    /**
     * Get default error message for status code
     */
    private fun getDefaultErrorMessage(statusCode: Int): String {
        // Generic fallback for unrecognized errors.
        // Important: don't hardcode Thai or category-delete specific messages here,
        // because the same mapper is also used by product/category/addon flows.
        val localeCode = languageLocalDataSource.getLanguageLocale()
        val isEnglish = localeCode == 1033

        return when (statusCode) {
            400 -> if (isEnglish) {
                "Invalid request. Please check your input."
            } else {
                "ข้อมูลไม่ถูกต้อง กรุณาตรวจสอบอีกครั้ง"
            }
            401 -> LocaleHelper.setLocale(context, localeCode).getString(R.string.api_error_unauthorized)
            403 -> if (isEnglish) {
                "Forbidden. You do not have permission."
            } else {
                "คุณไม่มีสิทธิ์เข้าถึงข้อมูลนี้"
            }
            404 -> if (isEnglish) {
                "Not found."
            } else {
                "ไม่พบข้อมูลที่ต้องการ"
            }
            409 -> if (isEnglish) {
                "Conflict. Duplicate data exists."
            } else {
                "ข้อมูลซ้ำกัน กรุณาตรวจสอบอีกครั้ง"
            }
            500 -> if (isEnglish) {
                "Server error. Please try again later."
            } else {
                "เกิดข้อผิดพลาดของเซิร์ฟเวอร์ กรุณาลองใหม่อีกครั้ง"
            }
            else -> if (isEnglish) {
                "Request failed. Please try again."
            } else {
                "เกิดข้อผิดพลาดในการดำเนินการ"
            }
        }
    }
    
    override fun getAllProductsForManagement(): Flow<List<ProductEntity>> {
        return flowOf(emptyList())
    }

    override suspend fun getProductsPaginated(
        page: Int,
        limit: Int,
        search: String?,
        categoryId: String?
    ): Result<ProductsPaginatedResult> {
        if (!networkConnectivityChecker.isConnected()) {
            return Result.failure(Exception("No network connection"))
        }
        return try {
            val response = productsApi.getProductsPaginated(
                page = page,
                limit = limit,
                search = search?.takeIf { it.isNotBlank() },
                categoryId = categoryId?.takeIf { it.isNotBlank() }
            )
            if (response.status != 200) {
                return Result.failure(Exception(response.message ?: "Failed to fetch products"))
            }
            val data = response.data
            val productsList = data?.products ?: emptyList()
            val pagination = data?.pagination
            val products = productsList.map { ProductMapper.toEntity(it) }
            Result.success(
                ProductsPaginatedResult(
                    products = products,
                    currentPage = pagination?.currentPage ?: page,
                    totalCount = pagination?.totalCount ?: products.size,
                    totalPages = pagination?.totalPages ?: 1,
                    hasNext = pagination?.hasNext ?: false,
                    hasPrevious = pagination?.hasPrevious ?: false
                )
            )
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                else -> e.message ?: "เกิดข้อผิดพลาดในการดึงข้อมูล"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดที่ไม่คาดคิด"))
        }
    }

    override suspend fun searchProductsPaginated(
        query: String,
        categoryId: String?,
        page: Int,
        limit: Int
    ): Result<ProductsPaginatedResult> {
        val qParam = query.trim().takeIf { it.isNotEmpty() }
        if (!networkConnectivityChecker.isConnected()) {
            return Result.failure(Exception(context.getString(R.string.logout_no_internet_title)))
        }
        return try {
            val response = productsApi.searchProducts(
                query = qParam,
                categoryId = categoryId?.takeIf { it.isNotBlank() },
                limit = limit.coerceIn(1, 50),
                page = page.coerceAtLeast(1)
            )
            if (response.status != 200) {
                val msg = response.error?.takeIf { it.isNotBlank() }
                    ?: response.message.takeIf { it.isNotBlank() }
                    ?: "Failed to search products"
                return Result.failure(Exception(msg))
            }
            val data = response.data
            val productsList = data?.products ?: emptyList()
            val pagination = data?.pagination
            val products = productsList.map { ProductMapper.toEntity(it) }
            Result.success(
                ProductsPaginatedResult(
                    products = products,
                    currentPage = pagination?.currentPage ?: page,
                    totalCount = pagination?.totalCount ?: products.size,
                    totalPages = pagination?.totalPages ?: 1,
                    hasNext = pagination?.hasNext ?: false,
                    hasPrevious = pagination?.hasPrevious ?: false
                )
            )
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                else -> e.message() ?: "เกิดข้อผิดพลาดในการค้นหา"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดที่ไม่คาดคิด"))
        }
    }
    
    override fun searchProducts(query: String, categoryId: String?): Flow<List<ProductEntity>> {
        return flowOf(emptyList())
    }
    
    override suspend fun getProductById(id: String): ProductEntity? {
        return null
    }
    
    override suspend fun getProductByCode(code: String): ProductEntity? {
        return null
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

    override suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception(context.getString(R.string.logout_no_internet_title)))
            }
            try {
                val response = productsApi.deleteProduct(productId)
                if (response.status == 200) {
                    cartRepository.clearCartItemsByProduct(productId)
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
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการลบสินค้า"))
        }
    }
    
    override suspend fun deleteMultipleProducts(productIds: List<String>): Result<DeleteProductsResult> {
        return try {
            if (productIds.isEmpty()) {
                return Result.failure(Exception("กรุณาเลือกสินค้าที่ต้องการลบ"))
            }
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception(context.getString(R.string.logout_no_internet_title)))
            }
            try {
                val request = DeleteProductsRequestDto(productIds = productIds)
                val response = productsApi.deleteMultipleProducts(request)
                if (response.status == 200) {
                    val deletedIds = response.deletedIds.orEmpty()
                    val totalDeleted = response.data?.totalDeleted ?: response.count
                    val totalFailed = response.data?.totalFailed ?: response.failedDeletions.orEmpty().size
                    val errors = response.errors.orEmpty()
                    deletedIds.forEach { id -> cartRepository.clearCartItemsByProduct(id) }
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
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการลบสินค้า"))
        }
    }
    
    override suspend fun toggleProductStatus(productId: String, newStatus: Boolean): Result<ProductEntity> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception(context.getString(R.string.logout_no_internet_title)))
            }
            try {
                val response = productsApi.toggleProductStatus(
                    productId,
                    ToggleProductStatusRequestDto(newStatus)
                )
                if (response.status == 200 && response.data != null) {
                    val productEntity = ProductMapper.toEntity(response.data)
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
        getCurrentUserId() ?: return Result.failure(
            Exception("ไม่พบข้อมูลผู้ใช้ กรุณาเข้าสู่ระบบใหม่")
        )

        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception(context.getString(R.string.product_management_no_internet)))
            }
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
                    val productDto = response.data.product
                    Result.success(ProductMapper.toEntity(productDto))
                } else {
                    val errorMessage = mapApiErrorFromResponseFields(
                        statusCode = response.status,
                        error = response.error,
                        message = response.message
                    )
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
                        } else {
                            parsed
                        }
                    }
                    403 -> parseApiErrorResponse(errorBody, e.code())
                    409 -> parseApiErrorResponse(errorBody, e.code())
                    500 -> parseApiErrorResponse(errorBody, e.code())
                    else -> parseApiErrorResponse(errorBody, e.code())
                }
                Result.failure(Exception(errorMessage))
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
        addonGroupIds: List<String>?,
        description: String?,
        popularityRank: Int?,
        minStockQuantity: Int?,
        selectedUnit: String?,
        isActive: Boolean?
    ): Result<ProductEntity> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception(context.getString(R.string.product_management_no_internet)))
            }

            val request = UpdateProductRequestDto(
                name = name,
                description = description,
                price = price,
                costPrice = costPrice,
                imageUrl = imageUrl,
                categoryId = categoryId,
                popularityRank = popularityRank,
                productCode = productCode,
                unit = unit,
                skuCode = skuCode,
                stockQuantity = stockQuantity,
                minStockQuantity = minStockQuantity,
                selectedUnit = selectedUnit,
                selectedColorHex = selectedColorHex,
                isSkuEnabled = isSkuEnabled,
                isStockEnabled = isStockEnabled,
                hasAdditionalOptions = hasAdditionalOptions,
                isActive = isActive,
                addonGroupIds = addonGroupIds
            )

            val response = productsApi.updateProduct(productId, request)

            if (response.status == 200 && response.data != null) {
                val productDto = response.data.product
                Result.success(ProductMapper.toEntity(productDto))
            } else {
                val errorMessage = mapApiErrorFromResponseFields(
                    statusCode = response.status,
                    error = response.error,
                    message = response.message
                )
                Result.failure(Exception(errorMessage))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()
            val errorMessage = parseApiErrorResponse(errorBody, e.code())
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการอัปเดตสินค้า"))
        }
    }
    
    override suspend fun getSyncStatistics(): ProductSyncStatistics {
        return ProductSyncStatistics(
            totalProducts = 0,
            syncedCount = 0,
            pendingSyncCount = 0,
            deletedCount = 0
        )
    }
    
    override suspend fun updateProductStock(productId: String, delta: Int): Result<ProductEntity> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception(context.getString(R.string.logout_no_internet_title)))
            }
            val detail = getProductDetailFromApi(productId).getOrNull()
                ?: return Result.failure(Exception("ไม่พบสินค้าที่ต้องการอัปเดต"))
            val existingProduct = detail.product
            val oldQuantity = existingProduct.stockQuantity ?: 0
            val newQuantity = oldQuantity + delta
            try {
                val response = productsApi.updateProductStock(
                    productId,
                    UpdateProductStockRequestDto(delta)
                )
                if (response.status == 200) {
                    Result.success(
                        existingProduct.copy(
                            stockQuantity = newQuantity,
                            isSynced = true,
                            updatedAt = Date()
                        )
                    )
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
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการอัปเดตสต็อก"))
        }
    }
    
    override suspend fun clearCartItemsByProduct(productId: String) {
        cartRepository.clearCartItemsByProduct(productId)
    }
    
    override suspend fun syncCategories(): Result<Unit> {
        return fetchAndSyncCategories()
    }
    
    override suspend fun syncProducts(): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun clearAllProductsAndCategories() {
    }
}

/**
 * Error response DTO for parsing API errors
 */
private data class ErrorResponse(
    val error: String?,
    val message: String?
)

