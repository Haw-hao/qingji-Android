# 轻记 APK 恢复记录

2026-10-21 更新：Kotlin 源文件及 Android 工程已重建到项目根目录 `app/`，见 `../README.md` 与 `../docs/restoration-status.md`。下面保留初次 APK 提取的记录及限制。

分析对象：用户提供的安装包 `base.apk(1).1`。未执行 APK。

## 已确认的版本

- 应用名：轻记
- 包名：`ink.qingji.app`
- versionName：`0.3.2`；versionCode：`6`
- Android minSdk：26；targetSdk / compileSdk：35
- Kotlin：2.0.21；Android Gradle Plugin：8.7.3；Gradle：8.9；JVM target：17
- 界面：Jetpack Compose / Material 3
- 原始文件 SHA-256：`2dcf4b538bf6a9a07491a22fd68e08851ca5ecdeef999ae911cf8ef8896d7a54`

版本、SDK 和包名来自解码后的 Manifest 与 BuildConfig；构建工具版本来自包内元数据。

## 文件位置

- `original/qingji-0.3.2.apk`：原包的字节一致副本，原始输入未改动。
- `decompiled/sources/ink/qingji/app/`：业务代码的 Java 反编译表示。
- `decompiled/resources/`：解码后的 Manifest、图标及 Android 资源。
- `apk-analysis/summary.json`：原包哈希、DEX 类名、构建元数据。
- `apk-analysis/classes*.dex.classes.txt`：直接解析 DEX 得到的原始类清单。
- `apk-analysis/classes*.dex.strings.txt`：DEX 字符串清单。
- `apk-analysis/jadx.log`：JADX 1.5.6 全量反编译日志。
- `apk-analysis/rea-package.json`：REA 6.5.0 的包级分析证据，命令退出码 0。
- `apk-analysis/rea-payment-class.json`：支付通知解析类的成员及方法描述符，命令退出码 0。
- `apk-analysis/rea-payment-method.json`：支付通知解析方法的 REA 分析证据；退出码 0，`body_status=available`、`representation=java`、`fell_back=false`。代码属于反编译推导结果，尚未验证运行等价性。

## 已定位的业务

`data/TransactionDb.java` 包含 SQLite 数据库 `qingji.db` 的第 4 版表结构和升级逻辑。表包括账户 `accounts` 与账单 `transactions`，含商户、分类、支付方式、金额、收支方向、日期、时间和账户关联。

`data/BalanceAnalyticsKt.java` 包含余额和消费分析逻辑；`notification/PaymentNotificationParserKt.java` 包含微信和支付宝通知的金额、商户、收支与分类识别逻辑；通知监听服务位于同一目录。

`MainActivityKt.java` 包含首页、账单、添加账单、分类选择和个人设置等界面逻辑。包内文案中存在日程功能的占位说明，不能据此视为已实现功能。

## 工具和复现

REA 官方仓库：https://github.com/morluto/rea

Android 指南：https://github.com/morluto/rea/blob/main/docs/android-analysis.md

本次以 CLI 使用 REA，没有执行全局 MCP 注册。REA CLI 6.5.0 由官方 npm 包 `rea-agents` 获取。Android 引擎 `jadx-headless-mcp-0.7.1-all.jar` 的 SHA-256 已与 REA 官方文档固定值核对：`6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`。

全量导出使用官方 JADX 1.5.6：https://github.com/skylot/jadx/releases/tag/v1.5.6

Java 使用本机 PyCharm 所附完整 JDK 25.0.3，已检查 `jdk.compiler` 模块存在。工具包暂存于本任务 `recovery/tools/`。用户约定的 `F:\tools` 在当前主机不可用。

## 恢复边界

这是一份可供重建的静态分析材料，尚不是可直接编译的 Kotlin 工程。原始注释、Gradle 工程和签名私钥未从 APK 恢复。安装设备中的账单数据库也不包含在本次输入中。

JADX 全量反编译退出码为 3，报告 275 个错误和 52 个警告，涉及依赖库和部分 Compose 业务界面。输出中有可读业务逻辑，也有反编译器生成的变量、跳转和错误段落，不能直接当作可编译源代码。下一阶段需按数据层、通知解析、界面的顺序整理 Kotlin 工程，并与旧版实际行为核对。

原始 DEX 与 REA 的类计数会因引擎合并内部类等表示方式不同而不同，不代表原始 DEX 内容丢失。未进行安装包签名验证或设备运行验证。
