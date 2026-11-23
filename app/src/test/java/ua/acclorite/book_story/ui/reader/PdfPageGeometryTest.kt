package ua.acclorite.book_story.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfPageGeometryTest {
    @Test fun mapsScreenPositionToNormalizedPageCoordinates() {
        val page = PdfPageGeometry(10f, -100f, 400f, 800f)
        val point = page.normalize(110f, 100f)!!
        assertEquals(.25f, point.first, .00001f)
        assertEquals(.25f, point.second, .00001f)
        assertNull(page.normalize(9f, 100f))
        assertNull(page.normalize(110f, 701f))
    }

    @Test fun doesNotMapEmptyPages() {
        assertNull(PdfPageGeometry(0f, 0f, 0f, 100f).normalize(0f, 0f))
    }
}
