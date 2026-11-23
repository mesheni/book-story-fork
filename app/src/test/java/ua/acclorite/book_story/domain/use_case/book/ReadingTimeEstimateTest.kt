package ua.acclorite.book_story.domain.use_case.book

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ua.acclorite.book_story.domain.model.reader.ReaderText

class ReadingTimeEstimateTest {
    private fun paragraph(text: String) = ReaderText.Text(AnnotatedString(text))
    private fun chapter(title: String) = ReaderText.Chapter(title = title, nested = false)

    @Test fun stopsAtNextChapter() {
        val text = listOf(chapter("One"), paragraph("one two three four"), chapter("Two"), paragraph("ignored words"))
        assertEquals(2, estimateReadingTime(text, 1, 2)?.minutes)
        assertEquals(true, estimateReadingTime(text, 1, 2)?.isChapter)
    }

    @Test fun fallsBackToEndOfBookWithoutChapters() {
        val text = listOf(paragraph("one two"), paragraph("three four"))
        assertEquals(2, estimateReadingTime(text, 0, 2)?.minutes)
        assertEquals(false, estimateReadingTime(text, 0, 2)?.isChapter)
    }

    @Test fun noEstimateForEmptyTextOrInvalidSpeed() {
        assertNull(estimateReadingTime(emptyList(), 0, 200))
        assertNull(estimateReadingTime(listOf(paragraph("word")), 0, 0))
    }

    @Test fun beforeFirstChapterEstimatesUntilEndOfBook() {
        val text = listOf(paragraph("one two"), chapter("Chapter"), paragraph("three four"))
        assertEquals(2, estimateReadingTime(text, 0, 2)?.minutes)
        assertEquals(false, estimateReadingTime(text, 0, 2)?.isChapter)
    }
}
