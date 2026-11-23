package com.team.itexperts.shrinkflation.data

import com.team.itexperts.shrinkflation.model.UnitType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao,
    private val variantDao: ProductVariantDao
) {
    suspend fun searchByName(q: String): List<ProductCard> {
        if (q.isBlank()) return emptyList()
        val products = productDao.searchByNameSync(q)
        val result = mutableListOf<ProductCard>()
        for (p in products) {
            val variants = variantDao.getByProductSync(p.id)
            if (variants.isEmpty()) {
                result.add(ProductCard(p.id, null, p.name, null, null, p.unitType))
            } else {
                for (v in variants) {
                    result.add(ProductCard(p.id, v.id, p.name, v.brand, v.spec, p.unitType))
                }
            }
        }
        return result.distinctBy { it.name }
    }

    suspend fun searchByBrand(q: String): List<ProductCard> {
        if (q.isBlank()) return emptyList()
        val variants = variantDao.searchByBrandSync(q)
        val result = mutableListOf<ProductCard>()
        for (v in variants) {
            val p = productDao.getById(v.productId) ?: continue
            result.add(ProductCard(p.id, v.id, p.name, v.brand, v.spec, p.unitType))
        }
        return result.distinctBy { it.brand }
    }

    suspend fun searchBySpec(q: String): List<ProductCard> {
        if (q.isBlank()) return emptyList()
        val variants = variantDao.searchBySpecSync(q)
        val result = mutableListOf<ProductCard>()
        for (v in variants) {
            val p = productDao.getById(v.productId) ?: continue
            result.add(ProductCard(p.id, v.id, p.name, v.brand, v.spec, p.unitType))
        }
        return result.distinctBy { it.spec }
    }

    suspend fun getProduct(id: Long): ProductEntity? = productDao.getById(id)

    suspend fun getVariant(id: Long): ProductVariantEntity? = variantDao.getById(id)

    suspend fun updateProduct(product: ProductEntity) = productDao.update(product)

    suspend fun updateVariant(variant: ProductVariantEntity) = variantDao.update(variant)

    suspend fun variantConflict(
        productId: Long,
        brand: String?,
        spec: String?,
        exceptId: Long?
    ): ProductVariantEntity? = variantDao.getByProductSync(productId).firstOrNull {
        it.id != exceptId && it.brand == brand && it.spec == spec
    }

    suspend fun findOrCreateProduct(name: String, unitType: UnitType): ProductEntity {
        val existing = productDao.findByName(name, unitType)
        if (existing != null) return existing
        val id = productDao.insert(ProductEntity(name = name, unitType = unitType))
        return productDao.getById(id)!!
    }

    suspend fun findOrCreateVariant(productId: Long, brand: String?, spec: String?): ProductVariantEntity {
        val existing = variantDao.findDuplicate(productId, brand, spec)
        if (existing != null) return existing
        val id = variantDao.insert(ProductVariantEntity(productId = productId, brand = brand, spec = spec))
        return variantDao.getById(id)!!
    }
}

@Singleton
class PriceEntryRepository @Inject constructor(private val dao: PriceEntryDao) {
    fun all(): Flow<List<PriceEntryEntity>> = dao.getAll()
    fun byVariant(id: Long): Flow<List<PriceEntryEntity>> = dao.getByVariant(id)
    suspend fun insert(entry: PriceEntryEntity): Long = dao.insert(entry)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun deleteAll() = dao.deleteAll()
    suspend fun count() = dao.count()
    suspend fun deleteOldest(n: Int) = dao.deleteOldest(n)
    suspend fun getLastByVariant(variantId: Long) = dao.getLastByVariant(variantId)
    suspend fun findRecentDuplicate(variantId: Long?, price: String, quantity: String, unit: String, since: Long) =
        dao.findRecentDuplicate(variantId, price, quantity, unit, since)
}

@Singleton
class CatalogRepository @Inject constructor(
    private val priceDao: PriceEntryDao,
    private val productDao: ProductDao,
    private val variantDao: ProductVariantDao
) {
    fun catalog(): Flow<List<CatalogRow>> = priceDao.catalog()

    suspend fun deleteRow(row: CatalogRow) {
        val variant = row.variantId?.let { variantDao.getById(it) }
        if (variant != null) {
            priceDao.deleteByVariant(variant.id)
            variantDao.delete(variant)
        }
        if (variantDao.countByProduct(row.productId) == 0) {
            priceDao.deleteOrphanPrices()
            productDao.getById(row.productId)?.let { productDao.delete(it) }
        }
    }
}
