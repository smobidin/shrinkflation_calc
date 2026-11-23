package com.team.itexperts.shrinkflation.model

import java.math.BigDecimal
import java.math.RoundingMode

enum class UnitType { MASS, VOLUME, COUNT }
enum class MassUnit(val factor: BigDecimal) { G(BigDecimal.ONE), KG(BigDecimal("1000")) }
enum class VolumeUnit(val factor: BigDecimal) { ML(BigDecimal.ONE), L(BigDecimal("1000")) }
enum class Currency { RUB, USD, EUR, GBP, CNY }

enum class AppLanguage(val code: String) { RU("ru"), EN("en") }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class PackageInput(
    val price: BigDecimal,
    val quantity: BigDecimal,
    val unitType: UnitType,
    val massUnit: MassUnit = MassUnit.G,
    val volumeUnit: VolumeUnit = VolumeUnit.ML
)

data class UnitPriceResult(
    val unitPrice: BigDecimal,
    val baseUnit: String
)

data class ShrinkflationResult(
    val unitPriceOld: BigDecimal,
    val unitPriceNew: BigDecimal,
    val changePercent: BigDecimal,
    val equivalentOldPrice: BigDecimal,
    val overpay: BigDecimal,
    val targetPrice: BigDecimal,
    val discountNeededPercent: BigDecimal
)
