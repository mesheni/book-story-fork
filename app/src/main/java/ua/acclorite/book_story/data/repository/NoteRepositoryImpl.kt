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
import ua.acclorite.book_story.data.mapper.note.NoteMapper
import ua.acclorite.book_story.domain.model.note.Note
import ua.acclorite.book_story.domain.repository.NoteRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val database: BookDatabase,
    private val noteMapper: NoteMapper
) : NoteRepository {

    override fun getNotesForBook(bookId: Int): Flow<List<Note>> {
        return database.noteDao.getNotesForBook(bookId)
            .map { entities -> entities.map { noteMapper.toNote(it) } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun insertNote(note: Note): Result<Long> = runCatching {
        withContext(Dispatchers.IO) {
            database.noteDao.insertNote(noteMapper.toNoteEntity(note))
        }
    }

    override suspend fun updateNote(note: Note): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            database.noteDao.updateNote(noteMapper.toNoteEntity(note))
        }
    }

    override suspend fun deleteNote(noteId: Int): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            database.noteDao.deleteNote(noteId)
        }
    }

    override suspend fun deleteNotesForBook(bookId: Int): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            database.noteDao.deleteNotesForBook(bookId)
        }
    }
}
