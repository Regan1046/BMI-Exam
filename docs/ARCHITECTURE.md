# MVVM 结构

- `MainVm.kt` 接收表单操作，校验并保存草稿，通过 StateFlow 提供输入页状态。有效提交后，在后台保存当天的 BMI。
- `ReportVm.kt` 提供本地 BMI 报告，通过 StateFlow 提供 AI 建议的加载、成功和错误状态。旋转时保留当前建议，同一份报告切换语言时会重新请求建议。
- `AdviceApi.kt` 在后台通过 HTTPS 调用 DeepSeek Chat Completions，发送 BMI、年龄、性别和指定目标语言的提示。
- `MainActivity.kt` 和 `ReportActivity.kt` 将用户操作交给 ViewModel，根据生命周期收集状态。`MainScreen.kt` 和 `ReportScreen.kt` 渲染控件并处理 Android 菜单、对话框和导航。
- `BMICalculator.kt`、`BmiReference.kt` 和 `BmiModels.kt` 保存计算、校验和参考表逻辑。`ProfileRepo.kt` 用 SharedPreferences 持久保存表单草稿。
- `BmiDb.kt` 用 SQLite 保存历史。日期是主键，时间戳决定当天保留哪次结果。`HistoryActivity.kt` 加载记录，`TrendView.kt` 绘制折线。
