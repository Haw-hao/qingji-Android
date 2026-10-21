package ink.qingji.app.data

// Reconstructed from Transaction.kt metadata and DEX in Qingji 0.3.2.
data class Transaction(
    val id: Long,
    val merchant: String,
    val category: String,
    val method: String,
    val amount: Double,
    val income: Boolean,
    val date: String,
    val time: String,
    val accountId: Long = 1,
)
