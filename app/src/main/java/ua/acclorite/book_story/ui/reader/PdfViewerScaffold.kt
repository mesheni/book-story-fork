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
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.ui.common.components.common.AnimatedVisibility

@Composable
fun PdfViewerScaffold(
    book: Book,
    uri: String?,
    initialPage: Int,
    pageCount: Int,
    showMenu: Boolean,
    lockMenu: Boolean,
    leave: (ReaderEvent.OnLeave) -> Unit,
    navigateBack: (ReaderEvent.OnNavigateBack) -> Unit,
    showPdfModeDialog: (ReaderEvent.OnShowPdfModeDialog) -> Unit,
    onPageChanged: (ReaderEvent.OnPdfPageChanged) -> Unit
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
                    showPdfModeDialog = showPdfModeDialog
                )
            }
        }
    ) {
        PdfViewerContent(
            uriString = uri,
            initialPage = initialPage,
            onPageChanged = onPageChanged
        )
    }
}