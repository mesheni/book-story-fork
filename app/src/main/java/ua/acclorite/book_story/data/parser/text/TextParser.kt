/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.text

import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.domain.model.reader.ReaderText

interface TextParser {
    /**
     * @param onProgress parsed document share in 0..1, reported only
     * by parsers that extract text per page (currently PDF).
     */
    suspend fun parse(cachedFile: CachedFile, onProgress: (Float) -> Unit = {}): List<ReaderText>
}