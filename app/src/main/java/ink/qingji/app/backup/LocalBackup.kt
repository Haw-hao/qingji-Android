package ink.qingji.app.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import ink.qingji.app.data.TransactionDb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

data class RestorePreview(val createdAt: String, val transactions: Int, val accounts: Int)
data class BackupState(
    val enabled: Boolean = false,
    val directory: String = "",
    val lastSuccess: String? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val pendingRestore: RestorePreview? = null,
    val restoreVersion: Int = 0,
)

/** All provider IO runs off the UI thread, in order, using application context only. */
class LocalBackup private constructor(context: Context) {
    private val context = context.applicationContext
    private val resolver = this.context.contentResolver
    private val prefs = this.context.getSharedPreferences("qingji_backup", Context.MODE_PRIVATE)
    private val executor = Executors.newSingleThreadExecutor { task -> Thread(task, "qingji-backup").apply { isDaemon = true } }
    private val queued = AtomicBoolean(false)
    private val mutableState = MutableStateFlow(BackupState(
        enabled = prefs.contains("directory_uri"), directory = prefs.getString("directory_name", "").orEmpty(),
        lastSuccess = prefs.getString("last_success", null)))
    val state: StateFlow<BackupState> = mutableState
    private data class Document(val uri: Uri, val name: String)
    private data class Candidate(val document: Document, val snapshot: BackupSnapshot)
    private data class Pending(val tree: Uri, val name: String, val candidate: Candidate)
    private var pending: Pending? = null // Accessed exclusively by the executor.

    fun requestBackup() {
        if (!prefs.contains("directory_uri") || !queued.compareAndSet(false, true)) return
        executor.execute {
            queued.set(false)
            if (pending == null && prefs.contains("directory_uri")) operation { writeBackup() }
        }
    }

    fun chooseDirectory(tree: Uri) = submit {
        require(DocumentsContract.isTreeUri(tree))
        resolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        inspect(tree, directoryName(tree))
    }

    fun inspectCurrentDirectory() = submit {
        val tree = currentTree()
        inspect(tree, directoryName(tree))
    }

    fun backupNow() = submit {
        check(pending == null) { "请先确认或取消恢复" }
        writeBackup()
    }

    fun cancelRestore() = submit {
        pending = null
        mutableState.value = mutableState.value.copy(pendingRestore = null, message = "已取消恢复，当前账单未改变")
        requestBackup()
    }

    fun restore() = submit {
        val selection = pending ?: return@submit
        // Re-read and validate: the file may have changed while confirmation was on screen.
        val snapshot = BackupCodec.decode(read(selection.candidate.document.uri))
        require(snapshot == selection.candidate.snapshot) { "备份文件已发生变化，请重新选择" }
        TransactionDb(context).use { it.restoreSnapshot(snapshot) }
        pending = null
        mutableState.value = mutableState.value.copy(pendingRestore = null,
            restoreVersion = mutableState.value.restoreVersion + 1,
            message = "已恢复 ${snapshot.transactions.size} 笔账单，正在启用本地自动备份")
        enable(selection.tree, selection.name)
        check(prefs.edit().putString("last_success", snapshot.createdAt).commit())
        mutableState.value = mutableState.value.copy(enabled = true, directory = selection.name,
            lastSuccess = snapshot.createdAt, pendingRestore = null,
            message = "已恢复 ${snapshot.transactions.size} 笔账单，本地自动备份已开启")
        requestBackup()
    }

    fun disable() = submit {
        check(prefs.edit().remove("directory_uri").remove("directory_name").remove("last_success").commit())
        pending = null
        mutableState.value = mutableState.value.copy(enabled = false, directory = "", lastSuccess = null,
            pendingRestore = null, message = "自动备份已停用，已有备份文件会保留")
    }

    private fun submit(action: () -> Unit) { executor.execute { operation(action) } }
    private fun operation(action: () -> Unit) {
        mutableState.value = mutableState.value.copy(busy = true, error = null, message = null)
        try { action() } catch (error: Exception) {
            val message = when (error) {
                is SecurityException -> "目录授权已失效，请重新选择本地备份目录。当前账单和已有备份未删除。"
                is FileNotFoundException -> "备份目录或文件不可访问，请检查目录是否被移动、删除或存储空间是否充足。"
                is IllegalArgumentException -> "备份文件未通过校验或目录不受支持，未覆盖现有备份。请检查目录并重试。"
                else -> "操作未完成，请检查存储空间和目录权限后重试。已有备份文件不会主动删除。"
            }
            mutableState.value = mutableState.value.copy(error = message)
        } finally { mutableState.value = mutableState.value.copy(busy = false) }
    }

    private fun inspect(tree: Uri, name: String) {
        val documents = documents(tree)
        val latest = latestValid(documents)
        if (latest != null) {
            pending = Pending(tree, name, latest)
            mutableState.value = mutableState.value.copy(pendingRestore = RestorePreview(latest.snapshot.createdAt,
                latest.snapshot.transactions.size, latest.snapshot.accounts.size))
        } else {
            // A new installation must never overwrite an unreadable old backup with an empty ledger.
            require(documents.isEmpty()) { "目录中没有可读取的有效备份" }
            enable(tree, name)
            writeBackup()
        }
    }

    private fun enable(tree: Uri, name: String) {
        val changed = prefs.getString("directory_uri", null) != tree.toString()
        val editor = prefs.edit().putString("directory_uri", tree.toString()).putString("directory_name", name)
        if (changed) editor.remove("last_success")
        check(editor.commit())
        mutableState.value = mutableState.value.copy(enabled = true, directory = name,
            lastSuccess = if (changed) null else mutableState.value.lastSuccess)
    }

    private fun currentTree(): Uri = Uri.parse(prefs.getString("directory_uri", null)
        ?: throw IllegalStateException("尚未选择本地备份目录"))

    private fun writeBackup() {
        val tree = currentTree()
        val before = documents(tree)
        val previous = latestValid(before)
        // Once a directory contains backups, a corrupt history is never silently replaced.
        require(before.isEmpty() || previous != null)
        val snapshot = TransactionDb(context).use { it.exportSnapshot() }
        val encoded = BackupCodec.encode(snapshot)
        val name = BackupFileNames.next(Instant.now(), before.map { it.name })
        val file = DocumentsContract.createDocument(resolver, rootDocument(tree), "application/json", name)
            ?: throw FileNotFoundException()
        try {
            val output = resolver.openOutputStream(file, "w") ?: throw FileNotFoundException()
            output.use { it.write(encoded.toByteArray(Charsets.UTF_8)); it.flush() }
            require(BackupCodec.decode(read(file)) == snapshot)
        } catch (error: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, file) }
            throw error
        }
        val completedAt = Instant.now().toString()
        check(prefs.edit().putString("last_success", completedAt).commit())
        var cleanupFailed = false
        // Only delete our own valid older snapshots, after the new file was read back successfully.
        for (old in before.filter { it.uri != previous?.document?.uri }) {
            if (runCatching { BackupCodec.decode(read(old.uri)) }.isSuccess) {
                if (runCatching { DocumentsContract.deleteDocument(resolver, old.uri) }.getOrDefault(false).not()) cleanupFailed = true
            }
        }
        mutableState.value = mutableState.value.copy(lastSuccess = completedAt,
            message = if (cleanupFailed) "备份已完成；部分旧快照未能清理" else "本地备份已完成，保留最近两份成功备份")
    }

    private fun rootDocument(tree: Uri): Uri = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
    private fun directoryName(tree: Uri): String = resolver.query(rootDocument(tree),
        arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    } ?: throw FileNotFoundException()

    private fun documents(tree: Uri): List<Document> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        return resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(1)
                    if (BackupFileNames.isBackupName(name)) add(Document(DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)), name))
                }
            }.sortedByDescending { it.name }
        } ?: throw FileNotFoundException()
    }

    private fun latestValid(documents: List<Document>): Candidate? {
        // Our writer retains two files; this cap also bounds work on externally populated directories.
        require(documents.size <= 100) { "目录包含过多备份文件" }
        for (document in documents) {
            val snapshot = runCatching { BackupCodec.decode(read(document.uri)) }.getOrNull()
            if (snapshot != null) return Candidate(document, snapshot)
        }
        return null
    }

    private fun read(uri: Uri): String {
        val input = resolver.openInputStream(uri) ?: throw FileNotFoundException()
        return input.use {
            val result = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = it.read(buffer)
                if (count < 0) break
                require(result.size().toLong() + count <= MaxBytes)
                result.write(buffer, 0, count)
            }
            result.toString(Charsets.UTF_8.name())
        }
    }

    companion object {
        private const val MaxBytes = 32 * 1024 * 1024
        @Volatile private var instance: LocalBackup? = null
        fun get(context: Context): LocalBackup = instance ?: synchronized(this) {
            instance ?: LocalBackup(context).also { instance = it }
        }
    }
}
