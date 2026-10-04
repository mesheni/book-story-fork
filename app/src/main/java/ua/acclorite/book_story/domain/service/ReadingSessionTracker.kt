/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.service

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.model.statistics.ReadingSession
import ua.acclorite.book_story.domain.repository.ReadingStatisticsRepository
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val IDLE_TIMEOUT_MILLIS = 120_000L
private const val CHECKPOINT_INTERVAL_MILLIS = 15_000L

@Singleton
class ReadingSessionTracker @Inject constructor(
    private val repository: ReadingStatisticsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var book: Book? = null
    private var sessionId: String? = null
    private var format = ""
    private var startProgress = 0f
    private var endProgress = 0f
    private var foreground = false
    private var enabled = true
    private var readerStarted = false
    private var readerReady = false
    private var lastActivityElapsed = 0L
    private var lastCheckpointElapsed = 0L
    private var lastWallTime = 0L
    private val dayDurations = mutableMapOf<Long, Long>()
    private val dayStarts = mutableMapOf<Long, Long>()

    init {
        scope.launch {
            while (isActive) {
                delay(CHECKPOINT_INTERVAL_MILLIS)
                mutex.withLock { checkpointLocked() }
            }
        }
    }

    suspend fun startReader(book: Book, format: String, enabled: Boolean) = mutex.withLock {
        val previousEnabled = this.enabled
        if (this.book?.id == book.id && sessionId != null) {
            if (!enabled && this.enabled) {
                checkpointLocked()
                readerStarted = false
            } else if (!previousEnabled && foreground && readerReady) {
                readerStarted = true
                sessionId = UUID.randomUUID().toString()
                startProgress = endProgress
                dayDurations.clear()
                dayStarts.clear()
                val now = SystemClock.elapsedRealtime()
                lastActivityElapsed = now
                lastCheckpointElapsed = now
                lastWallTime = System.currentTimeMillis()
            }
            this.enabled = enabled
            return@withLock
        }

        checkpointLocked()
        resetLocked()
        this.book = book
        this.format = format
        this.enabled = enabled
        readerReady = true
        readerStarted = enabled
        startProgress = progressFor(book)
        endProgress = startProgress
        sessionId = UUID.randomUUID().toString()
        val now = SystemClock.elapsedRealtime()
        lastActivityElapsed = now
        lastCheckpointElapsed = now
        lastWallTime = System.currentTimeMillis()
    }

    suspend fun setForeground(isForeground: Boolean) = mutex.withLock {
        if (foreground == isForeground) return@withLock
        if (!isForeground) checkpointLocked()
        foreground = isForeground
        val now = SystemClock.elapsedRealtime()
        if (!isForeground && readerStarted) {
            readerStarted = false
            sessionId = null
            dayDurations.clear()
            dayStarts.clear()
            lastCheckpointElapsed = now
        }
        if (
            isForeground && readerReady && book != null && enabled && !readerStarted
        ) {
            readerStarted = true
            sessionId = UUID.randomUUID().toString()
            startProgress = endProgress
            dayDurations.clear()
            dayStarts.clear()
            lastActivityElapsed = now
            lastCheckpointElapsed = now
            lastWallTime = System.currentTimeMillis()
        }
    }

    suspend fun activity(progress: Float? = null) = mutex.withLock {
        if (book == null || !readerReady || !enabled || !foreground) return@withLock
        if (!readerStarted) {
            readerStarted = true
            sessionId = UUID.randomUUID().toString()
            dayDurations.clear()
            dayStarts.clear()
            startProgress = endProgress
            val now = SystemClock.elapsedRealtime()
            lastActivityElapsed = now
            lastCheckpointElapsed = now
            lastWallTime = System.currentTimeMillis()
        }
        checkpointLocked()
        if (progress != null && progress.isFinite()) endProgress = progress.coerceIn(0f, 1f)
        lastActivityElapsed = SystemClock.elapsedRealtime()
        lastCheckpointElapsed = lastActivityElapsed
        lastWallTime = System.currentTimeMillis()
    }

    suspend fun setEnabled(enabled: Boolean) = mutex.withLock {
        val wasEnabled = this.enabled
        checkpointLocked()
        this.enabled = enabled
        val now = SystemClock.elapsedRealtime()
        lastCheckpointElapsed = now
        lastWallTime = System.currentTimeMillis()
        if (enabled && !wasEnabled && foreground && readerReady && book != null) {
            readerStarted = true
            sessionId = UUID.randomUUID().toString()
            dayDurations.clear()
            dayStarts.clear()
            startProgress = endProgress
            lastActivityElapsed = now
            lastWallTime = System.currentTimeMillis()
        } else if (!enabled) {
            readerStarted = false
            sessionId = null
            dayDurations.clear()
            dayStarts.clear()
        }
    }

    suspend fun stopReader() = mutex.withLock {
        checkpointLocked()
        resetLocked()
    }

    suspend fun clearStatistics() = mutex.withLock {
        repository.clear()
        if (book != null && readerStarted) {
            sessionId = UUID.randomUUID().toString()
            dayDurations.clear()
            dayStarts.clear()
            val now = SystemClock.elapsedRealtime()
            lastActivityElapsed = now
            lastCheckpointElapsed = now
            lastWallTime = System.currentTimeMillis()
            startProgress = endProgress
        }
    }

    private suspend fun checkpointLocked() {
        val currentBook = book ?: return
        val currentSessionId = sessionId ?: return
        val nowElapsed = SystemClock.elapsedRealtime()
        val from = lastCheckpointElapsed
        val until = minOf(nowElapsed, lastActivityElapsed + IDLE_TIMEOUT_MILLIS)
        if (readerStarted && foreground && enabled && until > from) {
            val duration = until - from
            val intervalStartWall = lastWallTime
            addDurationByLocalDay(intervalStartWall, duration)
            dayDurations.forEach { (day, dayDuration) ->
                repository.save(
                    ReadingSession(
                        sessionId = currentSessionId,
                        dayEpoch = day,
                        bookId = currentBook.id,
                        bookTitle = currentBook.title,
                        startedAt = dayStarts[day] ?: intervalStartWall,
                        durationMillis = dayDuration,
                        format = format,
                        startProgress = startProgress,
                        endProgress = endProgress
                    )
                )
            }
            lastCheckpointElapsed = until
            lastWallTime += duration
        } else if (readerStarted && foreground && enabled && until <= from) {
            lastCheckpointElapsed = nowElapsed
            lastWallTime = System.currentTimeMillis()
        }
        if (readerStarted && nowElapsed >= lastActivityElapsed + IDLE_TIMEOUT_MILLIS) {
            readerStarted = false
        }
    }

    private fun addDurationByLocalDay(startWall: Long, duration: Long) {
        val zone = ZoneId.systemDefault()
        var cursor = startWall
        var remaining = duration
        while (remaining > 0) {
            val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
            val dayEpoch = date.toEpochDay()
            val nextMidnight = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val portion = minOf(remaining, (nextMidnight - cursor).coerceAtLeast(1L))
            dayStarts.putIfAbsent(dayEpoch, cursor)
            dayDurations[dayEpoch] = (dayDurations[dayEpoch] ?: 0L) + portion
            remaining -= portion
            cursor += portion
        }
    }

    private fun resetLocked() {
        book = null
        readerReady = false
        readerStarted = false
        sessionId = null
        format = ""
        startProgress = 0f
        endProgress = 0f
        lastActivityElapsed = 0L
        lastCheckpointElapsed = 0L
        lastWallTime = 0L
        intervalWallOffset = 0L
        dayDurations.clear()
        dayStarts.clear()
    }

    private fun progressFor(book: Book): Float = when {
        book.filePath.endsWith(".pdf", true) && book.pdfOpenMode?.name == "NATIVE_PDF" -> book.pdfProgress
        else -> book.progress
    }
}