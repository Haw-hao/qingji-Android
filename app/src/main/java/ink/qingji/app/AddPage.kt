package ink.qingji.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ink.qingji.app.data.*
import java.time.LocalDateTime

@Composable
internal fun AddPage(accounts: List<Account>, customCategories: List<String>,
                     onUpdateCategories: (List<String>) -> Unit, onSave: (Transaction) -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }
    var merchant by rememberSaveable { mutableStateOf("") }
    var income by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf("其他") }
    var categoryPage by rememberSaveable { mutableStateOf(false) }
    var selectedAccountId by rememberSaveable { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }
    var accountMenu by remember { mutableStateOf(false) }
    val choices = accounts.ifEmpty { listOf(Account(0, "日常支付")) }
    val account = choices.find { it.id == selectedAccountId } ?: choices.first()
    if (categoryPage) {
        BackHandler { categoryPage = false }
        CategoryPickerPage(category, customCategories, { category = it; categoryPage = false },
            onUpdateCategories, { categoryPage = false })
        return
    }
    val numericAmount = amount.toDoubleOrNull()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("记下一笔", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("手动记账", color = Teal)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(selected = !income, onClick = { income = false }, label = { Text("支出") })
            FilterChip(selected = income, onClick = { income = true }, label = { Text("收入") })
        }
        OutlinedTextField(amount, { amount = it }, label = { Text("金额") }, prefix = { Text("¥") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(merchant, { merchant = it }, label = { Text("商户 / 来源（选填）") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { categoryPage = true }, modifier = Modifier.fillMaxWidth()) { Text("分类：$category") }
        Text("这笔账属于？", fontWeight = FontWeight.SemiBold)
        Box {
            OutlinedButton(onClick = { accountMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("账户：${account.name}") }
            DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }) {
                choices.forEach { choice ->
                    DropdownMenuItem(text = { Text(choice.name) }, onClick = { selectedAccountId = choice.id; accountMenu = false })
                }
            }
        }
        if (accounts.isEmpty()) Text("还没有添加账户，记账时会先使用“日常支付”。", fontSize = 12.sp, color = Color.Gray)
        Text("初始余额由账户独立管理", fontSize = 12.sp, color = Color.Gray)
        Button(enabled = numericAmount != null && numericAmount.isFinite() && numericAmount > 0,
            modifier = Modifier.fillMaxWidth(), onClick = {
                val value = amount.toDoubleOrNull()
                if (value != null && value.isFinite() && value > 0) {
                    val now = LocalDateTime.now()
                    onSave(Transaction(0, merchant.trim(), category, "", value, income,
                        transactionDate(now), transactionTime(now), account.id))
                }
            }) { Text("保存这笔账单") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryPickerPage(selected: String, customCategories: List<String>, onSelect: (String) -> Unit,
                                onUpdateCategories: (List<String>) -> Unit, onBack: () -> Unit) {
    var newCategory by rememberSaveable { mutableStateOf("") }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text("返回记账") }
        Text("选择分类", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BuiltInCategories.forEach { category ->
                FilterChip(selected = selected == category, onClick = { onSelect(category) }, label = { Text(category) })
            }
        }
        Text("自定义分类", fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            customCategories.forEach { category ->
                Row {
                    FilterChip(selected = selected == category, onClick = { onSelect(category) }, label = { Text(category) })
                    TextButton(onClick = { categoryToDelete = category }) { Text("删除") }
                }
            }
        }
        OutlinedTextField(newCategory, { newCategory = it }, label = { Text("例如：宠物、旅行") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = {
            val value = newCategory.trim()
            if (value.isNotBlank() && value !in BuiltInCategories && value !in customCategories) {
                onUpdateCategories(customCategories + value); newCategory = ""
            }
        }, enabled = newCategory.trim().isNotBlank() && newCategory.trim() !in BuiltInCategories && newCategory.trim() !in customCategories) { Text("添加") }
    }
    categoryToDelete?.let { category ->
        AlertDialog(onDismissRequest = { categoryToDelete = null }, title = { Text("删除自定义分类") },
            text = { Text("删除“$category”后，历史账单会保留原分类名称。确定删除吗？") },
            confirmButton = { TextButton(onClick = {
                onUpdateCategories(customCategories - category); categoryToDelete = null
            }) { Text("删除") } }, dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text("取消") } })
    }
}
