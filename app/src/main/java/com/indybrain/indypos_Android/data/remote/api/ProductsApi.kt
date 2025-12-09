package com.indybrain.indypos_Android.data.remote.api

import com.google.gson.annotations.SerializedName
import com.indybrain.indypos_Android.data.remote.dto.*
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for products endpoints
 */
interface ProductsApi {
    /**
     * Get categories endpoint
     */
    @GET("protected/indypos/categories")
    suspend fun getCategories(): ApiResponseDto<List<CategoryDto>>
    
    /**
     * Create category endpoint
     */
    @POST("protected/indypos/categories")
    suspend fun createCategory(@Body request: CreateCategoryRequestDto): ApiResponseDto<CategoryDto>
    
    /**
     * Update category endpoint
     */
    @PUT("protected/indypos/categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: String,
        @Body request: UpdateCategoryRequestDto
    ): ApiResponseDto<CategoryDto>
    
    /**
     * Toggle category status endpoint
     */
    @PATCH("protected/indypos/categories/{id}/toggle-status")
    suspend fun toggleCategoryStatus(
        @Path("id") id: String,
        @Body request: ToggleCategoryStatusRequestDto
    ): ApiResponseDto<CategoryDto>
    
    /**
     * Delete category endpoint
     */
    @DELETE("protected/indypos/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): ApiResponseDto<CategoryDto>
    
    /**
     * Get all products endpoint
     */
    @GET("protected/indypos/my-products-all")
    suspend fun getMyProductsAll(
        @Query("category_id") categoryId: String? = null
    ): ApiResponseDto<List<ProductDto>>
    
    /**
     * Get addon groups endpoint
     */
    @GET("protected/indypos/addon-groups")
    suspend fun getAddonGroups(): ApiResponseDto<List<AddonGroupDto>>
    
    /**
     * Create addon group endpoint
     */
    @POST("protected/indypos/addon-groups")
    suspend fun createAddonGroup(@Body request: CreateAddonGroupRequestDto): ApiResponseDto<AddonGroupDto>
    
    /**
     * Update addon group endpoint
     */
    @PUT("protected/indypos/addon-groups/update")
    suspend fun updateAddonGroup(@Body request: UpdateAddonGroupRequestDto): ApiResponseDto<AddonGroupDto>
    
    /**
     * Toggle addon group status endpoint
     */
    @PATCH("protected/indypos/addon-groups/{addonGroupId}/toggle-status")
    suspend fun toggleAddonGroupStatus(
        @Path("addonGroupId") addonGroupId: String,
        @Body request: ToggleAddonGroupStatusRequestDto
    ): ApiResponseDto<AddonGroupDto>
    
    /**
     * Delete single addon group endpoint
     */
    @DELETE("protected/indypos/addon-groups/{addonGroupId}")
    suspend fun deleteAddonGroup(@Path("addonGroupId") addonGroupId: String): ApiResponseDto<DeleteAddonGroupResponseDto>
    
    /**
     * Delete multiple addon groups endpoint
     */
    @HTTP(method = "DELETE", path = "protected/indypos/addon-groups", hasBody = true)
    suspend fun deleteMultipleAddonGroups(@Body request: DeleteAddonGroupsRequestDto): ApiResponseDto<DeleteMultipleAddonGroupsResponseDto>
    
    /**
     * Sync addon groups endpoint
     */
    @POST("protected/indypos/addon-groups/sync")
    suspend fun syncAddonGroups(@Body request: SyncAddonGroupsRequestDto): ApiResponseDto<List<SyncAddonGroupResultDto>>
    
    /**
     * Get addons endpoint
     */
    @GET("protected/indypos/addons")
    suspend fun getAddons(): ApiResponseDto<List<AddonDto>>
    
    /**
     * Create addon endpoint
     */
    @POST("protected/indypos/addons")
    suspend fun createAddon(@Body request: CreateAddonRequestDto): ApiResponseDto<AddonDto>
    
    /**
     * Update addon endpoint
     */
    @PUT("protected/indypos/addons/{addonId}")
    suspend fun updateAddon(
        @Path("addonId") addonId: String,
        @Body request: UpdateAddonRequestDto
    ): ApiResponseDto<AddonDto>
    
    /**
     * Toggle addon status endpoint
     */
    @PATCH("protected/indypos/addons/{addonId}/toggle-status")
    suspend fun toggleAddonStatus(
        @Path("addonId") addonId: String,
        @Body request: ToggleAddonStatusRequestDto
    ): ApiResponseDto<AddonDto>
    
    /**
     * Delete single addon endpoint
     */
    @DELETE("protected/indypos/addons/{addonId}")
    suspend fun deleteAddon(@Path("addonId") addonId: String): ApiResponseDto<DeleteAddonResponseDto>
    
    /**
     * Delete multiple addons endpoint
     */
    @HTTP(method = "DELETE", path = "protected/indypos/addons", hasBody = true)
    suspend fun deleteMultipleAddons(@Body request: DeleteAddonsRequestDto): ApiResponseDto<DeleteMultipleAddonsResponseDto>
    
    /**
     * Sync addons endpoint
     */
    @POST("protected/indypos/addons/sync")
    suspend fun syncAddons(@Body request: SyncAddonsRequestDto): ApiResponseDto<List<SyncAddonResultDto>>
    
    /**
     * Delete product endpoint
     */
    @DELETE("protected/indypos/products/{id}")
    suspend fun deleteProduct(@Path("id") id: String): ApiResponseDto<Any>
    
    /**
     * Delete multiple products endpoint
     * Note: Using @HTTP instead of @DELETE because DELETE with body is not directly supported
     */
    @HTTP(method = "DELETE", path = "protected/indypos/products", hasBody = true)
    suspend fun deleteMultipleProducts(@Body request: DeleteProductsRequestDto): DeleteProductsResponseDto
    
    /**
     * Toggle product status endpoint
     */
    @PATCH("protected/indypos/products/{id}/toggle-status")
    suspend fun toggleProductStatus(
        @Path("id") id: String,
        @Body request: ToggleProductStatusRequestDto
    ): ApiResponseDto<ProductDto>
    
    /**
     * Create product endpoint
     */
    @POST("protected/indypos/products")
    suspend fun createProduct(@Body request: CreateProductRequestDto): ApiResponseDto<CreateProductResponseDto>
    
    /**
     * Upload product image endpoint
     */
    @Multipart
    @POST("protected/upload/product-image")
    suspend fun uploadProductImage(@Part image: MultipartBody.Part): UploadImageResponseDto
}

/**
 * Request DTO for creating a category
 */
data class CreateCategoryRequestDto(
    val name: String,
    @SerializedName("sort_order")
    val sortOrder: Int,
    @SerializedName("is_active")
    val isActive: Boolean
)

/**
 * Request DTO for updating a category
 */
data class UpdateCategoryRequestDto(
    val name: String,
    @SerializedName("sort_order")
    val sortOrder: Int,
    @SerializedName("is_active")
    val isActive: Boolean,
    val id: String,
    val description: String? = "",
    @SerializedName("image_url")
    val imageUrl: String? = ""
)

/**
 * Request DTO for toggling category status
 */
data class ToggleCategoryStatusRequestDto(
    val status: Boolean
)

/**
 * Request DTO for deleting multiple products
 */
data class DeleteProductsRequestDto(
    @SerializedName("product_ids")
    val productIds: List<String>
)

/**
 * Request DTO for toggling product status
 */
data class ToggleProductStatusRequestDto(
    val status: Boolean
)

/**
 * Request DTO for creating a product
 */
data class CreateProductRequestDto(
    val name: String,
    val description: String? = null,
    val price: Double,
    @SerializedName("cost_price")
    val costPrice: Double? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("category_id")
    val categoryId: String? = null,
    @SerializedName("product_code")
    val productCode: String? = null,
    val unit: String? = null,
    @SerializedName("sku_code")
    val skuCode: String? = null,
    @SerializedName("stock_quantity")
    val stockQuantity: Int? = null,
    @SerializedName("min_stock_quantity")
    val minStockQuantity: Int? = null,
    @SerializedName("selected_unit")
    val selectedUnit: String? = null,
    @SerializedName("selected_color_hex")
    val selectedColorHex: String? = null,
    @SerializedName("is_sku_enabled")
    val isSkuEnabled: Boolean? = null,
    @SerializedName("is_stock_enabled")
    val isStockEnabled: Boolean? = null,
    @SerializedName("has_additional_options")
    val hasAdditionalOptions: Boolean? = null,
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("addon_group_ids")
    val addonGroupIds: List<String>? = null
)

/**
 * Request DTO for creating an addon group
 */
data class CreateAddonGroupRequestDto(
    val name: String,
    @SerializedName("is_required")
    val isRequired: Boolean,
    @SerializedName("is_single_selection")
    val isSingleSelection: Boolean,
    @SerializedName("max_selection")
    val maxSelection: Int,
    @SerializedName("min_selection")
    val minSelection: Int,
    @SerializedName("sort_order")
    val sortOrder: Int
)

/**
 * Request DTO for updating an addon group
 */
data class UpdateAddonGroupRequestDto(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    @SerializedName("is_required")
    val isRequired: Boolean? = null,
    @SerializedName("is_single_selection")
    val isSingleSelection: Boolean? = null,
    @SerializedName("max_selection")
    val maxSelection: Int? = null,
    @SerializedName("min_selection")
    val minSelection: Int? = null,
    @SerializedName("sort_order")
    val sortOrder: Int? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null
)

/**
 * Request DTO for toggling addon group status
 */
data class ToggleAddonGroupStatusRequestDto(
    val status: Boolean
)

/**
 * Request DTO for deleting multiple addon groups
 */
data class DeleteAddonGroupsRequestDto(
    @SerializedName("addon_group_ids")
    val addonGroupIds: List<String>
)

/**
 * Request DTO for syncing addon groups
 */
data class SyncAddonGroupsRequestDto(
    @SerializedName("addon_groups")
    val addonGroups: List<SyncAddonGroupItemDto>
)

/**
 * Sync addon group item DTO
 */
data class SyncAddonGroupItemDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerializedName("is_required")
    val isRequired: Boolean,
    @SerializedName("is_single_selection")
    val isSingleSelection: Boolean,
    @SerializedName("max_selection")
    val maxSelection: Int,
    @SerializedName("min_selection")
    val minSelection: Int,
    @SerializedName("sort_order")
    val sortOrder: Int,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("is_synced")
    val isSynced: Boolean? = null,
    @SerializedName("is_deleted_locally")
    val isDeletedLocally: Boolean? = null,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

/**
 * Response DTO for deleting single addon group
 */
data class DeleteAddonGroupResponseDto(
    @SerializedName("deleted_id")
    val deletedId: String
)

/**
 * Response DTO for deleting multiple addon groups
 */
data class DeleteMultipleAddonGroupsResponseDto(
    @SerializedName("deleted_ids")
    val deletedIds: List<String>,
    val count: Int
)

/**
 * Sync addon group result DTO
 */
data class SyncAddonGroupResultDto(
    val id: String?,
    val status: String?,
    val message: String?,
    @SerializedName("should_delete")
    val shouldDelete: Boolean,
    @SerializedName("server_data")
    val serverData: AddonGroupDto?
)

/**
 * Request DTO for creating an addon
 */
data class CreateAddonRequestDto(
    val name: String,
    val price: Double,
    @SerializedName("sort_order")
    val sortOrder: Int = 1,
    @SerializedName("is_active")
    val isActive: Boolean = true
)

/**
 * Request DTO for updating an addon
 */
data class UpdateAddonRequestDto(
    val name: String,
    val price: Double,
    @SerializedName("sort_order")
    val sortOrder: Int = 1,
    @SerializedName("is_active")
    val isActive: Boolean
)

/**
 * Request DTO for toggling addon status
 */
data class ToggleAddonStatusRequestDto(
    val status: Boolean
)

/**
 * Request DTO for deleting multiple addons
 */
data class DeleteAddonsRequestDto(
    @SerializedName("addon_ids")
    val addonIds: List<String>
)

/**
 * Request DTO for syncing addons
 */
data class SyncAddonsRequestDto(
    val addons: List<SyncAddonItemDto>
)

/**
 * Sync addon item DTO
 */
data class SyncAddonItemDto(
    val id: String?,
    val name: String?,
    val price: Double?,
    @SerializedName("sort_order")
    val sortOrder: Int?,
    @SerializedName("is_active")
    val isActive: Boolean?,
    @SerializedName("is_synced")
    val isSynced: Boolean?,
    @SerializedName("is_deleted_locally")
    val isDeletedLocally: Boolean?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
)

/**
 * Response DTO for deleting single addon
 */
data class DeleteAddonResponseDto(
    @SerializedName("deleted_id")
    val deletedId: String
)

/**
 * Response DTO for deleting multiple addons
 */
data class DeleteMultipleAddonsResponseDto(
    @SerializedName("deleted_ids")
    val deletedIds: List<String>,
    val count: Int
)

/**
 * Sync addon result DTO
 */
data class SyncAddonResultDto(
    val id: String?,
    val status: String?,
    val message: String?,
    @SerializedName("should_delete")
    val shouldDelete: Boolean?,
    @SerializedName("server_data")
    val serverData: AddonDto?
)

