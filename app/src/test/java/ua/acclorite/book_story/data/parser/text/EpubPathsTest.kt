package ua.acclorite.book_story.data.parser.text

import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Test
import ua.acclorite.book_story.data.parser.document.DocumentParser
import ua.acclorite.book_story.data.parser.document.MarkdownParser
import ua.acclorite.book_story.data.parser.document.decodeHref
import ua.acclorite.book_story.domain.model.reader.ReaderText

class EpubPathsTest {

    @Test
    fun `href is resolved relative to the OPF folder`() {
        assertEquals("OEBPS/Text/ch1.xhtml", resolveZipPath("OEBPS", "Text/ch1.xhtml"))
        assertEquals("Images/cover.jpg", resolveZipPath("OEBPS/Text", "../../Images/cover.jpg"))
        assertEquals("OPS/s 1.xhtml", resolveZipPath("", "OPS/./s 1.xhtml"))
    }

    @Test
    fun `href decoding keeps plus and tolerates malformed escapes`() {
        assertEquals("C++ 1.xhtml", "C++%201.xhtml".decodeHref())
        assertEquals("100%.xhtml", "100%.xhtml".decodeHref())
    }

    @Test
    fun `xhtml charset is taken from xml declaration`() {
        val cp1251 = """<?xml version="1.0" encoding="windows-1251"?><p>Привет</p>"""
            .toByteArray(charset("windows-1251"))
        val utf8 = (Char(0xFEFF) + "<?xml version='1.0'?><p>Привет</p>").toByteArray()

        assertEquals(true, cp1251.decodeXmlText().endsWith("<p>Привет</p>"))
        assertEquals("<?xml version='1.0'?><p>Привет</p>", utf8.decodeXmlText())
    }

    @Test
    fun `br and headings produce separate lines`() = runBlocking {
        val parser = DocumentParser(MarkdownParser(org.commonmark.parser.Parser.builder().build()))
        val html = "<html><body><h2>Стих</h2><p>Строка один<br/>Строка два</p><div>Подпись</div></body></html>"

        val lines = parser.parseDocument(Jsoup.parse(html), includeChapter = false)
            .filterIsInstance<ReaderText.Text>()
            .map { it.line.text }

        assertEquals(listOf("Стих", "Строка один", "Строка два", "Подпись"), lines)
    }

    @Test
    fun `fb2 section titles and verse lines are kept`() = runBlocking {
        val parser = DocumentParser(MarkdownParser(org.commonmark.parser.Parser.builder().build()))
        val fb2 = """<FictionBook><body><section><title><p>Глава 1</p></title><p>Текст.</p>""" +
                """<poem><stanza><v>Первая</v><v>Вторая</v></stanza></poem>""" +
                """</section><section><title><p>Глава 2</p></title><p>Ещё.</p></section></body></FictionBook>"""

        val result = parser.parseDocument(Jsoup.parse(fb2, "", org.jsoup.parser.Parser.xmlParser()))
        val lines = result.filterIsInstance<ReaderText.Text>().map { it.line.text }

        assertEquals("Глава 1", (result.first() as ReaderText.Chapter).title)
        assertEquals(listOf("Текст.", "Первая", "Вторая", "Глава 2", "Ещё."), lines)
    }
}
