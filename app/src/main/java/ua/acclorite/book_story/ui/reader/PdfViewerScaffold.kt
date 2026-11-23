/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.model.note.Note
import ua.acclorite.book_story.domain.model.reader.PdfSearchResult
import ua.acclorite.book_story.domain.model.reader.PdfWord
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.presentation.reader.PdfReadingTimeData
import ua.acclorite.book_story.presentation.reader.PdfReadingTimeModel
import ua.acclorite.book_story.domain.use_case.book.estimatePdfReadingTime
import ua.acclorite.book_story.domain.use_case.book.ReadingTimeEstimate
import ua.acclorite.book_story.ui.common.helpers.LocalSettings
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
    showBookmarksBottomSheet: (ReaderEvent.OnShowBookmarksBottomSheet) -> Unit,
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
    val settings = LocalSettings.current
    val showTime = settings.showReadingTime.value
    val manualSpeed = settings.readingWordsPerMinute.value
    val autoPace = settings.autoReadingPace.value
    val learnedPace = settings.learnedReadingPace.value
    val speed = if (autoPace && learnedPace in 50..600) learnedPace else manualSpeed
    val timeModel = hiltViewModel<PdfReadingTimeModel>()
    val timeData by produceState<PdfReadingTimeData?>(null, book.id, uri, showTime) {
        value = if (showTime && uri != null) timeModel.load(book.id, uri) else null
    }
    val estimate by produceState<ReadingTimeEstimate?>(null, timeData, initialPage, speed) {
        value = withContext(Dispatchers.Default) {
            timeData?.let { estimatePdfReadingTime(it.pages, it.chapterPages, initialPage, speed) }
        }
    }
    var showPageDialog by remember { mutableStateOf(false) }
    var showThumbnails by remember { mutableStateOf(false) }
    var showOutline by remember { mutableStateOf(false) }
    var pageInput by remember { mutableStateOf("") }
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
                    readingTime = estimate?.let { stringResource(if (it.isChapter) R.string.reading_time_chapter else R.string.reading_time_book, it.minutes) },
                    lockMenu = lockMenu,
                    leave = leave,
                    navigateBack = navigateBack,
                    showPdfModeDialog = showPdfModeDialog,
                    showNotesBottomSheet = showNotesBottomSheet,
                    showBookmarksBottomSheet = showBookmarksBottomSheet,
                    showPdfSearch = showPdfSearchAction,
                    showPageDialog = {
                        pageInput = (initialPage + 1).toString()
                        showPageDialog = true
                    }
                )
            }
        }
    ) { innerPadding ->
        if (viewerError != null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.pdf_viewer_error))
                    TextButton(onClick = { onViewerInitialized(ReaderEvent.OnPdfViewerInitialized(0)) }) {
                        Text(stringResource(R.string.pdf_retry))
                    }
                }
            }
        } else PdfNativeViewer(
            uriString = uri,
            initialPage = initialPage,
            navigationRequest = navigationRequest,
            onPageChanged = onPageChanged,
            onViewerInitialized = onViewerInitialized,
            onToggleMenu = {
                onToggleMenu(ReaderEvent.OnMenuVisibility(show = !showMenu, saveCheckpoint = false))
            },
            notes = notes,
            loadWordBoxes = loadPdfWordBoxes,
            addPdfNote = addPdfNote,
            openTranslator = openTranslator,
            openShareApp = openShareApp,
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (showPageDialog) {
        val selectedPage = parsePdfPage(pageInput, pageCount)
        AlertDialog(
            onDismissRequest = { showPageDialog = false },
            title = { Text(stringResource(R.string.pdf_go_to_page)) },
            text = {
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(stringResource(R.string.pdf_page_range, pageCount)) },
                    isError = pageInput.isNotEmpty() && selectedPage == null
                )
            },
            confirmButton = {
                TextButton(enabled = selectedPage != null, onClick = {
                    selectedPage?.let { selectPdfSearchResult(ReaderEvent.OnSelectPdfSearchResult(it)) }
                    showPageDialog = false
                }) { Text(stringResource(R.string.pdf_go_to_page)) }
            },
            dismissButton = {
                Row {
                TextButton(enabled = uri != null && pageCount > 0, onClick = { showPageDialog = false; showOutline = true }) {
                    Text(stringResource(R.string.pdf_outline))
                }
                TextButton(enabled = uri != null && pageCount > 0, onClick = { showPageDialog = false; showThumbnails = true }) {
                    Text(stringResource(R.string.pdf_thumbnails))
                }
                }
            },
        )
    }

    if (showThumbnails && uri != null) {
        PdfThumbnailsDialog(uri, pageCount, dismiss = { showThumbnails = false }) { page ->
            showThumbnails = false
            selectPdfSearchResult(ReaderEvent.OnSelectPdfSearchResult(page))
        }
    }

    if (showOutline && uri != null) {
        PdfOutlineDialog(uri, dismiss = { showOutline = false }) { page ->
            showOutline = false
            selectPdfSearchResult(ReaderEvent.OnSelectPdfSearchResult(page))
        }
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

internal fun parsePdfPage(input: String, pageCount: Int): Int? =
    input.toIntOrNull()?.takeIf { it in 1..pageCount }?.minus(1)
