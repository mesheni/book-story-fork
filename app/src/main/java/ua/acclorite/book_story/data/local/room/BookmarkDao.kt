/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ua.acclorite.book_story.data.local.dto.BookmarkEntity

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM BookmarkEntity WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getBookmarksForBook(bookId: Int): Flow<List<BookmarkEntity>>

    @Insert
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM BookmarkEntity WHERE id = :id")
    suspend fun deleteBookmark(id: Int)

    @Query("DELETE FROM BookmarkEntity WHERE bookId = :bookId")
    suspend fun deleteBookmarksForBook(bookId: Int)
}
