# 0.3.3 稳定性修复验证（2026-10-22）

## 仓库

确认新库 `https://github.com/Haw-hao/qingji-Android` 存在，公开，默认分支 main；本轮开始时本地 HEAD 与远程 main 同为 b238ab5。

## 修改与回归验证

- 支付通知金额按完整格式识别，支持整数、多组千分位、两位以内小数及原有小数逗号；拒绝非正、非有限、非法分组和超过两位的小数。英文逗号可作为商户分隔符。
- 新增三项行为测试，合计 10 项。首次回归测试确认 `￥1,234.56` 被旧代码识别为 1.23，零金额被旧代码接受；独立审查发现多组千分位整数及尾随英文逗号边界，补测确认失败后修正。最终独立复审无 P1/P2。
- Compose rememberSaveable 保留记账输入、分类页输入、当前导航和账户输入；临时弹出菜单及删除确认保持临时状态。未承诺跨主动切换导航页保留草稿。
- versionName 0.3.3，versionCode 7。数据库版本和签名身份保持不变。

## 构建与安装

执行：`gradlew.bat -g .local-tools/gradle-home :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --offline --no-daemon`，JAVA_HOME 使用 `.local-tools/java/` 下的 JDK 21。

最终结果：BUILD SUCCESSFUL in 36s，53 个任务，退出码 0。JUnit 10 项、0 失败、0 错误、0 跳过；Lint 0 错误、2 个既有警告。未屏蔽 lint 规则。

最终日志：`build/stability-final-build.log`；失败复现日志：`build/stability-red.log`、`build/stability-grouping-red.log`、`build/stability-review-red.log`。报告位于 `app/build/test-results/testDebugUnitTest/` 和 `app/build/reports/`。

`apksigner verify --verbose --print-certs` 通过，v2 签名有效，Android Debug 签名证书 SHA-256：`1bab781d45f5b9b83373a0abc3688fc7692a524318eba2437b0658ca2c047ac2`。

独立雷电模拟器 Qingji-Preview（emulator-5556）通过 `adb install -r` 安装最终 APK，启动成功。设备确认版本 0.3.3、code 7、minSdk 26、targetSdk 35。

真实 UI 操作验证：金额 1234.56、商户 StabilityTest 在横竖屏切换后仍保留，仍停留在添加页面；保存后强制停止并重新启动，账单列表显示 StabilityTest、-¥1234.56。UI XML 证据位于 `build/qingji-rotated.xml`、`build/qingji-restored.xml`、`build/qingji-persisted.xml`。未操作用户实体机。设置表单与分类页状态通过静态审查及编译验证，未逐项完成设备测试。

## 安装包

`apk/qingji-0.3.3-debug-20261022.apk`，16,463,055 字节。

SHA-256：`407582aa091f26f28959eac7f39d94ba981f3fef17b6554556d313a17b12c515`。

实体机、真实支付通知、原版数据库迁移尚未验证。Debug 签名无法直接覆盖原版签名；原版账单应先妥善备份，不能用卸载应用作为默认解决方法。
