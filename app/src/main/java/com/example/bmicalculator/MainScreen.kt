package com.example.bmicalculator

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast

/** Renders the form and forwards user actions to MainActivity. */
abstract class MainScreen : LocalizedActivity() {
    protected lateinit var repository: ProfileRepository
    private lateinit var height: EditText
    private lateinit var weight: EditText
    private lateinit var age: EditText
    private lateinit var gender: RadioGroup
    private var rendering = false

    protected abstract fun connect(initial: FormState)
    protected abstract fun formChanged(form: FormData)
    protected abstract fun calculate()
    protected abstract fun reset()
    protected abstract fun snapshot(): FormState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupChrome()
        repository = SharedProfileRepository(applicationContext)
        height = findViewById(R.id.heightInput)
        weight = findViewById(R.id.weightInput)
        age = findViewById(R.id.ageInput)
        gender = findViewById(R.id.genderInput)
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!rendering) formChanged(readForm())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        }
        listOf(height, weight, age).forEach { it.addTextChangedListener(watcher) }
        gender.setOnCheckedChangeListener { _, _ -> if (!rendering) formChanged(readForm()) }
        findViewById<View>(R.id.calculateButton).setOnClickListener { calculate() }
        findViewById<View>(R.id.historyButton).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        findViewById<View>(R.id.aboutButton).setOnClickListener { showAbout() }
        registerForContextMenu(findViewById(R.id.measurementsCard))
        registerForContextMenu(findViewById(R.id.contextHint))
        // 新启动没有 Bundle，从 SharedPreferences 读取上次的输入并填回表单。
        val form = if (savedInstanceState?.containsKey("formHeight") == true) FormData(
            savedInstanceState.getString("formHeight").orEmpty(), savedInstanceState.getString("formWeight").orEmpty(),
            savedInstanceState.getString("formAge").orEmpty(), BMICalculator.Gender.entries.firstOrNull {
                it.name == savedInstanceState.getString("formGender")
            }
        ) else repository.load()
        connect(FormState(form, savedInstanceState?.getBoolean("submitted") ?: false))
    }

    // State restoration belongs to the state holder; Android must not replay stale widget values over it.
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        renderForm(snapshot())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val state = snapshot()
        outState.putString("formHeight", state.form.height)
        outState.putString("formWeight", state.form.weight)
        outState.putString("formAge", state.form.age)
        outState.putString("formGender", state.form.gender?.name)
        outState.putBoolean("submitted", state.submitted)
        super.onSaveInstanceState(outState)
    }

    private fun readForm() = FormData(height.text.toString(), weight.text.toString(), age.text.toString(), when (gender.checkedRadioButtonId) {
        R.id.maleOption -> BMICalculator.Gender.MALE
        R.id.femaleOption -> BMICalculator.Gender.FEMALE
        else -> null
    })

    fun renderForm(state: FormState) {
        rendering = true
        findViewById<View>(R.id.calculateButton).isEnabled = !state.saving
        fun EditText.update(text: String) { if (this.text.toString() != text) setText(text) }
        height.update(state.form.height); weight.update(state.form.weight); age.update(state.form.age)
        gender.check(when (state.form.gender) {
            BMICalculator.Gender.MALE -> R.id.maleOption
            BMICalculator.Gender.FEMALE -> R.id.femaleOption
            null -> -1
        })
        val errors = BmiUseCase.errors(state.form)
        height.error = if (state.submitted && errors.height) getString(R.string.height_error) else null
        weight.error = if (state.submitted && errors.weight) getString(R.string.weight_error) else null
        age.error = if (state.submitted && errors.age) getString(R.string.age_error) else null
        findViewById<View>(R.id.genderError).visibility = if (state.submitted && errors.gender) View.VISIBLE else View.GONE
        val years = state.form.age.trim().toIntOrNull()
        findViewById<TextView>(R.id.chartHint).setText(when {
            years == null || years !in 1..120 -> R.string.chart_hint
            years < 6 -> R.string.unsupported_hint
            years <= 18 -> R.string.child_hint
            else -> R.string.adult_hint
        })
        rendering = false
    }

    fun handleEffect(effect: MainEffect) {
        // ViewModel 决定何时打开报告，页面负责把校验后的数据放进 Intent。
        when (effect) {
            is MainEffect.OpenReport -> startActivity(Intent(this, ReportActivity::class.java)
                .putExtra("height", effect.data.height).putExtra("weight", effect.data.weight)
                .putExtra("age", effect.data.age).putExtra("gender", effect.data.gender.name))
            MainEffect.HistoryWriteFailed ->
                Toast.makeText(this, R.string.history_write_error, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_options, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menuClear -> { reset(); true }
        else -> super.onOptionsItemSelected(item)
    }
    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu.setHeaderTitle(R.string.context_measurements)
        // Android collects context-menu callbacks from the pressed view and its ancestors.
        if (menu.findItem(R.id.contextAdult) == null) menuInflater.inflate(R.menu.form_context, menu)
    }
    override fun onContextItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.contextAdult -> { formChanged(BmiUseCase.adultExample); true }
        R.id.contextChild -> { formChanged(BmiUseCase.childExample); true }
        R.id.contextClear -> { reset(); true }
        else -> super.onContextItemSelected(item)
    }
}
