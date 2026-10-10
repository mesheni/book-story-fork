/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.ui.common.components.common.StyledText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerTopBar(
    book: Book,
    page: Int,
    pageCount: Int,
    lockMenu: Boolean,
    leave: (ReaderEvent.OnLeave) -> Unit,
    navigateBack: (ReaderEvent.OnNavigateBack) -> Unit,
    showPdfModeDialog: (ReaderEvent.OnShowPdfModeDialog) -> Unit,
    showNotesBottomSheet: (ReaderEvent.OnShowNotesBottomSheet) -> Unit,
    showPdfSearch: (ReaderEvent.OnShowPdfSearch) -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(
                enabled = !lockMenu,
                onClick = {
                    leave(
                        ReaderEvent.OnLeave {
                            navigateBack(ReaderEvent.OnNavigateBack)
                        }
                    )
                }
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            }
        },
        title = {
            StyledText(
                text = book.title,
                modifier = Modifier,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
        },
        actions = {
            IconButton(
                enabled = !lockMenu,
                onClick = { showPdfSearch(ReaderEvent.OnShowPdfSearch) }
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = androidx.compose.ui.res.stringResource(
                        ua.acclorite.book_story.R.string.search_pdf
                    )
                )
            }
            IconButton(
                enabled = !lockMenu,
                onClick = { showNotesBottomSheet(ReaderEvent.OnShowNotesBottomSheet) }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Notes,
                    contentDescription = androidx.compose.ui.res.stringResource(
                        ua.acclorite.book_story.R.string.notes
                    )
                )
            }
            StyledText(
                text = if (pageCount > 0) "${page + 1} / $pageCount" else "",
                style = MaterialTheme.typography.labelLarge
            )
            IconButton(
                enabled = !lockMenu,
                onClick = { showPdfModeDialog(ReaderEvent.OnShowPdfModeDialog) }
            ) {
                Icon(Icons.Default.TextSnippet, contentDescription = null)
            }
        }
    )
}