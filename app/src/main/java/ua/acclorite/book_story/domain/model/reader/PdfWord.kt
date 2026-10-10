/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.model.reader

import androidx.compose.runtime.Immutable

/**
 * A PDF word with its bounding box normalized to the page size
 * (each coordinate in 0..1), in reading order.
 */
@Immutable
data class PdfWord(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)
