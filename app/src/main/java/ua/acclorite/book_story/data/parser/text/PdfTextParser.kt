/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.text

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.yield
import ua.acclorite.book_story.core.helpers.clearAllMarkdown
import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.data.parser.document.MarkdownParser
import ua.acclorite.book_story.domain.model.reader.ReaderText
import javax.inject.Inject

private const val TAG = "PdfTextParser"

// Progress stage boundaries: extraction 0-60%, char filter 60-70%,
// line merge 70-85%, markdown build 85-100%
private const val EXTRACTION_END = 0.6f
private const val CHAR_FILTER_END = 0.7f
private const val LINE_MERGE_END = 0.85f

class PdfTextParser @Inject constructor(
    private val markdownParser: MarkdownParser,
    private val application: Application
) : TextParser {

    override suspend fun parse(cachedFile: CachedFile, onProgress: (Float) -> Unit): List<ReaderText> {
        logI(TAG, "Started PDF parsing: ${cachedFile.name}.")

        return try {
            yield()

            PDFBoxResourceLoader.init(application)

            yield()

            val oldText: String

            val pdfStripper = PDFTextStripper()
            pdfStripper.paragraphStart = "</br>"

            PDDocument.load(cachedFile.openInputStream()).use { document ->
                // Per-page extraction instead of a single getText over the whole
                // document, so reading progress can be reported
                val totalPages = document.numberOfPages
                val textBuilder = StringBuilder()
                onProgress(0f)
                for (pageIndex in 1..totalPages) {
                    yield()
                    pdfStripper.startPage = pageIndex
                    pdfStripper.endPage = pageIndex
                    textBuilder.append(pdfStripper.getText(document))
                    onProgress(EXTRACTION_END * pageIndex / totalPages)
                }
                oldText = textBuilder.toString().replace("\r", "")
            }

            yield()

            val readerText = mutableListOf<ReaderText>()
            val textLength = oldText.length.coerceAtLeast(1)
            val text = oldText.filterIndexed { index, c ->
                yield()

                if (index % 8192 == 0) {
                    onProgress(
                        EXTRACTION_END +
                                (CHAR_FILTER_END - EXTRACTION_END) * index / textLength
                    )
                }

                if (c == ' ') {
                    oldText[index - 1] != ' '
                } else {
                    true
                }
            }

            yield()

            val unformattedLines = text.split("${pdfStripper.paragraphStart}|\\n".toRegex())
                .filter { it.isNotBlank() }

            yield()

            val lines = mutableListOf<String>()
            val unformattedLineCount = unformattedLines.size.coerceAtLeast(1)
            unformattedLines.forEachIndexed { index, string ->
                try {
                    yield()

                    if (index % 64 == 0) {
                        onProgress(
                            CHAR_FILTER_END +
                                    (LINE_MERGE_END - CHAR_FILTER_END) * index / unformattedLineCount
                        )
                    }

                    val line = string.trim()

                    if (index == 0) {
                        lines.add(line)
                        return@forEachIndexed
                    }

                    if (line.all { it.isDigit() }) {
                        return@forEachIndexed
                    }

                    if (line.first().isLowerCase()) {
                        val currentLine = lines[lines.lastIndex]

                        if (currentLine.last() == '-') {
                            if (currentLine[currentLine.lastIndex - 1].isLowerCase()) {
                                lines[lines.lastIndex] = currentLine.dropLast(1) + line
                                return@forEachIndexed
                            }
                        }

                        lines[lines.lastIndex] += " $line"
                        return@forEachIndexed
                    }

                    if (line.first().isUpperCase() || line.first().isDigit()) {
                        lines.add(line)
                        return@forEachIndexed
                    }

                    if (line.first().isLetter()) {
                        lines[lines.lastIndex] += " $line"
                        return@forEachIndexed
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    return@forEachIndexed
                }
            }

            yield()

            var chapterAdded = false
            val lineCount = lines.size.coerceAtLeast(1)
            lines.forEachIndexed { lineIndex, line ->
                yield()

                if (lineIndex % 16 == 0) {
                    onProgress(
                        LINE_MERGE_END +
                                (1f - LINE_MERGE_END) * lineIndex / lineCount
                    )
                }

                if (line.isNotBlank()) {
                    when (line) {
                        "***", "---" -> readerText.add(
                            ReaderText.Separator
                        )

                        else -> {
                            if (!chapterAdded && line.clearAllMarkdown().isNotBlank()) {
                                readerText.add(
                                    0, ReaderText.Chapter(
                                        title = line.clearAllMarkdown(),
                                        nested = false
                                    )
                                )
                                chapterAdded = true
                            } else readerText.add(
                                ReaderText.Text(
                                    line = markdownParser.parse(line)
                                )
                            )
                        }
                    }
                }
            }

            yield()
            onProgress(1f)

            if (
                readerText.filterIsInstance<ReaderText.Text>().isEmpty() ||
                readerText.filterIsInstance<ReaderText.Chapter>().isEmpty()
            ) {
                logE(TAG, "Could not extract text from PDF.")
                return emptyList()
            }

            logI(TAG, "Successfully finished PDF parsing.")
            readerText
        } catch (e: Exception) {
            logE(TAG, "Could not parse text with message: ${e.message}.")
            emptyList()
        }
    }
}