/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.model.reader

data class PdfPageText(
    val pageIndex: Int,
    val text: String
)

data class PdfSearchResult(
    val pageIndex: Int,
    val excerpt: String
)