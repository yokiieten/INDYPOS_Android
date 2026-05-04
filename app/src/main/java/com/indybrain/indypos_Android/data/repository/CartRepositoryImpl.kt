package com.indybrain.indypos_Android.data.repository

import com.indybrain.indypos_Android.data.local.dao.AddonDao
import com.indybrain.indypos_Android.data.local.dao.CartDao
import com.indybrain.indypos_Android.data.local.dao.CategoryDao
import com.indybrain.indypos_Android.data.local.dao.ProductDao
import com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import com.indybrain.indypos_Android.data.local.entity.CategoryEntity
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.data.mapper.CartItemMapper
import com.indybrain.indypos_Android.domain.model.CartItem
import com.indybrain.indypos_Android.domain.repository.CartRepository
import com.indybrain.indypos_Android.domain.repository.ProductDetailData
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class CartRepositoryImpl @Inject constructor(
    private val cartDao: CartDao,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val addonDao: AddonDao,
    private val mapper: CartItemMapper,
    private val productRepository: Lazy<ProductRepository>
) : CartRepository {
    
    override fun getCartItems(): Flow<List<CartItemEntity>> {
        return cartDao.getAllCartItems()
    }
    
    override suspend fun getCartItemsSync(): List<CartItemEntity> {
        return cartDao.getAllCartItemsSync()
    }
    
    override suspend fun getCartAddonsByItemId(itemId: String): List<CartAddonEntity> {
        return cartDao.getCartAddonsByItemId(itemId)
    }
    
    override fun getCartItemCount(): Flow<Int> {
        return cartDao.getCartItemCount()
    }
    
    override suspend fun getQuantityForProduct(productId: String): Int {
        return cartDao.getQuantityForProduct(productId)
    }
    
    override suspend fun addToCart(
        productId: String?,
        productName: String,
        productImageUrl: String?,
        productColorHex: String?,
        unitPrice: Double,
        quantity: Int,
        specialRequest: String?,
        addons: List<CartAddonEntity>
    ) {
        // Generate UUID for cart item id
        val cartItemId = UUID.randomUUID().toString()
        
        val cartItem = CartItemEntity(
            id = cartItemId,
            productId = productId,
            quantity = quantity,
            specialRequest = specialRequest,
            createdAt = Date(),
            // Extra fields (snapshot data for display)
            productName = productName,
            productImageUrl = productImageUrl,
            productColorHex = productColorHex,
            unitPrice = unitPrice
        )
        
        cartDao.insertCartItem(cartItem)
        
        // Update addons with cartItemId
        val addonsWithItemId = addons.map { it.copy(cartItemId = cartItemId) }
        cartDao.insertCartAddons(addonsWithItemId)
    }
    
    override suspend fun deleteCartItem(itemId: String) {
        cartDao.deleteCartItemWithAddons(itemId)
    }
    
    /**
     * Clear all cart items and addons
     * WARNING: This should ONLY be called when user logs out
     * Cart items are persisted in Room database and should remain
     * when navigating between screens or reopening the app
     */
    override suspend fun clearCart() {
        cartDao.deleteAllCartItems()
        cartDao.deleteAllCartAddons()
    }
    
    override suspend fun restoreProductIdsForCartItems(products: List<com.indybrain.indypos_Android.data.local.entity.ProductEntity>) {
        // Get all cart items with null productId
        val cartItemsWithNullProductId = cartDao.getCartItemsWithNullProductId()
        
        // Create a map of product name to product id for quick lookup
        val productNameToIdMap = products.associateBy { it.name }
        
        // Restore productId for each cart item by matching product name
        cartItemsWithNullProductId.forEach { cartItem ->
            val productId = productNameToIdMap[cartItem.productName]?.id
            if (productId != null) {
                cartDao.updateCartItemProductId(cartItem.id, productId)
            }
        }
    }
    
    override suspend fun ensureProductForCart(product: ProductEntity, category: CategoryEntity?) {
        if (product.categoryId != null && category != null && categoryDao.getCategoryById(category.id) == null) {
            categoryDao.insert(category)
        }
        // Replace always: list/catalog rows can be stale; product detail (+ stock checks after this) use fresh API data.
        productDao.insertAll(listOf(product))
    }

    override suspend fun clearCartItemsByProduct(productId: String) {
        cartDao.deleteCartItemsByProductId(productId)
    }
    
    // Product Edit Screen methods
    override fun getCartItemsByProduct(productId: String): Flow<List<CartItem>> {
        return cartDao.getCartItemsByProduct(productId)
            .map { entities ->
                entities.mapNotNull { entity ->
                    // Get product
                    val product = productDao.getProductById(productId) 
                        ?: return@mapNotNull null
                    
                    // Get all addon IDs from selected addons
                    val addonIds = entity.cartAddons.map { it.addonId }
                    val addonEntities = addonDao.getAddonsByIds(addonIds)
                    
                    // Group addons by groupId
                    val addonsByGroup = entity.cartAddons
                        .groupBy { it.addonGroupId }
                        .mapValues { (_, cartAddons) ->
                            cartAddons.mapNotNull { cartAddon ->
                                addonEntities.find { it.id == cartAddon.addonId }
                            }
                        }
                    
                    mapper.toDomain(entity, product, addonsByGroup)
                }
            }
    }
    
    override fun getCartItemsDomain(): Flow<List<CartItem>> {
        return cartDao.getAllCartItemsWithAddons()
            .map { entities ->
                entities.mapNotNull { entity ->
                    val productId = entity.cartItem.productId ?: return@mapNotNull null
                    val product = productDao.getProductById(productId) ?: return@mapNotNull null
                    
                    // Get all addon IDs from selected addons
                    val addonIds = entity.cartAddons.map { it.addonId }
                    val addonEntities = addonDao.getAddonsByIds(addonIds)
                    
                    // Group addons by groupId
                    val addonsByGroup = entity.cartAddons
                        .groupBy { it.addonGroupId }
                        .mapValues { (_, cartAddons) ->
                            cartAddons.mapNotNull { cartAddon ->
                                addonEntities.find { it.id == cartAddon.addonId }
                            }
                        }
                    
                    mapper.toDomain(entity, product, addonsByGroup)
                }
            }
    }
    
    override suspend fun getCartItemById(cartItemId: String): CartItem? {
        val entity = cartDao.getCartItemById(cartItemId) ?: return null
        val productId = entity.cartItem.productId ?: return null
        val product = productDao.getProductById(productId) ?: return null
        
        // Get addons mapping
        val addonIds = entity.cartAddons.map { it.addonId }
        val addonEntities = addonDao.getAddonsByIds(addonIds)
        val addonsByGroup = entity.cartAddons
            .groupBy { it.addonGroupId }
            .mapValues { (_, cartAddons) ->
                cartAddons.mapNotNull { cartAddon ->
                    addonEntities.find { it.id == cartAddon.addonId }
                }
            }
        
        return mapper.toDomain(entity, product, addonsByGroup)
    }
    
    override suspend fun updateCartItemQuantity(
        cartItemId: String, 
        quantity: Int
    ): Result<Unit> {
        return try {
            cartDao.updateQuantity(cartItemId, quantity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteCartItems(cartItemIds: List<String>): Result<Unit> {
        return try {
            cartDao.deleteCartItemGroup(cartItemIds)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateCartItemConfiguration(
        cartItemId: String,
        specialRequest: String?,
        unitPrice: Double,
        addons: List<CartAddonEntity>
    ): Result<Unit> {
        return try {
            // Update main cart item fields (keep createdAt, productId, etc.)
            cartDao.updateCartItemConfiguration(
                id = cartItemId,
                specialRequest = specialRequest,
                unitPrice = unitPrice
            )
            
            // Replace addons: delete old then insert new with same cartItemId
            cartDao.deleteSelectedAddons(cartItemId)
            if (addons.isNotEmpty()) {
                val addonsWithItemId = addons.map { it.copy(cartItemId = cartItemId) }
                cartDao.insertCartAddons(addonsWithItemId)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun checkStockAvailability(
        productId: String, 
        quantity: Int
    ): Boolean {
        val product = productDao.getProductById(productId) ?: return false
        
        // If stock is not enabled, allow any quantity
        if (product.isStockEnabled != true) {
            return true
        }
        
        // If stock is enabled, check stock quantity
        val availableStock = product.stockQuantity ?: return true // If no stock limit set, allow
        return quantity <= availableStock
    }

    override suspend fun syncCartRelatedCatalogFromPosList(
        categories: List<CategoryEntity>,
        products: List<ProductEntity>
    ) {
        val cartItems = cartDao.getAllCartItemsSync()
        val cartProductIds = cartItems.mapNotNull { it.productId }.toSet()
        if (cartProductIds.isEmpty()) return

        val productsToUpsert = products.filter { it.id in cartProductIds }
        if (productsToUpsert.isNotEmpty()) {
            val categoryIdsNeeded = productsToUpsert.mapNotNull { it.categoryId }.toSet()
            val categoriesToUpsert = categories.filter { it.id in categoryIdsNeeded }
            if (categoriesToUpsert.isNotEmpty()) {
                categoryDao.insertAll(categoriesToUpsert)
            }
            productDao.insertAll(productsToUpsert)
        }

        syncCartAddonsFromProductDetailApi(cartItems)
        refreshCartItemSnapshotsFromRoom()
    }

    /**
     * For each product that has cart lines with options, GET product detail and align [cart_addons] with server.
     *
     * Resolves addons by (**cart group's id + addon id**) first so option lines stay grouped as the guest chose.
     * A flat map keyed only by addon id can mis-assign the group when the same id appears under multiple groups,
     * or reordering overwrites ambiguous entries — which breaks Detail / Edit / Order grouping after list sync.
     *
     * If the addon is no longer listed under that group, falls back to the first occurrence in API sort order.
     *
     * Addon IDs not present in the catalog response leave the row unchanged.
     */
    private suspend fun syncCartAddonsFromProductDetailApi(cartItems: List<CartItemEntity>) {
        val productIdsWithAddons = cartItems.mapNotNull { item ->
            val pid = item.productId ?: return@mapNotNull null
            if (cartDao.getCartAddonsByItemId(item.id).isEmpty()) null else pid
        }.toSet()
        if (productIdsWithAddons.isEmpty()) return

        val repo = productRepository.get()
        val groupSort = compareBy<AddonGroupEntity> { it.sortOrder ?: Int.MAX_VALUE }
            .thenBy { it.id }

        fun resolveCatalogInfo(
            data: ProductDetailData,
            cartAddon: CartAddonEntity,
        ): AddonCatalogInfo? {
            val storedGroupId = cartAddon.addonGroupId
            val addon = data.addonsByGroup[storedGroupId]
                ?.firstOrNull { it.id == cartAddon.addonId }
                ?: return null
            val grp = data.addonGroups.firstOrNull { it.id == storedGroupId }
            return AddonCatalogInfo(
                name = addon.name,
                price = addon.price,
                groupId = storedGroupId,
                groupName = grp?.name ?: cartAddon.addonGroupName
            )
        }

        fun resolveMovedAddon(
            data: ProductDetailData,
            cartAddon: CartAddonEntity,
        ): AddonCatalogInfo? {
            val sortedGroups = data.addonGroups.sortedWith(groupSort)
            for (group in sortedGroups) {
                val addon = data.addonsByGroup[group.id]
                    ?.firstOrNull { it.id == cartAddon.addonId }
                    ?: continue
                return AddonCatalogInfo(
                    name = addon.name,
                    price = addon.price,
                    groupId = group.id,
                    groupName = group.name
                )
            }
            return null
        }

        for (productId in productIdsWithAddons) {
            val data = repo.getProductDetailFromApi(productId).getOrNull() ?: continue

            for (item in cartItems.filter { it.productId == productId }) {
                for (cartAddon in cartDao.getCartAddonsByItemId(item.id)) {
                    val info =
                        resolveCatalogInfo(data, cartAddon)
                            ?: resolveMovedAddon(data, cartAddon)
                            ?: continue
                    cartDao.updateCartAddonSnapshot(
                        rowId = cartAddon.id,
                        addonName = info.name,
                        addonPrice = info.price,
                        addonGroupId = info.groupId,
                        addonGroupName = info.groupName
                    )
                }
            }
        }
    }

    private data class AddonCatalogInfo(
        val name: String,
        val price: Double,
        val groupId: String,
        val groupName: String
    )

    private suspend fun refreshCartItemSnapshotsFromRoom() {
        for (item in cartDao.getAllCartItemsSync()) {
            val productId = item.productId ?: continue
            val product = productDao.getProductById(productId) ?: continue
            val addonsTotal = cartDao.getCartAddonsByItemId(item.id).sumOf { it.addonPrice }
            val unitPrice = product.price + addonsTotal
            cartDao.updateCartItemProductSnapshot(
                id = item.id,
                productName = product.name,
                productImageUrl = product.imageUrl,
                productColorHex = product.selectedColorHex,
                unitPrice = unitPrice
            )
        }
    }
}

