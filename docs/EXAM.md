# BMI 项目考试参考

这个项目可以用来复习 Kotlin 表单校验、SharedPreferences、SQLite 和自定义折线图。下面的源码链接可以在 GitHub 上直接点开，也可以在 Android Studio 中按文件名搜索。

## 搭好考试开发环境

| 项目 | 版本 | 配置位置 |
| --- | --- | --- |
| Android Studio | Narwhal 3 Feature Drop 2025.1.3，或 Narwhal 4 Feature Drop 2025.1.4 | **Help → About** |
| Gradle JDK 与 JVM 目标 | 17 | IDE 的 **Gradle JDK** 和 [app/build.gradle.kts](../app/build.gradle.kts) |
| Kotlin / AGP | 2.2.10 / 8.13.0 | [gradle/libs.versions.toml](../gradle/libs.versions.toml) |
| Gradle | 8.13 | [gradle-wrapper.properties](../gradle/wrapper/gradle-wrapper.properties) |
| Android SDK | compileSdk 和 targetSdk 均为 36 | [app/build.gradle.kts](../app/build.gradle.kts) |

先用 **Help → About** 确认小版本。Android Studio 2025.1.1 的 AGP 上限为 8.11，2025.1.2 的上限为 8.12。[Android Studio 兼容表](https://developer.android.com/studio/releases#android_gradle_plugin_and_android_studio_compatibility)

1. 在 Android Studio 选择 **Open**，打开含有 `settings.gradle.kts` 的项目根目录。
2. 打开 **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**，将 **Gradle JDK** 设为已安装的 **JDK 17**。[Android Gradle JDK 设置](https://developer.android.com/build/jdks#gradle-jdk-configuration)
3. 用 **SDK Manager** 安装 **Android SDK Platform 36**，然后同步 Gradle。第一次同步需要下载 Gradle 和项目依赖。准备离线使用时，提前完成同步，并在断网后试一次构建。

也可以在项目根目录用 PowerShell 复查。若系统已设置 `JAVA_HOME` 指向 JDK 17，可直接跳到版本检查。

```powershell
# 把示例路径换成这台电脑上的 JDK 17 安装目录。
$env:JAVA_HOME = 'C:\Path\To\JDK17'

# 检查 Gradle 版本是 8.13，Launcher JVM 和 Daemon JVM 使用 JDK 17。
.\gradlew.bat --version

# 构建调试 APK，并执行 BMI 算法和表单校验的单元测试。
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest

# 联网构建成功后，可用 --offline 验证当前机器的依赖缓存。
.\gradlew.bat --offline :app:assembleDebug
```

`--version` 显示的 Kotlin 2.0.21 是 Gradle 内置的 Kotlin 版本。应用源码由版本目录指定的 Kotlin 2.2.10 插件编译。

APK 生成在 `app/build/outputs/apk/debug/app-debug.apk`。若 Android Studio 提示升级 AGP，先检查 IDE 小版本，按考试要求选定工具版本后再同步。

## 源码速查

| 想复习的写法 | 源码入口 |
| --- | --- |
| 将厘米换算为米、校验表单 | [BMICalculator.kt](../app/src/main/java/com/example/bmicalculator/BMICalculator.kt) · [BmiModels.kt](../app/src/main/java/com/example/bmicalculator/BmiModels.kt) |
| SharedPreferences 的读写和启动时自动填表 | [ProfileRepo.kt](../app/src/main/java/com/example/bmicalculator/ProfileRepo.kt) · [MainScreen.kt](../app/src/main/java/com/example/bmicalculator/MainScreen.kt) |
| StateFlow 驱动界面、Channel 传递一次性导航、把数据库写入放到后台 | [MainVm.kt](../app/src/main/java/com/example/bmicalculator/MainVm.kt) · [MainActivity.kt](../app/src/main/java/com/example/bmicalculator/MainActivity.kt) |
| SQLite 创建表、同日只保留最新 BMI、按日期排序查询 | [BmiDb.kt](../app/src/main/java/com/example/bmicalculator/BmiDb.kt) · [BmiDbTest.kt](../app/src/androidTest/java/com/example/bmicalculator/BmiDbTest.kt) |
| 加载历史、将日期和 BMI 映射到折线坐标 | [HistoryActivity.kt](../app/src/main/java/com/example/bmicalculator/HistoryActivity.kt) · [TrendView.kt](../app/src/main/java/com/example/bmicalculator/TrendView.kt) |
| 中英文资源、跟随应用语言发送 HTTPS 请求 | [LocaleActivity.kt](../app/src/main/java/com/example/bmicalculator/LocaleActivity.kt) · [ReportVm.kt](../app/src/main/java/com/example/bmicalculator/ReportVm.kt) · [AdviceApi.kt](../app/src/main/java/com/example/bmicalculator/AdviceApi.kt) |

先改一次身高、年龄和性别，退出应用后重开，核对自动填充。再只改体重，在同一天算两次，进 **BMI 历史** 确认当天只有一行，BMI 显示第二次的结果。多日测试在 [TrendFlowTest.kt](../app/src/androidTest/java/com/example/bmicalculator/TrendFlowTest.kt) 准备前几天的记录，再通过正常界面加入当天的结果。BMI 计算与本地存储都可离线练习。

## 上传到 GitHub

解压源码包。在 GitHub 新建空仓库，例如 `BMI-Exam`，可见性按课程要求选择。若没有公开要求，可以选 **Private**。创建时将 **Add a README**、**Add .gitignore** 和 **Choose a license** 留空，项目已有 README 和忽略规则。[GitHub 导入本地代码说明](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github)

在解压后的项目根目录打开 PowerShell。先准备提交并检查文件列表。

```powershell
# 将当前项目初始化为 Git 仓库，并使用 main 分支。
git init -b main

# 将源码、文档和 Gradle Wrapper 加入本次提交。
git add .

# 让 Linux 或 macOS 克隆后也能直接运行 ./gradlew。
git update-index --chmod=+x gradlew

# 检查待提交文件，确认包含 app/src 和 gradle/wrapper。
git status --short
```

`local.properties`、构建产物和 IDE 缓存应由 `.gitignore` 排除。API 密钥和签名密钥都留在本机。调试 APK 可从源码重新构建。

```powershell
# 保存这次提交。
git commit -m "Add annotated BMI exam reference"

# 把示例地址替换成 GitHub 新仓库的 HTTPS 地址。
git remote add origin 'https://github.com/YOUR-ACCOUNT/BMI-Exam.git'

# 推送后在 GitHub 仓库首页查看源码和本参考。
git push -u origin main
```

上传后，可从 GitHub 重新克隆一份到空文件夹，按上面的构建步骤打开和测试。考试时使用 GitHub 或本地副本，以课程允许的方式为准。
