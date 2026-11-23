package com.team.itexperts.shrinkflation.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.team.itexperts.shrinkflation.model.Currency
import com.team.itexperts.shrinkflation.model.UnitType

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unitType: UnitType = UnitType.MASS,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "product_variants",
    foreignKeys = [ForeignKey(
        entity = ProductEntity::class,
        parentColumns = ["id"],
        childColumns = ["productId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("productId")]
)
data class ProductVariantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val brand: String? = null,
    val spec: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "price_entries",
    foreignKeys = [ForeignKey(
        entity = ProductVariantEntity::class,
        parentColumns = ["id"],
        childColumns = ["variantId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("variantId")]
)
data class PriceEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val variantId: Long? = null,
    val price: String,
    val quantity: String,
    val unit: String,
    val currency: Currency,
    val store: String? = null,
    val note: String? = null,
    val source: String,
    val createdAt: Long = System.currentTimeMillis()
)
