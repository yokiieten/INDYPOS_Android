package com.indybrain.indypos_Android.core.order

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.indybrain.indypos_Android.R
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.util.Locale

/**
 * Maps [POST /protected/indypos/orders][com.indybrain.indypos_Android.data.remote.api.OrdersApi.createOrder]
 * failure payloads (`error` string / HTTP bodies) into user-facing localized messages.
 */
object CreateOrderErrorMapper {

    fun mapRawError(
        context: Context,
        rawError: String?,
        httpStatusCode: Int?
    ): String {
        val err = rawError?.trim().orEmpty()
        if (err.isEmpty()) {
            return if (httpStatusCode != null) {
                context.getString(R.string.create_order_error_connection_failed, httpStatusCode.toString())
            } else {
                context.getString(R.string.common_error)
            }
        }

        val lower = err.lowercase(Locale.US)

        if (lower == "free_plan_limit_exceeded") {
            return context.getString(R.string.create_order_error_free_plan_limit)
        }

        if (lower.startsWith("product is not active:")) {
            val detail = detailAfterPrefix(full = err, prefixLower = "product is not active:")
            return context.getString(R.string.create_order_error_product_inactive, detail)
        }

        if (lower.startsWith("category is not active:")) {
            val detail = detailAfterPrefix(full = err, prefixLower = "category is not active:")
            return context.getString(R.string.create_order_error_category_inactive, detail)
        }

        if (lower.startsWith("product category not found for product:")) {
            val detail = detailAfterPrefix(full = err, prefixLower = "product category not found for product:")
            return context.getString(R.string.create_order_error_product_category_missing, detail)
        }

        if (lower.contains("invalid request body")) {
            return context.getString(R.string.create_order_error_invalid_body)
        }

        if (lower.contains("at least one item is required")) {
            return context.getString(R.string.create_order_error_no_line_items)
        }

        if (lower.contains("insufficient stock") ||
            lower.contains("out of stock") ||
            (lower.contains("insufficient") && lower.contains("stock"))
        ) {
            return context.getString(R.string.product_detail_insufficient_stock)
        }

        val failedProductIdx = lower.indexOf("failed to get product")
        if (failedProductIdx >= 0) {
            val tail = err.substring(failedProductIdx + "failed to get product".length).trim().removePrefix(":").trim()
            return context.getString(
                R.string.create_order_error_failed_get_product,
                tail.ifBlank { err }
            )
        }

        if (lower.contains("product not found")) {
            return context.getString(R.string.create_order_error_product_not_found)
        }

        return err
    }

    fun localizedMessageFromHttpException(context: Context, e: HttpException, gson: Gson): String {
        val raw = parseRawFromHttpException(e, gson)
        return mapRawError(context, raw, e.code())
    }

    private fun parseRawFromHttpException(e: HttpException, gson: Gson): String? {
        return try {
            val errorBody: ResponseBody? = e.response()?.errorBody()
            if (errorBody != null) {
                val errorJson = errorBody.string()
                if (errorJson.isNotBlank()) {
                    try {
                        val parsed = gson.fromJson(errorJson, CreateOrderApiErrorPayload::class.java)
                        parsed.error?.takeIf { it.isNotBlank() }
                            ?: parsed.message?.takeIf { it.isNotBlank() }
                            ?: e.message()
                    } catch (_: Exception) {
                        errorJson.trim().takeIf { it.isNotEmpty() }
                    }
                } else {
                    e.message()
                }
            } else {
                e.message()
            }
        } catch (_: Exception) {
            e.message()
        }
    }

    private fun detailAfterPrefix(full: String, prefixLower: String): String {
        val lower = full.lowercase(Locale.US)
        if (!lower.startsWith(prefixLower)) return full.trim()
        val afterPrefix = full.substring(prefixLower.length).trim().removePrefix(":").trim()
        return afterPrefix.ifBlank { full.trim() }
    }
}

@Keep
private data class CreateOrderApiErrorPayload(
    @SerializedName("error") val error: String?,
    @SerializedName("message") val message: String?
)
