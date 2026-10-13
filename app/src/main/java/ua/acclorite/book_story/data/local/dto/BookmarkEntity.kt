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
    indices = [Index("bookId")]
)
data class BookmarkEntity(
    @PrimaryKey(true) val id: Int = 0,
    val bookId: Int,
    val createdAt: Long,
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
    val progress: Float = 0f,
    val previewText: String = "",
    val chapterTitle: String? = null,
    val pageIndex: Int? = null
)
