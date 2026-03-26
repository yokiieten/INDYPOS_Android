package com.indybrain.indypos_Android.data.repository

import android.content.Context
import com.google.gson.Gson
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.data.local.dao.AddonDao
import com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity
import com.indybrain.indypos_Android.data.local.dao.AddonGroupDao
import com.indybrain.indypos_Android.data.local.dao.AddonGroupAddonJunctionDao
import com.indybrain.indypos_Android.data.local.entity.AddonGroupAddonJunctionEntity
import com.indybrain.indypos_Android.data.local.entity.AddonGroupWithAddons
import com.indybrain.indypos_Android.data.mapper.ProductMapper
import com.indybrain.indypos_Android.data.remote.api.*
import com.indybrain.indypos_Android.data.remote.dto.AddonGroupDto
import com.indybrain.indypos_Android.data.remote.dto.DeleteAddonGroupsResponseDto
import com.indybrain.indypos_Android.domain.repository.AddonGroupsPaginatedResult
import com.indybrain.indypos_Android.domain.repository.AddonGroupRepository
import com.indybrain.indypos_Android.domain.repository.DeleteAddonGroupsResult
import com.indybrain.indypos_Android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.util.Date
import javax.inject.Inject

class AddonGroupRepositoryImpl @Inject constructor(
    private val productsApi: ProductsApi,
    private val addonGroupDao: AddonGroupDao,
    private val addonDao: AddonDao,
    private val junctionDao: AddonGroupAddonJunctionDao,
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    private val languageLocalDataSource: LanguageLocalDataSource,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) : AddonGroupRepository {

    private fun getLocalizedString(resourceName: String, fallback: String): String {
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

    /**
     * Map API error fields returned in the response body (e.g. status=409) into localized UI messages.
     * Retrofit may not throw HttpException in these cases, so we can't rely on parseApiErrorResponse(errorBody, code).
     */
    private fun mapApiErrorFromResponseFields(
        statusCode: Int,
        error: String?,
        message: String?
    ): String {
        val errorText = error?.lowercase() ?: ""
        val messageText = message?.lowercase() ?: ""
        val combinedErrorText = "$errorText $messageText"

        return when {
            combinedErrorText.contains("duplicate addon group name") ||
                (combinedErrorText.contains("duplicate name") && combinedErrorText.contains("addon")) ->
                getLocalizedString(
                    "addon_group_form_error_duplicate_name",
                    "This addon group name already exists"
                )

            else -> getDefaultErrorMessage(statusCode)
        }
    }
    
    override fun getAllAddonGroupsFlow(): Flow<List<com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity>> {
        return addonGroupDao.getAllAddonGroupsForManagementFlow()
    }

    override fun getAllAddonGroupsWithCountFlow(): Flow<List<com.indybrain.indypos_Android.data.local.entity.AddonGroupWithAddonCount>> {
        return addonGroupDao.getAddonGroupsWithAddonCountForManagementFlow()
    }

    override suspend fun getAddonGroupsPaginated(
        page: Int,
        limit: Int,
        search: String?
    ): Result<AddonGroupsPaginatedResult> {
        if (!networkConnectivityChecker.isConnected()) {
            return Result.failure(Exception("No network connection"))
        }
        return try {
            val response = productsApi.getAddonGroupsPaginated(
                page = page,
                limit = limit,
                search = search?.takeIf { it.isNotBlank() }
            )
            if (response.status != 200) {
                return Result.failure(Exception(response.message ?: "Failed to fetch addon groups"))
            }
            val data = response.data
            val groupsList = data?.addonGroups ?: emptyList()
            val pagination = data?.pagination
            val addonGroups = groupsList.map { ProductMapper.toEntity(it) }
            val addonCounts = groupsList.associate { it.id to (it.addons?.size ?: 0) }
            Result.success(
                AddonGroupsPaginatedResult(
                    addonGroups = addonGroups,
                    addonCounts = addonCounts,
                    currentPage = pagination?.currentPage ?: page,
                    totalCount = pagination?.totalCount ?: addonGroups.size,
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

    override suspend fun getAllAddonGroupsFromApi(): Result<List<AddonGroupEntity>> {
        if (!networkConnectivityChecker.isConnected()) {
            return Result.failure(Exception(context.getString(R.string.product_management_no_internet)))
        }
        return try {
            val response = productsApi.getAddonGroups()
            if (response.status != 200) {
                return Result.failure(Exception(response.message ?: "Failed to fetch addon groups"))
            }
            val list = (response.data ?: emptyList()).map { ProductMapper.toEntity(it) }
            Result.success(list)
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
    
    override suspend fun getAddonGroupById(id: String): com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity? {
        return addonGroupDao.getAddonGroupById(id)
    }
    
    override suspend fun getAddonGroupWithAddonsById(id: String): AddonGroupWithAddons? {
        val addonGroup = addonGroupDao.getAddonGroupById(id) ?: return null
        val addonIds = junctionDao.getAddonIdsByAddonGroupIdSync(id)
        val addons = addonIds.mapNotNull { addonId -> addonDao.getAddonById(addonId) }
        return AddonGroupWithAddons(addonGroup, addons)
    }

    private fun addonGroupDtoToWithAddons(dto: AddonGroupDto): AddonGroupWithAddons {
        val group = ProductMapper.toEntity(dto)
        val addons = (dto.addons ?: emptyList()).map { ProductMapper.toEntity(it, dto.id) }
        return AddonGroupWithAddons(group, addons)
    }

    override suspend fun getAddonGroupWithAddonsFromApi(id: String): Result<AddonGroupWithAddons> {
        if (!networkConnectivityChecker.isConnected()) {
            return Result.failure(Exception(context.getString(R.string.addon_group_management_no_internet)))
        }
        return try {
            val response = productsApi.getAddonGroupDetail(id)
            if (response.status != 200) {
                return Result.failure(Exception(response.message ?: "Failed to fetch addon group detail"))
            }
            val dto = response.data
                ?: return Result.failure(Exception(context.getString(R.string.addon_group_form_error_not_found)))
            Result.success(addonGroupDtoToWithAddons(dto))
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()
            val errorMessage = when (e.code()) {
                401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                403 -> parseApiErrorResponse(errorBody, e.code())
                404 -> context.getString(R.string.addon_group_form_error_not_found)
                500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                else -> parseApiErrorResponse(errorBody, e.code())
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดที่ไม่คาดคิด"))
        }
    }
    
    override suspend fun createAddonGroup(
        name: String,
        isRequired: Boolean,
        isSingleSelection: Boolean,
        maxSelection: Int,
        minSelection: Int,
        sortOrder: Int,
        selectedAddonIds: List<String>
    ): Result<AddonGroupEntity> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception(context.getString(R.string.addon_group_management_no_internet)))
            }
            try {
                val request = CreateAddonGroupRequestDto(
                    addonGroup = CreateAddonGroupRequestDto.AddonGroupData(
                        name = name.trim(),
                        isRequired = isRequired,
                        isSingleSelection = isSingleSelection,
                        maxSelection = maxSelection,
                        minSelection = minSelection,
                        sortOrder = sortOrder,
                        isActive = true
                    ),
                    addons = selectedAddonIds.mapIndexed { index, addonId ->
                        CreateAddonGroupRequestDto.AddonData(
                            addonId = addonId,
                            sortOrder = index + 1
                        )
                    }
                )

                val response = productsApi.createAddonGroup(request)

                val isSuccessStatus = response.status == 200 || response.status == 201
                val hasSuccessMessage = response.message?.contains("success", ignoreCase = true) == true
                    || response.message?.contains("created", ignoreCase = true) == true

                if ((isSuccessStatus && response.data != null) || (hasSuccessMessage && response.data != null)) {
                    Result.success(ProductMapper.toEntity(response.data!!))
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
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการสร้างกลุ่ม Addon"))
        }
    }

    private suspend fun fetchAddonGroupDtoById(addonGroupId: String): Result<AddonGroupDto> {
        if (!networkConnectivityChecker.isConnected()) {
            return Result.failure(Exception(context.getString(R.string.addon_group_management_no_internet)))
        }
        return try {
            val response = productsApi.getAddonGroupDetail(addonGroupId)
            if (response.status != 200) {
                return Result.failure(Exception(response.message ?: "Failed to fetch addon group detail"))
            }
            val dto = response.data
                ?: return Result.failure(Exception(context.getString(R.string.addon_group_form_error_not_found)))
            Result.success(dto)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()
            Result.failure(Exception(parseApiErrorResponse(errorBody, e.code())))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการโหลดกลุ่ม Addon"))
        }
    }
    
    override suspend fun updateAddonGroup(
        addonGroupId: String,
        name: String?,
        isRequired: Boolean?,
        isSingleSelection: Boolean?,
        maxSelection: Int?,
        minSelection: Int?,
        sortOrder: Int?,
        isActive: Boolean?,
        selectedAddonIds: List<String>?
    ): Result<AddonGroupEntity> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception(context.getString(R.string.addon_group_management_no_internet)))
            }
            val hasFullPayloadFromCaller =
                name != null && isRequired != null && isSingleSelection != null && maxSelection != null &&
                    minSelection != null && sortOrder != null && isActive != null && selectedAddonIds != null

            val existingDto = if (hasFullPayloadFromCaller) {
                null
            } else {
                fetchAddonGroupDtoById(addonGroupId).getOrElse { return Result.failure(it) }
            }

            val finalName = when {
                name != null -> name.trim()
                existingDto != null -> existingDto.name
                else -> return Result.failure(Exception("Invalid update state"))
            }
            val finalIsRequired = isRequired ?: existingDto?.isRequired
                ?: return Result.failure(Exception("Invalid update state"))
            val finalIsSingleSelection = isSingleSelection ?: existingDto?.isSingleSelection
                ?: return Result.failure(Exception("Invalid update state"))
            val finalMaxSelection = maxSelection ?: existingDto?.maxSelection ?: 1
            val finalMinSelection = minSelection ?: existingDto?.minSelection ?: 0
            val finalSortOrder = sortOrder ?: existingDto?.sortOrder
                ?: return Result.failure(Exception("Invalid update state"))
            val finalIsActive = isActive ?: existingDto?.isActive
                ?: return Result.failure(Exception("Invalid update state"))
            val finalSelectedAddonIds = selectedAddonIds ?: existingDto?.addons?.map { it.id } ?: emptyList()

            try {
                val request = UpdateAddonGroupRequestDto(
                    addonGroup = UpdateAddonGroupRequestDto.AddonGroupData(
                        id = addonGroupId,
                        name = finalName,
                        isRequired = finalIsRequired,
                        isSingleSelection = finalIsSingleSelection,
                        maxSelection = finalMaxSelection,
                        minSelection = finalMinSelection,
                        sortOrder = finalSortOrder,
                        isActive = finalIsActive
                    ),
                    addons = finalSelectedAddonIds.mapIndexed { index, addonId ->
                        UpdateAddonGroupRequestDto.AddonData(
                            addonId = addonId,
                            sortOrder = index + 1
                        )
                    }
                )

                val response = productsApi.updateAddonGroup(request)

                val isSuccessStatus = response.status == 200 || response.status == 201
                val hasSuccessMessage = response.message?.contains("success", ignoreCase = true) == true
                    || response.message?.contains("updated", ignoreCase = true) == true

                if ((isSuccessStatus && response.data != null) || (hasSuccessMessage && response.data != null)) {
                    Result.success(ProductMapper.toEntity(response.data!!))
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
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการแก้ไขกลุ่ม Addon"))
        }
    }
    
    override suspend fun toggleAddonGroupStatus(
        addonGroupId: String,
        newStatus: Boolean
    ): Result<AddonGroupEntity> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception(context.getString(R.string.addon_group_management_no_internet)))
            }
            try {
                val request = ToggleAddonGroupStatusRequestDto(status = newStatus)
                val response = productsApi.toggleAddonGroupStatus(addonGroupId, request)

                if (response.status == 200 && response.data != null) {
                    Result.success(ProductMapper.toEntity(response.data))
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
                    500 -> parseApiErrorResponse(errorBody, e.code())
                    else -> parseApiErrorResponse(errorBody, e.code())
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะกลุ่ม Addon"))
        }
    }
    
    override suspend fun deleteAddonGroup(addonGroupId: String): Result<Unit> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception(context.getString(R.string.addon_group_management_no_internet)))
            }
            try {
                val response = productsApi.deleteAddonGroup(addonGroupId)

                if (response.status == 200) {
                    Result.success(Unit)
                } else {
                    val errorMessage = response.message?.takeIf { it.isNotBlank() }
                        ?: "เกิดข้อผิดพลาดในการลบกลุ่ม Addon"
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
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการลบกลุ่ม Addon"))
        }
    }
    
    override suspend fun deleteMultipleAddonGroups(addonGroupIds: List<String>): Result<DeleteAddonGroupsResult> {
        return try {
            if (addonGroupIds.isEmpty()) {
                return Result.failure(Exception("กรุณาเลือกกลุ่ม Addon ที่ต้องการลบ"))
            }
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception(context.getString(R.string.addon_group_management_no_internet)))
            }
            try {
                val request = DeleteAddonGroupsRequestDto(addonGroupIds = addonGroupIds)
                val response = productsApi.deleteMultipleAddonGroups(request)

                if (response.status == 200) {
                    val totalDeleted = response.data?.totalDeleted ?: response.count
                    val totalFailed = response.data?.totalFailed ?: response.failedDeletions.orEmpty().size
                    val errors = response.errors.orEmpty()

                    Result.success(
                        DeleteAddonGroupsResult(
                            deletedCount = totalDeleted,
                            failedCount = totalFailed,
                            errors = errors
                        )
                    )
                } else {
                    val errorMessage = response.message.takeIf { it.isNotBlank() }
                        ?: "เกิดข้อผิดพลาดในการลบกลุ่ม Addon"
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
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการลบกลุ่ม Addon"))
        }
    }
    
    override suspend fun fetchAndSyncAddonGroups(): Result<Unit> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception("กรุณาเชื่อมต่ออินเทอร์เน็ต"))
            }

            val response = productsApi.getAddonGroups()

            if (response.status == 200) {
                // If status is 200, treat as success even if data is null or empty (new user might have no data)
                val addonGroupsList = response.data ?: emptyList()
                // Convert and save addon groups + their addons + junctions
                if (addonGroupsList.isNotEmpty()) {
                    // 1) Save groups themselves
                    val addonGroups = addonGroupsList.map { ProductMapper.toEntity(it) }
                    addonGroupDao.insertAll(addonGroups)

                    // 2) Optionally refresh junctions ONLY when server actually sends addons list
                    //    (บาง environment อาจไม่ส่ง field addons มาเลย ถ้าลบทุกครั้งจะทำให้ความสัมพันธ์ใน Room หาย)
                    addonGroupsList.forEach { groupDto ->
                        val serverAddons = groupDto.addons
                        if (serverAddons != null) {
                            // Server บอกความสัมพันธ์มาอย่างชัดเจน → sync ตาม server
                            junctionDao.deleteByAddonGroupId(groupDto.id)

                            serverAddons.forEachIndexed { index, addonDto ->
                                val addonEntity = ProductMapper.toEntity(addonDto, groupDto.id)
                                addonDao.insert(addonEntity)
                                junctionDao.insert(
                                    AddonGroupAddonJunctionEntity(
                                        addonGroupId = groupDto.id,
                                        addonId = addonEntity.id,
                                        sortOrder = index + 1
                                    )
                                )
                            }
                        }
                    }
                }
                // Remove local addon groups that are no longer in API (e.g. deleted on another device)
                val apiAddonGroupIds = addonGroupsList.map { it.id }.toSet()
                val existingAddonGroupIds = addonGroupDao.getAllAddonGroups().map { it.id }.toSet()
                (existingAddonGroupIds - apiAddonGroupIds).forEach { id ->
                    junctionDao.deleteByAddonGroupId(id)
                    addonGroupDao.permanentlyDeleteAddonGroup(id)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "เกิดข้อผิดพลาดในการดึงข้อมูล"))
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
                500 -> parseApiErrorResponse(errorBody, e.code())
                else -> parseApiErrorResponse(errorBody, e.code())
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดที่ไม่คาดคิด"))
        }
    }
    
    override suspend fun syncAddonGroups(): Result<Unit> {
        return fetchAndSyncAddonGroups()
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
                val errorResponse = gson.fromJson(errorJson, AddonGroupErrorResponse::class.java)
                val errorText = errorResponse.error?.lowercase() ?: ""
                val messageText = errorResponse.message?.lowercase() ?: ""
                
                // Check for specific error keys first
                val errorKey = errorResponse.error?.takeIf { it.isNotBlank() }
                val messageKey = errorResponse.message?.takeIf { it.isNotBlank() }
                val combinedErrorText = "$errorText $messageText"
                
                // Addon group delete API errors (map en/th locale)
                when {
                    combinedErrorText.contains("addon group id is required") ||
                    combinedErrorText.contains("กรุณาระบุกลุ่ม addon") -> {
                        return getLocalizedString("api_error_delete_addon_group_id_required", "จำเป็นต้องระบุรหัสกลุ่มแอดออน")
                    }
                    combinedErrorText.contains("addon group ids are required") ||
                    combinedErrorText.contains("at least one addon group id") ||
                    combinedErrorText.contains("กรุณาเลือกกลุ่ม addon") -> {
                        return getLocalizedString("api_error_delete_addon_group_ids_required", "ต้องระบุรหัสกลุ่มแอดออนอย่างน้อย 1 รายการ")
                    }
                    combinedErrorText.contains("addon group not found") ||
                    combinedErrorText.contains("ไม่พบกลุ่ม addon") -> {
                        return getLocalizedString("api_error_delete_addon_group_not_found", "ไม่พบกลุ่มแอดออน")
                    }
                    combinedErrorText.contains("access denied") ||
                    combinedErrorText.contains("addon group does not belong") ||
                    combinedErrorText.contains("กลุ่ม addon ของคุณเอง") -> {
                        return getLocalizedString("api_error_delete_addon_group_access_denied", "ไม่มีสิทธิ์เข้าถึง กลุ่มแอดออนนี้ไม่ใช่ของคุณ")
                    }
                    combinedErrorText.contains("invalid request body") ||
                    combinedErrorText.contains("invalid character") ||
                    combinedErrorText.contains("ข้อมูลที่ส่งไม่ถูกต้อง") -> {
                        return getLocalizedString("api_error_delete_addon_group_bad_request", "คำขอไม่ถูกต้อง กรุณาตรวจสอบข้อมูล")
                    }
                    combinedErrorText.contains("missing authorization") ||
                    combinedErrorText.contains("authorization") ||
                    combinedErrorText.contains("unauthorized") -> {
                        return getLocalizedString("api_error_delete_addon_group_generic", "ไม่สามารถลบกลุ่มแอดออนได้ กรุณาลองใหม่อีกครั้ง")
                    }
                }
                
                // Handle specific status codes
                when (statusCode) {
                    400 -> {
                        // Bad Request - return message or error field
                        // Check for specific error messages
                        when {
                            messageText.contains("addon group name is required", ignoreCase = true) -> {
                                "กรุณากรอกชื่อกลุ่ม Addon"
                            }
                            messageText.contains("addon id is required", ignoreCase = true) ||
                            messageText.contains("all addons must have an addon_id", ignoreCase = true) -> {
                                "กรุณาเลือก Addon"
                            }
                            else -> {
                                errorResponse.message?.takeIf { it.isNotBlank() }
                                    ?: errorResponse.error?.takeIf { it.isNotBlank() }
                                    ?: "ข้อมูลไม่ถูกต้อง กรุณาตรวจสอบอีกครั้ง"
                            }
                        }
                    }
                    401 -> {
                        // Unauthorized - return message or error field
                        errorResponse.message?.takeIf { it.isNotBlank() }
                            ?: errorResponse.error?.takeIf { it.isNotBlank() }
                            ?: getLocalizedString("api_error_delete_addon_group_generic", "ไม่สามารถลบกลุ่มแอดออนได้ กรุณาลองใหม่อีกครั้ง")
                    }
                    403 -> {
                        // Forbidden - Free plan limit exceeded or Access denied
                        val message = errorResponse.message?.takeIf { it.isNotBlank() }
                            ?: errorResponse.error?.takeIf { it.isNotBlank() }
                            ?: ""
                        
                        when {
                            message.contains("free_plan_limit", ignoreCase = true) -> {
                                "คุณใช้กลุ่มตัวเลือกเพิ่มเติมครบจำนวนที่กำหนดแล้ว กรุณาอัปเกรดแผน"
                            }
                            message.contains("access denied", ignoreCase = true) -> {
                                getLocalizedString("api_error_delete_addon_group_access_denied", "ไม่มีสิทธิ์เข้าถึง กลุ่มแอดออนนี้ไม่ใช่ของคุณ")
                            }
                            else -> {
                                message.ifBlank { getLocalizedString("api_error_delete_addon_group_access_denied", "ไม่มีสิทธิ์เข้าถึง กลุ่มแอดออนนี้ไม่ใช่ของคุณ") }
                            }
                        }
                    }
                    404 -> {
                        // Not Found - Addon group not found
                        errorResponse.message?.takeIf { it.isNotBlank() }
                            ?: errorResponse.error?.takeIf { it.isNotBlank() }
                            ?: getLocalizedString("api_error_delete_addon_group_not_found", "ไม่พบกลุ่มแอดออน")
                    }
                    409 -> {
                        // Conflict - Duplicate addon group name
                        when {
                            combinedErrorText.contains("duplicate addon group name") || 
                            combinedErrorText.contains("duplicate name") -> {
                                getLocalizedString(
                                    "addon_group_form_error_duplicate_name",
                                    "This addon group name already exists"
                                )
                            }
                            errorKey != null && messageKey != null -> "$errorKey ($messageKey)"
                            errorKey != null -> errorKey
                            messageKey != null -> messageKey
                            else -> getLocalizedString(
                                "addon_group_form_error_duplicate_name",
                                "This addon group name already exists"
                            )
                        }
                    }
                    500 -> {
                        // Internal Server Error
                        errorResponse.message?.takeIf { it.isNotBlank() }
                            ?: errorResponse.error?.takeIf { it.isNotBlank() }
                            ?: getLocalizedString("api_error_delete_addon_group_server_error", "เกิดข้อผิดพลาดของเซิร์ฟเวอร์ กรุณาลองใหม่อีกครั้ง")
                    }
                    else -> {
                        // Return error or message if available
                        errorResponse.error?.takeIf { it.isNotBlank() }
                            ?: errorResponse.message?.takeIf { it.isNotBlank() }
                            ?: getLocalizedString("api_error_delete_addon_group_generic", "ไม่สามารถลบกลุ่มแอดออนได้ กรุณาลองใหม่อีกครั้ง")
                    }
                }
            } catch (e: Exception) {
                // If parsing fails, check raw string
                val errorLower = errorJson.lowercase()
                
                when {
                    statusCode == 409 && errorLower.contains("duplicate addon group name") -> {
                        getLocalizedString(
                            "addon_group_form_error_duplicate_name",
                            "This addon group name already exists"
                        )
                    }
                    statusCode == 403 && errorLower.contains("free_plan_limit_exceeded") -> {
                        "คุณใช้กลุ่มตัวเลือกเพิ่มเติมครบจำนวนที่กำหนดแล้ว กรุณาอัปเกรดแผน"
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
     * Uses localized strings where applicable (delete errors)
     */
    private fun getDefaultErrorMessage(statusCode: Int): String {
        return when (statusCode) {
            400 -> getLocalizedString("api_error_delete_addon_group_bad_request", "คำขอไม่ถูกต้อง กรุณาตรวจสอบข้อมูล")
            401 -> getLocalizedString("api_error_delete_addon_group_generic", "ไม่สามารถลบกลุ่มแอดออนได้ กรุณาลองใหม่อีกครั้ง")
            403 -> getLocalizedString("api_error_delete_addon_group_access_denied", "ไม่มีสิทธิ์เข้าถึง กลุ่มแอดออนนี้ไม่ใช่ของคุณ")
            404 -> getLocalizedString("api_error_delete_addon_group_not_found", "ไม่พบกลุ่มแอดออน")
            409 -> getLocalizedString(
                "addon_group_form_error_duplicate_name",
                "This addon group name already exists"
            )
            500 -> getLocalizedString("api_error_delete_addon_group_server_error", "เกิดข้อผิดพลาดของเซิร์ฟเวอร์ กรุณาลองใหม่อีกครั้ง")
            else -> getLocalizedString("api_error_delete_addon_group_generic", "ไม่สามารถลบกลุ่มแอดออนได้ กรุณาลองใหม่อีกครั้ง")
        }
    }
    
    override suspend fun getSyncStatistics(): com.indybrain.indypos_Android.domain.repository.AddonGroupSyncStatistics {
        val allAddonGroups = addonGroupDao.getAllAddonGroups()
        val unsynced = addonGroupDao.getUnsyncedAddonGroups()
        val deleted = addonGroupDao.getDeletedUnsyncedAddonGroups()
        
        val total = allAddonGroups.size + deleted.size
        val synced = allAddonGroups.count { it.isSynced }
        val unsyncedCount = unsynced.size
        val deletedCount = deleted.size
        
        return com.indybrain.indypos_Android.domain.repository.AddonGroupSyncStatistics(
            total = total,
            synced = synced,
            unsynced = unsyncedCount,
            deleted = deletedCount
        )
    }
}

/**
 * Error response DTO for parsing API errors
 */
private data class AddonGroupErrorResponse(
    val error: String?,
    val message: String?
)
