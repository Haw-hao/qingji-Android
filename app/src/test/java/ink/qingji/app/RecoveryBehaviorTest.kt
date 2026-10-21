package ink.qingji.app

import ink.qingji.app.data.*
import ink.qingji.app.notification.parsePaymentNotification
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class RecoveryBehaviorTest {
    private fun bill(amount: Double, income: Boolean = false, date: String = "2026-09-08",
                     account: Long = 1, category: String = "餐饮") =
        Transaction(1, "商户", category, "微信", amount, income, date, "10:00", account)

    @Test fun balancesKeepAccountsSeparate() {
        val balances = calculateAccountBalances(listOf(Account(1, "现金", 100.0), Account(2, "卡", 500.0)),
            listOf(bill(20.0), bill(50.0, true, account = 2), bill(999.0, account = 99)))
        assertEquals(80.0, balances.getValue(1), 0.001)
        assertEquals(550.0, balances.getValue(2), 0.001)
        assertEquals(630.0, calculateTotalBalance(listOf(Account(1, "现金", 100.0), Account(2, "卡", 500.0)),
            listOf(bill(20.0), bill(50.0, true, account = 2))), 0.001)
    }

    @Test fun balanceSeriesIncludesEarlierTransactionsAndExcludesFutureOnes() {
        val series = buildBalanceSeries(listOf(bill(10.0, date = "2026-09-01"), bill(20.0),
            bill(500.0, true, "2026-10-01")), 100.0, "2026-09-08", 2)
        assertEquals(listOf(BalancePoint("2026-09-07", 90.0), BalancePoint("2026-09-08", 70.0)), series)
        assertEquals(8, historyRangeDays(listOf(bill(10.0, date = "2026-09-01")), "2026-09-08"))
        assertEquals(1, historyRangeDays(emptyList(), "2026-09-08"))
    }

    @Test fun negativeBalanceTakesPriorityOverCategoryWarning() {
        assertEquals(AnalysisLevel.NEGATIVE, analyzeBalance(listOf(bill(200.0)), 100.0, "2026-09-08").level)
        assertEquals("余额下降较快", analyzeBalance(listOf(bill(300.0)), 1000.0, "2026-09-08").title)
        assertEquals("餐饮", analyzeBalance(listOf(bill(120.0)), 1000.0, "2026-09-08").topCategory)
        assertEquals(AnalysisLevel.NORMAL, analyzeBalance(emptyList(), null, "2026-09-08").level)
    }

    @Test fun wechatPaymentRecoversAmountMerchantAndCategory() {
        val result = parsePaymentNotification("com.tencent.mm", "微信支付", "付款￥19.50 付款给奶茶店")!!
        assertEquals(19.5, result.amount, 0.001)
        assertEquals("奶茶店", result.merchant)
        assertEquals("饮料", result.category)
        assertEquals("微信", result.method)
        assertFalse(result.income)
    }

    @Test fun alipayIncomeAndDecimalCommaAreRecognized() {
        val result = parsePaymentNotification("com.eg.android.AlipayGphone", "支付宝收款", "到账 RMB 20,50 来自公司")!!
        assertEquals(20.5, result.amount, 0.001)
        assertEquals("公司", result.merchant)
        assertEquals("工资", result.category)
        assertTrue(result.income)
    }

    @Test fun unrelatedPackagesAndChatMessagesAreIgnored() {
        assertNull(parsePaymentNotification("other.app", "微信支付", "付款￥10"))
        assertNull(parsePaymentNotification("com.tencent.mm", "朋友", "还你￥10"))
        assertNull(parsePaymentNotification("com.tencent.mm", "微信支付", "付款成功"))
    }

    @Test fun transactionDateAndTimeUseOriginalStorageFormat() {
        val now = LocalDateTime.of(2026, 9, 8, 9, 4)
        assertEquals("2026-09-08", transactionDate(now))
        assertEquals("09:04", transactionTime(now))
    }
}
