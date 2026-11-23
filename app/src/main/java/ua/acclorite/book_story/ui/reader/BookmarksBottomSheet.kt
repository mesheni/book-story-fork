/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.bookmark.Bookmark
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.presentation.reader.model.resolvePdfBookmark
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.common.components.modal_bottom_sheet.ModalBottomSheet
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The book's bookmarks list. The top button saves the current
 * position; clicking a bookmark jumps to its location.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksBottomSheet(
    bookmarks: List<Bookmark>,
    text: List<ReaderText>,
    isPdfText: Boolean,
    addBookmark: (ReaderEvent.OnAddBookmark) -> Unit,
    selectBookmark: (ReaderEvent.OnSelectBookmark) -> Unit,
    deleteBookmark: (ReaderEvent.OnDeleteBookmark) -> Unit,
    dismissBottomSheet: (ReaderEvent.OnDismissBottomSheet) -> Unit
) {
    val resolvedBookmarks by key(bookmarks, text, isPdfText) { produceState<Map<Int, Pair<Int, Int>?>?>(null) {
        value = if (isPdfText) withContext(Dispatchers.Default) {
            bookmarks.associate { it.id to resolvePdfBookmark(it, text) }
        } else null
    } }
    ModalBottomSheet(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f),
        onDismissRequest = {
            dismissBottomSheet(ReaderEvent.OnDismissBottomSheet)
        },
        sheetGesturesEnabled = true
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.bookmarks),
                    style = MaterialTheme.typography.titleMedium
                )

                TextButton(onClick = { addBookmark(ReaderEvent.OnAddBookmark) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(stringResource(R.string.bookmark_add))
                }
            }

            if (bookmarks.isEmpty()) {
                StyledText(
                    text = stringResource(R.string.bookmarks_empty),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(bookmarks.size) { index ->
                        val bookmark = bookmarks[index]

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectBookmark(ReaderEvent.OnSelectBookmark(bookmark))
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val location = if (isPdfText && !bookmark.anchoredToPdf) {
                                    when {
                                        resolvedBookmarks == null -> null
                                        resolvedBookmarks?.get(bookmark.id) == null -> stringResource(R.string.pdf_anchor_unavailable)
                                        else -> bookmarkLocation(bookmark)
                                    }
                                } else bookmarkLocation(bookmark)
                                location?.let {
                                    StyledText(
                                        text = it,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (bookmark.previewText.isNotBlank()) {
                                    StyledText(
                                        text = bookmark.previewText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                StyledText(
                                    text = "${(bookmark.progress * 100).toInt()}% · " +
                                            bookmarkDate(bookmark),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    maxLines = 1
                                )
                            }

                            IconButton(
                                onClick = {
                                    deleteBookmark(ReaderEvent.OnDeleteBookmark(bookmark.id))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(
                                        R.string.bookmark_delete_content_desc
                                    ),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun bookmarkLocation(bookmark: Bookmark): String? {
    return when {
        bookmark.anchoredToPdf -> stringResource(R.string.note_pdf_page, (bookmark.pageIndex ?: 0) + 1)
        bookmark.chapterTitle != null -> bookmark.chapterTitle
        else -> null
    }
}

private val bookmarkDateFormatter = DateTimeFormatter
    .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    .withZone(ZoneId.systemDefault())

private fun bookmarkDate(bookmark: Bookmark): String {
    return bookmarkDateFormatter.format(Instant.ofEpochMilli(bookmark.createdAt))
}
