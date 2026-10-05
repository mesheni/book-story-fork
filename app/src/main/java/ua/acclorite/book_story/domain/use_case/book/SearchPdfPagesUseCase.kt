/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.book

import ua.acclorite.book_story.domain.model.reader.PdfSearchResult
import ua.acclorite.book_story.domain.model.reader.PdfPageText
import javax.inject.Inject

class SearchPdfPagesUseCase @Inject constructor(
    private val getPdfPageTexts: GetPdfPageTextsUseCase
) {
    suspend operator fun invoke(bookId: Int, query: String): Result<List<PdfSearchResult>> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return Result.success(emptyList())

        return getPdfPageTexts(bookId).map { pages -> findMatches(pages, normalizedQuery) }
    }
}

internal fun findMatches(pages: List<PdfPageText>, query: String): List<PdfSearchResult> {
    if (query.isBlank()) return emptyList()

    return pages.mapNotNull { page ->
        val matchIndex = page.text.indexOf(query, ignoreCase = true)
        if (matchIndex < 0) return@mapNotNull null

        val start = (matchIndex - 48).coerceAtLeast(0)
        val end = (matchIndex + query.length + 72).coerceAtMost(page.text.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < page.text.length) "…" else ""
        PdfSearchResult(
            pageIndex = page.pageIndex,
            excerpt = prefix + page.text.substring(start, end).replace('\n', ' ') + suffix
        )
    }
}