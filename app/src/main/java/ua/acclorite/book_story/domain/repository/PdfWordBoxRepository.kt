/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.repository

import ua.acclorite.book_story.domain.model.reader.PdfWord

interface PdfWordBoxRepository {
    /**
     * Word boxes of a single page, normalized to the page size.
     * An empty list means the page has no text layer.
     */
    suspend fun getWordBoxes(bookId: Int, pageIndex: Int): Result<List<PdfWord>>

    /** Releases the cached document of a book; call when the reader closes. */
    suspend fun close(bookId: Int)
}
