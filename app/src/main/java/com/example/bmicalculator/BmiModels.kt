package com.example.bmicalculator

/** UI-independent inputs, validation, report model and repository contract. */
data class FormData(
    val height: String = "", val weight: String = "", val age: String = "",
    val gender: BMICalculator.Gender? = null
)

data class FormErrors(val height: Boolean, val weight: Boolean, val age: Boolean, val gender: Boolean) {
    val any: Boolean get() = height || weight || age || gender
}

data class FormState(
    val form: FormData = FormData(),
    val submitted: Boolean = false,
    val saving: Boolean = false
)

data class ReportState(
    val data: BMICalculator.BodyData, val bmi: Double, val assessment: BmiReference.Assessment
)

sealed interface MainEffect {
    data class OpenReport(val data: BMICalculator.BodyData) : MainEffect
    data object HistoryWriteFailed : MainEffect
}

sealed interface ReportEffect {
    data class ShowChart(val report: ReportState) : ReportEffect
    data class Copy(val report: ReportState) : ReportEffect
    data object Edit : ReportEffect
}

interface ProfileRepository {
    fun load(): FormData
    fun save(form: FormData)
    fun clear()
}

object BmiUseCase {
    // 表单保留原始字符串供编辑，校验通过后再转换为有明确单位的数值。
    fun errors(form: FormData) = FormErrors(
        BMICalculator.parseDecimal(form.height)?.let { it !in 30.0..250.0 } ?: true,
        BMICalculator.parseDecimal(form.weight)?.let { it !in 1.0..500.0 } ?: true,
        form.age.trim().toIntOrNull()?.let { it !in 1..120 } ?: true,
        form.gender == null
    )

    fun parse(form: FormData): BMICalculator.BodyData? {
        if (errors(form).any) return null
        return BMICalculator.BodyData(
            requireNotNull(BMICalculator.parseDecimal(form.weight)),
            requireNotNull(BMICalculator.parseDecimal(form.height)),
            form.age.trim().toInt(), requireNotNull(form.gender)
        )
    }

    fun report(data: BMICalculator.BodyData): ReportState {
        // 在生成报告的入口再检查范围，计算结果和参考表分类都在设备上完成。
        require(data.height in 30.0..250.0 && data.weight in 1.0..500.0 && data.age in 1..120)
        val bmi = BMICalculator.bmi(data)
        return ReportState(data, bmi, BmiReference.assess(bmi, data.age, data.gender))
    }

    val adultExample = FormData("170", "65", "25", BMICalculator.Gender.FEMALE)
    val childExample = FormData("150", "70", "12", BMICalculator.Gender.FEMALE)
}
