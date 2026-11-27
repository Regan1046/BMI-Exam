package com.example.bmicalculator

import android.content.Context

/** Uses only the application context, so a retained ViewModel cannot retain an Activity. */
class SharedProfileRepository(context: Context) : ProfileRepository {
    // 保留原来的文件名和键，升级后也能恢复旧版保存的身高、体重、年龄和性别。
    private val prefs = context.applicationContext.getSharedPreferences("body_data", Context.MODE_PRIVATE)

    // 尚未输入的字段恢复为空字符串，性别按枚举名称恢复。
    override fun load() = FormData(
        prefs.getString("height", "").orEmpty(), prefs.getString("weight", "").orEmpty(),
        prefs.getString("age", "").orEmpty(),
        BMICalculator.Gender.entries.firstOrNull { it.name == prefs.getString("gender", "") }
    )

    override fun save(form: FormData) {
        // apply() 立即更新内存中的偏好，再异步写盘，适合随表单编辑保存草稿。
        prefs.edit().putString("height", form.height).putString("weight", form.weight)
            .putString("age", form.age).putString("gender", form.gender?.name).apply()
    }
    override fun clear() { prefs.edit().clear().apply() }
}
