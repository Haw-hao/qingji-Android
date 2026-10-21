package ink.qingji.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ink.qingji.app.data.*
import java.time.LocalDate

@Composable
internal fun Home(txs: List<Transaction>, accounts: List<Account>, onOpenCategory: (String) -> Unit) {
    var range by remember { mutableIntStateOf(7) }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    val today = LocalDate.now().toString()
    val month = today.take(7)
    val monthly = txs.filter { it.date.startsWith(month) }
    val initial = accounts.sumOf { it.initialBalance }
    val balance = if (accounts.isEmpty()) calculateCurrentBalance(txs, null) else calculateTotalBalance(accounts, txs)
    val days = if (range == 0) historyRangeDays(txs, today) else range
    val series = buildBalanceSeries(txs, initial, today, days)
    val analysis = analyzeBalance(txs, initial, today, days)
    val change = series.last().balance - series.first().balance
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("轻记", fontWeight = FontWeight.Bold, fontSize = 27.sp, color = Ink)
        Text("今天，也要轻一点", color = Color.Gray)
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Teal)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("当前总余额", color = Color.White.copy(alpha = 0.72f))
                Text("¥${money(balance)}", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SummaryStat("本月收入", monthly.filter { it.income }.sumOf { it.amount })
                    SummaryStat("本月支出", monthly.filter { !it.income }.sumOf { it.amount })
                }
            }
        }
        if (accounts.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                accounts.forEach { account ->
                    FilterChip(selected = selectedAccountId == account.id,
                        onClick = { selectedAccountId = if (selectedAccountId == account.id) null else account.id },
                        label = { Text("${account.name} · ¥${money(calculateAccountBalances(listOf(account), txs).getValue(account.id))}") })
                }
            }
            accounts.find { it.id == selectedAccountId }?.let { AccountDetails(it, txs) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("余额走势", fontWeight = FontWeight.Bold, color = Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7 to "7天", 30 to "30天", 0 to "一直以来").forEach { (value, label) ->
                        FilterChip(selected = range == value, onClick = { range = value }, label = { Text(label) })
                    }
                }
                Text(if (change == 0.0) "余额没有变化" else "${if (change > 0) "增加" else "减少"} ¥${money(kotlin.math.abs(change))}",
                    color = if (change >= 0) Green else Coral)
                BalanceChart(series)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(series.first().date, fontSize = 11.sp, color = Color.Gray)
                    Text(series.last().date, fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
        AnalysisCard(analysis, onOpenCategory)
        Text("今日账单", fontWeight = FontWeight.Bold, color = Ink)
        txs.filter { it.date == today }.forEach { TransactionRow(it) }
    }
}

@Composable
private fun SummaryStat(label: String, amount: Double) {
    Column { Text(label, color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp); Text("¥${money(amount)}", color = Color.White) }
}

@Composable
private fun AccountDetails(account: Account, txs: List<Transaction>) {
    val bills = txs.filter { it.accountId == account.id }
    val monthly = bills.filter { it.date.startsWith(LocalDate.now().toString().take(7)) }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(account.name, fontWeight = FontWeight.Bold)
            Text("初始余额 ¥${money(account.initialBalance)}")
            Text("本月收入 ¥${money(monthly.filter { it.income }.sumOf { it.amount })}")
            Text("本月支出 ¥${money(monthly.filter { !it.income }.sumOf { it.amount })}")
            Text("当前余额 ¥${money(calculateCurrentBalance(bills, account.initialBalance))}", color = Teal)
        }
    }
}

@Composable
private fun BalanceChart(series: List<BalancePoint>) {
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val minimum = series.minOfOrNull { it.balance } ?: 0.0
        val maximum = series.maxOfOrNull { it.balance } ?: 0.0
        val span = (maximum - minimum).coerceAtLeast(1.0)
        val inset = 8.dp.toPx()
        val path = Path()
        series.forEachIndexed { index, point ->
            val x = inset + (size.width - inset * 2) * index / (series.size - 1).coerceAtLeast(1)
            val y = size.height - inset - ((point.balance - minimum) / span * (size.height - inset * 2)).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            if (series.size == 1) drawCircle(Teal, 3.dp.toPx(), Offset(x, y))
        }
        drawPath(path, Teal, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun AnalysisCard(analysis: BalanceAnalysis, onOpenCategory: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("智能分析", fontWeight = FontWeight.Bold, color = Ink)
            Text(analysis.title, color = if (analysis.level == AnalysisLevel.NORMAL) Teal else Coral)
            Text(analysis.description, fontSize = 13.sp)
            analysis.topCategory?.let { category -> TextButton(onClick = { onOpenCategory(category) }) { Text("查看相关账单") } }
        }
    }
}
