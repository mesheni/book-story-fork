package ua.acclorite.book_story.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfNavigationStateTest {
    @Test fun latestRequestIsAppliedAfterOpening() {
        val state = PdfNavigationState(0)
        assertNull(state.request(3))
        assertNull(state.request(8))
        assertEquals(8, state.finishLoading(10))
        assertEquals(2, state.request(2))
    }

    @Test fun clampsRestoredPageToDocument() {
        val state = PdfNavigationState(20)
        assertEquals(2, state.finishLoading(3))
        state.startLoading(1)
        assertEquals(1, state.finishLoading(3))
    }
}
