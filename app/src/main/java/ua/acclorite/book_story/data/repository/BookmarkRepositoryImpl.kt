/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.data.local.room.BookDatabase
import ua.acclorite.book_story.data.mapper.bookmark.BookmarkMapper
import ua.acclorite.book_story.domain.model.bookmark.Bookmark
import ua.acclorite.book_story.domain.repository.BookmarkRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val database: BookDatabase,
    private val bookmarkMapper: BookmarkMapper
) : BookmarkRepository {

    override fun getBookmarksForBook(bookId: Int): Flow<List<Bookmark>> {
        return database.bookmarkDao.getBookmarksForBook(bookId)
            .map { entities -> entities.map { bookmarkMapper.toBookmark(it) } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun insertBookmark(bookmark: Bookmark): Result<Long> = runCatching {
        withContext(Dispatchers.IO) {
            database.bookmarkDao.insertBookmark(bookmarkMapper.toBookmarkEntity(bookmark))
        }
    }

    override suspend fun deleteBookmark(bookmarkId: Int): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            database.bookmarkDao.deleteBookmark(bookmarkId)
        }
    }

    override suspend fun deleteBookmarksForBook(bookId: Int): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            database.bookmarkDao.deleteBookmarksForBook(bookId)
        }
    }
}
