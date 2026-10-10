/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.repository

import android.app.Application
import android.util.LruCache
import androidx.core.net.toUri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.StringWriter
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.domain.model.file.File as BookFile
import ua.acclorite.book_story.domain.model.reader.PdfWord
import ua.acclorite.book_story.domain.repository.BookRepository
import ua.acclorite.book_story.domain.repository.PdfWordBoxRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts per-word bounding boxes with PDFBox. Unlike the page-text cache
 * ([PdfPageTextRepositoryImpl]) the boxes are not persisted: the PDDocument
 * is kept open for the reading session and word boxes are cached in memory
 * page by page, because only the visible pages are ever needed.
 */
@Singleton
class PdfWordBoxRepositoryImpl @Inject constructor(
    private val application: Application,
    private val bookRepository: BookRepository
) : PdfWordBoxRepository {

    /** Pages with word boxes kept in memory per book. */
    private companion object {
        const val WORD_CACHE_PAGES = 64
    }

    private class Session(
        val signature: String,
        val document: PDDocument,
        val cache: LruCache<Int, List<PdfWord>>
    ) {
        val lock = Mutex()
    }

    private val sessions = ConcurrentHashMap<Int, Session>()
    private val creationMutex = Mutex()

    override suspend fun getWordBoxes(bookId: Int, pageIndex: Int): Result<List<PdfWord>> = try {
        withContext(Dispatchers.IO) {
            // Fast path: an open session serves cached pages without
            // repeated book/file database reads.
            sessions[bookId]?.let { session ->
                session.cache.get(pageIndex)?.let { return@withContext it }
            }
            val session = obtainSession(bookId)
            session.lock.withLock {
                session.cache.get(pageIndex)?.let { return@withLock it }
                if (pageIndex !in 0 until session.document.numberOfPages) {
                    return@withLock emptyList()
                }
                val words = extractWords(session.document, pageIndex)
                session.cache.put(pageIndex, words)
                words
            }
        }.let { Result.success(it) }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }

    override suspend fun close(bookId: Int) {
        val session = sessions.remove(bookId) ?: return
        // NonCancellable: a cancelled reader must still release the document.
        withContext(Dispatchers.IO + NonCancellable) {
            session.lock.withLock {
                runCatching { session.document.close() }
            }
        }
    }

    private suspend fun obtainSession(bookId: Int): Session {
        creationMutex.withLock {
            val book = bookRepository.getBook(bookId).getOrThrow()
            require(book.filePath.endsWith(".pdf", ignoreCase = true)) {
                "Book is not a PDF"
            }
            val cachedFile = bookRepository.getFileFromBook(bookId).getOrThrow()
            val signature = listOf(
                cachedFile.uri, cachedFile.path, cachedFile.size, cachedFile.lastModified
            ).joinToString("|")

            sessions[bookId]?.takeIf { it.signature == signature }?.let { return it }

            close(bookId)
            PDFBoxResourceLoader.init(application)
            val input = application.contentResolver.openInputStream(cachedFile.uri.toUri())
                ?: throw IllegalStateException("Unable to open PDF file")
            val document = input.use { stream -> PDDocument.load(stream) }
            return Session(
                signature = signature,
                document = document,
                cache = object : LruCache<Int, List<PdfWord>>(WORD_CACHE_PAGES) {}
            ).also { sessions[bookId] = it }
        }
    }

    private fun extractWords(document: PDDocument, pageIndex: Int): List<PdfWord> {
        val page: PDPage = document.getPage(pageIndex)
        val cropBox = page.cropBox
        val rotation = ((page.rotation % 360) + 360) % 360
        // getXDirAdj/getYDirAdj coordinates live in the rotated ("visual")
        // space, so the normalizer uses the page size the reader renders.
        val visualWidth = if (rotation == 90 || rotation == 270) cropBox.height else cropBox.width
        val visualHeight = if (rotation == 90 || rotation == 270) cropBox.width else cropBox.height
        if (visualWidth <= 0f || visualHeight <= 0f) return emptyList()

        val extractor = object : PDFTextStripper() {
            val words = mutableListOf<PdfWord>()

            override fun writeString(text: String, textPositions: List<TextPosition>) {
                var builder: StringBuilder? = null
                var minX = 0f
                var minY = 0f
                var maxX = 0f
                var maxY = 0f

                fun flush() {
                    val word = builder ?: return
                    if (word.isNotBlank()) {
                        words += PdfWord(
                            text = word.toString(),
                            left = (minX / visualWidth).coerceIn(0f, 1f),
                            top = (minY / visualHeight).coerceIn(0f, 1f),
                            right = (maxX / visualWidth).coerceIn(0f, 1f),
                            bottom = (maxY / visualHeight).coerceIn(0f, 1f)
                        )
                    }
                    builder = null
                }

                textPositions.forEach { position ->
                    val glyph = position.unicode ?: return@forEach
                    if (glyph.isNotBlank()) {
                        val x = position.xDirAdj
                        val y = position.yDirAdj - position.heightDir
                        val right = x + position.widthDirAdj
                        val bottom = y + position.heightDir
                        if (builder == null) {
                            builder = StringBuilder()
                            minX = x
                            minY = y
                            maxX = right
                            maxY = bottom
                        } else {
                            minX = min(minX, x)
                            minY = min(minY, y)
                            maxX = max(maxX, right)
                            maxY = max(maxY, bottom)
                        }
                        builder?.append(glyph)
                    } else {
                        flush()
                    }
                }
                flush()
            }
        }

        // Visual order: without it words arrive in content-stream order and
        // a selection spanning multiple columns would jump between them.
        extractor.sortByPosition = true
        extractor.startPage = pageIndex + 1
        extractor.endPage = pageIndex + 1
        val writer = StringWriter()
        extractor.writeText(document, writer)
        return extractor.words
    }
}
