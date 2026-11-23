package com.team.itexperts.shrinkflation.data

import androidx.room.TypeConverter
import com.team.itexperts.shrinkflation.model.Currency
import com.team.itexperts.shrinkflation.model.UnitType

class Converters {
    @TypeConverter
    fun fromCurrency(c: Currency): String = c.name
    @TypeConverter
    fun toCurrency(s: String): Currency = Currency.valueOf(s)

    @TypeConverter
    fun fromUnitType(u: UnitType): String = u.name
    @TypeConverter
    fun toUnitType(s: String): UnitType = UnitType.valueOf(s)
}
