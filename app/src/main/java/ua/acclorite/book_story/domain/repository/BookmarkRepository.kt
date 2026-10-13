/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.repository

import kotlinx.coroutines.flow.Flow
import ua.acclorite.book_story.domain.model.bookmark.Bookmark

interface BookmarkRepository {
    fun getBookmarksForBook(bookId: Int): Flow<List<Bookmark>>

    suspend fun insertBookmark(bookmark: Bookmark): Result<Long>

    suspend fun deleteBookmark(bookmarkId: Int): Result<Unit>

    suspend fun deleteBookmarksForBook(bookId: Int): Result<Unit>
}
