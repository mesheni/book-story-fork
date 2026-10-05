/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.book

import ua.acclorite.book_story.domain.model.reader.PdfPageText
import ua.acclorite.book_story.domain.repository.PdfPageTextRepository
import javax.inject.Inject

class GetPdfPageTextsUseCase @Inject constructor(
    private val repository: PdfPageTextRepository
) {
    suspend operator fun invoke(bookId: Int): Result<List<PdfPageText>> =
        repository.getPageTexts(bookId)
}