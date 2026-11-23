package com.team.itexperts.shrinkflation.data

import com.team.itexperts.shrinkflation.model.UnitType

data class ProductCard(
    val productId: Long,
    val variantId: Long?,
    val name: String,
    val brand: String?,
    val spec: String?,
    val unitType: UnitType
)
