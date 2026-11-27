package com.example.bmicalculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.IOException

// 用 sealed interface 列出建议区的所有状态，渲染时由 when 逐一处理。
sealed interface AdviceState {
    data object Idle : AdviceState
    data object Loading : AdviceState
    data class Ready(val text: String) : AdviceState
    data class Failed(val error: AdviceError) : AdviceState
}

enum class AdviceError { KEY, LIMIT, NETWORK, RESPONSE }

// Shared within this app process so the user can open another report without re-entering the key.
private object AdviceSession {
    var key: String? = null
}

class ReportViewModel(
    data: BMICalculator.BodyData,
    // 默认走真实 API，测试时可传入模拟函数来验证状态变化。
    private val fetch: suspend (ReportState, AdviceLang, String) -> String = AdviceApi()::fetch
) : ViewModel() {
    val report = BmiUseCase.report(data)
    private val mutableAdvice = MutableStateFlow<AdviceState>(AdviceState.Idle)
    val advice = mutableAdvice.asStateFlow()
    private val events = Channel<ReportEffect>(Channel.BUFFERED)
    val effects = events.receiveAsFlow()
    private var language = AdviceLang.EN
    private var request: Job? = null

    val needsKey: Boolean get() = AdviceSession.key == null

    fun selectLanguage(next: AdviceLang) {
        if (language == next) return
        language = next
        // 切换语言时取消旧请求。如果用户已开始生成，就用新语言重新请求。
        val reload = advice.value == AdviceState.Loading || advice.value is AdviceState.Ready
        request?.cancel()
        if (reload) {
            mutableAdvice.value = AdviceState.Idle
            generate()
        }
    }

    fun generate(newKey: String? = null) {
        if (advice.value == AdviceState.Loading) return
        newKey?.trim()?.let { key ->
            if (key.isEmpty() || key.any { it !in '!'..'~' }) {
                mutableAdvice.value = AdviceState.Failed(AdviceError.KEY)
                return
            }
            AdviceSession.key = key
        }
        val key = AdviceSession.key ?: return
        val chosenLanguage = language
        mutableAdvice.value = AdviceState.Loading
        // 请求随 ViewModel 生命周期管理。旋转会重新收集状态，退出报告后协程会取消。
        request = viewModelScope.launch {
            val result = try {
                AdviceState.Ready(fetch(report, chosenLanguage, key))
            } catch (error: AdviceHttp) {
                AdviceState.Failed(when (error.status) {
                    401, 403 -> AdviceError.KEY
                    429 -> AdviceError.LIMIT
                    else -> AdviceError.RESPONSE
                })
            } catch (_: IOException) {
                AdviceState.Failed(AdviceError.NETWORK)
            } catch (_: JSONException) {
                AdviceState.Failed(AdviceError.RESPONSE)
            }
            // 取消后的旧语言请求可能刚好返回，在写入状态前再次检查协程是否有效。
            ensureActive()
            if (result == AdviceState.Failed(AdviceError.KEY) && AdviceSession.key == key) {
                AdviceSession.key = null
            }
            mutableAdvice.value = result
        }
    }

    fun showChart() { events.trySend(ReportEffect.ShowChart(report)) }
    fun copy() { events.trySend(ReportEffect.Copy(report)) }
    fun edit() { events.trySend(ReportEffect.Edit) }
}
