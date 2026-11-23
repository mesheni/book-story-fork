package ua.acclorite.book_story.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfThumbnailSizeTest {
    @Test fun preservesAspectRatioAndCapsLongEdge() {
        assertEquals(120 to 240, pdfThumbnailSize(300, 600, 240))
        assertEquals(240 to 120, pdfThumbnailSize(600, 300, 240))
    }

    @Test fun rejectsInvalidPageDimensions() {
        assertNull(pdfThumbnailSize(0, 300, 240))
        assertNull(pdfThumbnailSize(300, 300, 0))
    }
}
