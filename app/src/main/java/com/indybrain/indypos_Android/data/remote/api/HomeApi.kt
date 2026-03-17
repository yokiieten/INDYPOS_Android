package com.indybrain.indypos_Android.data.remote.api

import com.indybrain.indypos_Android.data.remote.dto.ApiResponseDto
import com.indybrain.indypos_Android.data.remote.dto.TodaySalesResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API interface for Home Today Sales endpoint
 * GET /api/v1/protected/indypos/home/today-sales
 */
interface HomeApi {
    /**
     * Get today's sales summary for Home screen
     *
     * @param date Optional date (YYYY-MM-DD). Default: today
     * @param timezone IANA timezone, default Asia/Bangkok
     */
    @GET("protected/indypos/home/today-sales")
    suspend fun getTodaySales(
        @Query("date") date: String? = null,
        @Query("timezone") timezone: String? = "Asia/Bangkok"
    ): ApiResponseDto<TodaySalesResponseDto>
}
