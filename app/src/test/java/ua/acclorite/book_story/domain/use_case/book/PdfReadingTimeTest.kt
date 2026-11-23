package ua.acclorite.book_story.domain.use_case.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ua.acclorite.book_story.domain.model.reader.PdfPageText

class PdfReadingTimeTest {
    @Test fun estimatesUntilNextChapterPage() {
        val pages = listOf(PdfPageText(0, "one two"), PdfPageText(1, "three four"), PdfPageText(2, "five six"))
        assertEquals(ReadingTimeEstimate(2, true), estimatePdfReadingTime(pages, listOf(0, 2), 0, 2))
        assertEquals(ReadingTimeEstimate(1, true), estimatePdfReadingTime(pages, listOf(0, 2), 1, 2))
    }

    @Test fun withoutOutlineEstimatesUntilEndOfBook() {
        val pages = listOf(PdfPageText(0, "one two"), PdfPageText(1, "three four"))
        assertEquals(ReadingTimeEstimate(2, false), estimatePdfReadingTime(pages, emptyList(), 0, 2))
    }

    @Test fun noEstimateForScannedOrMissingPages() {
        assertNull(estimatePdfReadingTime(listOf(PdfPageText(0, "")), listOf(0), 0, 200))
        assertNull(estimatePdfReadingTime(emptyList(), emptyList(), 0, 200))
        assertNull(estimatePdfReadingTime(listOf(PdfPageText(0, ""), PdfPageText(1, "Extractable text")), emptyList(), 0, 200))
    }
}
