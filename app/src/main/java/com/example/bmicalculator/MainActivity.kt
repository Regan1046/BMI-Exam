package com.example.bmicalculator

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class MainActivity : MainScreen() {
    private lateinit var viewModel: MainViewModel
    override fun connect(initial: FormState) {
        // Factory 提供仓库和初始表单。ViewModelProvider 在旋转后复用已有实例。
        val appContext = applicationContext
        viewModel = ViewModelProvider(this, modelFactory {
            MainViewModel(repository, initial) { bmi, timestamp ->
                BmiDb(appContext).use { it.save(bmi, timestamp) }
            }
        })[MainViewModel::class.java]
        renderForm(viewModel.state.value)
        lifecycleScope.launch {
            // 页面进入 STARTED 时开始收集，离开时停止。两个流需要并行收集。
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { renderForm(it) } }
                launch { viewModel.effects.collect { handleEffect(it) } }
            }
        }
    }
    override fun formChanged(form: FormData) = viewModel.updateForm(form)
    override fun calculate() = viewModel.calculate()
    override fun reset() = viewModel.clear()
    override fun snapshot() = viewModel.state.value
}
