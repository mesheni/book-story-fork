/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.local.dto

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [Index(value = ["sessionId", "dayEpoch"], unique = true)]
)
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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