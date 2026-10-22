package ink.qingji.app.backup

import ink.qingji.app.data.Account
import ink.qingji.app.data.Transaction
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

data class BackupSnapshot(
    val createdAt: String,
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val customCategories: List<String>,
)

/** Versioned, self-contained ledger snapshots. Error messages never contain ledger data. */
object BackupCodec {
    const val MAX_BYTES = 32 * 1024 * 1024
    private const val MAX_ITEMS = 100_000
    private const val FORMAT = "qingji-local-backup"

    fun encode(snapshot: BackupSnapshot): String = jsonOperation {
        validate(snapshot)
        val accounts = JSONArray()
        snapshot.accounts.forEach {
            accounts.put(JSONObject().put("id", it.id).put("name", it.name)
                .put("initialBalance", it.initialBalance))
        }
        val transactions = JSONArray()
        snapshot.transactions.forEach {
            transactions.put(JSONObject().put("id", it.id).put("merchant", it.merchant)
                .put("category", it.category).put("method", it.method).put("amount", it.amount)
                .put("income", it.income).put("date", it.date).put("time", it.time)
                .put("accountId", it.accountId))
        }
        val payload = JSONObject().put("createdAt", snapshot.createdAt).put("accounts", accounts)
            .put("transactions", transactions).put("customCategories", JSONArray(snapshot.customCategories))
            .toString()
        val encoded = JSONObject().put("format", FORMAT).put("version", 1)
            .put("payload", payload).put("checksum", checksum(payload)).toString()
        checkSize(encoded)
        encoded
    }

    fun decode(encoded: String): BackupSnapshot = jsonOperation {
        checkSize(encoded)
        val envelope = parseObject(encoded)
        require(envelope.string("format") == FORMAT) { "Unsupported backup format" }
        require(envelope.integer("version") == 1L) { "Unsupported backup version" }
        val payload = envelope.string("payload")
        val expected = envelope.string("checksum")
        require(expected.matches(Regex("[0-9a-f]{64}")) && expected == checksum(payload)) {
            "Backup checksum mismatch"
        }
        val data = parseObject(payload)
        val accounts = data.array("accounts")
        val transactions = data.array("transactions")
        val categories = data.array("customCategories")
        BackupSnapshot(
            createdAt = data.string("createdAt"),
            accounts = List(accounts.length()) { index ->
                accounts.getJSONObject(index).let {
                    Account(it.integer("id"), it.string("name"), it.number("initialBalance"))
                }
            },
            transactions = List(transactions.length()) { index ->
                transactions.getJSONObject(index).let {
                    Transaction(it.integer("id"), it.string("merchant"), it.string("category"),
                        it.string("method"), it.number("amount"), it.boolean("income"),
                        it.string("date"), it.string("time"), it.integer("accountId"))
                }
            },
            customCategories = List(categories.length()) { index ->
                categories.get(index) as? String ?: invalid()
            },
        ).also(::validate)
    }

    fun validate(snapshot: BackupSnapshot) {
        require(listOf(snapshot.accounts.size, snapshot.transactions.size, snapshot.customCategories.size)
            .all { it <= MAX_ITEMS }) { "Backup contains too many entries" }
        try {
            Instant.parse(snapshot.createdAt)
            val ids = HashSet<Long>()
            val names = HashSet<String>()
            snapshot.accounts.forEach {
                require(it.id > 0 && ids.add(it.id)) { "Invalid or duplicate account ID" }
                require(it.name.isNotBlank() && names.add(it.name)) { "Invalid or duplicate account name" }
                require(it.initialBalance.isFinite()) { "Invalid account balance" }
            }
            val transactionIds = HashSet<Long>()
            snapshot.transactions.forEach {
                require(it.id > 0 && transactionIds.add(it.id)) { "Invalid or duplicate transaction ID" }
                require(it.accountId in ids) { "Transaction account missing" }
                require(it.amount.isFinite() && it.amount > 0) { "Invalid transaction amount" }
                require(it.date.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))) { "Invalid transaction date" }
                require(it.time.matches(Regex("[0-9]{2}:[0-9]{2}"))) { "Invalid transaction time" }
                LocalDate.parse(it.date)
                LocalTime.parse(it.time)
            }
            val categories = HashSet<String>()
            snapshot.customCategories.forEach {
                require(it.isNotBlank() && categories.add(it)) { "Invalid or duplicate custom category" }
            }
        } catch (_: DateTimeParseException) {
            throw IllegalArgumentException("Invalid backup date or time")
        }
    }

    private fun checkSize(value: String) {
        require(value.length <= MAX_BYTES && value.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) {
            "Backup exceeds size limit"
        }
    }

    private fun checksum(payload: String): String = MessageDigest.getInstance("SHA-256")
        .digest(payload.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun parseObject(value: String): JSONObject {
        val reader = JSONTokener(value)
        val result = reader.nextValue() as? JSONObject ?: invalid()
        require(reader.nextClean() == '\u0000') { "Unexpected data after backup" }
        return result
    }

    private fun JSONObject.string(key: String): String = get(key) as? String ?: invalid()
    private fun JSONObject.boolean(key: String): Boolean = get(key) as? Boolean ?: invalid()
    private fun JSONObject.integer(key: String): Long =
        (get(key) as? Number)?.toString()?.toLongOrNull() ?: invalid()
    private fun JSONObject.number(key: String): Double = (get(key) as? Number)?.toDouble() ?: invalid()
    private fun JSONObject.array(key: String): JSONArray = getJSONArray(key).also {
        require(it.length() <= MAX_ITEMS) { "Backup contains too many entries" }
    }
    private fun invalid(): Nothing = throw IllegalArgumentException("Invalid backup structure")
    private inline fun <T> jsonOperation(operation: () -> T): T = try {
        operation()
    } catch (_: JSONException) {
        throw IllegalArgumentException("Invalid backup JSON")
    }
}
