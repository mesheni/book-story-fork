/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.statistics

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.acclorite.book_story.domain.model.statistics.ReadingSession
import ua.acclorite.book_story.domain.repository.ReadingStatisticsRepository

class CsvExportTest {
    @Test
    fun exportQuotesFieldsWithQuotesAndCommas() = runBlocking {
        val useCase = ExportReadingStatisticsUseCase(FakeReadingStatisticsRepository())

        val csv = useCase()

        assertTrue(csv.contains("\"Книга, \"\"тест\"\"\""))
        assertTrue(csv.contains("session_id,day,started_at,book_title,format"))
    }

    private class FakeReadingStatisticsRepository : ReadingStatisticsRepository {
        override suspend fun save(session: ReadingSession) = Unit
        override suspend fun getAll() = listOf(
            ReadingSession(
                sessionId = "abc",
                dayEpoch = 20_000,
                bookId = 1,
                bookTitle = "Книга, \"тест\"",
                startedAt = 1_750_000_000_000,
                durationMillis = 90_000,
                format = "EPUB",
                startProgress = 0.1f,
                endProgress = 0.2f
            )
        )
        override suspend fun getBetween(fromDay: Long, toDay: Long) = getAll().filter {
            it.dayEpoch in fromDay..toDay
        }
        override suspend fun clear() = Unit
    }
}