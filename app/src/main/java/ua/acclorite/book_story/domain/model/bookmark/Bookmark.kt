/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.model.bookmark

import androidx.compose.runtime.Immutable

/**
 * Saved reading position. Text books anchor to a LazyColumn
 * item ([scrollIndex] + [scrollOffset]); the native PDF viewer
 * anchors to [pageIndex].
 */
@Immutable
data class Bookmark(
    val id: Int = 0,
    val bookId: Int = 0,
    val createdAt: Long = 0L,
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
    val progress: Float = 0f,
    val previewText: String = "",
    val chapterTitle: String? = null,
    val pageIndex: Int? = null
) {
    val anchoredToPdf: Boolean get() = pageIndex != null
}
