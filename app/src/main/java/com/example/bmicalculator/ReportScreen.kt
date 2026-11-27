package com.example.bmicalculator

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

/** Rendering and platform dialogs only; report computation and actions are delegated. */
abstract class ReportScreen : LocalizedActivity() {
    private var chartAvailable = false
    protected abstract fun connect(data: BMICalculator.BodyData)
    protected abstract fun chartRequested()
    protected abstract fun editRequested()
    protected abstract fun copyRequested()
    protected abstract fun adviceRequested()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gender = BMICalculator.Gender.entries.firstOrNull { it.name == intent.getStringExtra("gender") }
        val data = gender?.let { BMICalculator.BodyData(intent.getDoubleExtra("weight", Double.NaN),
            intent.getDoubleExtra("height", Double.NaN), intent.getIntExtra("age", 0), it) }
        if (data == null || data.height !in 30.0..250.0 || data.weight !in 1.0..500.0 || data.age !in 1..120) {
            finish(); return
        }
        setContentView(R.layout.activity_report)
        setupChrome(report = true)
        findViewById<View>(R.id.editButton).setOnClickListener { editRequested() }
        findViewById<View>(R.id.chartButton).setOnClickListener { chartRequested() }
        findViewById<View>(R.id.adviceButton).setOnClickListener { adviceRequested() }
        registerForContextMenu(findViewById(R.id.resultCard))
        registerForContextMenu(findViewById(R.id.reportContextHint))
        connect(data)
    }

    protected fun renderAdvice(state: AdviceState) {
        // 根据同一份状态更新提示、进度条和按钮，让加载与重试状态保持一致。
        val text = findViewById<TextView>(R.id.adviceStatus)
        val button = findViewById<Button>(R.id.adviceButton)
        val loading = state == AdviceState.Loading
        findViewById<View>(R.id.adviceProgress).visibility = if (loading) View.VISIBLE else View.GONE
        button.visibility = if (state is AdviceState.Ready) View.GONE else View.VISIBLE
        button.isEnabled = !loading
        button.setText(if (state is AdviceState.Failed) R.string.advice_retry else R.string.advice_generate)
        when (state) {
            AdviceState.Idle -> text.setText(R.string.advice_share)
            AdviceState.Loading -> text.setText(R.string.advice_loading)
            is AdviceState.Ready -> text.text = state.text
            is AdviceState.Failed -> text.setText(when (state.error) {
                AdviceError.KEY -> R.string.advice_bad_key
                AdviceError.LIMIT -> R.string.advice_limit
                AdviceError.NETWORK -> R.string.advice_network
                AdviceError.RESPONSE -> R.string.advice_error
            })
        }
    }

    fun renderReport(report: ReportState) {
        val (data, bmi, assessment) = report
        val category = assessment.category
        val hasChart = assessment.bands.isNotEmpty()
        if (chartAvailable != hasChart) {
            chartAvailable = hasChart
            invalidateOptionsMenu()
        }
        findViewById<TextView>(R.id.bmiValue).text = getString(R.string.bmi_number, bmi)
        findViewById<TextView>(R.id.reportGroup).setText(if (data.isAdult) R.string.adult_report else R.string.child_report)
        findViewById<TextView>(R.id.profileSummary).text = getString(R.string.profile_summary, data.age,
            getString(if (data.gender == BMICalculator.Gender.MALE) R.string.male else R.string.female))
        findViewById<TextView>(R.id.measurementSummary).text = getString(R.string.measurement_summary, data.height, data.weight)
        findViewById<TextView>(R.id.categoryText).setText(category.label)
        findViewById<TextView>(R.id.adviceText).setText(when (category) {
            BmiReference.Category.SEVERELY_UNDERWEIGHT, BmiReference.Category.SEVERELY_OVERWEIGHT -> R.string.advice_severe_child
            BmiReference.Category.UNDERWEIGHT, BmiReference.Category.OVERWEIGHT -> R.string.advice_outside
            BmiReference.Category.OBESE -> R.string.advice_obese
            BmiReference.Category.NORMAL, BmiReference.Category.ACCEPTABLE -> R.string.advice_normal
            BmiReference.Category.OUTSIDE_AGE_RANGE -> R.string.advice_unsupported
        })
        val (background, foreground) = when {
            category.needsWarning -> Color.rgb(255, 233, 231) to Color.rgb(148, 33, 47)
            category == BmiReference.Category.UNDERWEIGHT || category == BmiReference.Category.OVERWEIGHT ->
                Color.rgb(255, 241, 215) to Color.rgb(122, 74, 17)
            category == BmiReference.Category.OUTSIDE_AGE_RANGE -> Color.rgb(230, 235, 239) to Color.rgb(60, 78, 88)
            else -> Color.rgb(227, 242, 233) to Color.rgb(21, 60, 59)
        }
        findViewById<View>(R.id.resultCard).background = GradientDrawable().apply {
            cornerRadius = 24 * resources.displayMetrics.density
            setColor(background)
            if (category.needsWarning) setStroke((2 * resources.displayMetrics.density).toInt(), foreground)
        }
        listOf(R.id.bmiValue, R.id.categoryText, R.id.adviceText, R.id.reportGroup).forEach {
            findViewById<TextView>(it).setTextColor(foreground)
        }
        findViewById<TextView>(R.id.warningBadge).apply {
            visibility = if (category.needsWarning) View.VISIBLE else View.GONE
            setTextColor(foreground)
        }
        findViewById<TextView>(R.id.referenceText).text = when {
            data.isAdult -> getString(R.string.adult_reference)
            data.age < 6 -> getString(R.string.unsupported_reference)
            else -> getString(R.string.child_reference, data.age, childGender(data), assessment.chartBmi)
        }
        findViewById<View>(R.id.chartButton).visibility = if (assessment.bands.isEmpty()) View.GONE else View.VISIBLE
    }

    fun handleEffect(effect: ReportEffect) {
        when (effect) {
            ReportEffect.Edit -> finish()
            is ReportEffect.ShowChart -> showChart(effect.report)
            is ReportEffect.Copy -> {
                val report = effect.report
                val text = getString(R.string.copy_report, getString(R.string.bmi_number, report.bmi),
                    getString(report.assessment.category.label), report.data.age,
                    getString(if (report.data.gender == BMICalculator.Gender.MALE) R.string.male else R.string.female),
                    report.data.height, report.data.weight)
                getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(getString(R.string.report_title), text))
                Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun childGender(data: BMICalculator.BodyData) = getString(
        if (data.gender == BMICalculator.Gender.MALE) R.string.boys else R.string.girls)

    private fun showChart(report: ReportState) {
        if (report.assessment.bands.isEmpty()) return
        val title = if (report.data.isAdult) getString(R.string.adult_chart_title)
            else getString(R.string.child_chart_title, report.data.age, childGender(report.data))
        val rows = report.assessment.bands.joinToString("\n\n") { band ->
            "${if (band.category == report.assessment.category) "● " else ""}${getString(band.category.label)}: ${band.range}"
        }
        AlertDialog.Builder(this).setTitle(title).setMessage(rows + "\n\n" +
            getString(if (report.data.isAdult) R.string.adult_chart_note else R.string.child_chart_note))
            .setPositiveButton(R.string.got_it, null).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.report_options, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home, R.id.menuEdit -> { editRequested(); true }
        R.id.menuChart -> { chartRequested(); true }
        R.id.menuHistory -> { startActivity(Intent(this, HistoryActivity::class.java)); true }
        else -> super.onOptionsItemSelected(item)
    }
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menuChart)?.isVisible = chartAvailable
        return true
    }
    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu.setHeaderTitle(R.string.report_title)
        if (menu.findItem(R.id.contextCopy) == null) menuInflater.inflate(R.menu.report_context, menu)
        menu.findItem(R.id.contextChart).isVisible = chartAvailable
    }
    override fun onContextItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.contextCopy -> { copyRequested(); true }
        R.id.contextEdit -> { editRequested(); true }
        R.id.contextChart -> { chartRequested(); true }
        else -> super.onContextItemSelected(item)
    }
}

val BmiReference.Category.label: Int get() = when (this) {
    BmiReference.Category.SEVERELY_UNDERWEIGHT -> R.string.severely_underweight
    BmiReference.Category.UNDERWEIGHT -> R.string.underweight
    BmiReference.Category.NORMAL -> R.string.normal_range
    BmiReference.Category.ACCEPTABLE -> R.string.acceptable_weight
    BmiReference.Category.OVERWEIGHT -> R.string.overweight
    BmiReference.Category.OBESE -> R.string.obese
    BmiReference.Category.SEVERELY_OVERWEIGHT -> R.string.severely_overweight
    BmiReference.Category.OUTSIDE_AGE_RANGE -> R.string.outside_age_range
}
