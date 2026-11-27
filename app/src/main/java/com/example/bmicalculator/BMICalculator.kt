package com.example.bmicalculator

import java.math.BigDecimal
import java.math.MathContext

/** Pure calculation and validation, independent of the Android UI. */
object BMICalculator {
    enum class Gender { MALE, FEMALE }
    data class BodyData(val weight: Double, val height: Double, val age: Int, val gender: Gender) {
        val isAdult: Boolean get() = age > 18
    }

    fun calculateBMI(w: Double, h: Double): Double {
        require(w.isFinite() && w > 0) { "Weight must be positive." }
        require(h.isFinite() && h > 0) { "Height must be positive." }
        // Decimal arithmetic keeps exact chart boundaries from drifting through
        // binary division (for example, a BMI of exactly 23 becoming 22.999...).
        return BigDecimal.valueOf(w).divide(BigDecimal.valueOf(h).pow(2), MathContext.DECIMAL128)
            .toDouble().also { require(it.isFinite() && it > 0) }
    }

    // 表单身高单位为厘米。代入 BMI = kg / m² 前，先把厘米换算为米。
    fun bmi(data: BodyData): Double = calculateBMI(data.weight,
        BigDecimal.valueOf(data.height).movePointLeft(2).toDouble())
    fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
        ?.takeIf { it.isFinite() }
}
