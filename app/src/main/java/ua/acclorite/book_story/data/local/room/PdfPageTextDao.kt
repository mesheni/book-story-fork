/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import ua.acclorite.book_story.data.local.dto.PdfPageTextEntity

@Dao
interface PdfPageTextDao {
    @Query("SELECT * FROM PdfPageTextEntity WHERE bookId = :bookId AND sourceSignature = :sourceSignature ORDER BY pageIndex")
    suspend fun getPages(bookId: Int, sourceSignature: String): List<PdfPageTextEntity>

    @Query("SELECT COUNT(*) FROM PdfPageTextEntity WHERE bookId = :bookId AND sourceSignature = :sourceSignature")
    suspend fun countPages(bookId: Int, sourceSignature: String): Int

    @Query("DELETE FROM PdfPageTextEntity WHERE bookId = :bookId")
    suspend fun deletePagesForBook(bookId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: PdfPageTextEntity)

    @Transaction
    suspend fun replacePagesForBook(bookId: Int, pages: List<PdfPageTextEntity>) {
        deletePagesForBook(bookId)
        pages.forEach { insertPage(it) }
    }
}