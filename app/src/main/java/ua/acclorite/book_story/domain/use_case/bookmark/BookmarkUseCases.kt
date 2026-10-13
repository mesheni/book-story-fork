/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.bookmark

import kotlinx.coroutines.flow.Flow
import ua.acclorite.book_story.domain.model.bookmark.Bookmark
import ua.acclorite.book_story.domain.repository.BookmarkRepository
import javax.inject.Inject

class GetBookmarksUseCase @Inject constructor(
    private val repository: BookmarkRepository
) {
    operator fun invoke(bookId: Int): Flow<List<Bookmark>> = repository.getBookmarksForBook(bookId)
}

class AddBookmarkUseCase @Inject constructor(
    private val repository: BookmarkRepository
) {
    suspend operator fun invoke(bookmark: Bookmark): Result<Long> = repository.insertBookmark(bookmark)
}

class DeleteBookmarkUseCase @Inject constructor(
    private val repository: BookmarkRepository
) {
    suspend operator fun invoke(bookmarkId: Int): Result<Unit> = repository.deleteBookmark(bookmarkId)
}
