/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.mapper.bookmark

import ua.acclorite.book_story.data.local.dto.BookmarkEntity
import ua.acclorite.book_story.domain.model.bookmark.Bookmark
import javax.inject.Inject

interface BookmarkMapper {
    fun toBookmarkEntity(bookmark: Bookmark): BookmarkEntity
    fun toBookmark(entity: BookmarkEntity): Bookmark
}

class BookmarkMapperImpl @Inject constructor() : BookmarkMapper {

    override fun toBookmarkEntity(bookmark: Bookmark): BookmarkEntity {
        return BookmarkEntity(
            id = bookmark.id,
            bookId = bookmark.bookId,
            createdAt = bookmark.createdAt,
            scrollIndex = bookmark.scrollIndex,
            scrollOffset = bookmark.scrollOffset,
            progress = bookmark.progress,
            previewText = bookmark.previewText,
            chapterTitle = bookmark.chapterTitle,
            pageIndex = bookmark.pageIndex
        )
    }

    override fun toBookmark(entity: BookmarkEntity): Bookmark {
        return Bookmark(
            id = entity.id,
            bookId = entity.bookId,
            createdAt = entity.createdAt,
            scrollIndex = entity.scrollIndex,
            scrollOffset = entity.scrollOffset,
            progress = entity.progress,
            previewText = entity.previewText,
            chapterTitle = entity.chapterTitle,
            pageIndex = entity.pageIndex
        )
    }
}
