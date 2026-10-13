/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.runtime.Composable
import ua.acclorite.book_story.core.BottomSheet
import ua.acclorite.book_story.domain.model.bookmark.Bookmark
import ua.acclorite.book_story.domain.model.note.Note
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.presentation.reader.ReaderScreen

@Composable
fun ReaderBottomSheet(
    bottomSheet: BottomSheet?,
    notes: List<Note>,
    bookmarks: List<Bookmark>,
    text: List<ReaderText>,
    menuVisibility: (ReaderEvent.OnMenuVisibility) -> Unit,
    selectNote: (ReaderEvent.OnSelectNote) -> Unit,
    editNote: (ReaderEvent.OnEditNote) -> Unit,
    exportNotes: (ReaderEvent.OnExportNotes) -> Unit,
    addBookmark: (ReaderEvent.OnAddBookmark) -> Unit,
    selectBookmark: (ReaderEvent.OnSelectBookmark) -> Unit,
    deleteBookmark: (ReaderEvent.OnDeleteBookmark) -> Unit,
    dismissBottomSheet: (ReaderEvent.OnDismissBottomSheet) -> Unit
) {
    when (bottomSheet) {
        ReaderScreen.SETTINGS_BOTTOM_SHEET -> {
            ReaderSettingsBottomSheet(
                menuVisibility = menuVisibility,
                dismissBottomSheet = dismissBottomSheet
            )
        }

        ReaderScreen.NOTES_BOTTOM_SHEET -> {
            NotesBottomSheet(
                notes = notes,
                text = text,
                selectNote = selectNote,
                editNote = editNote,
                exportNotes = exportNotes,
                dismissBottomSheet = dismissBottomSheet
            )
        }

        ReaderScreen.BOOKMARKS_BOTTOM_SHEET -> {
            BookmarksBottomSheet(
                bookmarks = bookmarks,
                addBookmark = addBookmark,
                selectBookmark = selectBookmark,
                deleteBookmark = deleteBookmark,
                dismissBottomSheet = dismissBottomSheet
            )
        }
    }
}
