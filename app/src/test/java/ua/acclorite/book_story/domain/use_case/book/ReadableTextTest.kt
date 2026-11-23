package ua.acclorite.book_story.domain.use_case.book

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.acclorite.book_story.domain.model.reader.ReaderText

class ReadableTextTest {
    @Test fun pdfWithSingleParagraphIsReadable() {
        assertTrue(hasReadableText(listOf(ReaderText.Chapter(title = "Only paragraph", nested = false)), true))
    }

    @Test fun emptyOrChapterOnlyOtherBooksAreRejected() {
        assertFalse(hasReadableText(emptyList(), true))
        assertFalse(hasReadableText(listOf(ReaderText.Chapter(title = "Heading", nested = false)), false))
    }
}
