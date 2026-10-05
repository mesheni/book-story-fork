/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.model.statistics

data class DailyReadingStatistics(
    val dayEpoch: Long,
    val durationMillis: Long,
    val sessionCount: Int
)

data class BookReadingStatistics(
    val bookId: Int?,
    val title: String,
    val durationMillis: Long,
    val sessionCount: Int,
    val progress: Float
)

data class StatisticsSummary(
    val todayMillis: Long,
    val todaySessions: Int,
    val weekMillis: Long,
    val weekSessions: Int,
    val streakDays: Int,
    val days: List<DailyReadingStatistics>,
    val books: List<BookReadingStatistics>,
    val sessions: List<ReadingSession>
)