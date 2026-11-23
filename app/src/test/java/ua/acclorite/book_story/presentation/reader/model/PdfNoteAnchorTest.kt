package ua.acclorite.book_story.presentation.reader.model

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.acclorite.book_story.domain.model.note.NoteSegment
import ua.acclorite.book_story.domain.model.reader.ReaderText

class PdfNoteAnchorTest {
    private fun text(vararg paragraphs: String) = paragraphs.map { ReaderText.Text(AnnotatedString(it)) }

    @Test fun keepsMatchingOriginalEvenWhenTextRepeats() {
        val original = listOf(NoteSegment(1, 0, 5))
        assertEquals(original, NoteAnchorMatcher.resolveExisting("hello", text("hello", "hello"), original))
    }

    @Test fun movesOnlyUniqueStaleAnchor() {
        val original = listOf(NoteSegment(0, 0, 5))
        assertEquals(listOf(NoteSegment(1, 0, 5)), NoteAnchorMatcher.resolveExisting("hello", text("new", "hello"), original))
        assertTrue(NoteAnchorMatcher.resolveExisting("hello", text("new", "hello", "hello"), original).isEmpty())
    }
}
