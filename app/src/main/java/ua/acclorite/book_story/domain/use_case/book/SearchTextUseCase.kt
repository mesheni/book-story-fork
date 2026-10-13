/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.book

import kotlinx.coroutines.yield
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.domain.model.reader.ReaderText.Chapter
import ua.acclorite.book_story.domain.model.reader.TextSearchResult
import javax.inject.Inject

private const val RESULTS_LIMIT = 200
private const val EXCERPT_PADDING = 40

/**
 * Case-insensitive full-text search over the already parsed
 * book text (EPUB / FB2 / TXT / HTML / MD).
 */
class SearchTextUseCase @Inject constructor() {

    suspend operator fun invoke(text: List<ReaderText>, query: String): List<TextSearchResult> {
        if (query.isBlank() || text.isEmpty()) return emptyList()

        val results = mutableListOf<TextSearchResult>()
        var chapterTitle: String? = null

        text.forEachIndexed { index, item ->
            yield()

            when (item) {
                is Chapter -> chapterTitle = item.title

                is ReaderText.Text -> {
                    val plain = item.line.text
                    val match = plain.indexOf(query, ignoreCase = true)
                    if (match >= 0) {
                        results.add(
                            TextSearchResult(
                                listItemIndex = index,
                                chapterTitle = chapterTitle,
                                excerpt = excerpt(plain, match, query.length)
                            )
                        )
                        if (results.size >= RESULTS_LIMIT) return results
                    }
                }

                else -> Unit
            }
        }

        return results
    }

    private fun excerpt(plain: String, match: Int, queryLength: Int): String {
        val start = (match - EXCERPT_PADDING).coerceAtLeast(0)
        val end = (match + queryLength + EXCERPT_PADDING).coerceAtMost(plain.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < plain.length) "…" else ""
        return prefix + plain.substring(start, end).trim() + suffix
    }
}
