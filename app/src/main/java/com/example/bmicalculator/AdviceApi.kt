package com.example.bmicalculator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

enum class AdviceLang(val instruction: String) {
    EN("Write the entire response in English."),
    ZH("请完全使用简体中文回答。")
}

class AdviceHttp(val status: Int) : IOException()

class AdviceApi(
    private val connect: () -> HttpsURLConnection = {
        URL("https://api.deepseek.com/chat/completions").openConnection() as HttpsURLConnection
    }
) {
    // HTTPS 读写是阻塞操作，放到 IO 调度器上，避免在等待回复时卡住界面。
    suspend fun fetch(report: ReportState, language: AdviceLang, key: String): String =
        withContext(Dispatchers.IO) {
            val connection = connect()
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 10_000
                connection.readTimeout = 30_000
                connection.instanceFollowRedirects = false
                connection.doOutput = true
                connection.setRequestProperty("Authorization", "Bearer $key")
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                val payload = request(report, language).toString().toByteArray(Charsets.UTF_8)
                connection.setFixedLengthStreamingMode(payload.size)
                connection.outputStream.use { it.write(payload) }

                val status = connection.responseCode
                if (status !in 200..299) throw AdviceHttp(status)
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                // 非流式回复从 choices[0].message.content 取正文。只有完整结束且有内容才算成功。
                val choice = JSONObject(body).getJSONArray("choices").getJSONObject(0)
                val text = (choice.getJSONObject("message").opt("content") as? String)?.trim()
                if (choice.optString("finish_reason") != "stop" || text.isNullOrEmpty()) {
                    throw JSONException("Missing or incomplete advice")
                }
                text
            } finally {
                connection.disconnect()
            }
        }

    private fun request(report: ReportState, language: AdviceLang): JSONObject {
        // user 消息只传 BMI、年龄、性别。回复语言和建议范围写在 system 消息里。
        val profile = JSONObject()
            .put("bmi", report.bmi)
            .put("age", report.data.age)
            .put("gender", report.data.gender.name.lowercase())
        val prompt = """
            ${language.instruction}
            Give three short, practical wellness suggestions appropriate to the provided age and BMI.
            Use plain text and a supportive tone. Stay within general guidance about everyday habits.
            BMI alone cannot establish health or a diagnosis. Do not classify BMI, diagnose, prescribe,
            or give calorie or weight targets. For ages 18 and under, focus on healthy growth and
            family habits. Avoid weight-loss advice and suggest consulting a pediatric professional
            if there are concerns about growth.
        """.trimIndent()
        return JSONObject()
            .put("model", "deepseek-flash")
            .put("thinking", JSONObject().put("type", "disabled"))
            .put("max_tokens", 600)
            .put("stream", false)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", prompt))
                .put(JSONObject().put("role", "user").put("content", profile.toString())))
    }
}
