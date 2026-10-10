/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.model.note.Note
import ua.acclorite.book_story.domain.model.reader.PdfSearchResult
import ua.acclorite.book_story.domain.model.reader.PdfWord
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.ui.common.components.common.AnimatedVisibility

@Composable
fun PdfViewerScaffold(
    book: Book,
    uri: String?,
    initialPage: Int,
    navigationRequest: Int,
    pageCount: Int,
    isLoading: Boolean,
    viewerError: String?,
    showMenu: Boolean,
    lockMenu: Boolean,
    notes: List<Note>,
    loadPdfWordBoxes: suspend (Int) -> List<PdfWord>?,
    leave: (ReaderEvent.OnLeave) -> Unit,
    navigateBack: (ReaderEvent.OnNavigateBack) -> Unit,
    showPdfModeDialog: (ReaderEvent.OnShowPdfModeDialog) -> Unit,
    showNotesBottomSheet: (ReaderEvent.OnShowNotesBottomSheet) -> Unit,
    onPageChanged: (ReaderEvent.OnPdfPageChanged) -> Unit,
    onViewerInitialized: (ReaderEvent.OnPdfViewerInitialized) -> Unit,
    showPdfSearch: Boolean,
    pdfSearchQuery: String,
    pdfSearchResults: List<PdfSearchResult>,
    isPdfSearching: Boolean,
    pdfSearchProgress: Float?,
    pdfSearchError: String?,
    showPdfSearchAction: (ReaderEvent.OnShowPdfSearch) -> Unit,
    onToggleMenu: (ReaderEvent.OnMenuVisibility) -> Unit,
    dismissPdfSearch: (ReaderEvent.OnDismissPdfSearch) -> Unit,
    pdfSearchQueryChanged: (ReaderEvent.OnPdfSearchQueryChanged) -> Unit,
    selectPdfSearchResult: (ReaderEvent.OnSelectPdfSearchResult) -> Unit,
    addPdfNote: (ReaderEvent.OnAddPdfNote) -> Unit,
    openTranslator: (ReaderEvent.OnOpenTranslator) -> Unit,
    openShareApp: (ReaderEvent.OnOpenShareApp) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AnimatedVisibility(
                visible = showMenu,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it }
            ) {
                PdfViewerTopBar(
                    book = book,
                    page = initialPage,
                    pageCount = pageCount,
                    lockMenu = lockMenu,
                    leave = leave,
                    navigateBack = navigateBack,
                    showPdfModeDialog = showPdfModeDialog,
                    showNotesBottomSheet = showNotesBottomSheet,
                    showPdfSearch = showPdfSearchAction
                )
            }
        }
    ) {
        PdfViewerContent(
            uriString = uri,
            initialPage = initialPage,
            navigationRequest = navigationRequest,
            onPageChanged = onPageChanged,
            isLoading = isLoading,
            viewerError = viewerError,
            onViewerInitialized = onViewerInitialized,
            onToggleMenu = {
                onToggleMenu(ReaderEvent.OnMenuVisibility(show = !showMenu, saveCheckpoint = false))
            },
            notes = notes,
            loadWordBoxes = loadPdfWordBoxes,
            addPdfNote = addPdfNote,
            openTranslator = openTranslator,
            openShareApp = openShareApp
        )
    }

    if (showPdfSearch) {
        PdfSearchDialog(
            query = pdfSearchQuery,
            results = pdfSearchResults,
            isSearching = isPdfSearching,
            progress = pdfSearchProgress,
            error = pdfSearchError,
            onQueryChange = { pdfSearchQueryChanged(ReaderEvent.OnPdfSearchQueryChanged(it)) },
            onDismiss = { dismissPdfSearch(ReaderEvent.OnDismissPdfSearch) },
            onSelectResult = { selectPdfSearchResult(ReaderEvent.OnSelectPdfSearchResult(it)) }
        )
    }
}
