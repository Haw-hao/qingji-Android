package ink.qingji.app.backup

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

/** Names sort in snapshot creation order even when the device clock moves backward. */
object BackupFileNames {
    private val pattern = Regex("qingji-backup-([0-9]{8}T[0-9]{9}Z)-[0-9a-f]{8}\\.json")
    private val timestampFormat = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmssSSS'Z'", Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT)
    private val minimum = Instant.parse("0001-01-01T00:00:00Z")
    private val maximum = Instant.parse("9999-12-31T23:59:59.999Z")

    fun next(now: Instant, existingNames: List<String>): String {
        require(now >= minimum && now < maximum.plusMillis(1)) { "Backup date is out of range" }
        var timestamp = now.truncatedTo(ChronoUnit.MILLIS)
        existingNames.forEach { name ->
            parseTimestamp(name)?.let { existing ->
                if (existing >= timestamp) timestamp = existing.plusMillis(1)
            }
        }
        require(timestamp <= maximum) { "Backup date is out of range" }
        val date = timestampFormat.format(LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC))
        return "qingji-backup-$date-${UUID.randomUUID().toString().take(8)}.json"
    }

    fun isBackupName(name: String): Boolean = parseTimestamp(name) != null

    private fun parseTimestamp(name: String): Instant? {
        val date = pattern.matchEntire(name)?.groupValues?.get(1) ?: return null
        return try {
            LocalDateTime.parse(date, timestampFormat).toInstant(ZoneOffset.UTC)
                .takeIf { it >= minimum && it <= maximum }
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
