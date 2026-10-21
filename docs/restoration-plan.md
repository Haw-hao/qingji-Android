# 轻记 0.3.2 文件恢复计划

目标：依据本目录 APK 和反编译证据，在项目根目录整理可维护的 Kotlin Android 工程。用户已明确要求尝试恢复各文件；本轮保持旧版功能，不另行设计新功能。

恢复方式：数据模型、SQLite v4、日期与分析算法、通知解析和服务按反编译逐项翻译；Compose 编译器生成的状态机不能直接逆变换为原文件，按可见组件、文案、颜色、参数和事件重建并标注。原始 APK、Java 与证据保留在 recovery/，不加入应用源集。

- [x] 建立 Gradle 8.9 / AGP 8.7.3 / Kotlin 2.0.21 / SDK 35 工程、wrapper、Manifest 和资源。
- [x] 为余额计算、异常消费规则、支付通知过滤与金额解析写行为测试，先确认恢复文件缺失时无法通过。
- [x] 恢复 data/Transaction.kt、Account.kt、BalanceAnalytics.kt、DateTimeSupport.kt、TransactionDb.kt。
- [x] 恢复 notification/PaymentNotificationParser.kt、QingjiNotificationListenerService.kt。
- [x] 恢复 MainActivity.kt 与 Compose 各页面，保留原色值、导航、账户选择、账单搜索删除、分类管理、通知设置、日程占位。
- [x] 运行业务测试、Android 编译与 lint；完整构建通过，测试 7 项通过，lint 0 错误、2 警告。
- [x] 写恢复文件清单、来源、已知差异、运行方式和验证结果。

不恢复 APK 中不存在的用户账单、签名私钥和源码注释。重建 APK 的签名不同，不直接覆盖原安装或操作设备数据。
