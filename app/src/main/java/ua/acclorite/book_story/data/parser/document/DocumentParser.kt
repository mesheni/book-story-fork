/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.document

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.yield
import org.jsoup.nodes.Document
import ua.acclorite.book_story.core.helpers.clearAllMarkdown
import ua.acclorite.book_story.core.helpers.clearMarkdown
import ua.acclorite.book_story.core.helpers.containsVisibleText
import ua.acclorite.book_story.domain.model.reader.ReaderText
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * Non-void HTML elements that XHTML documents commonly write as self-closing (`<title/>`).
 * Void elements (img, br, hr, ...) and SVG/MathML tags are intentionally excluded.
 */
private val selfClosingNonVoidTag = Regex(
    "<(title|p|a|span|div|script|style|i|b|u|s|em|strong|small|big|sup|sub|font|" +
            "h[1-6]|section|article|aside|header|footer|nav|blockquote|pre|code|" +
            "ul|ol|li|dl|dt|dd|table|thead|tbody|tr|td|th|textarea|iframe|noscript)" +
            "(\\s[^<>]*?)?\\s*/>",
    RegexOption.IGNORE_CASE
)

/**
 * The HTML parser ignores the self-closing flag of non-void elements, so XHTML `<title/>`
 * swallows the whole document (RCDATA) and `<p/>`/`<a/>` wrap the following content.
 * Expands such tags into explicit open/close pairs before HTML parsing.
 */
fun String.expandSelfClosingTags(): String =
    replace(selfClosingNonVoidTag) { match ->
        val tag = match.groupValues[1]
        "<$tag${match.groupValues[2]}></$tag>"
    }

/**
 * Percent-decodes an EPUB href. Unlike raw [URLDecoder], keeps literal '+' (hrefs are URIs,
 * not form data) and returns the input as-is for malformed escapes (e.g. "100%.xhtml")
 * instead of throwing.
 */
fun String.decodeHref(): String = try {
    URLDecoder.decode(replace("+", "%2B"), StandardCharsets.UTF_8.name())
} catch (_: IllegalArgumentException) {
    this
}

class DocumentParser @Inject constructor(
    private val markdownParser: MarkdownParser
) {
    /**
     * Parses document to get it's text.
     * Fixes issues such as manual line breaking in <p>.
     * Applies Markdown to the text: Bold(**), Italic(_), Section separator(---), and Links(a > href).
     *
     * @return Parsed text line by line with Markdown(all lines are not blank).
     */
    suspend fun parseDocument(
        document: Document,
        zipFile: ZipFile? = null,
        imageEntries: List<ZipEntry>? = null,
        includeChapter: Boolean = true
    ): List<ReaderText> {
        yield()

        val readerText = mutableListOf<ReaderText>()
        var chapterAdded = false

        document.selectFirst("body")
            .run { this ?: document.body() }
            .apply {
                // Remove manual line breaks from all <p>, <a>
                select("p").forEach { element ->
                    yield()
                    element.html(element.html().replace(Regex("\\n+"), " "))
                    element.append("\n")
                }
                select("a").forEach { element ->
                    yield()
                    element.html(element.html().replace(Regex("\\n+"), ""))
                }

                // Remove <head>'s title (HTML only: in FB2 <title> is a section
                // heading inside <body> and must stay in the text)
                if (document.outputSettings().syntax() != Document.OutputSettings.Syntax.xml) {
                    select("title").remove()
                }

                // Markdown
                select("hr").append("\n---\n")
                select("b").append("**").prepend("**")
                select("h1").append("**").prepend("**")
                select("h2").append("**").prepend("**")
                select("h3").append("**").prepend("**")
                select("strong").append("**").prepend("**")
                select("em").append("_").prepend("_")
                select("a").forEach { element ->
                    var link = element.attr("href")
                    if (!link.startsWith("http") || element.wholeText().isBlank()) return@forEach

                    if (link.startsWith("http://")) {
                        link = link.replaceFirst("http://", "https://")
                    }

                    element.prepend("[")
                    element.append("]($link)")
                }

                // Image (<img>)
                select("img").forEach { element ->
                    val src = element.attr("src")
                        .trim()
                        .substringAfterLast(File.separator)
                        .lowercase()
                        .decodeHref()
                        .takeIf {
                            it.containsVisibleText() && imageEntries?.any { image ->
                                it == image.name.substringAfterLast(File.separator).lowercase()
                            } == true
                        } ?: return@forEach

                    val alt = element.attr("alt").trim().takeIf {
                        it.clearMarkdown().containsVisibleText()
                    } ?: "Image"

                    element.append("\n[[$src|$alt]]\n")
                }

                // Image (<image>)
                select("image").forEach { element ->
                    val src = element.attr("xlink:href")
                        .trim()
                        .substringAfterLast(File.separator)
                        .lowercase()
                        .decodeHref()
                        .takeIf {
                            it.containsVisibleText() && imageEntries?.any { image ->
                                it == image.name.substringAfterLast(File.separator).lowercase()
                            } == true
                        } ?: return@forEach

                    val alt = "Image"

                    element.append("\n[[$src|$alt]]\n")
                }

                // Line breaks for block elements other than <p> (wholeText() ignores
                // element boundaries): <br> in poetry, headings/divs/list items in
                // documents without source formatting would otherwise merge with
                // the next paragraph. Extra blank lines are skipped below.
                select("br").forEach { it.after("\n") }
                // (v/subtitle/text-author: FB2 verse lines and captions)
                select("h1, h2, h3, h4, h5, h6, div, li, blockquote, tr, v, subtitle, text-author").forEach {
                    it.prepend("\n")
                    it.append("\n")
                }
            }.wholeText().lines().forEach { line ->
                yield()

                val formattedLine = line.replace(
                    Regex("""\*\*\*\s*(.*?)\s*\*\*\*"""), "_**$1**_"
                ).replace(
                    Regex("""\*\*\s*(.*?)\s*\*\*"""), "**$1**"
                ).replace(
                    Regex("""_\s*(.*?)\s*_"""), "_$1_"
                ).trim()

                val imageRegex = Regex("""\[\[(.*?)\|(.*?)]]""")

                if (line.containsVisibleText()) {
                    when {
                        imageRegex.matches(line) -> {
                            val trimmedLine = line.removeSurrounding("[[", "]]")
                            val src = trimmedLine.substringBefore("|")
                            val alt = "_${trimmedLine.substringAfter("|")}_"

                            val image = try {
                                val imageEntry = imageEntries?.find { image ->
                                    src == image.name.substringAfterLast(File.separator).lowercase()
                                } ?: return@forEach

                                zipFile?.getImage(imageEntry)?.asImageBitmap()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                null
                            } ?: return@forEach

                            image.prepareToDraw()
                            readerText.add( // Adding image
                                ReaderText.Image(
                                    imageBitmap = image
                                )
                            )
                            readerText.add( // Adding alternative text (caption) for image
                                ReaderText.Text(
                                    markdownParser.parse(alt)
                                )
                            )
                        }

                        line == "---" || line == "***" -> readerText.add(ReaderText.Separator)

                        else -> {
                            if (
                                !chapterAdded &&
                                formattedLine.clearAllMarkdown().containsVisibleText() &&
                                includeChapter
                            ) {
                                readerText.add(
                                    0, ReaderText.Chapter(
                                        title = formattedLine.clearAllMarkdown(),
                                        nested = false
                                    )
                                )
                                chapterAdded = true
                            } else if (
                                formattedLine.clearMarkdown().containsVisibleText()
                            ) {
                                readerText.add(
                                    ReaderText.Text(
                                        line = markdownParser.parse(formattedLine)
                                    )
                                )
                            }
                        }
                    }
                }
            }

        yield()

        if (
            readerText.filterIsInstance<ReaderText.Text>().isEmpty() ||
            (includeChapter && readerText.filterIsInstance<ReaderText.Chapter>().isEmpty())
        ) {
            return emptyList()
        }

        return readerText
    }

    /**
     * Getting bitmap from [ZipFile] with compression
     * that depends on the [imageEntry] size.
     */
    private fun ZipFile.getImage(imageEntry: ZipEntry): Bitmap? {
        fun getBitmapFromInputStream(compressionLevel: Int = 1): Bitmap? {
            return getInputStream(imageEntry).use { inputStream ->
                BitmapFactory.decodeStream(
                    inputStream,
                    null,
                    BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565
                        inSampleSize = compressionLevel
                    }
                )
            }
        }


        val uncompressedBitmap = getBitmapFromInputStream() ?: return null
        return uncompressedBitmap
    }
}