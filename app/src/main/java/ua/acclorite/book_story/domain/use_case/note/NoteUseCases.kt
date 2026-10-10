/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.note

import kotlinx.coroutines.flow.Flow
import ua.acclorite.book_story.domain.model.note.Note
import ua.acclorite.book_story.domain.repository.NoteRepository
import javax.inject.Inject

class GetNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(bookId: Int): Flow<List<Note>> = repository.getNotesForBook(bookId)
}

class InsertNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note): Result<Long> = repository.insertNote(note)
}

class UpdateNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note): Result<Unit> = repository.updateNote(note)
}

class DeleteNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(noteId: Int): Result<Unit> = repository.deleteNote(noteId)
}
