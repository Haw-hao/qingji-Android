package ink.qingji.app.notification

import java.util.Locale

data class ParsedPaymentNotification(
    val amount: Double,
    val income: Boolean,
    val method: String,
    val merchant: String,
    val category: String,
    val originalText: String,
)

private val amountPattern = Regex("(?:¥|￥|RMB)\\s*([+-]?[0-9][0-9.,]*(?:[eE][+-]?[0-9]+)?)", RegexOption.IGNORE_CASE)
private val groupedAmountPattern = Regex("[0-9]{1,3}(?:,[0-9]{3})+(?:\\.[0-9]{1,2})?")
private val decimalAmountPattern = Regex("[0-9]+(?:[.,][0-9]{1,2})?")
private val incomeWords = listOf("收款", "到账", "收入", "转入")

// Control flow cross-checked with payment-simple.java (linear DEX decompilation).
fun parsePaymentNotification(packageName: String, title: String, text: String): ParsedPaymentNotification? {
    val combined = "$title $text"
    val method = when {
        packageName == "com.tencent.mm" && listOf("微信支付", "微信收款", "微信转账", "微信红包")
            .any { combined.contains(it) } -> "微信"
        packageName == "com.eg.android.AlipayGphone" && listOf("支付宝付款", "支付宝收款", "支付宝转账", "支付宝红包")
            .any { combined.contains(it) } -> "支付宝"
        else -> return null
    }
    // A final comma separates the merchant; internal commas must match a valid numeric format.
    val rawAmount = amountPattern.find(combined)?.groupValues?.getOrNull(1)?.removeSuffix(",") ?: return null
    val normalizedAmount = when {
        groupedAmountPattern.matches(rawAmount) -> rawAmount.replace(",", "")
        decimalAmountPattern.matches(rawAmount) -> rawAmount.replace(",", ".")
        else -> return null
    }
    val amount = normalizedAmount.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 } ?: return null
    val income = incomeWords.any { combined.contains(it) }
    val merchant = extractMerchant(text, amountPattern.find(text)?.range?.last ?: -1)
    return ParsedPaymentNotification(amount, income, method, merchant, inferCategory(merchant, income), text.trim())
}

private fun extractMerchant(text: String, amountEnd: Int): String {
    if (amountEnd < 0 || amountEnd + 1 >= text.length) return ""
    return text.substring(amountEnd + 1).trim(' ', '\t', '\n', '\r', ',', '，', ':', '：', '-', '—')
        .removePrefix("来自").removePrefix("付款给").trim()
}

private fun inferCategory(merchant: String, income: Boolean): String {
    if (income) return "工资"
    val text = merchant.lowercase(Locale.ROOT)
    return when {
        listOf("奶茶", "咖啡", "饮料", "茶").any { text.contains(it) } -> "饮料"
        listOf("餐厅", "饭", "面", "麦当劳", "肯德基", "外卖").any { text.contains(it) } -> "餐饮"
        listOf("滴滴", "打车", "地铁", "公交").any { text.contains(it) } -> "交通"
        listOf("淘宝", "京东", "购物").any { text.contains(it) } -> "购物"
        else -> "其他"
    }
}
