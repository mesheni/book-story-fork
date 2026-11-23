package ua.acclorite.book_story.data.parser.text

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfTextLinesTest {
    @Test fun keepsPunctuationAndNumericLines() {
        val lines = mutableListOf<String>()
        listOf("First", "— Dialogue", "123", "\"Quoted").forEach { appendPdfLine(lines, it) }
        assertEquals(listOf("First", "— Dialogue", "123", "\"Quoted"), lines)
    }

    @Test fun joinsHyphenationOnlyAfterAWord() {
        val lines = mutableListOf<String>()
        listOf("inter-", "national", "-", "continued").forEach { appendPdfLine(lines, it) }
        assertEquals(listOf("international", "- continued"), lines)
    }

    @Test fun keepsLeadingSpaceWithoutCrashing() {
        assertEquals(" first second", collapsePdfSpaces("  first   second"))
    }
}
