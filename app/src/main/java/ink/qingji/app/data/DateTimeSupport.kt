package ink.qingji.app.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val transactionDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val transactionTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun transactionDate(value: LocalDateTime): String = value.format(transactionDateFormatter)
fun transactionTime(value: LocalDateTime): String = value.format(transactionTimeFormatter)
