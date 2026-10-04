/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.repository

import ua.acclorite.book_story.domain.model.statistics.ReadingSession

interface ReadingStatisticsRepository {
    suspend fun save(session: ReadingSession)
    suspend fun getAll(): List<ReadingSession>
    suspend fun getBetween(fromDay: Long, toDay: Long): List<ReadingSession>
    suspend fun clear()
}