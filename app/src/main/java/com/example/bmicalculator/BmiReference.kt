package com.example.bmicalculator

import java.math.BigDecimal
import java.math.RoundingMode

/** Thresholds transcribed from the two supplied PDFs in docs/references.
 * Adults use continuous intervals; the child table is published to one decimal.
 * Child BMI is rounded HALF_UP once to that precision before matching a row.
 */
object BmiReference {
    enum class Category {
        SEVERELY_UNDERWEIGHT, UNDERWEIGHT, NORMAL, ACCEPTABLE,
        OVERWEIGHT, OBESE, SEVERELY_OVERWEIGHT, OUTSIDE_AGE_RANGE;

        val needsWarning: Boolean get() = this in setOf(SEVERELY_UNDERWEIGHT, SEVERELY_OVERWEIGHT, OBESE)
    }

    data class Band(val category: Category, val range: String)
    data class Assessment(val category: Category, val chartBmi: Double, val bands: List<Band>)

    // Four inclusive lower bounds (in BMI tenths): underweight, acceptable,
    // overweight, severely overweight. The row index is completed age minus 6.
    private val boys = listOf(
        listOf(129, 132, 189, 215), // 6
        listOf(131, 134, 199, 231), // 7
        listOf(133, 137, 210, 247), // 8
        listOf(136, 139, 219, 261), // 9
        listOf(139, 142, 228, 274), // 10
        listOf(142, 146, 237, 284), // 11
        listOf(145, 149, 244, 293), // 12
        listOf(148, 152, 251, 301), // 13
        listOf(151, 155, 256, 307), // 14
        listOf(154, 159, 262, 313), // 15
        listOf(157, 162, 266, 318), // 16
        listOf(160, 164, 271, 322), // 17
        listOf(162, 167, 275, 325)  // 18
    )
    private val girls = listOf(
        listOf(127, 129, 184, 206), // 6
        listOf(129, 132, 192, 219), // 7
        listOf(132, 135, 202, 232), // 8
        listOf(135, 138, 211, 245), // 9
        listOf(138, 142, 220, 257), // 10
        listOf(142, 145, 228, 267), // 11
        listOf(145, 149, 235, 276), // 12
        listOf(149, 153, 241, 284), // 13
        listOf(152, 156, 247, 290), // 14
        listOf(155, 159, 251, 295), // 15
        listOf(158, 162, 255, 298), // 16
        listOf(160, 164, 258, 301), // 17
        listOf(162, 166, 260, 304)  // 18
    )

    // 成人和儿童使用不同参考表，分类时也要考虑年龄与儿童表的性别分组。
    fun assess(bmi: Double, age: Int, gender: BMICalculator.Gender): Assessment {
        require(bmi.isFinite() && bmi > 0)
        require(age in 1..120)
        if (age > 18) {
            val category = when {
                bmi < 18.5 -> Category.UNDERWEIGHT
                bmi < 23.0 -> Category.NORMAL
                bmi < 25.0 -> Category.OVERWEIGHT
                else -> Category.OBESE
            }
            return Assessment(category, bmi, listOf(
                Band(Category.UNDERWEIGHT, "< 18.5"),
                Band(Category.NORMAL, "18.5 ≤ BMI < 23"),
                Band(Category.OVERWEIGHT, "23 ≤ BMI < 25"),
                Band(Category.OBESE, "≥ 25")
            ))
        }
        if (age < 6) return Assessment(Category.OUTSIDE_AGE_RANGE, bmi, emptyList())

        val row = (if (gender == BMICalculator.Gender.MALE) boys else girls)[age - 6]
        val tenths = BigDecimal.valueOf(bmi).setScale(1, RoundingMode.HALF_UP).movePointRight(1)
        val categories = listOf(Category.SEVERELY_UNDERWEIGHT, Category.UNDERWEIGHT,
            Category.ACCEPTABLE, Category.OVERWEIGHT, Category.SEVERELY_OVERWEIGHT)
        val index = row.count { tenths >= BigDecimal.valueOf(it.toLong()) }
        fun value(number: Int) = BigDecimal.valueOf(number.toLong(), 1).toPlainString()
        val bands = categories.mapIndexed { i, category ->
            Band(category, when (i) {
                0 -> "≤ ${value(row[0] - 1)}"
                4 -> "≥ ${value(row[3])}"
                else -> "${value(row[i - 1])} – ${value(row[i] - 1)}"
            })
        }
        return Assessment(categories[index], tenths.movePointLeft(1).toDouble(), bands)
    }
}
