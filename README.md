# BMI 计算器

本项目采用 MVVM。输入身高、体重、年龄和性别后，应用在设备上计算 BMI，并显示适用的参考范围。支持简体中文和英文、横竖屏以及草稿恢复。

## 查看 BMI 历史

- 退出后重新打开应用，会从 SharedPreferences 恢复上次输入的身高、体重、年龄和性别。
- 每次成功提交计算后，应用将 BMI 按设备当地日期写入 SQLite。同一天多次计算只保留最后一次的结果。
- 从主页的 **BMI 历史** 按钮或报告页菜单进入历史页。可以查看日历日期上的 BMI 折线及每天的具体数值。
- 历史保存在当前 Android 用户的应用存储中。清空表单只会清除保存的输入。

## 在报告中生成 AI 健康建议

1. 在 [DeepSeek 开放平台](https://platform.deepseek.com/api_keys) 创建自己的 API 密钥。
2. 生成 BMI 报告，点按 **生成建议**（英文界面为 **Generate suggestions**）。首次使用时在应用内粘贴 API 密钥。
3. DeepSeek 根据 BMI、年龄和性别生成当前应用语言的日常健康建议。已生成建议时切换语言，应用会用新语言重新请求。

请求只发送 BMI、年龄和性别，以及生成建议所需的提示。密钥仅保存在当前应用进程的内存中。API 调用计入密钥所属的 DeepSeek 账户。BMI 计算、参考范围与本地草稿可离线使用。[DeepSeek API 文档](https://api-docs.deepseek.com/)

## 构建与验证

在 Android Studio 中将 Gradle JDK 设为 17，并安装 Android SDK 36。项目使用 Kotlin 2.2.10、JVM 17、AGP 8.13.0 和 Gradle 8.13，最低支持 Android 7.0（API 24）。应用 ID 与原 MVVM 版相同。

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

调试 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。模拟器测试使用本地模拟响应验证中英文请求，无需 API 密钥。

- [MVVM 结构](docs/ARCHITECTURE.md)
- [考试复习与 GitHub 上传参考](docs/EXAM.md)
- [BMI 参考表规则](docs/CHART_RULES.md)
