package com.indybrain.indypos_Android.data.repository

import com.indybrain.indypos_Android.data.local.dao.*
import com.indybrain.indypos_Android.data.mapper.ProductMapper
import com.indybrain.indypos_Android.data.remote.api.ProductsApi
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val productsApi: ProductsApi,
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val addonGroupDao: AddonGroupDao,
    private val addonDao: AddonDao
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
            productDao.deleteAll()
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
            productDao.deleteAll()
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
}

