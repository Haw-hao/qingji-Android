package ink.qingji.app

import ink.qingji.app.backup.BackupFileNames
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class BackupFileNamesTest {
    @Test fun generatesUtcNameWithMillisecondPrecision() {
        val name = BackupFileNames.next(Instant.parse("2026-10-22T03:04:05.123456Z"), emptyList())
        assertTrue(name.matches(Regex("qingji-backup-20261022T030405123Z-[0-9a-f]{8}\\.json")))
        assertTrue(BackupFileNames.isBackupName(name))
    }

    @Test fun clockRollbackStillPlacesNewSnapshotFirst() {
        val old = listOf("qingji-backup-20261022T030405123Z-ffffffff.json",
            "qingji-backup-20261023T030405123Z-01234567.json")
        val name = BackupFileNames.next(Instant.parse("2026-01-01T00:00:00Z"), old)
        assertTrue(name.startsWith("qingji-backup-20261023T030405124Z-"))
        assertTrue(old.all { name > it })
    }

    @Test fun sameMillisecondAlwaysAdvancesTimestamp() {
        val now = Instant.parse("2026-10-22T23:59:59.999Z")
        val old = "qingji-backup-20261022T235959999Z-ffffffff.json"
        val name = BackupFileNames.next(now, listOf(old))
        assertTrue(name.startsWith("qingji-backup-20261023T000000000Z-"))
        assertTrue(name > old)
    }

    @Test fun unrelatedAndImpossibleNamesAreIgnored() {
        val invalid = listOf("notes.json", "qingji-backup-20260230T000000000Z-12345678.json",
            "qingji-backup-99991301T000000000Z-12345678.json",
            "qingji-backup-20261022T240000000Z-12345678.json",
            "qingji-backup-20261022T000000000Z-xyz12345.json",
            "qingji-backup-20261022T000000000Z-12345678.json.bak",
            "qingji-backup-00001022T000000000Z-12345678.json")
        assertTrue(invalid.none(BackupFileNames::isBackupName))
        val name = BackupFileNames.next(Instant.parse("2026-10-22T00:00:00Z"), invalid)
        assertTrue(name.startsWith("qingji-backup-20261022T000000000Z-"))
    }

    @Test fun rejectsTimestampOverflowRatherThanGeneratingMisorderedName() {
        val maximum = "qingji-backup-99991231T235959999Z-ffffffff.json"
        assertTrue(BackupFileNames.isBackupName(maximum))
        rejects { BackupFileNames.next(Instant.parse("2026-10-22T00:00:00Z"), listOf(maximum)) }
        rejects { BackupFileNames.next(Instant.parse("+10000-01-01T00:00:00Z"), emptyList()) }
    }

    private fun rejects(action: () -> Unit) {
        try { action(); fail("Out-of-range backup date accepted") } catch (_: IllegalArgumentException) { }
    }
}
