# 0.3.4 本地自动备份验证（2026-10-22）

## 实现

通过系统目录选择器选择手机本地目录，持久授权后自动写入 JSON 快照。包含全部账户、初始余额、账单与自定义分类。目录配置和备份状态留在应用设置；账本及分类现在都保存在 SQLite，旧 SharedPreferences 分类仅在首次迁移时导入一次。

后台串行备份覆盖数据库所有变更入口和分类修改，自动入账也经过同一触发点。新文件写完后重新读取并验证 SHA-256，再清理更旧有效快照，保留两份成功备份。无效文件不主动清理。手机时钟回调时，文件名逻辑时戳仍大于已有文件名，保证选择顺序正确；预览时间使用快照实际创建时间。

重选已有备份目录时必须先预览并确认，不能用新安装的空账本直接写入。恢复将账单、账户、分类和演示数据迁移保护位放在同一 SQLite 事务中，避免进程中断造成半恢复。独立只读审查发现并推动修复了跨存储提交及设备时钟回调问题，最终复审无剩余 P1/P2。

## 构建与单元测试

命令：`gradlew.bat -g .local-tools/gradle-home :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --offline --no-daemon`。

最终结果：BUILD SUCCESSFUL in 45s，退出码 0。24 项测试，0 失败、0 错误：备份格式与数据校验 9 项、文件名顺序及日期边界 5 项、既有业务 10 项。备份格式测试先确认缺少实现导致编译失败；文件名帮助类先做独立编译红测，再通过 5 项 JUnit 测试，最终统一 Android 构建。

Lint：0 错误、3 警告。两项既有提示为备份配置及通知图标；新增 StaticFieldLeak 提示来自进程单例持有 applicationContext，代码未保存 Activity。另有 Compose 状态装箱优化建议和既有图标弃用编译提示，未屏蔽检查。

日志：`build/backup-red.log`、`build/backup-build.log`、`build/backup-final-build.log`；测试 XML 在 `app/build/test-results/testDebugUnitTest/`。

## 模拟器完整验收

仅操作独立 Qingji-Preview（emulator-5556），未操作用户实体机。

1. 在系统选择器授权 `Documents/Qingji-Backup-QA`，首次备份成功。
2. 新增账单 BackupRestoreQA / 23.45，确认自动产生包含 3 笔账单的快照。
3. 新增账户 BackupAccountQA / 初始余额 321.09、自定义分类 BackupCategoryQA，自动备份包含 3 笔账单、2 个账户、1 个自定义分类；目录只保留两份成功快照。
4. 保存备份到本机测试证据目录，强制停止并卸载测试应用。两份共享文档仍存在，卸载前后 SHA-256 完全一致。
5. 全新安装最终 0.3.4 APK。数据库为 0 笔账单、0 账户、空分类，备份未开启。
6. 重新授权同一目录，预览正确显示 3 笔账单、2 账户，当前账单数为 0；确认前旧文件 SHA-256 仍完全一致，没有空账本写入。
7. 确认恢复后，数据库为 3 笔账单、2 账户，初始余额分别为 1000.0 和 321.09，分类为 BackupCategoryQA，restored_snapshot 为 true。
8. 自动重新备份后，对比卸载前后快照的 accounts、transactions、customCategories 三个数组，逐项完全一致（只忽略快照创建时间）。
9. 强制停止并重启后，3 笔账单、2 账户及分类仍保留，应用启动成功。

证据：`build/backup-reinstall-baseline/`、`build/backup-restored-final/`、`build/qingji-restore-preview.xml`、`build/qingji-restored-backup.xml`。文件名时钟回调使用纯 JVM 测试验证，未修改模拟器系统时钟。未模拟物理断电、磁盘写满和真实支付通知。

## 交付

`apk/qingji-0.3.4-debug-20261022.apk`，16,608,141 字节，versionCode 8，minSdk 26，targetSdk 35。APK v2 签名验证通过，继续使用本机前次恢复版的 Android Debug 签名。

SHA-256：`ebc0ec8de756f2e48a0ce4fb6bfbe7c1263d8eee0dbfa05556bef1817bdd5ca1`。

完整操作见 [本地备份使用说明](local-backup-guide.md)。正常卸载不删除共享文档，但重新安装仍须用户重新授权；此功能不能找回启用备份之前已因卸载删除的账单。未经实际备份成功，不应卸载有重要账单的应用。
