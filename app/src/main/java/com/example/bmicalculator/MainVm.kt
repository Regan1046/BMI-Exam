package com.example.bmicalculator

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** MVVM: exposes observable state and commands, with no reference to a View. */
class MainViewModel(
    private val repository: ProfileRepository,
    initial: FormState,
    private val saveBmi: (Double, Long) -> Unit
) : ViewModel() {
    // StateFlow 保留最新表单，页面重建后可以重新渲染。只有 ViewModel 可以修改它。
    private val mutableState = MutableStateFlow(initial)
    val state = mutableState.asStateFlow()
    // 打开报告属于一次性事件，用 Channel 传递，避免重新订阅状态时重复跳转。
    private val events = Channel<MainEffect>(Channel.BUFFERED)
    val effects = events.receiveAsFlow()

    fun updateForm(form: FormData) {
        repository.save(form)
        mutableState.value = state.value.copy(form = form)
    }

    fun calculate() {
        if (state.value.saving) return
        // 先标记已提交，让页面显示校验提示。解析成功才发送导航事件。
        mutableState.value = state.value.copy(submitted = true)
        val data = BmiUseCase.parse(state.value.form) ?: return
        val timestamp = System.currentTimeMillis()
        mutableState.value = state.value.copy(saving = true)
        viewModelScope.launch {
            // 提交时记录日期，后台写入成功后再打开报告，避免查看历史时记录还没入库。
            try {
                withContext(Dispatchers.IO) { saveBmi(BMICalculator.bmi(data), timestamp) }
            } catch (_: SQLiteException) {
                events.send(MainEffect.HistoryWriteFailed)
            } finally {
                mutableState.value = state.value.copy(saving = false)
            }
            events.send(MainEffect.OpenReport(data))
        }
    }

    fun clear() {
        repository.clear()
        mutableState.value = FormState(saving = state.value.saving)
    }
}
