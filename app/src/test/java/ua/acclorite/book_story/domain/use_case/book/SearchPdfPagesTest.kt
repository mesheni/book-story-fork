/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.acclorite.book_story.domain.model.reader.PdfPageText

class SearchPdfPagesTest {
    @Test
    fun findsRussianAndEnglishMatchesIgnoringCase() {
        val results = findMatches(
            listOf(
                PdfPageText(0, "Начало. КНИГА о море."),
                PdfPageText(1, "A BOOK about the sea."),
                PdfPageText(2, "No match here")
            ),
            "книга"
        )

        assertEquals(listOf(0), results.map { it.pageIndex })
        assertTrue(results.single().excerpt.contains("КНИГА"))
    }

    @Test
    fun returnsEmptyForBlankQueryAndNoMatches() {
        assertTrue(findMatches(listOf(PdfPageText(0, "Text")), "  ").isEmpty())
        assertTrue(findMatches(listOf(PdfPageText(0, "Text")), "missing").isEmpty())
    }

    @Test
    fun excerptIncludesContextAndFlattensLineBreaks() {
        val results = findMatches(
            listOf(PdfPageText(7, "Prefix\nImportant phrase\nSuffix")),
            "important"
        )

        assertEquals(7, results.single().pageIndex)
        assertTrue(results.single().excerpt.contains("Prefix Important phrase Suffix"))
    }
}