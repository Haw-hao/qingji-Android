# 轻记 0.3.3 稳定性修复

用户于 2026-10-22 确认：修正支付金额误识别，并保留屏幕旋转前的表单与页面状态，然后交付新版 APK。

采用小范围修复，保留 SQLite v4 和当前页面结构。支付解析先捕获完整金额，再验证千分位与小数格式；不能截取无效金额的前半部分。兼容原有小数逗号，金额必须为有限正数。UI 使用 Compose rememberSaveable 保留可保存的字符串、布尔值、账户 ID 与导航位置；数据库列表继续从数据库读取。

- [x] 在 RecoveryBehaviorTest 中增加千分位、非法分组、零金额、溢出和小数精度回归测试，先运行确认失败。
- [x] 修改 PaymentNotificationParser，仅接受完整有效金额；再次运行业务测试。
- [x] AddPage、ProfilePage、QingjiApp 的表单与导航状态改用 rememberSaveable；临时菜单与删除对话框不持久化。
- [x] 升级 versionName 至 0.3.3、versionCode 至 7。
- [x] 运行 :app:testDebugUnitTest :app:assembleDebug :app:lintDebug，检查报告、APK 签名与元数据。
- [x] 如本机模拟器可用，验证输入、旋转恢复、保存和重启后的持久化；无法验证的项目明确记录。
- [x] 独立代码审查后修正问题，交付带版本日期的 APK、SHA-256 与测试记录，并同步 GitHub。

可交付标准：解析回归测试通过；Android 构建和 lint 无错误；安装包使用本机现有 debug 签名。原版签名私钥不可恢复，因此原版应用不能保证可直接覆盖安装；不得为安装测试删除用户账单。
