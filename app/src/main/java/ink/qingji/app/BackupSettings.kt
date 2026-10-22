package ink.qingji.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ink.qingji.app.backup.LocalBackup
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun BackupSettings(currentTransactionCount: Int) {
    val context = LocalContext.current
    val backup = remember(context) { LocalBackup.get(context) }
    val state by backup.state.collectAsState()
    val folderPicker = rememberLauncherForActivityResult(object : ActivityResultContracts.OpenDocumentTree() {
        override fun createIntent(context: Context, input: Uri?): Intent =
            super.createIntent(context, input).putExtra(Intent.EXTRA_LOCAL_ONLY, true)
    }) { tree -> if (tree != null) backup.chooseDirectory(tree) }
    Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("本地自动备份", fontWeight = FontWeight.Bold)
            Text(if (state.enabled) "已开启 · ${state.directory}" else "尚未开启，卸载应用会删除应用内账单", color = Teal)
            Text("选择手机本地文件夹，例如 Documents/轻记备份。账单、账户和自定义分类会自动保存到该目录，保留最近两份成功备份。卸载后文件仍保留，重装后重选同一目录即可恢复。", fontSize = 13.sp)
            Text("备份文件包含明文账单，请放在自己管理的本地目录。", fontSize = 12.sp)
            state.lastSuccess?.let { Text("最近成功备份：${backupTime(it)}", fontSize = 12.sp) }
            if (state.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("正在处理本地备份…", fontSize = 12.sp) }
            state.message?.let { Text(it, fontSize = 12.sp, color = Teal) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            OutlinedButton(enabled = !state.busy, onClick = { folderPicker.launch(null) }) {
                Text(if (state.enabled) "更换备份目录 / 选择目录恢复" else "选择本地备份目录 / 恢复")
            }
            if (state.enabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !state.busy, onClick = backup::backupNow) { Text("立即备份") }
                    OutlinedButton(enabled = !state.busy, onClick = backup::inspectCurrentDirectory) { Text("恢复备份") }
                }
                TextButton(enabled = !state.busy, onClick = backup::disable) { Text("停用自动备份（保留文件）") }
            }
        }
    }
    state.pendingRestore?.let { preview ->
        AlertDialog(onDismissRequest = { if (!state.busy) backup.cancelRestore() },
            title = { Text("发现本地备份") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("备份时间：${backupTime(preview.createdAt)}")
                Text("${preview.transactions} 笔账单，${preview.accounts} 个账户，包含账户初始余额和自定义分类。")
                Text("恢复将替换当前 $currentTransactionCount 笔账单及账户、分类，不会合并。确认前不会用当前数据覆盖此备份。")
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { TextButton(enabled = !state.busy, onClick = backup::restore) { Text("确认恢复并开启备份") } },
            dismissButton = { TextButton(enabled = !state.busy, onClick = backup::cancelRestore) { Text("取消") } })
    }
}

private fun backupTime(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.parse(value))
}.getOrDefault(value)
