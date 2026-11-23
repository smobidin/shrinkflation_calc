package com.team.itexperts.shrinkflation.calc

import com.team.itexperts.shrinkflation.model.*
import java.math.BigDecimal
import java.math.RoundingMode

object UnitConverter {
    fun toBaseQuantity(input: PackageInput): BigDecimal {
        return when (input.unitType) {
            UnitType.MASS -> input.quantity.multiply(input.massUnit.factor)
            UnitType.VOLUME -> input.quantity.multiply(input.volumeUnit.factor)
            UnitType.COUNT -> input.quantity
        }
    }

    fun unitPrice(input: PackageInput): BigDecimal {
        val baseQty = toBaseQuantity(input)
        if (baseQty.compareTo(BigDecimal.ZERO) == 0) throw IllegalArgumentException("quantity zero")
        return input.price.divide(baseQty, 6, RoundingMode.HALF_UP)
    }

    fun baseUnitLabel(type: UnitType): String = when (type) {
        UnitType.MASS -> "г"
        UnitType.VOLUME -> "мл"
        UnitType.COUNT -> "шт"
    }

    fun unitPriceWhole(input: PackageInput): BigDecimal {
        val base = unitPrice(input)
        return when (input.unitType) {
            UnitType.MASS -> base.multiply(BigDecimal("1000"))
            UnitType.VOLUME -> base.multiply(BigDecimal("1000"))
            UnitType.COUNT -> base
        }
    }

    fun wholeUnitLabel(type: UnitType): String = when (type) {
        UnitType.MASS -> "кг"
        UnitType.VOLUME -> "л"
        UnitType.COUNT -> "шт"
    }
}

object ShrinkflationCalculator {
    fun calculate(old: PackageInput, new: PackageInput): ShrinkflationResult {
        require(old.unitType == new.unitType) { "incompatible units" }
        val uOld = UnitConverter.unitPrice(old)
        val uNew = UnitConverter.unitPrice(new)
        val change = if (uOld.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO
        else uNew.subtract(uOld).divide(uOld, 6, RoundingMode.HALF_UP).multiply(BigDecimal("100"))
        val equiv = uOld.multiply(UnitConverter.toBaseQuantity(new))
        val overpay = new.price.subtract(equiv)
        val target = equiv
        val discount = if (new.price.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO
        else new.price.subtract(target).divide(new.price, 6, RoundingMode.HALF_UP).multiply(BigDecimal("100"))
        return ShrinkflationResult(uOld, uNew, change, equiv, overpay, target, discount)
    }
}

data class ComparisonItem(
    val name: String,
    val input: PackageInput,
    val brand: String = "",
    val spec: String = ""
)

data class ComparisonResult(
    val name: String,
    val unitPrice: BigDecimal,
    val diffPercent: BigDecimal,
    val brand: String = "",
    val spec: String = "",
    val unit: UnitType = UnitType.MASS,
    val wholeUnitPrice: BigDecimal = BigDecimal.ZERO,
    val absoluteDiff: BigDecimal = BigDecimal.ZERO,
    val isCheapest: Boolean = false
)

object ComparisonCalculator {
    fun compare(items: List<ComparisonItem>): List<ComparisonResult> {
        require(items.size in 2..4) { "2-4 items" }
        val types = items.map { it.input.unitType }.distinct()
        require(types.size == 1) { "incompatible units" }

        val exact = items.map { item ->
            val base = UnitConverter.toBaseQuantity(item.input)
            if (base.compareTo(BigDecimal.ZERO) == 0) throw IllegalArgumentException("quantity zero")
            item.input.price.divide(base, 12, RoundingMode.HALF_UP)
        }
        val minExact = exact.minOrNull() ?: BigDecimal.ZERO
        val multiplier = when (types.single()) {
            UnitType.MASS, UnitType.VOLUME -> BigDecimal("1000")
            UnitType.COUNT -> BigDecimal.ONE
        }

        val results = items.mapIndexed { idx, item ->
            val diff = if (minExact.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO
            else exact[idx].subtract(minExact).divide(minExact, 6, RoundingMode.HALF_UP).multiply(BigDecimal("100"))
            ComparisonResult(
                name = item.name,
                unitPrice = UnitConverter.unitPrice(item.input),
                diffPercent = diff,
                brand = item.brand,
                spec = item.spec,
                unit = item.input.unitType,
                wholeUnitPrice = exact[idx].multiply(multiplier),
                absoluteDiff = exact[idx].subtract(minExact).multiply(multiplier),
                isCheapest = exact[idx].compareTo(minExact) == 0
            )
        }
        return results.sortedBy { it.unitPrice }
    }
}
