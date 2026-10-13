/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.model.reader

import androidx.compose.runtime.Immutable

/**
 * A full-text search hit inside a text-format book:
 * [listItemIndex] points at the LazyColumn item to scroll to.
 */
@Immutable
data class TextSearchResult(
    val listItemIndex: Int,
    val chapterTitle: String?,
    val excerpt: String
)
