package ua.acclorite.book_story

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StaleCacheFileTest {
    @Test
    fun `book copies and viewer fallbacks are stale, exports are kept`() {
        assertTrue(isStaleCacheFile("3f2b1c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d"))
        assertTrue(isStaleCacheFile("pdf-view-123456.pdf"))
        assertFalse(isStaleCacheFile("notes"))
        assertFalse(isStaleCacheFile("statistics.csv"))
        assertFalse(isStaleCacheFile("3f2b1c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d.pdf"))
    }
}
