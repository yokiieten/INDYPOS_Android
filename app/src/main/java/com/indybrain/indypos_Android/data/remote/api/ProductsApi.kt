package com.indybrain.indypos_Android.data.remote.api

import com.google.gson.annotations.SerializedName
import com.indybrain.indypos_Android.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
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
     * Get addons endpoint
     */
    @GET("protected/indypos/addons")
    suspend fun getAddons(): ApiResponseDto<List<AddonDto>>
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

