package com.example.bmicalculator

import org.junit.Assert.*
import org.junit.Test

class BMICalculatorTest {
    @Test fun calculatesUsingMetres() {
        assertEquals(22.49134948, BMICalculator.calculateBMI(65.0, 1.70), 0.000001)
    }
    @Test fun convertsCentimetresOnlyOnce() {
        val data = BMICalculator.BodyData(65.0, 170.0, 25, BMICalculator.Gender.FEMALE)
        assertEquals(22.49134948, BMICalculator.bmi(data), 0.000001)
    }
    @Test fun ageEighteenUsesChildReference() {
        assertFalse(BMICalculator.BodyData(65.0, 170.0, 18, BMICalculator.Gender.MALE).isAdult)
        assertTrue(BMICalculator.BodyData(65.0, 170.0, 19, BMICalculator.Gender.MALE).isAdult)
    }
    @Test fun validatesFinitePositiveMeasurements() {
        for (invalid in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { BMICalculator.calculateBMI(invalid, 1.7) }
            assertThrows(IllegalArgumentException::class.java) { BMICalculator.calculateBMI(65.0, invalid) }
        }
    }
    @Test fun decimalInputRejectsNonNumbersAndAcceptsLocaleSeparator() {
        assertEquals(65.5, BMICalculator.parseDecimal("65,5")!!, 0.0)
        assertEquals(65.5, BMICalculator.parseDecimal(" 65.5 ")!!, 0.0)
        for (invalid in listOf("", "abc", "NaN", "Infinity", "1.2.3")) {
            assertNull(BMICalculator.parseDecimal(invalid))
        }
    }
}
