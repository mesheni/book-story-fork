/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.statistics

import ua.acclorite.book_story.domain.model.statistics.BookReadingStatistics
import ua.acclorite.book_story.domain.model.statistics.DailyReadingStatistics
import ua.acclorite.book_story.domain.model.statistics.StatisticsSummary
import ua.acclorite.book_story.domain.repository.ReadingStatisticsRepository
import java.time.LocalDate
import javax.inject.Inject

class GetStatisticsSummaryUseCase @Inject constructor(
    private val statisticsRepository: ReadingStatisticsRepository
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now()): StatisticsSummary {
        val weekStart = today.minusDays(6).toEpochDay()
        val todayEpoch = today.toEpochDay()
        val allRecords = statisticsRepository.getAll().filter { it.durationMillis > 0L }
        val sessions = allRecords.filter { it.dayEpoch in weekStart..todayEpoch }
        val byDay = sessions.groupBy { it.dayEpoch }
        val days = (6 downTo 0).map { offset ->
            val epoch = today.minusDays(offset.toLong()).toEpochDay()
            val records = byDay[epoch].orEmpty()
            DailyReadingStatistics(
                dayEpoch = epoch,
                durationMillis = records.sumOf { it.durationMillis },
                sessionCount = records.map { it.sessionId }.distinct().size
            )
        }

        val todayRecords = byDay[todayEpoch].orEmpty()
        val streakRecords = allRecords.groupBy { it.dayEpoch }
        var streak = 0
        var streakDate = today
        while (true) {
            val dayRecords = streakRecords[streakDate.toEpochDay()].orEmpty()
            if (dayRecords.sumOf { it.durationMillis } < 60_000L) break
            streak++
            streakDate = streakDate.minusDays(1)
        }

        val books = allRecords.groupBy { it.bookId to it.bookTitle }
            .map { (key, records) ->
                BookReadingStatistics(
                    bookId = key.first,
                    title = key.second,
                    durationMillis = records.sumOf { it.durationMillis },
                    sessionCount = records.map { it.sessionId }.distinct().size,
                    progress = records.maxOf { it.endProgress }
                )
            }
            .sortedByDescending { it.durationMillis }

        return StatisticsSummary(
            todayMillis = todayRecords.sumOf { it.durationMillis },
            todaySessions = todayRecords.map { it.sessionId }.distinct().size,
            weekMillis = sessions.sumOf { it.durationMillis },
            weekSessions = sessions.map { it.sessionId }.distinct().size,
            streakDays = streak,
            days = days,
            books = books,
            sessions = allRecords
        )
    }
}