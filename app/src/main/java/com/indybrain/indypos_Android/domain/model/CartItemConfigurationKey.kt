package com.indybrain.indypos_Android.domain.model

private fun addonsKeyFromSelections(
    selectedAddons: Map<String, Set<String>>
): String {
    val sortedGroups = selectedAddons.keys.sorted()
    return sortedGroups.joinToString("|") { groupId ->
        val addonIds = selectedAddons[groupId]?.sorted()?.joinToString(",") ?: ""
        "$groupId:$addonIds"
    }
}

private fun addonsKeyFromCartItem(selectedAddons: Map<String, List<Addon>>): String {
    val asSets = selectedAddons.mapValues { (_, addons) ->
        addons.map { it.id }.toSet()
    }
    return addonsKeyFromSelections(asSets)
}

/**
 * Key for merging lines with identical product configuration (addons + special request).
 * When [includeProductId] is true, the full cart merges only lines with matching product IDs.
 */
fun addonsSelectionConfigurationKey(
    productId: String,
    specialRequest: String?,
    selectedAddons: Map<String, Set<String>>,
    includeProductId: Boolean
): String {
    val normalizedSpecialRequest = specialRequest ?: ""
    val addonsKey = addonsKeyFromSelections(selectedAddons)
    return if (includeProductId) {
        "${productId}|$normalizedSpecialRequest|$addonsKey"
    } else {
        "$normalizedSpecialRequest|$addonsKey"
    }
}

fun CartItem.configurationKey(includeProductId: Boolean): String {
    val normalizedSpecialRequest = specialRequest ?: ""
    val addonsKey = addonsKeyFromCartItem(selectedAddons)
    return if (includeProductId) {
        "${product.id}|$normalizedSpecialRequest|$addonsKey"
    } else {
        "$normalizedSpecialRequest|$addonsKey"
    }
}
