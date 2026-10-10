/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.note.Note
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.domain.model.reader.ReaderText.Chapter
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.common.components.modal_bottom_sheet.ModalBottomSheet

/**
 * The book's notes list. Clicking a note jumps to its location,
 * the edit icon reopens [NoteEditorDialog].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesBottomSheet(
    notes: List<Note>,
    text: List<ReaderText>,
    selectNote: (ReaderEvent.OnSelectNote) -> Unit,
    editNote: (ReaderEvent.OnEditNote) -> Unit,
    dismissBottomSheet: (ReaderEvent.OnDismissBottomSheet) -> Unit
) {
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
            Text(
                text = stringResource(R.string.notes),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (notes.isEmpty()) {
                StyledText(
                    text = stringResource(R.string.notes_empty),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(notes.size) { index ->
                        val note = notes[index]

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectNote(ReaderEvent.OnSelectNote(note)) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 4.dp, height = 44.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(note.color)
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                StyledText(
                                    text = note.selectedText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (note.note.isNotBlank()) {
                                    StyledText(
                                        text = note.note,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                StyledText(
                                    text = noteLocation(note, text),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { editNote(ReaderEvent.OnEditNote(note)) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit),
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
private fun noteLocation(note: Note, text: List<ReaderText>): String {
    val pageLabel = note.pageIndex?.let { stringResource(R.string.note_pdf_page, it + 1) }
    return when {
        pageLabel != null -> pageLabel
        note.segments.isNotEmpty() -> chapterTitleFor(text, note.segments.first().listItemIndex)
            ?: ""
        else -> ""
    }
}

/** Walks back from [index] to the enclosing chapter title. */
private fun chapterTitleFor(text: List<ReaderText>, index: Int): String? {
    if (text.isEmpty()) return null
    for (i in index.coerceIn(text.indices) downTo 0) {
        val title = (text[i] as? Chapter)?.title
        if (title != null) return title
    }
    return null
}
