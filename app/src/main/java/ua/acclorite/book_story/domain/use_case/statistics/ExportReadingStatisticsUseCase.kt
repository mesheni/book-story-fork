/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.statistics

import ua.acclorite.book_story.domain.repository.ReadingStatisticsRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExportReadingStatisticsUseCase @Inject constructor(
    private val repository: ReadingStatisticsRepository
) {
    suspend operator fun invoke(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
        return buildString {
            appendLine("session_id,day,started_at,book_title,format,duration_seconds,start_progress,end_progress")
            repository.getAll().forEach { session ->
                val date = java.time.LocalDate.ofEpochDay(session.dayEpoch)
                val start = formatter.format(Instant.ofEpochMilli(session.startedAt))
                val values = listOf(
                    session.sessionId,
                    date.toString(),
                    start,
                    session.bookTitle,
                    session.format,
                    (session.durationMillis / 1000.0).toString(),
                    session.startProgress.toString(),
                    session.endProgress.toString()
                )
                appendLine(values.joinToString(",") { value ->
                    "\"${value.csvSafe().replace("\"", "\"\"")}\""
                })
            }
        }
    }

    /**
     * Neutralizes CSV formula injection: spreadsheet apps execute
     * quoted cells starting with =, +, - or @ as formulas.
     */
    private fun String.csvSafe(): String =
        if (isNotEmpty() && first() in "=+-@\t\r") "'$this" else this
}