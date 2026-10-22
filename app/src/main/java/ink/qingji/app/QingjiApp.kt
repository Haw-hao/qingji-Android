package ink.qingji.app

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ink.qingji.app.data.*
import ink.qingji.app.backup.LocalBackup
import java.util.Locale

// The original MainActivity.kt held these colors and all pages. Pages are split for readability.
internal val Teal = Color(0xFF0F766E)
internal val Coral = Color(0xFFE76F51)
internal val Soft = Color(0xFFF7FAF9)
internal val Ink = Color(0xFF17302E)
internal val Green = Color(0xFF21966B)
internal val BuiltInCategories = listOf("餐饮", "饮料", "购物", "交通", "住房", "娱乐", "医疗", "学习", "工资", "投资", "其他")
internal fun money(value: Double): String = String.format(Locale.CHINA, "%.2f", value)

@Composable
fun QingjiApp(db: TransactionDb, prefs: SharedPreferences) {
    val backup = LocalBackup.get(LocalContext.current)
    val backupState by backup.state.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var txs by remember { mutableStateOf(db.list()) }
    var accounts by remember { mutableStateOf(db.accounts()) }
    var categories by remember { mutableStateOf(db.customCategories()) }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    fun refresh() { txs = db.list(); accounts = db.accounts() }
    LaunchedEffect(backupState.restoreVersion) {
        refresh()
        categories = db.customCategories()
    }
    DisposableEffect(lifecycleOwner, db) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val updateCategories: (List<String>) -> Unit = { values ->
        categories = values.map { it.trim() }.filter { it.isNotBlank() && it !in BuiltInCategories }.distinct()
        db.updateCustomCategories(categories)
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, background = Soft, surface = Color.White)) {
        Scaffold(containerColor = Soft, bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val labels = listOf("首页", "账单", "添加", "日程", "我的")
                val icons = listOf(Icons.Default.Home, Icons.Default.ReceiptLong, Icons.Default.Add,
                    Icons.Default.CalendarMonth, Icons.Default.Person)
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(selected = tab == index, onClick = { tab = index },
                        icon = {
                            if (index == 2) Icon(icons[index], label, tint = Color.White,
                                modifier = Modifier.background(Coral, RoundedCornerShape(14.dp)).padding(10.dp))
                            else Icon(icons[index], label)
                        }, label = { Text(label) })
                }
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    0 -> Home(txs, accounts) { filter = it; tab = 1 }
                    1 -> Bills(txs, filter, { filter = null }) { id -> db.delete(id); refresh() }
                    2 -> AddPage(accounts, categories, updateCategories) { bill ->
                        db.add(bill); refresh(); tab = 0
                    }
                    3 -> Schedule()
                    4 -> ProfilePage(accounts, txs, prefs) { name, balance -> db.addAccount(name, balance); refresh() }
                }
            }
        }
    }
}
