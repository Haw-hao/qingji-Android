package ink.qingji.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

// Models, thresholds and messages recovered from BalanceAnalytics.kt bytecode.
enum class AnalysisLevel { NORMAL, WARNING, NEGATIVE }
data class BalancePoint(val date: String, val balance: Double)
data class BalanceAnalysis(
    val level: AnalysisLevel,
    val title: String,
    val description: String,
    val topCategory: String? = null,
)

fun calculateAccountBalances(accounts: List<Account>, transactions: List<Transaction>): Map<Long, Double> =
    accounts.associate { account ->
        account.id to (account.initialBalance + transactions.filter { it.accountId == account.id }
            .sumOf { if (it.income) it.amount else -it.amount })
    }

fun calculateTotalBalance(accounts: List<Account>, transactions: List<Transaction>): Double =
    calculateAccountBalances(accounts, transactions).values.sum()

fun calculateCurrentBalance(transactions: List<Transaction>, initialBalance: Double?): Double =
    (initialBalance ?: 0.0) + transactions.sumOf { if (it.income) it.amount else -it.amount }

fun buildBalanceSeries(
    transactions: List<Transaction>, initialBalance: Double?, endDate: String, days: Int = 7,
): List<BalancePoint> {
    val first = LocalDate.parse(endDate).minusDays(days.toLong() - 1)
    return (0 until days).map { offset ->
        val date = first.plusDays(offset.toLong())
        BalancePoint(date.toString(), calculateCurrentBalance(
            transactions.filter { !LocalDate.parse(it.date).isAfter(date) }, initialBalance))
    }
}

fun historyRangeDays(transactions: List<Transaction>, endDate: String): Int {
    val first = transactions.minOfOrNull { LocalDate.parse(it.date) } ?: return 1
    return (ChronoUnit.DAYS.between(first, LocalDate.parse(endDate)).toInt() + 1).coerceAtLeast(1)
}

fun analyzeBalance(
    transactions: List<Transaction>, initialBalance: Double?, endDate: String, days: Int = 7,
): BalanceAnalysis {
    val current = calculateCurrentBalance(transactions, initialBalance)
    if (current < 0) return BalanceAnalysis(AnalysisLevel.NEGATIVE, "余额已低于 0",
        "当前余额为 ${money(abs(current))}，请检查近期支出或补充初始余额。")

    val series = buildBalanceSeries(transactions, initialBalance, endDate, days)
    val change = series.last().balance - series.first().balance
    val expenses = transactions.filter { !it.income && it.date >= series.first().date && it.date <= endDate }
    val top = expenses.groupBy { it.category }.mapValues { (_, bills) -> bills.sumOf { it.amount } }
        .maxByOrNull { it.value }
    val total = expenses.sumOf { it.amount }
    if (change < -maxOf(100.0, abs(series.first().balance) * 0.2)) {
        val category = top?.key ?: "近期支出"
        return BalanceAnalysis(AnalysisLevel.WARNING, "余额下降较快",
            "过去 $days 天余额减少 ${money(abs(change))}，主要来自$category。建议检查近期大额消费。", category)
    }
    if (top != null && top.value >= 100 && total > 0 && top.value / total >= 0.45) {
        return BalanceAnalysis(AnalysisLevel.WARNING, "本周开销较高",
            "${top.key}支出 ${money(top.value)}，占本周支出的 ${(top.value / total * 100).toInt()}%。", top.key)
    }
    val peak = expenses.groupBy { it.date }.mapValues { (_, bills) -> bills.sumOf { it.amount } }
        .maxByOrNull { it.value }
    if (peak != null && peak.value > maxOf(100.0, total / days * 1.8)) {
        return BalanceAnalysis(AnalysisLevel.WARNING, "单日开销偏高",
            "${peak.key} 支出 ${money(peak.value)}，明显高于近 $days 天平均水平。")
    }
    return BalanceAnalysis(AnalysisLevel.NORMAL, "本周消费整体平稳", if (top != null)
        "${top.key}是主要开销，占本周支出的 ${(top.value / total * 100).toInt()}%。"
        else "暂时还没有足够的消费数据。", top?.key)
}

private fun money(value: Double) = "¥" + String.format(Locale.CHINA, "%.2f", value)
