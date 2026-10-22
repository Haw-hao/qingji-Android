package ink.qingji.app

import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ink.qingji.app.data.*

@Composable
internal fun ProfilePage(accounts: List<Account>, txs: List<Transaction>, prefs: SharedPreferences,
                         onAddAccount: (String, Double) -> Unit) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var initialBalance by rememberSaveable { mutableStateOf("") }
    var notificationMode by remember { mutableStateOf(prefs.getString("notification_mode", "remind") ?: "remind") }
    val balances = calculateAccountBalances(accounts, txs)
    val numericBalance = if (initialBalance.isBlank()) 0.0 else initialBalance.toDoubleOrNull()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("轻记设置", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("我的数据", fontWeight = FontWeight.Bold)
                Text("账单只属于你", color = Teal)
                Text("轻记默认将数据保存在本地设备，不上传消费记录、银行卡信息或通知内容。", fontSize = 13.sp)
            }
        }
        BackupSettings(txs.size)
        Text("账户余额", fontWeight = FontWeight.Bold)
        accounts.forEach { account ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(account.name, fontWeight = FontWeight.SemiBold)
                    Text("初始余额 ¥${money(account.initialBalance)}", fontSize = 12.sp, color = Color.Gray)
                    Text("当前余额 ¥${money(balances.getValue(account.id))}", color = Teal)
                }
            }
        }
        OutlinedTextField(name, { name = it }, label = { Text("账户名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(initialBalance, { initialBalance = it }, label = { Text("初始余额") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
            modifier = Modifier.fillMaxWidth())
        Button(enabled = name.trim().isNotBlank() && accounts.none { it.name == name.trim() } &&
            numericBalance != null && numericBalance.isFinite(), onClick = {
                val value = if (initialBalance.isBlank()) 0.0 else initialBalance.toDoubleOrNull()
                if (value != null && value.isFinite()) {
                    onAddAccount(name.trim(), value); name = ""; initialBalance = ""
                }
            }) { Text("添加账户") }
        HorizontalDivider()
        Text("支付通知识别", fontWeight = FontWeight.Bold)
        Text("仅在本地识别微信和支付宝的支付通知", fontSize = 12.sp, color = Color.Gray)
        listOf("off" to "关闭", "remind" to "提醒记账", "auto" to "自动记账").forEach { (mode, label) ->
            Row {
                RadioButton(selected = notificationMode == mode, onClick = {
                    notificationMode = mode; prefs.edit().putString("notification_mode", mode).apply()
                })
                TextButton(onClick = { notificationMode = mode; prefs.edit().putString("notification_mode", mode).apply() }) { Text(label) }
            }
        }
        OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) {
            Text("打开通知访问权限")
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("评论与建议", fontWeight = FontWeight.Bold)
                Text("通过飞书接收大家的反馈", fontSize = 12.sp, color = Color.Gray)
            }
        }
        Text("轻记 ${BuildConfig.VERSION_NAME}", color = Color.Gray, fontSize = 12.sp)
    }
}
