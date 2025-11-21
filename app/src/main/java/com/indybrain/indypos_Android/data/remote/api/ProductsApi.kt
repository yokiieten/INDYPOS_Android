package com.indybrain.indypos_Android.data.remote.api

import com.indybrain.indypos_Android.data.remote.dto.*
import retrofit2.http.GET
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

