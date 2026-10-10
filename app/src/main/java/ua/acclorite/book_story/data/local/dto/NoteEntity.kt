/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.local.dto

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Reader note with a highlight anchor.
 *
 * Text books anchor to char ranges inside LazyColumn items
 * ([segmentsJson]); native PDF pages anchor to normalized
 * (0..1) page rectangles ([rectsJson]).
 */
@Entity(
    indices = [Index("bookId")]
)
data class NoteEntity(
    @PrimaryKey(true) val id: Int = 0,
    val bookId: Int,
    val selectedText: String,
    val note: String,
    val color: Int,
    val chapterIndex: Int? = null,
    val segmentsJson: String? = null,
    val pageIndex: Int? = null,
    val rectsJson: String? = null,
    val createdAt: Long
)
