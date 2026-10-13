/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.text

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.mozilla.universalchardet.UniversalDetector
import ua.acclorite.book_story.core.helpers.clearAllMarkdown
import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.data.parser.document.MarkdownParser
import ua.acclorite.book_story.domain.model.reader.ReaderText
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.SequenceInputStream
import java.nio.charset.Charset
import javax.inject.Inject

private const val TAG = "TxtTextParser"

private const val CHARSET_DETECTION_LIMIT = 64 * 1024

class TxtTextParser @Inject constructor(
    private val markdownParser: MarkdownParser
) : TextParser {

    override suspend fun parse(cachedFile: CachedFile, onProgress: (Float) -> Unit): List<ReaderText> {
        logI(TAG, "Started TXT parsing: ${cachedFile.name}.")

        return try {
            val readerText = mutableListOf<ReaderText>()
            var chapterAdded = false

            withContext(Dispatchers.IO) {
                cachedFile.openInputStream()?.let { stream ->
                    // Sniff the head of the stream first: TXT files are often
                    // windows-1251, while a plain reader would force UTF-8.
                    val head = readHead(stream)
                    val charset = detectCharset(head)
                    logI(TAG, "Detected TXT charset: $charset.")

                    val fullStream = SequenceInputStream(ByteArrayInputStream(head), stream)
                    BufferedReader(InputStreamReader(fullStream, charset)).use { reader ->
                        reader.forEachLine { line ->
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
                    }
                }
            }

            yield()

            if (
                readerText.filterIsInstance<ReaderText.Text>().isEmpty() ||
                readerText.filterIsInstance<ReaderText.Chapter>().isEmpty()
            ) {
                logE(TAG, "Could not extract text from TXT.")
                return emptyList()
            }

            logI(TAG, "Successfully finished TXT parsing.")
            readerText
        } catch (e: Exception) {
            logE(TAG, "Could not parse text with message: ${e.message}.")
            emptyList()
        }
    }

    private fun readHead(stream: java.io.InputStream): ByteArray {
        val buffer = ByteArray(CHARSET_DETECTION_LIMIT)
        var total = 0
        while (total < buffer.size) {
            val read = stream.read(buffer, total, buffer.size - total)
            if (read <= 0) break
            total += read
        }
        return if (total > 0) buffer.copyOf(total) else ByteArray(0)
    }

    private fun detectCharset(head: ByteArray): Charset {
        if (head.isEmpty()) return Charsets.UTF_8

        val detector = UniversalDetector(null)
        detector.handleData(head, 0, head.size)
        detector.dataEnd()
        val detected = detector.detectedCharset
        detector.reset()

        if (detected == null) return Charsets.UTF_8
        return runCatching { Charset.forName(detected) }
            .getOrDefault(Charsets.UTF_8)
    }
}