package com.example.bmicalculator

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection

@RunWith(AndroidJUnit4::class)
class AdviceTest {
    @Test fun sendsOnlyReportInputsAndRequestsTheSelectedLanguage() = runBlocking {
        val report = BmiUseCase.report(BMICalculator.BodyData(70.0, 150.0, 12, BMICalculator.Gender.FEMALE))
        val cases = listOf(
            Triple(AdviceLang.ZH, "请完全使用简体中文回答。", "和家人一起保持规律作息。"),
            Triple(AdviceLang.EN, "Write the entire response in English.", "Keep a regular sleep routine.")
        )
        for ((language, instruction, reply) in cases) {
            val connection = ChatStub(reply)
            assertEquals(reply, AdviceApi { connection }.fetch(report, language, "test-key"))
            val request = JSONObject(connection.body.toString(Charsets.UTF_8.name()))
            val messages = request.getJSONArray("messages")
            assertTrue(messages.getJSONObject(0).getString("content").contains(instruction))
            val inputs = JSONObject(messages.getJSONObject(1).getString("content"))
            assertEquals(3, inputs.length())
            assertEquals(report.bmi, inputs.getDouble("bmi"), 0.0)
            assertEquals(12, inputs.getInt("age"))
            assertEquals("female", inputs.getString("gender"))
            assertEquals("POST", connection.requestMethod)
            assertEquals("Bearer test-key", connection.getRequestProperty("Authorization"))
            assertTrue(connection.closed)
        }
    }

    @Test fun retriesAndReloadsAdviceAfterChangingLanguage() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val data = BMICalculator.BodyData(65.0, 170.0, 25, BMICalculator.Gender.FEMALE)
        var attempts = 0
        val model = ReportViewModel(data) { _, language, key ->
            assertEquals("test-key", key)
            when (++attempts) {
                1 -> throw AdviceHttp(429)
                2 -> {
                    assertEquals(AdviceLang.EN, language)
                    "Keep a regular sleep routine."
                }
                3 -> {
                    assertEquals(AdviceLang.ZH, language)
                    "保持规律作息。"
                }
                else -> throw AdviceHttp(401)
            }
        }
        suspend fun awaitAdvice() = withTimeout(5_000) {
            model.advice.first { it is AdviceState.Ready || it is AdviceState.Failed }
        }

        instrumentation.runOnMainSync { model.generate("test-key") }
        assertEquals(AdviceState.Failed(AdviceError.LIMIT), awaitAdvice())
        instrumentation.runOnMainSync { model.generate() }
        assertEquals(AdviceState.Ready("Keep a regular sleep routine."), awaitAdvice())
        instrumentation.runOnMainSync { model.selectLanguage(AdviceLang.ZH) }
        assertEquals(AdviceState.Ready("保持规律作息。"), awaitAdvice())
        instrumentation.runOnMainSync { model.selectLanguage(AdviceLang.EN) }
        assertEquals(AdviceState.Failed(AdviceError.KEY), awaitAdvice())
        assertTrue(model.needsKey)
    }

    private class ChatStub(private val reply: String) :
        HttpsURLConnection(URL("https://api.deepseek.com/chat/completions")) {
        val body = ByteArrayOutputStream()
        var closed = false
        override fun getOutputStream() = body
        override fun getResponseCode() = HTTP_OK
        override fun getInputStream() =
            """{"choices":[{"finish_reason":"stop","message":{"content":${JSONObject.quote(reply)}}}]}"""
                .byteInputStream()
        override fun disconnect() { closed = true }
        override fun connect() = Unit
        override fun usingProxy() = false
        override fun getCipherSuite() = ""
        override fun getLocalCertificates(): Array<Certificate>? = null
        override fun getServerCertificates() = emptyArray<Certificate>()
    }
}
