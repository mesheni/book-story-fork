/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.book

import ua.acclorite.book_story.domain.model.reader.PdfWord
import ua.acclorite.book_story.domain.repository.PdfWordBoxRepository
import javax.inject.Inject

class GetPdfWordBoxesUseCase @Inject constructor(
    private val repository: PdfWordBoxRepository
) {
    suspend operator fun invoke(
        bookId: Int,
        sessionId: String,
        pageIndex: Int
    ): Result<List<PdfWord>> = repository.getWordBoxes(bookId, sessionId, pageIndex)
}

class ClosePdfWordBoxesUseCase @Inject constructor(
    private val repository: PdfWordBoxRepository
) {
    suspend operator fun invoke(sessionId: String) = repository.close(sessionId)
}
