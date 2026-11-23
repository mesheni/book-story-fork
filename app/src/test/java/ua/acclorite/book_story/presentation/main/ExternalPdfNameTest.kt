package ua.acclorite.book_story.presentation.main

import org.junit.Assert.assertEquals
import org.junit.Test

class ExternalPdfNameTest {
    @Test fun keepsRealPdfPathAndUsesDisplayNameForPathlessProviders() {
        assertEquals("/books/document.PDF", externalPdfBookPath("/books/document.PDF", "ignored"))
        assertEquals("document.pdf", externalPdfBookPath("", "document.pdf"))
        assertEquals("untitled.pdf", externalPdfBookPath("", "untitled"))
    }
}
