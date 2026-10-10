/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.model.note

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Highlight anchor for text books: an exclusive char range
 * inside a single LazyColumn item (paragraph).
 */
data class NoteSegment(
    val listItemIndex: Int,
    val start: Int,
    val end: Int
)

/**
 * Highlight anchor for native PDF pages: a rectangle
 * normalized to the page size (each coordinate in 0..1).
 */
data class NoteRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@Immutable
data class Note(
    val id: Int = 0,
    val bookId: Int = 0,
    val selectedText: String,
    val note: String = "",
    val color: Color = DEFAULT_COLOR,
    val chapterIndex: Int? = null,
    val segments: List<NoteSegment> = emptyList(),
    val pageIndex: Int? = null,
    val rects: List<NoteRect> = emptyList(),
    val createdAt: Long = 0L
) {
    val anchoredToText: Boolean get() = segments.isNotEmpty()
    val anchoredToPdf: Boolean get() = pageIndex != null && rects.isNotEmpty()

    companion object {
        val DEFAULT_COLOR: Color = Color(0xFFFDD835)

        /** Fixed highlight palette, the first one is the default. */
        val COLORS = listOf(
            Color(0xFFFDD835), // yellow
            Color(0xFFA5D6A7), // green
            Color(0xFF90CAF9), // blue
            Color(0xFFF48FB1), // pink
            Color(0xFFCE93D8)  // purple
        )
    }
}
