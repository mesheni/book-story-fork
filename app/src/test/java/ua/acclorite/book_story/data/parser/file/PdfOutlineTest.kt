package ua.acclorite.book_story.data.parser.file

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfOutlineTest {
    @Test fun extractsNestedEntriesAndPageNumbers() {
        PDDocument().use { doc ->
            val page1 = PDPage()
            val page2 = PDPage()
            doc.addPage(page1)
            doc.addPage(page2)
            val root = PDDocumentOutline()
            doc.documentCatalog.documentOutline = root
            val parent = PDOutlineItem().apply { title = "Chapter"; setDestination(page1) }
            parent.addLast(PDOutlineItem().apply { title = "Section"; setDestination(page2) })
            root.addLast(parent)
            assertEquals(listOf(PdfOutlineEntry("Chapter", 0, 0), PdfOutlineEntry("Section", 1, 1)), readPdfOutline(doc))
        }
    }

    @Test fun missingOutlineIsEmpty() {
        PDDocument().use { assertEquals(emptyList<PdfOutlineEntry>(), readPdfOutline(it)) }
    }
}
