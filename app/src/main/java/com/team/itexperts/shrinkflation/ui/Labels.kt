package com.team.itexperts.shrinkflation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.team.itexperts.shrinkflation.R
import com.team.itexperts.shrinkflation.model.MassUnit
import com.team.itexperts.shrinkflation.model.UnitType
import com.team.itexperts.shrinkflation.model.VolumeUnit

@Composable
fun unitTypeLabel(type: UnitType): String = when (type) {
    UnitType.MASS -> stringResource(R.string.unit_mass)
    UnitType.VOLUME -> stringResource(R.string.unit_volume)
    UnitType.COUNT -> stringResource(R.string.unit_count)
}

@Composable
fun massUnitLabel(unit: MassUnit): String = when (unit) {
    MassUnit.G -> stringResource(R.string.unit_g)
    MassUnit.KG -> stringResource(R.string.unit_kg)
}

@Composable
fun volumeUnitLabel(unit: VolumeUnit): String = when (unit) {
    VolumeUnit.ML -> stringResource(R.string.unit_ml)
    VolumeUnit.L -> stringResource(R.string.unit_l)
}

@Composable
fun wholeUnitLabelLocalized(type: UnitType): String = when (type) {
    UnitType.MASS -> stringResource(R.string.unit_kg)
    UnitType.VOLUME -> stringResource(R.string.unit_l)
    UnitType.COUNT -> stringResource(R.string.unit_pcs)
}

@Composable
fun comparisonItemLabel(name: String, brand: String, spec: String): String {
    val n = name.ifBlank { stringResource(R.string.field_product) }
    return when {
        brand.isNotBlank() && spec.isNotBlank() -> stringResource(R.string.comparison_label_full, n, brand, spec)
        brand.isNotBlank() -> stringResource(R.string.comparison_label_brand, n, brand, "")
        spec.isNotBlank() -> stringResource(R.string.comparison_label_spec, n, "", spec)
        else -> n
    }
}

@Composable
fun comparisonPriceText(value: String, symbol: String, unit: String): String =
    stringResource(R.string.comparison_price, value, symbol, unit)
