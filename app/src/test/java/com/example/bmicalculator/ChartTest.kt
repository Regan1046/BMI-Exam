package com.example.bmicalculator

import com.example.bmicalculator.BMICalculator.Gender.*
import com.example.bmicalculator.BmiReference.Category.*
import org.junit.Assert.*
import org.junit.Test

class BmiReferenceTest {
    @Test fun adultChartUsesAsianCutoffsWithoutRounding() {
        for (gender in BMICalculator.Gender.entries) {
            for ((bmi, expected) in listOf(18.499 to UNDERWEIGHT, 18.5 to NORMAL,
                22.99 to NORMAL, 23.0 to OVERWEIGHT, 24.999 to OVERWEIGHT, 25.0 to OBESE, 50.0 to OBESE)) {
                assertEquals("BMI $bmi", expected, BmiReference.assess(bmi, 19, gender).category)
            }
        }
    }

    @Test fun everyChildRowMatchesEveryPrintedEndpoint() {
        val rows = requireNotNull(javaClass.getResourceAsStream("/child-chart.csv"))
            .bufferedReader().use { it.readLines().drop(1) }
        assertEquals(26, rows.size)
        val expected = listOf(SEVERELY_UNDERWEIGHT, UNDERWEIGHT, UNDERWEIGHT, ACCEPTABLE,
            ACCEPTABLE, OVERWEIGHT, OVERWEIGHT, SEVERELY_OVERWEIGHT)
        for (line in rows) {
            val cells = line.split(',')
            val gender = BMICalculator.Gender.valueOf(cells[0])
            val age = cells[1].toInt()
            for ((index, number) in cells.drop(2).withIndex()) {
                assertEquals("$gender age $age BMI $number", expected[index],
                    BmiReference.assess(number.toDouble(), age, gender).category)
            }
        }
    }

    @Test fun childRoundingClosesTheGapsBetweenPrintedTenths() {
        assertEquals(SEVERELY_UNDERWEIGHT, BmiReference.assess(12.849, 6, MALE).category)
        assertEquals(UNDERWEIGHT, BmiReference.assess(12.85, 6, MALE).category)
        assertEquals(ACCEPTABLE, BmiReference.assess(18.849, 6, MALE).category)
        assertEquals(OVERWEIGHT, BmiReference.assess(18.85, 6, MALE).category)
        assertEquals(27.6, BmiReference.assess(27.55, 12, FEMALE).chartBmi, 0.0)
        assertEquals(SEVERELY_OVERWEIGHT, BmiReference.assess(27.55, 12, FEMALE).category)
    }

    @Test fun genderAndAgeChangeTheReference() {
        assertEquals(ACCEPTABLE, BmiReference.assess(24.0, 12, MALE).category)
        assertEquals(OVERWEIGHT, BmiReference.assess(24.0, 12, FEMALE).category)
        assertEquals(SEVERELY_OVERWEIGHT, BmiReference.assess(26.5, 9, MALE).category)
        assertEquals(ACCEPTABLE, BmiReference.assess(26.5, 18, MALE).category)
        assertEquals(OBESE, BmiReference.assess(26.5, 19, MALE).category)
    }

    @Test fun calculatedBmiKeepsExactCategoryBoundaries() {
        for ((weight, expected) in listOf(41.625 to NORMAL, 51.75 to OVERWEIGHT, 56.25 to OBESE)) {
            val data = BMICalculator.BodyData(weight, 150.0, 25, FEMALE)
            assertEquals(expected, BmiReference.assess(BMICalculator.bmi(data), data.age, data.gender).category)
        }
        val child = BMICalculator.BodyData(28.9125, 150.0, 6, MALE)
        assertEquals(UNDERWEIGHT, BmiReference.assess(BMICalculator.bmi(child), 6, MALE).category)
    }

    @Test fun agesBelowSixHaveNoInventedClassification() {
        for (age in 1..5) {
            val result = BmiReference.assess(20.0, age, FEMALE)
            assertEquals(OUTSIDE_AGE_RANGE, result.category)
            assertTrue(result.bands.isEmpty())
        }
        assertThrows(IllegalArgumentException::class.java) { BmiReference.assess(20.0, 0, FEMALE) }
        assertThrows(IllegalArgumentException::class.java) { BmiReference.assess(Double.NaN, 12, FEMALE) }
    }

    @Test fun severeCategoriesRequireVisibleWarnings() {
        assertTrue(BmiReference.assess(10.0, 12, FEMALE).category.needsWarning)
        assertTrue(BmiReference.assess(31.1, 12, FEMALE).category.needsWarning)
        assertTrue(BmiReference.assess(30.0, 25, FEMALE).category.needsWarning)
        assertFalse(BmiReference.assess(20.0, 12, FEMALE).category.needsWarning)
    }
}
