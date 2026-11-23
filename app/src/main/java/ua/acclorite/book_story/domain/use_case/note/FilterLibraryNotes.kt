package ua.acclorite.book_story.domain.use_case.note

import ua.acclorite.book_story.domain.model.note.Note
import androidx.compose.ui.graphics.Color

fun filterLibraryNotes(notes: List<Note>, query: String, bookId: Int?, color: Color? = null): List<Note> {
    val term = query.trim()
    return notes.filter { note ->
        (bookId == null || note.bookId == bookId) && (color == null || note.color == color) &&
            (term.isEmpty() || note.selectedText.contains(term, ignoreCase = true) ||
                note.note.contains(term, ignoreCase = true))
    }
}
