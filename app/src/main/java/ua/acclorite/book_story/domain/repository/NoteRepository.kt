/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.repository

import kotlinx.coroutines.flow.Flow
import ua.acclorite.book_story.domain.model.note.Note

interface NoteRepository {
    fun getNotesForBook(bookId: Int): Flow<List<Note>>

    suspend fun insertNote(note: Note): Result<Long>

    suspend fun updateNote(note: Note): Result<Unit>

    suspend fun deleteNote(noteId: Int): Result<Unit>

    suspend fun deleteNotesForBook(bookId: Int): Result<Unit>
}
