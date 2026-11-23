package com.team.itexperts.shrinkflation.data

import com.team.itexperts.shrinkflation.model.UnitType

data class CatalogRow(
    val productId: Long,
    val variantId: Long?,
    val name: String,
    val brand: String?,
    val spec: String?,
    val unitType: UnitType,
    val price: String?,
    val quantity: String?,
    val unit: String?
) {
    val key: Long get() = variantId ?: -productId
}
