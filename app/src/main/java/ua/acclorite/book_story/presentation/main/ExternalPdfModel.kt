/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ua.acclorite.book_story.data.model.file.CachedFileCompat
import ua.acclorite.book_story.data.service.ExternalPdfSnapshotStore
import ua.acclorite.book_story.domain.repository.BookRepository
import ua.acclorite.book_story.domain.model.file.File
import ua.acclorite.book_story.domain.use_case.book.AddBookUseCase
import ua.acclorite.book_story.domain.use_case.book.GetBookBySourceUriUseCase
import ua.acclorite.book_story.domain.use_case.file_system.GetBookFromFileUseCase
import ua.acclorite.book_story.presentation.history.HistoryScreen
import ua.acclorite.book_story.presentation.library.LibraryScreen
import javax.inject.Inject

internal fun externalPdfBookPath(path: String, displayName: String): String {
    if (path.endsWith(".pdf", ignoreCase = true)) return path
    val name = displayName.substringAfterLast('/').substringAfterLast('\\').ifBlank { "document.pdf" }
    return if (name.endsWith(".pdf", ignoreCase = true)) name else "$name.pdf"
}

@HiltViewModel
class ExternalPdfModel @Inject constructor(
    private val application: Application,
    private val getBookBySourceUriUseCase: GetBookBySourceUriUseCase,
    private val getBookFromFileUseCase: GetBookFromFileUseCase,
    private val addBookUseCase: AddBookUseCase,
    private val bookRepository: BookRepository,
    private val snapshots: ExternalPdfSnapshotStore
) : ViewModel() {

    private val _effects = Channel<ExternalPdfEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private val pendingUris = Channel<Uri>(Channel.BUFFERED)

    fun openUri(uri: Uri) {
        pendingUris.trySend(uri)
    }

    init {
        viewModelScope.launch {
            pendingUris.receiveAsFlow().collect { uri ->
                try {
                    snapshots.withLock { open(uri) }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _effects.send(ExternalPdfEffect.OpenError)
                }
            }
        }
    }

    private suspend fun open(uri: Uri) {
        val sourceUri = uri.toString()
        val cachedFile = CachedFileCompat.fromUri(application, uri)
        check(cachedFile.canAccess() && !cachedFile.isDirectory)
        val name = cachedFile.name
        val existingBook = getBookBySourceUriUseCase(sourceUri)
        val hasPersistedAccess = application.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
        }
        // Re-copy only when the source changed: a repeated open of the same file
        // must not copy the whole PDF again
        if (!hasPersistedAccess && !snapshots.isUpToDate(sourceUri, cachedFile.size)) snapshots.save(uri)
        if (existingBook != null) {
            val path = externalPdfBookPath(existingBook.filePath, name)
            if (path != existingBook.filePath) {
                bookRepository.updateBook(existingBook.copy(filePath = path)).getOrThrow()
            }
            HistoryScreen.insertHistoryChannel.trySend(existingBook.id)
            _effects.send(ExternalPdfEffect.OpenBook(existingBook.id))
            return
        }

        val parsed = getBookFromFileUseCase(
            File(
                name = cachedFile.name,
                uri = cachedFile.uri.toString(),
                path = cachedFile.path,
                size = cachedFile.size,
                lastModified = cachedFile.lastModified,
                isDirectory = false
            )
        ) ?: error("Unable to parse PDF")

        addBookUseCase(
            parsed.first.copy(sourceUri = sourceUri, filePath = externalPdfBookPath(parsed.first.filePath, name)),
            parsed.second
        )
        val addedBook = getBookBySourceUriUseCase(sourceUri) ?: error("Unable to add PDF")

        LibraryScreen.refreshListChannel.trySend(0)
        HistoryScreen.insertHistoryChannel.trySend(addedBook.id)
        _effects.send(ExternalPdfEffect.OpenBook(addedBook.id))
    }
}

sealed interface ExternalPdfEffect {
    data class OpenBook(val bookId: Int) : ExternalPdfEffect
    data object OpenError : ExternalPdfEffect
}
