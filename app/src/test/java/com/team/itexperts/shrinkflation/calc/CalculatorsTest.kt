package com.team.itexperts.shrinkflation.calc

import com.team.itexperts.shrinkflation.model.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class CalculatorsTest {
    // UnitConverter
    @Test
    fun unitConverter_mass() {
        val input = PackageInput(BigDecimal("100"), BigDecimal("1000"), UnitType.MASS, MassUnit.G)
        assertEquals(BigDecimal("1000"), UnitConverter.toBaseQuantity(input))
        assertEquals(0, UnitConverter.unitPrice(input).compareTo(BigDecimal("0.100000")))
    }

    @Test
    fun unitConverter_volume() {
        val input = PackageInput(BigDecimal("50"), BigDecimal("500"), UnitType.VOLUME, volumeUnit = VolumeUnit.ML)
        assertEquals(BigDecimal("500"), UnitConverter.toBaseQuantity(input))
        assertEquals(0, UnitConverter.unitPrice(input).compareTo(BigDecimal("0.100000")))
    }

    @Test
    fun unitConverter_count() {
        val input = PackageInput(BigDecimal("30"), BigDecimal("3"), UnitType.COUNT)
        assertEquals(BigDecimal("3"), UnitConverter.toBaseQuantity(input))
        assertEquals(0, UnitConverter.unitPrice(input).compareTo(BigDecimal("10.000000")))
    }

    @Test
    fun unitConverter_zeroQuantityThrows() {
        val input = PackageInput(BigDecimal("100"), BigDecimal.ZERO, UnitType.MASS, MassUnit.G)
        try { UnitConverter.unitPrice(input); fail("expected") } catch (e: IllegalArgumentException) { }
    }

    @Test
    fun unitConverter_baseUnitLabel() {
        assertEquals("г", UnitConverter.baseUnitLabel(UnitType.MASS))
        assertEquals("мл", UnitConverter.baseUnitLabel(UnitType.VOLUME))
        assertEquals("шт", UnitConverter.baseUnitLabel(UnitType.COUNT))
    }

    // ShrinkflationCalculator
    @Test
    fun shrinkflation_basic() {
        val old = PackageInput(BigDecimal("100"), BigDecimal("1000"), UnitType.MASS, MassUnit.G)
        val new = PackageInput(BigDecimal("100"), BigDecimal("900"), UnitType.MASS, MassUnit.G)
        val r = ShrinkflationCalculator.calculate(old, new)
        assertEquals(0, r.changePercent.compareTo(BigDecimal("11.111000")))
        assertTrue(r.overpay.compareTo(BigDecimal.ZERO) > 0)
    }

    @Test
    fun shrinkflation_noChange() {
        val old = PackageInput(BigDecimal("100"), BigDecimal("1000"), UnitType.MASS, MassUnit.G)
        val new = PackageInput(BigDecimal("100"), BigDecimal("1000"), UnitType.MASS, MassUnit.G)
        val r = ShrinkflationCalculator.calculate(old, new)
        assertEquals(0, r.changePercent.compareTo(BigDecimal.ZERO))
        assertEquals(0, r.overpay.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun shrinkflation_incompatibleThrows() {
        val old = PackageInput(BigDecimal("100"), BigDecimal("1000"), UnitType.MASS, MassUnit.G)
        val new = PackageInput(BigDecimal("100"), BigDecimal("1000"), UnitType.VOLUME, volumeUnit = VolumeUnit.ML)
        try { ShrinkflationCalculator.calculate(old, new); fail("expected") } catch (e: IllegalArgumentException) { }
    }

    @Test
    fun shrinkflation_equivAndTarget() {
        val old = PackageInput(BigDecimal("100"), BigDecimal("1000"), UnitType.MASS, MassUnit.G)
        val new = PackageInput(BigDecimal("110"), BigDecimal("900"), UnitType.MASS, MassUnit.G)
        val r = ShrinkflationCalculator.calculate(old, new)
        assertEquals(r.equivalentOldPrice, r.targetPrice)
    }

    // ComparisonCalculator
    @Test
    fun comparison_ranks() {
        val items = listOf(
            ComparisonItem("A", PackageInput(BigDecimal("100"), BigDecimal("1000"), UnitType.MASS, MassUnit.G)),
            ComparisonItem("B", PackageInput(BigDecimal("90"), BigDecimal("1000"), UnitType.MASS, MassUnit.G))
        )
        val res = ComparisonCalculator.compare(items)
        assertEquals("B", res.first().name)
        assertEquals(0, res.first().diffPercent.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun comparison_diffPercent() {
        val items = listOf(
            ComparisonItem("A", PackageInput(BigDecimal("100"), BigDecimal("1000"), UnitType.MASS, MassUnit.G)),
            ComparisonItem("B", PackageInput(BigDecimal("110"), BigDecimal("1000"), UnitType.MASS, MassUnit.G))
        )
        val res = ComparisonCalculator.compare(items)
        val b = res.find { it.name == "B" }!!
        assertEquals(0, b.diffPercent.compareTo(BigDecimal("10.000000")))
    }

    @Test
    fun comparison_richResult() {
        val items = listOf(
            ComparisonItem("Паста", PackageInput(BigDecimal("61"), BigDecimal("450"), UnitType.MASS, MassUnit.G), "Макфа", "перья"),
            ComparisonItem("Паста", PackageInput(BigDecimal("63"), BigDecimal("450"), UnitType.MASS, MassUnit.G), "Макфа", "спагетти")
        )
        val res = ComparisonCalculator.compare(items)
        assertTrue(res[0].isCheapest)
        assertTrue(!res[1].isCheapest)
        assertEquals("Макфа", res[0].brand)
        assertEquals("перья", res[0].spec)
        assertEquals(UnitType.MASS, res[0].unit)
        assertEquals("135.555555556000", res[0].wholeUnitPrice.toPlainString())
        assertEquals("4.44", res[1].absoluteDiff.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
        assertTrue(res[1].diffPercent.toDouble() > 3.27 && res[1].diffPercent.toDouble() < 3.29)
    }

    @Test
    fun comparison_invalidCountThrows() {
        val items = listOf(ComparisonItem("A", PackageInput(BigDecimal("100"), BigDecimal("1000"), UnitType.MASS, MassUnit.G)))
        try { ComparisonCalculator.compare(items); fail("expected") } catch (e: IllegalArgumentException) { }
    }

    @Test
    fun comparison_incompatibleThrows() {
        val items = listOf(
            ComparisonItem("A", PackageInput(BigDecimal("100"), BigDecimal("1000"), UnitType.MASS, MassUnit.G)),
            ComparisonItem("B", PackageInput(BigDecimal("90"), BigDecimal("1000"), UnitType.VOLUME, volumeUnit = VolumeUnit.ML))
        )
        try { ComparisonCalculator.compare(items); fail("expected") } catch (e: IllegalArgumentException) { }
    }

    @Test
    fun comparison_fourItems() {
        val items = (1..4).map { i -> ComparisonItem("T$i", PackageInput(BigDecimal(100 + i), BigDecimal("1000"), UnitType.MASS, MassUnit.G)) }
        val res = ComparisonCalculator.compare(items)
        assertEquals(4, res.size)
    }
}
