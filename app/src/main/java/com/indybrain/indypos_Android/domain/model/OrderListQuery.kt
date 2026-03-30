package com.indybrain.indypos_Android.domain.model

/**
 * Parameters for [com.indybrain.indypos_Android.domain.repository.OrderRepository] paginated orders API.
 * Maps to GET `.../orders/paginated` query params.
 */
data class OrderListQuery(
    val tab: String,
    val sortBy: String,
    val page: Int,
    val limit: Int = 20,
    val startDate: String? = null,
    val endDate: String? = null
)

data class OrderListPageInfo(
    val hasNext: Boolean,
    val currentPage: Int
)
