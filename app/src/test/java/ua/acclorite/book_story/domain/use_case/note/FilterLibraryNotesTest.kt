package ua.acclorite.book_story.domain.use_case.note

import org.junit.Assert.assertEquals
import org.junit.Test
import androidx.compose.ui.graphics.Color
import ua.acclorite.book_story.domain.model.note.Note

class FilterLibraryNotesTest {
    @Test fun findsTextOrCommentWithoutCaseSensitivity() {
        val notes = listOf(
            Note(id = 1, bookId = 1, selectedText = "First Quote"),
            Note(id = 2, bookId = 2, selectedText = "Other", note = "My QUOTE"),
            Note(id = 3, bookId = 1, selectedText = "Different")
        )
        assertEquals(listOf(1, 2), filterLibraryNotes(notes, " quote ", null).map { it.id })
        assertEquals(listOf(2), filterLibraryNotes(notes, "quote", 2).map { it.id })
    }

    @Test fun emptySearchIncludesAllNotes() {
        val notes = listOf(Note(id = 1, selectedText = "Text"))
        assertEquals(notes, filterLibraryNotes(notes, " ", null))
    }

    @Test fun combinesColorBookAndSearchFilters() {
        val notes = listOf(
            Note(id = 1, bookId = 1, selectedText = "Quote", color = Color.Red),
            Note(id = 2, bookId = 1, selectedText = "Quote", color = Color.Blue),
            Note(id = 3, bookId = 2, selectedText = "Quote", color = Color.Red),
            Note(id = 4, bookId = 1, selectedText = "Other", color = Color.Red)
        )
        assertEquals(listOf(1), filterLibraryNotes(notes, "quote", 1, Color.Red).map { it.id })
    }
}
