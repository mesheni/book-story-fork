package ua.acclorite.book_story.data.parser.document

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfClosingTagsTest {

    private val xhtml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <html xmlns="http://www.w3.org/1999/xhtml">
        <head>
        <title/>
        <link rel="stylesheet" href="style.css" type="text/css"/>
        </head>
        <body>
        <div class="title1"><p>Глава 1</p></div>
        <p class="empty-line"/>
        <p>Первый абзац.</p>
        <a id="n1"/>
        <p>Второй абзац.</p>
        <img src="images/i.png"/><br/>
        </body>
        </html>
    """.trimIndent()

    @Test
    fun `self-closing title no longer swallows the body`() {
        val body = Jsoup.parse(xhtml.expandSelfClosingTags(), Parser.htmlParser()).body()

        assertTrue(body.text().contains("Первый абзац."))
        assertTrue(body.text().contains("Второй абзац."))
        assertEquals(4, body.select("p").size)
        assertTrue(body.select("a").single().childNodeSize() == 0)
    }

    @Test
    fun `void elements and attributes are preserved`() {
        val expanded = """<p class="x" /><img src="a.png"/><br/><TITLE/><path d="M0"/>""".expandSelfClosingTags()

        assertEquals("""<p class="x"></p><img src="a.png"/><br/><TITLE></TITLE><path d="M0"/>""", expanded)
    }
}
