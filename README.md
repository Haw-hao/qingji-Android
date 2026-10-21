# 轻记 Qingji

从用户提供的轻记 0.3.2 APK 重建的原生 Android 项目。源码在本目录 `app/` 中，可用 Android Studio 打开项目根目录。这里是依据字节码重新整理的 Kotlin 工程，不能视作逐字恢复的原始 Git 源码。

## 文件

| 文件 | 恢复内容 |
| --- | --- |
| `MainActivity.kt` | 启动、原版演示数据迁移、通知权限申请 |
| `QingjiApp.kt` | 五项导航、页面状态、恢复前台刷新、原配色和自定义分类持久化 |
| `Home.kt` | 总余额、月收支、账户明细、趋势图和消费分析 |
| `Bills.kt` | 账单列表、分类筛选、搜索和删除确认 |
| `AddPage.kt` | 手动记账、账户选择、内置及自定义分类管理 |
| `ProfilePage.kt` | 账户初始余额、通知识别模式、权限设置和原版反馈文案 |
| `Schedule.kt` | 原版日程占位页 |
| `data/Transaction.kt`、`data/Account.kt` | 按 Kotlin 元数据还原的账单及账户模型 |
| `data/DateTimeSupport.kt` | 原版日期、时间格式 |
| `data/BalanceAnalytics.kt` | 余额、曲线、分类占比及警告阈值 |
| `data/TransactionDb.kt` | `qingji.db` v4 表结构、升级和数据操作 |
| `notification/PaymentNotificationParser.kt` | 微信/支付宝通知过滤、金额、商户与分类识别 |
| `notification/QingjiNotificationListenerService.kt` | 提醒、自动入账、通知去重 |

以上源码的公共路径前缀为 `app/src/main/java/ink/qingji/app/`。原图标位于 `app/src/main/res/drawable-nodpi/`，Manifest 和样式也已恢复。原始分析材料见 `recovery/README.md`。

## 构建

环境：JDK 17 或 21、Android SDK platform 35。工程使用 Gradle 8.9、AGP 8.7.3、Kotlin/Compose 插件 2.0.21，最低支持 Android 8.0（API 26）。

1. 在 Android Studio 打开本目录，选择 JDK 17/21，设置本地 Android SDK。
2. 在本机 `local.properties` 写入 `sdk.dir=你的SDK目录`（该文件不应提交）。
3. 执行：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Debug 安装包输出路径：`app/build/outputs/apk/debug/app-debug.apk`。

本任务下载的临时 JDK、SDK 和独立 Gradle 缓存都在被忽略的 `.local-tools/`，不属于应用源码。如使用本任务环境，可将 `JAVA_HOME` 指向 `.local-tools/java/` 下的 JDK，并给 Gradle 添加 `-g .local-tools/gradle-home`，避开机器上原有的损坏缓存。

本机已验证的运行方式：

```powershell
$env:JAVA_HOME = (Get-ChildItem .local-tools/java -Directory | Select-Object -First 1).FullName
.\gradlew.bat -g .local-tools/gradle-home :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --no-daemon
```

## 已知恢复边界

- 数据模型、数据库结构、日期函数和分析阈值按反编译逻辑翻译；支付解析还核对了线性字节码输出。
- Compose 的编译器生成代码无法逆变换为原始声明。当前界面保留页面、功能、主要文案和原色值，布局与曲线绘制属于重建，未做旧机逐像素或运行行为对照。原来的单个 MainActivity.kt 已拆分为多个页面文件。
- 新工程补全了金额有限值校验，并关闭通知写库后不再使用的数据库连接；其余恢复以旧版行为为依据。
- 日程在旧版就是占位页；反馈区域在旧包中也只有文案，没有可恢复的飞书链接。
- 原版的一次性演示数据清理与通知去重策略已保留，未在本次恢复中改变逻辑。
- 用户设备中的真实账单不在 APK 中；签名私钥和原始构建脚本也无法从 APK 恢复。新构建的 debug 签名不能直接覆盖原版安装。

## 验证

业务测试位于 `app/src/test/java/ink/qingji/app/RecoveryBehaviorTest.kt`，覆盖账户余额、历史曲线、分析优先级、支付识别、无关通知过滤和日期格式。

无 Android SDK 时，本任务环境还可以执行 `scripts/Test-Core.ps1` 编译纯 Kotlin 业务文件并运行同一组 JUnit 测试。该脚本使用本机 Gradle 8.9 自带 Kotlin 1.9.23 编译器做额外验证，不能替代 Android 工程的 Kotlin 2.0.21/Compose 构建。

完整 Android 构建已通过，7 项测试通过，lint 为 0 错误、2 个保留原版配置的警告。Debug APK 已在独立雷电模拟器安装启动，实际界面截图位于 `preview/`；实体机和真实支付通知尚未验证。详细结果见 `docs/restoration-status.md`。
