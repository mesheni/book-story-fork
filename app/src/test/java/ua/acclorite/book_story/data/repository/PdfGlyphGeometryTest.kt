package ua.acclorite.book_story.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfGlyphGeometryTest {
    @Test fun rotatesUnrotatedTextInsideCroppedPage() {
        val rect = mapPdfGlyph(80f, 162f, 60f, 20f, 572f, 732f, 0, 90)
        assertEquals(570f / 732f, rect.left, .0001f)
        assertEquals(80f / 572f, rect.top, .0001f)
        assertEquals(590f / 732f, rect.right, .0001f)
        assertEquals(140f / 572f, rect.bottom, .0001f)
    }

    @Test fun textRotatedWithPageKeepsSameVisualGeometry() {
        val rect = mapPdfGlyph(570f, 100f, 60f, 20f, 572f, 732f, 90, 90)
        assertEquals(570f / 732f, rect.left, .0001f)
        assertEquals(80f / 572f, rect.top, .0001f)
        assertEquals(630f / 732f, rect.right, .0001f)
        assertEquals(100f / 572f, rect.bottom, .0001f)
    }

    @Test fun mapsOtherPageRotations() {
        val zero = mapPdfGlyph(80f, 162f, 60f, 20f, 572f, 732f, 0, 0)
        assertEquals(80f / 572f, zero.left, .0001f)
        assertEquals(142f / 732f, zero.top, .0001f)
        val half = mapPdfGlyph(80f, 162f, 60f, 20f, 572f, 732f, 0, 180)
        assertEquals(432f / 572f, half.left, .0001f)
        assertEquals(570f / 732f, half.top, .0001f)
        val threeQuarters = mapPdfGlyph(80f, 162f, 60f, 20f, 572f, 732f, 0, 270)
        assertEquals(142f / 732f, threeQuarters.left, .0001f)
        assertEquals(432f / 572f, threeQuarters.top, .0001f)
    }
}
