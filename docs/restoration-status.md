# 恢复状态（2026-10-21）

源码：根目录 `app/src/main/` 已整理 14 个 Kotlin 文件、Manifest、主题和原 PNG 图标。工程设置、Gradle wrapper、7 个 JUnit 测试和本地测试脚本已建立。

证据来源：`recovery/decompiled/sources/ink/qingji/app/`、包内 Kotlin/Gradle 元数据、解码 Manifest、REA 分析证据；通知控制流另核对 `recovery/apk-analysis/payment-simple.java`。

## 已完成的验证

- 测试先在恢复文件缺失时编译失败，日志：`recovery/apk-analysis/tests-red.log`。
- 恢复后，同一组纯 Kotlin/JUnit 测试全部通过（7 项），日志：`recovery/apk-analysis/tests-green.log`。
- Gradle 8.9 wrapper 生成成功，日志：`recovery/apk-analysis/wrapper-isolated.log`。
- 独立只读审查核对了数据库迁移、通知解析、自动入账、分类存储及分析算法，未发现高优先级还原回归。未以此替代构建或运行测试。

## Android 验证

本机验证命令（JAVA_HOME 指向任务下载的 JDK 21）：

```powershell
.\gradlew.bat -g E:\codex\qingji\.local-tools\gradle-home :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --no-daemon
```

结果：`BUILD SUCCESSFUL in 3m 11s`，53 个任务完成，进程退出码 0。

- Android 单元测试：7 项，0 失败、0 错误、0 跳过。报告：`app/build/test-results/testDebugUnitTest/TEST-ink.qingji.app.RecoveryBehaviorTest.xml`。
- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`，16,463,055 字节。
- APK SHA-256：`c72972025a43cc6891b49690d4989ef025d35e67366ebe19be3b9d2fe85f0dfe`。
- 构建产物元数据确认包名 `ink.qingji.app`、versionCode 6、versionName 0.3.2、minSdk 26。
- 原图标与恢复资源 SHA-256 一致：`ae28c1a2c07e2709eeffadc051fe67df4ac4bb2791f05fcaea28447d1c320ec0`。
- Lint：0 错误、2 警告。保留旧 Manifest 的 allowBackup 配置会触发 DataExtractionRules 提示；原彩色图标用于通知会触发 IconColors 提示。另有一条 Compose 装箱状态优化建议。未通过屏蔽规则隐藏检查。
- 编译有 ReceiptLong 图标 API 弃用提示；打包有原生库调试符号未剥离提示，均未阻止构建。

完整日志：`recovery/apk-analysis/android-build.log`；lint 报告：`app/build/reports/lint-results-debug.html`。

环境处理：机器原有 Gradle 缓存缺失元数据，因此使用独立缓存；任务目录已安装 Android 35 platform、Build Tools 34.0.0，构建自动补齐 Platform Tools。JDK、SDK 与下载缓存均不属于应用源码。

## 尚未验证

已在独立雷电模拟器 `Qingji-Preview` 安装并启动恢复版，截取了实际首页、记账与设置界面，保存在 `preview/`。未核对旧机截图、真实微信/支付宝通知和设备账单迁移；实体机运行尚未验证。原 APK 和输入文件未修改。
