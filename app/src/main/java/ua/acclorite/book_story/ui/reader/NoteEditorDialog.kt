/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.note.Note
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.ui.common.components.common.StyledText

/**
 * Create/edit dialog for a note: read-only quote, an optional comment
 * and a fixed highlight color palette.
 */
@Composable
fun NoteEditorDialog(
    note: Note,
    saveNote: (ReaderEvent.OnSaveNote) -> Unit,
    deleteNote: (ReaderEvent.OnDeleteNote) -> Unit,
    dismiss: (ReaderEvent.OnDismissNoteEditor) -> Unit
) {
    var comment by remember(note.id) { mutableStateOf(note.note) }
    var color by remember(note.id) { mutableStateOf(note.color) }

    AlertDialog(
        onDismissRequest = { dismiss(ReaderEvent.OnDismissNoteEditor) },
        title = { Text(stringResource(R.string.note_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StyledText(
                    text = note.selectedText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .heightIn(max = 128.dp)
                        .verticalScroll(rememberScrollState())
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Note.COLORS.forEach { paletteColor ->
                        val selected = paletteColor == color
                        Box(
                            modifier = Modifier
                                .size(if (selected) 32.dp else 28.dp)
                                .clip(CircleShape)
                                .background(paletteColor)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = CircleShape
                                )
                                .clickable { color = paletteColor },
                            contentAlignment = Alignment.Center
                        ) {}
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(stringResource(R.string.note_comment_hint))
                    },
                    minLines = 2,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    saveNote(
                        ReaderEvent.OnSaveNote(
                            note.copy(note = comment.trim(), color = color)
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.note_save))
            }
        },
        dismissButton = {
            Row {
                if (note.id != 0) {
                    TextButton(
                        onClick = { deleteNote(ReaderEvent.OnDeleteNote(note.id)) }
                    ) {
                        Text(
                            stringResource(R.string.delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                TextButton(
                    onClick = { dismiss(ReaderEvent.OnDismissNoteEditor) }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}
