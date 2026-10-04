/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.model.statistics

data class ReadingSession(
    val sessionId: String,
    val dayEpoch: Long,
    val bookId: Int?,
    val bookTitle: String,
    val startedAt: Long,
    val durationMillis: Long,
    val format: String,
    val startProgress: Float,
    val endProgress: Float
)