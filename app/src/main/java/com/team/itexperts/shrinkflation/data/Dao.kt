package com.team.itexperts.shrinkflation.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.team.itexperts.shrinkflation.model.UnitType

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name")
    fun getAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :q || '%'")
    suspend fun searchByNameSync(q: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE name = :name AND unitType = :unitType LIMIT 1")
    suspend fun findByName(name: String, unitType: UnitType): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Delete
    suspend fun delete(product: ProductEntity)
}

@Dao
interface ProductVariantDao {
    @Query("SELECT * FROM product_variants WHERE productId = :productId")
    suspend fun getByProductSync(productId: Long): List<ProductVariantEntity>

    @Query("SELECT * FROM product_variants WHERE id = :id")
    suspend fun getById(id: Long): ProductVariantEntity?

    @Query("SELECT * FROM product_variants WHERE brand LIKE '%' || :q || '%'")
    suspend fun searchByBrandSync(q: String): List<ProductVariantEntity>

    @Query("SELECT * FROM product_variants WHERE spec LIKE '%' || :q || '%'")
    suspend fun searchBySpecSync(q: String): List<ProductVariantEntity>

    @Query("SELECT * FROM product_variants WHERE productId = :productId AND brand IS :brand AND spec IS :spec LIMIT 1")
    suspend fun findDuplicate(productId: Long, brand: String?, spec: String?): ProductVariantEntity?

    @Query("SELECT COUNT(*) FROM product_variants WHERE productId = :productId")
    suspend fun countByProduct(productId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(variant: ProductVariantEntity): Long

    @Update
    suspend fun update(variant: ProductVariantEntity)

    @Delete
    suspend fun delete(variant: ProductVariantEntity)
}

@Dao
interface PriceEntryDao {
    @Query("SELECT * FROM price_entries ORDER BY createdAt DESC")
    fun getAll(): Flow<List<PriceEntryEntity>>

    @Query("SELECT * FROM price_entries WHERE variantId = :variantId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastByVariant(variantId: Long): PriceEntryEntity?

    @Query("SELECT * FROM price_entries WHERE variantId = :variantId")
    fun getByVariant(variantId: Long): Flow<List<PriceEntryEntity>>

    @Query("SELECT * FROM price_entries WHERE variantId = :variantId AND price = :price AND quantity = :quantity AND unit = :unit AND createdAt > :since LIMIT 1")
    suspend fun findRecentDuplicate(variantId: Long?, price: String, quantity: String, unit: String, since: Long): PriceEntryEntity?

    @Insert
    suspend fun insert(entry: PriceEntryEntity): Long

    @Query("DELETE FROM price_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM price_entries")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM price_entries")
    suspend fun count(): Int

    @Query("DELETE FROM price_entries WHERE id IN (SELECT id FROM price_entries ORDER BY createdAt ASC LIMIT :n)")
    suspend fun deleteOldest(n: Int)

    @Query("""
        SELECT p.id AS productId, v.id AS variantId, p.name AS name,
               v.brand AS brand, v.spec AS spec, p.unitType AS unitType,
               pe.price AS price, pe.quantity AS quantity, pe.unit AS unit
        FROM products p
        LEFT JOIN product_variants v ON v.productId = p.id
        LEFT JOIN price_entries pe ON pe.variantId = v.id AND pe.id = (
            SELECT pe2.id FROM price_entries pe2
            WHERE pe2.variantId = v.id ORDER BY pe2.createdAt DESC, pe2.id DESC LIMIT 1
        )
        ORDER BY p.name COLLATE NOCASE, v.brand COLLATE NOCASE, v.spec COLLATE NOCASE
    """)
    fun catalog(): Flow<List<CatalogRow>>

    @Query("DELETE FROM price_entries WHERE variantId = :variantId")
    suspend fun deleteByVariant(variantId: Long)

    @Query("DELETE FROM price_entries WHERE variantId IS NULL")
    suspend fun deleteOrphanPrices()
}
