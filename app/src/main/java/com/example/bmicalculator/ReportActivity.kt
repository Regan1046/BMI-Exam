package com.example.bmicalculator

import android.app.AlertDialog
import android.os.Build
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.view.setPadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class ReportActivity : ReportScreen() {
    private lateinit var viewModel: ReportViewModel
    override fun connect(data: BMICalculator.BodyData) {
        viewModel = ViewModelProvider(this, modelFactory { ReportViewModel(data) })[ReportViewModel::class.java]
        // 用当前页面资源的语言决定回复语言，让界面文案和 DeepSeek 回复对应。
        val language = if (resources.configuration.locales[0].language == "zh") AdviceLang.ZH else AdviceLang.EN
        viewModel.selectLanguage(language)
        renderReport(viewModel.report)
        renderAdvice(viewModel.advice.value)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.advice.collect { renderAdvice(it) } }
                launch { viewModel.effects.collect { handleEffect(it) } }
            }
        }
    }
    override fun chartRequested() = viewModel.showChart()
    override fun copyRequested() = viewModel.copy()
    override fun editRequested() = viewModel.edit()

    override fun adviceRequested() {
        // 有会话密钥就直接生成。首次使用时，等用户确认对话框后再发请求。
        if (!viewModel.needsKey) {
            viewModel.generate()
            return
        }
        val input = EditText(this).apply {
            hint = getString(R.string.advice_key)
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSaveEnabled = false
            if (Build.VERSION.SDK_INT >= 26) importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }
        val container = FrameLayout(this).apply {
            setPadding((24 * resources.displayMetrics.density).toInt())
            addView(input)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.advice_key_title)
            .setMessage(R.string.advice_key_note)
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.advice_generate) { _, _ ->
                viewModel.generate(input.text.toString())
            }
            .show()
        val confirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        confirm.isEnabled = false
        input.doAfterTextChanged { confirm.isEnabled = !it.isNullOrBlank() }
    }
}
