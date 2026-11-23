package com.team.itexperts.shrinkflation.ui

import com.team.itexperts.shrinkflation.data.ProductCard
import com.team.itexperts.shrinkflation.data.ProductRepository

object ProductSearch {
    suspend fun search(repo: ProductRepository, field: String, query: String): List<ProductCard> = when (field) {
        "name" -> repo.searchByName(query).distinctBy { it.name }
        "brand" -> repo.searchByBrand(query).distinctBy { it.brand }
        "spec" -> repo.searchBySpec(query).distinctBy { it.spec }
        else -> repo.searchByName(query).distinctBy { it.name }
    }

    fun value(card: ProductCard, field: String?): String = when (field) {
        "name" -> card.name
        "brand" -> card.brand ?: ""
        "spec" -> card.spec ?: ""
        else -> card.name
    }

    fun label(card: ProductCard, field: String?): String = value(card, field)

    fun applySelection(
        card: ProductCard,
        field: String?,
        curName: String,
        curBrand: String,
        curSpec: String
    ): Triple<String, String, String> {
        val name = if (field == "name") card.name else curName
        val brand = if (field == "brand") (card.brand ?: "") else curBrand
        val spec = if (field == "spec") (card.spec ?: "") else curSpec
        return Triple(name, brand, spec)
    }
}
