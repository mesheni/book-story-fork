/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.repository

import ua.acclorite.book_story.domain.model.reader.PdfPageText

interface PdfPageTextRepository {
    /**
     * @param onProgress extracted page share in 0..1, reported only while
     * the text index is actually being built (not on cache hits).
     */
    suspend fun getPageTexts(bookId: Int, onProgress: (Float) -> Unit = {}): Result<List<PdfPageText>>
}