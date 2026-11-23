package ua.acclorite.book_story.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfPageInputTest {
    @Test fun acceptsOneBasedPagesAndReturnsZeroBasedIndex() {
        assertEquals(0, parsePdfPage("1", 12))
        assertEquals(11, parsePdfPage("12", 12))
    }

    @Test fun rejectsInvalidOrOutOfRangePages() {
        listOf("", "0", "13", "-1", "9999999999999999999", "1.5").forEach {
            assertNull(parsePdfPage(it, 12))
        }
        assertNull(parsePdfPage("1", 0))
    }
}
