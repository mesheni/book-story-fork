package ua.acclorite.book_story.data.parser.file

import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode
import java.util.Collections
import java.util.IdentityHashMap

data class PdfOutlineEntry(val title: String, val page: Int?, val depth: Int)

fun readPdfOutline(document: PDDocument): List<PdfOutlineEntry> {
    val root = document.documentCatalog.documentOutline ?: return emptyList()
    val entries = mutableListOf<PdfOutlineEntry>()
    val visited = Collections.newSetFromMap(IdentityHashMap<COSDictionary, Boolean>())
    fun walk(node: PDOutlineNode, depth: Int) {
        if (depth > 32 || entries.size >= 10000) return
        var item = node.firstChild
        while (item != null && entries.size < 10000) {
            val current = item
            if (!visited.add(current.cosObject)) break
            val page = runCatching { current.findDestinationPage(document)?.let { document.pages.indexOf(it).takeIf { i -> i >= 0 } } }.getOrNull()
            entries.add(PdfOutlineEntry(current.title.orEmpty(), page, depth))
            walk(current, depth + 1)
            item = current.nextSibling
        }
    }
    walk(root, 0)
    return entries
}
