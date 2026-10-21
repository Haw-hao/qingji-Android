package ink.qingji.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ink.qingji.app.data.Transaction

@Composable
internal fun Bills(txs: List<Transaction>, categoryFilter: String?, onClearCategory: () -> Unit, onDelete: (Long) -> Unit) {
    var query by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<Transaction?>(null) }
    val filtered = txs.filter { (categoryFilter == null || it.category == categoryFilter) &&
        (query.isBlank() || it.merchant.contains(query, true) || it.category.contains(query, true)) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("全部账单", fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Ink) }
        item { OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索商户或分类") }, singleLine = true) }
        if (categoryFilter != null) item {
            TextButton(onClick = onClearCategory) { Text("分类：$categoryFilter · 清除筛选") }
        }
        filtered.groupBy { it.date }.forEach { (date, bills) ->
            item(key = "date:$date") { Text(date, color = Color.Gray, fontSize = 12.sp) }
            items(bills, key = { it.id }) { bill -> TransactionRow(bill) { pending = it } }
        }
    }
    pending?.let { bill ->
        AlertDialog(onDismissRequest = { pending = null }, title = { Text("确认删除") },
            text = { Text("确定要删除“${bill.merchant.ifBlank { bill.category }}”这笔账单吗？删除后无法恢复。") },
            confirmButton = { TextButton(onClick = { onDelete(bill.id); pending = null }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("取消") } })
    }
}

@Composable
internal fun TransactionRow(t: Transaction, onClick: ((Transaction) -> Unit)? = null) {
    Card(modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick(t) } else Modifier),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(t.merchant.ifBlank { t.category }, fontWeight = FontWeight.SemiBold)
                Text("${t.category} · ${t.time}", color = Color.Gray, fontSize = 11.sp)
            }
            Column {
                Text("${if (t.income) "+" else "-"}¥${money(t.amount)}", fontWeight = FontWeight.Bold,
                    color = if (t.income) Green else Coral)
                Text(t.method, color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}
