package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO for upload shop image endpoint
 * Matches iOS structure:
 *
 * {
 *   "file": {
 *     "url": "...",
 *     "file_name": "...",
 *     "file_size": 123
 *   },
 *   "message": "...",
 *   "profile_updated": true
 * }
 */
data class UploadShopImageResponseDto(
    val file: UploadFileDataDto,
    val message: String,
    @SerializedName("profile_updated")
    val profileUpdated: Boolean
)

data class UploadFileDataDto(
    val url: String,
    @SerializedName("file_name")
    val fileName: String,
    @SerializedName("file_size")
    val fileSize: Long
)


