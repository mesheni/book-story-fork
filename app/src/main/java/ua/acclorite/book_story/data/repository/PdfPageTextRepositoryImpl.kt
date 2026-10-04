/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.repository

import android.app.Application
import androidx.core.net.toUri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.data.local.dto.PdfPageTextEntity
import ua.acclorite.book_story.data.local.room.BookDatabase
import ua.acclorite.book_story.domain.model.file.File as BookFile
import ua.acclorite.book_story.domain.model.reader.PdfPageText
import ua.acclorite.book_story.domain.repository.BookRepository
import ua.acclorite.book_story.domain.repository.PdfPageTextRepository
import javax.inject.Inject
import javax.inject.Singleton
import java.security.MessageDigest
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

@Singleton
class PdfPageTextRepositoryImpl @Inject constructor(
    private val application: Application,
    private val bookRepository: BookRepository,
    private val database: BookDatabase
) : PdfPageTextRepository {

    private val cacheMutex = Mutex()
    private val memoryCache = ConcurrentHashMap<Int, Pair<String, List<PdfPageText>>>()

    override suspend fun getPageTexts(bookId: Int): Result<List<PdfPageText>> = try {
        Result.success(cacheMutex.withLock { withContext(Dispatchers.IO) {
            val book = bookRepository.getBook(bookId).getOrThrow()
            require(book.filePath.endsWith(".pdf", ignoreCase = true)) {
                "Book is not a PDF"
            }
            val cachedFile = bookRepository.getFileFromBook(bookId).getOrThrow()
            val sourceUri = cachedFile.uri.toUri()
            val metadataSignature = cachedFile.metadataSignature(
                fallbackUri = book.sourceUri ?: cachedFile.uri,
                sourcePath = cachedFile.path
            )
            memoryCache[bookId]?.takeIf { it.first == metadataSignature }?.let {
                return@withContext it.second
            }

            val sourceSignature = application.contentResolver.openInputStream(sourceUri)
                ?.use { it.contentSignature(metadataSignature) }
                ?: throw IllegalStateException("Unable to read PDF file")
            val dao = database.pdfPageTextDao

            PDFBoxResourceLoader.init(application)
            val extractedPages = mutableListOf<PdfPageTextEntity>()
            val stripper = PDFTextStripper()

            val input = application.contentResolver.openInputStream(sourceUri)
                ?: throw IllegalStateException("Unable to open PDF file")
            input.use { stream ->
                PDDocument.load(stream).use { document ->
                    val cachedPages = dao.getPages(bookId, sourceSignature)
                    if (
                        cachedPages.isNotEmpty() &&
                        dao.countPages(bookId, sourceSignature) == document.numberOfPages
                    ) {
                        return@withContext cachedPages.map { PdfPageText(it.pageIndex, it.text) }
                    }

                    for (pageIndex in 0 until document.numberOfPages) {
                        coroutineContext.ensureActive()
                        stripper.startPage = pageIndex + 1
                        stripper.endPage = pageIndex + 1
                        extractedPages += PdfPageTextEntity(
                            bookId = bookId,
                            pageIndex = pageIndex,
                            sourceSignature = sourceSignature,
                            text = stripper.getText(document).trim()
                        )
                    }
                }
            }

            dao.replacePagesForBook(bookId, extractedPages)
            extractedPages.map { PdfPageText(it.pageIndex, it.text) }.also { pages ->
                memoryCache[bookId] = metadataSignature to pages
            }
        } })
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }

    private fun BookFile.metadataSignature(
        fallbackUri: String,
        sourcePath: String
    ): String = listOf(uri.ifBlank { fallbackUri }, sourcePath, size, lastModified).joinToString("|")

    private suspend fun InputStream.contentSignature(metadataSignature: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            coroutineContext.ensureActive()
            val bytesRead = read(buffer)
            if (bytesRead < 0) break
            digest.update(buffer, 0, bytesRead)
        }
        digest.update(metadataSignature.toByteArray())
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }
}