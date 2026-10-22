package ink.qingji.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import ink.qingji.app.backup.BackupCodec
import ink.qingji.app.backup.BackupSnapshot
import ink.qingji.app.backup.LocalBackup
import java.time.Instant
import org.json.JSONArray

// Retains the original database name, version, schema and migration conditions.
class TransactionDb(context: Context) : SQLiteOpenHelper(context, "qingji.db", null, 4) {
    private val appContext = context.applicationContext
    init { initializeLocalState() }
    private fun changed() { LocalBackup.get(appContext).requestBackup() }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE accounts(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE, initial_balance REAL NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE transactions(id INTEGER PRIMARY KEY AUTOINCREMENT, merchant TEXT, category TEXT, method TEXT, amount REAL, income INTEGER, date TEXT, time TEXT, account_id INTEGER NOT NULL DEFAULT 1)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE transactions ADD COLUMN date TEXT NOT NULL DEFAULT '2026-09-06'")
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE accounts(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE, initial_balance REAL NOT NULL DEFAULT 0)")
            listOf("银行", "券商", "支付宝", "微信", "现金").forEach {
                db.execSQL("INSERT INTO accounts(name) VALUES(?)", arrayOf(it))
            }
            db.execSQL("ALTER TABLE transactions ADD COLUMN account_id INTEGER NOT NULL DEFAULT 1")
        }
        if (oldVersion < 4) db.execSQL("DELETE FROM accounts WHERE name IN ('银行','券商','支付宝','微信','现金') AND initial_balance=0 AND id NOT IN (SELECT account_id FROM transactions)")
    }

    fun list(): List<Transaction> = readableDatabase.rawQuery(
        "SELECT id,merchant,category,method,amount,income,date,time,account_id FROM transactions ORDER BY id DESC", null,
    ).use { cursor -> buildList {
        while (cursor.moveToNext()) add(Transaction(cursor.getLong(0), cursor.getString(1), cursor.getString(2),
            cursor.getString(3), cursor.getDouble(4), cursor.getInt(5) == 1,
            cursor.getString(6), cursor.getString(7), cursor.getLong(8)))
    } }

    fun add(t: Transaction) {
        val db = writableDatabase
        var accountId = db.rawQuery("SELECT id FROM accounts WHERE id=?", arrayOf(t.accountId.toString())).use {
            if (it.moveToFirst()) it.getLong(0) else 0L
        }
        if (accountId == 0L) {
            db.execSQL("INSERT OR IGNORE INTO accounts(name) VALUES('日常支付')")
            accountId = db.rawQuery("SELECT id FROM accounts WHERE name='日常支付'", null).use {
                it.moveToFirst(); it.getLong(0)
            }
        }
        db.execSQL("INSERT INTO transactions(merchant,category,method,amount,income,date,time,account_id) VALUES(?,?,?,?,?,?,?,?)",
            arrayOf(t.merchant, t.category, t.method, t.amount, if (t.income) 1 else 0, t.date, t.time, accountId))
        changed()
    }

    fun accounts(): List<Account> = readableDatabase.rawQuery(
        "SELECT id,name,initial_balance FROM accounts ORDER BY id", null,
    ).use { cursor -> buildList {
        while (cursor.moveToNext()) add(Account(cursor.getLong(0), cursor.getString(1), cursor.getDouble(2)))
    } }

    fun addAccount(name: String, initialBalance: Double = 0.0) {
        writableDatabase.execSQL("INSERT INTO accounts(name,initial_balance) VALUES(?,?)", arrayOf(name, initialBalance))
        changed()
    }

    fun updateAccount(account: Account) {
        writableDatabase.execSQL("UPDATE accounts SET name=?, initial_balance=? WHERE id=?",
            arrayOf(account.name, account.initialBalance, account.id))
        changed()
    }

    fun deleteAccount(id: Long) {
        writableDatabase.delete("accounts", "id=? AND id NOT IN (SELECT account_id FROM transactions)", arrayOf(id.toString()))
        changed()
    }

    fun delete(id: Long) { writableDatabase.delete("transactions", "id=?", arrayOf(id.toString())); changed() }

    fun exportSnapshot(): BackupSnapshot {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val result = BackupSnapshot(Instant.now().toString(), accounts(), list(), customCategories())
            db.setTransactionSuccessful()
            result
        } finally { db.endTransaction() }
    }

    fun restoreSnapshot(snapshot: BackupSnapshot) {
        BackupCodec.validate(snapshot)
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("transactions", null, null)
            db.delete("accounts", null, null)
            snapshot.accounts.forEach { account ->
                db.execSQL("INSERT INTO accounts(id,name,initial_balance) VALUES(?,?,?)",
                    arrayOf(account.id, account.name, account.initialBalance))
            }
            snapshot.transactions.forEach { bill ->
                db.execSQL("INSERT INTO transactions(id,merchant,category,method,amount,income,date,time,account_id) VALUES(?,?,?,?,?,?,?,?,?)",
                    arrayOf(bill.id, bill.merchant, bill.category, bill.method, bill.amount,
                        if (bill.income) 1 else 0, bill.date, bill.time, bill.accountId))
            }
            // Categories and the demo-migration guard are part of the same atomic ledger commit.
            db.execSQL("INSERT OR REPLACE INTO local_app_state(key,value) VALUES('custom_categories',?)",
                arrayOf(JSONArray(snapshot.customCategories).toString()))
            db.execSQL("INSERT OR REPLACE INTO local_app_state(key,value) VALUES('restored_snapshot','true')")
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    private fun initializeLocalState() {
        val db = writableDatabase
        db.execSQL("CREATE TABLE IF NOT EXISTS local_app_state(key TEXT PRIMARY KEY,value TEXT NOT NULL)")
        val legacy = appContext.getSharedPreferences("qingji_settings", Context.MODE_PRIVATE)
            .getStringSet("custom_categories", emptySet()).orEmpty().sorted()
        db.execSQL("INSERT OR IGNORE INTO local_app_state(key,value) VALUES('custom_categories',?)",
            arrayOf(JSONArray(legacy).toString()))
    }

    fun customCategories(): List<String> = readableDatabase.rawQuery(
        "SELECT value FROM local_app_state WHERE key='custom_categories'", null).use { cursor ->
        val values = JSONArray(if (cursor.moveToFirst()) cursor.getString(0) else "[]")
        (0 until values.length()).map { values.getString(it) }
    }

    fun updateCustomCategories(values: List<String>) {
        writableDatabase.execSQL("INSERT OR REPLACE INTO local_app_state(key,value) VALUES('custom_categories',?)",
            arrayOf(JSONArray(values).toString()))
        changed()
    }

    fun wasRestored(): Boolean = readableDatabase.rawQuery(
        "SELECT value FROM local_app_state WHERE key='restored_snapshot'", null).use {
        it.moveToFirst() && it.getString(0) == "true"
    }

    fun removeLegacyDemoData() {
        val fields = "(merchant=? AND category=? AND method=? AND amount=? AND income=? AND time=?)"
        writableDatabase.delete("transactions", List(4) { fields }.joinToString(" OR "), arrayOf(
            "喜茶", "饮料", "微信支付", "19", "0", "15:32",
            "沙县小吃", "餐饮", "支付宝", "25", "0", "12:18",
            "滴滴出行", "交通", "微信支付", "16", "0", "09:04",
            "九月工资", "工资", "银行卡", "6800", "1", "10:02"))
        changed()
    }

}
