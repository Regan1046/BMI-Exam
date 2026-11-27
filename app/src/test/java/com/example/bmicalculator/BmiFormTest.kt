package com.example.bmicalculator

import org.junit.Assert.*
import org.junit.Test

class BmiFormTest {
    @Test fun invalidDraftsNeverBecomeReports() {
        for (form in listOf(FormData(), BmiUseCase.adultExample.copy(height = "NaN"),
            BmiUseCase.adultExample.copy(weight = "501"), BmiUseCase.adultExample.copy(age = "12.5"),
            BmiUseCase.adultExample.copy(gender = null))) {
            assertNull(BmiUseCase.parse(form))
        }
    }
    @Test fun decimalCommaAndTrimmedAgeProduceTheSameReport() {
        val form = FormData("170,0", " 65,0 ", " 25 ", BMICalculator.Gender.FEMALE)
        val report = BmiUseCase.report(requireNotNull(BmiUseCase.parse(form)))
        assertEquals(22.49134948, report.bmi, 0.000001)
        assertEquals(BmiReference.Category.NORMAL, report.assessment.category)
    }
}
