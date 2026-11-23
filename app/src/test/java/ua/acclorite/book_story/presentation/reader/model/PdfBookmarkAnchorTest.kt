package ua.acclorite.book_story.presentation.reader.model

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ua.acclorite.book_story.domain.model.bookmark.Bookmark
import ua.acclorite.book_story.domain.model.reader.ReaderText

class PdfBookmarkAnchorTest {
    private fun text(vararg values: String) = values.map { ReaderText.Text(AnnotatedString(it)) }

    @Test fun keepsOriginalPositionWhenPreviewStillMatches() {
        assertEquals(1 to 50, resolvePdfBookmark(Bookmark(scrollIndex = 1, scrollOffset = 50, previewText = "same"), text("same", "same")))
    }

    @Test fun movesOnlyUniqueStalePreviewAndResetsOffset() {
        val bookmark = Bookmark(scrollIndex = 0, scrollOffset = 50, previewText = "target")
        assertEquals(1 to 0, resolvePdfBookmark(bookmark, text("new", "target")))
        assertNull(resolvePdfBookmark(bookmark, text("new", "target", "target")))
    }

    @Test fun newBookmarkOnSeparatorUsesNearbyReadableItem() {
        val items = listOf(ReaderText.Separator, ReaderText.Text(AnnotatedString("next")))
        assertEquals(1 to 0, pdfBookmarkPosition(items, 0, 30))
    }
}
