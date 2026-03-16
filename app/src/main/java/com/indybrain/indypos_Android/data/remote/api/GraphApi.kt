package com.indybrain.indypos_Android.data.remote.api

import com.indybrain.indypos_Android.data.remote.dto.ApiResponseDto
import com.indybrain.indypos_Android.data.remote.dto.GraphDashboardResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API interface for Graph Dashboard endpoint
 * GET /api/v1/protected/indypos/graph/dashboard
 */
interface GraphApi {
    /**
     * Get graph dashboard data
     *
     * @param filterType today | week | month | custom
     * @param startDate Required when filter_type=custom (YYYY-MM-DD)
     * @param endDate Required when filter_type=custom (YYYY-MM-DD)
     * @param timezone IANA timezone, default Asia/Bangkok
     */
    @GET("protected/indypos/graph/dashboard")
    suspend fun getDashboard(
        @Query("filter_type") filterType: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("timezone") timezone: String? = "Asia/Bangkok"
    ): ApiResponseDto<GraphDashboardResponseDto>
}
