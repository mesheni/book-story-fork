/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ua.acclorite.book_story.data.local.dto.NoteEntity

@Dao
interface NoteDao {
    @Query("SELECT * FROM NoteEntity WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getNotesForBook(bookId: Int): Flow<List<NoteEntity>>

    @Insert
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM NoteEntity WHERE id = :id")
    suspend fun deleteNote(id: Int)

    @Query("DELETE FROM NoteEntity WHERE bookId = :bookId")
    suspend fun deleteNotesForBook(bookId: Int)
}
