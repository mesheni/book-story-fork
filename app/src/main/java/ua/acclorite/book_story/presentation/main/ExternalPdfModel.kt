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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ua.acclorite.book_story.data.model.file.CachedFileCompat
import ua.acclorite.book_story.domain.model.file.File
import ua.acclorite.book_story.domain.use_case.book.AddBookUseCase
import ua.acclorite.book_story.domain.use_case.book.GetBookBySourceUriUseCase
import ua.acclorite.book_story.domain.use_case.file_system.GetBookFromFileUseCase
import ua.acclorite.book_story.presentation.history.HistoryScreen
import ua.acclorite.book_story.presentation.library.LibraryScreen
import javax.inject.Inject

@HiltViewModel
class ExternalPdfModel @Inject constructor(
    private val application: Application,
    private val getBookBySourceUriUseCase: GetBookBySourceUriUseCase,
    private val getBookFromFileUseCase: GetBookFromFileUseCase,
    private val addBookUseCase: AddBookUseCase
) : ViewModel() {

    private val _effects = Channel<ExternalPdfEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            MainActivity.externalPdfUris.collect { uri ->
                open(uri)
            }
        }
    }

    private suspend fun open(uri: Uri) {
        val sourceUri = uri.toString()
        val existingBook = getBookBySourceUriUseCase(sourceUri)
        if (existingBook != null) {
            HistoryScreen.insertHistoryChannel.trySend(existingBook.id)
            _effects.send(ExternalPdfEffect.OpenBook(existingBook.id))
            return
        }

        val cachedFile = CachedFileCompat.fromUri(application, uri)
        if (cachedFile.isDirectory || !cachedFile.canAccess()) return

        val parsed = getBookFromFileUseCase(
            File(
                name = cachedFile.name,
                uri = cachedFile.uri.toString(),
                path = cachedFile.path,
                size = cachedFile.size,
                lastModified = cachedFile.lastModified,
                isDirectory = false
            )
        ) ?: return

        addBookUseCase(
            parsed.first.copy(sourceUri = sourceUri),
            parsed.second
        )
        val addedBook = getBookBySourceUriUseCase(sourceUri) ?: return

        LibraryScreen.refreshListChannel.trySend(0)
        HistoryScreen.insertHistoryChannel.trySend(addedBook.id)
        _effects.send(ExternalPdfEffect.OpenBook(addedBook.id))
    }
}

sealed interface ExternalPdfEffect {
    data class OpenBook(val bookId: Int) : ExternalPdfEffect
}
