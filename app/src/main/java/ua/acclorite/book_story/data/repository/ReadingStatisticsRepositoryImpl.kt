/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.data.local.dto.ReadingSessionEntity
import ua.acclorite.book_story.data.local.room.BookDatabase
import ua.acclorite.book_story.domain.model.statistics.ReadingSession
import ua.acclorite.book_story.domain.repository.ReadingStatisticsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingStatisticsRepositoryImpl @Inject constructor(
    private val database: BookDatabase
) : ReadingStatisticsRepository {
    override suspend fun save(session: ReadingSession) = withContext(Dispatchers.IO) {
        database.readingSessionDao.upsert(
            ReadingSessionEntity(
                sessionId = session.sessionId,
                dayEpoch = session.dayEpoch,
                bookId = session.bookId,
                bookTitle = session.bookTitle,
                startedAt = session.startedAt,
                durationMillis = session.durationMillis,
                format = session.format,
                startProgress = session.startProgress,
                endProgress = session.endProgress
            )
        )
        Unit
    }

    override suspend fun getAll(): List<ReadingSession> = withContext(Dispatchers.IO) {
        database.readingSessionDao.getAll().map(ReadingSessionEntity::toModel)
    }

    override suspend fun getBetween(fromDay: Long, toDay: Long): List<ReadingSession> =
        withContext(Dispatchers.IO) {
            database.readingSessionDao.getBetween(fromDay, toDay).map(ReadingSessionEntity::toModel)
        }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        database.readingSessionDao.deleteAll()
        Unit
    }

    private fun ReadingSessionEntity.toModel() = ReadingSession(
        sessionId = sessionId,
        dayEpoch = dayEpoch,
        bookId = bookId,
        bookTitle = bookTitle,
        startedAt = startedAt,
        durationMillis = durationMillis,
        format = format,
        startProgress = startProgress,
        endProgress = endProgress
    )
}