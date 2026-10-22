package ink.qingji.app

import ink.qingji.app.backup.BackupCodec
import ink.qingji.app.backup.BackupSnapshot
import ink.qingji.app.data.Account
import ink.qingji.app.data.Transaction
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

class BackupCodecTest {
    private fun sample() = BackupSnapshot("2026-10-22T01:00:00Z", listOf(Account(7, "现金\"与卡", -50.25)),
        listOf(Transaction(9, "奶茶\n商店", "宠物", "微信", 1234.56, false, "2026-10-22", "09:04", 7)), listOf("宠物", "旅行"))

    @Test fun roundTripPreservesIdsBalancesUnicodeAndCategories() {
        assertEquals(sample(), BackupCodec.decode(BackupCodec.encode(sample())))
    }

    @Test fun emptyLedgerCanBeBackedUp() {
        val empty = BackupSnapshot("2026-10-22T01:00:00Z", emptyList(), emptyList(), emptyList())
        assertEquals(empty, BackupCodec.decode(BackupCodec.encode(empty)))
    }

    @Test fun tamperedBackupIsRejected() {
        val encoded = BackupCodec.encode(sample())
        rejects { BackupCodec.decode(encoded.replace("1234.56", "2234.56")) }
    }

    @Test fun truncatedOrUnrelatedFilesAreRejected() {
        val encoded = BackupCodec.encode(sample())
        rejects { BackupCodec.decode(encoded.dropLast(8)) }
        rejects { BackupCodec.decode("{}") }
        rejects { BackupCodec.decode(encoded.replace("qingji-local-backup", "another-app")) }
        rejects { BackupCodec.decode(JSONObject(encoded).put("version", 99).toString()) }
    }

    @Test fun invalidSnapshotIsRejectedBeforeBackupOrRestore() {
        val source = sample()
        val bill = source.transactions.single()
        for (invalid in listOf(
            source.copy(accounts = source.accounts + source.accounts),
            source.copy(transactions = source.transactions + source.transactions),
            source.copy(transactions = listOf(bill.copy(accountId = 999))),
            source.copy(transactions = listOf(bill.copy(amount = Double.NaN))),
            source.copy(transactions = listOf(bill.copy(amount = Double.POSITIVE_INFINITY))),
            source.copy(transactions = listOf(bill.copy(amount = 0.0))),
            source.copy(transactions = listOf(bill.copy(date = "2026-02-30"))),
            source.copy(transactions = listOf(bill.copy(time = "25:00"))),
            source.copy(accounts = listOf(source.accounts.single().copy(id = 0))),
            source.copy(accounts = listOf(source.accounts.single().copy(initialBalance = Double.NaN))),
            source.copy(accounts = listOf(source.accounts.single().copy(name = "  "))),
            source.copy(accounts = source.accounts + source.accounts.single().copy(id = 8)),
            source.copy(transactions = listOf(bill.copy(id = -1))),
            source.copy(createdAt = "2026-02-30T00:00:00Z"),
            source.copy(customCategories = listOf(" ")),
            source.copy(customCategories = listOf("旅行", "旅行")),
        )) rejects { BackupCodec.encode(invalid) }
    }

    @Test fun restoresRequireValidationEvenWhenChecksumMatches() {
        val encoded = BackupCodec.encode(sample())
        rejects { BackupCodec.decode(rewritePayload(encoded) {
            it.getJSONArray("transactions").getJSONObject(0).put("accountId", 999)
        }) }
        rejects { BackupCodec.decode(rewritePayload(encoded) {
            it.getJSONArray("transactions").getJSONObject(0).put("income", "false")
        }) }
        rejects { BackupCodec.decode(rewritePayload(encoded) {
            it.getJSONArray("accounts").getJSONObject(0).put("id", "7")
        }) }
        rejects { BackupCodec.decode(rewritePayload(encoded) {
            it.put("customCategories", JSONArray().put(123))
        }) }
    }

    @Test fun preservesLongIdsBeyondDoublePrecision() {
        val id = 9_007_199_254_740_993L
        val source = sample().let { it.copy(accounts = listOf(it.accounts.single().copy(id = id)),
            transactions = listOf(it.transactions.single().copy(id = Long.MAX_VALUE, accountId = id))) }
        assertEquals(source, BackupCodec.decode(BackupCodec.encode(source)))
    }

    @Test fun rejectsTrailingContentAndOversizedInputOrArrays() {
        rejects { BackupCodec.decode(BackupCodec.encode(sample()) + "{}") }
        rejects { BackupCodec.decode(" ".repeat(BackupCodec.MAX_BYTES + 1)) }
        rejects { BackupCodec.encode(sample().copy(customCategories = List(100_001) { "类别$it" })) }
    }

    @Test fun checksumUsesOriginalPayloadIncludingWhitespace() {
        val envelope = JSONObject(BackupCodec.encode(sample()))
        val payload = JSONObject(envelope.getString("payload")).toString(2)
        envelope.put("payload", payload).put("checksum", digest(payload))
        assertEquals(sample(), BackupCodec.decode(envelope.toString()))
    }

    private fun rewritePayload(encoded: String, mutation: (JSONObject) -> Unit): String {
        val envelope = JSONObject(encoded)
        val payload = JSONObject(envelope.getString("payload")).also(mutation).toString()
        return envelope.put("payload", payload).put("checksum", digest(payload)).toString()
    }

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun rejects(action: () -> Unit) {
        try { action(); fail("Invalid backup was accepted") } catch (_: IllegalArgumentException) { }
    }
}
