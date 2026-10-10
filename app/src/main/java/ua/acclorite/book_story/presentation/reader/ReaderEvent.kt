/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import androidx.compose.runtime.Immutable
import ua.acclorite.book_story.domain.model.library.PdfOpenMode
import ua.acclorite.book_story.domain.model.note.Note
import ua.acclorite.book_story.domain.model.note.NoteRect
import ua.acclorite.book_story.domain.model.reader.ReaderText.Chapter
import ua.acclorite.book_story.presentation.reader.model.Checkpoint

@Immutable
sealed class ReaderEvent {

    data object OnLoadText : ReaderEvent()

    data class OnSelectPdfMode(
        val mode: PdfOpenMode
    ) : ReaderEvent()

    data object OnShowPdfModeDialog : ReaderEvent()

    data object OnDismissPdfModeDialog : ReaderEvent()

    data class OnPdfPageChanged(
        val page: Int,
        val pageCount: Int
    ) : ReaderEvent()

    data class OnPdfViewerInitialized(val pageCount: Int, val error: String? = null) : ReaderEvent()

    data object OnShowPdfSearch : ReaderEvent()

    data object OnDismissPdfSearch : ReaderEvent()

    data class OnPdfSearchQueryChanged(val query: String) : ReaderEvent()

    data class OnSelectPdfSearchResult(val page: Int) : ReaderEvent()

    data object OnRestoreScroll : ReaderEvent()

    data class OnMenuVisibility(
        val show: Boolean,
        val saveCheckpoint: Boolean
    ) : ReaderEvent()

    data class OnChangeProgress(
        val progress: Float,
        val firstVisibleItemIndex: Int,
        val firstVisibleItemOffset: Int
    ) : ReaderEvent()

    data class OnUpdateChapter(
        val index: Int
    ) : ReaderEvent()

    data class OnScrollToChapter(
        val chapter: Chapter
    ) : ReaderEvent()

    data class OnScroll(
        val progress: Float
    ) : ReaderEvent()

    data class OnRestoreCheckpoint(
        val checkpoint: Checkpoint
    ) : ReaderEvent()

    data class OnLeave(
        val navigate: () -> Unit
    ) : ReaderEvent()

    data class OnOpenTranslator(
        val textToTranslate: String,
        val translateWholeParagraph: Boolean
    ) : ReaderEvent()

    data class OnOpenShareApp(
        val textToShare: String
    ) : ReaderEvent()

    data class OnOpenWebBrowser(
        val textToSearch: String
    ) : ReaderEvent()

    data class OnOpenDictionary(
        val textToDefine: String
    ) : ReaderEvent()

    /**
     * The user picked "add to notes" on a text selection; the anchor
     * is resolved against the loaded text from the visible position.
     */
    data class OnAddTextNote(
        val selectedText: String
    ) : ReaderEvent()

    /** The user picked "add to notes" on a selection in the native PDF viewer. */
    data class OnAddPdfNote(
        val pageIndex: Int,
        val selectedText: String,
        val rects: List<NoteRect>
    ) : ReaderEvent()

    /** Insert (id == 0) or update an existing note. */
    data class OnSaveNote(
        val note: Note
    ) : ReaderEvent()

    data class OnDeleteNote(
        val noteId: Int
    ) : ReaderEvent()

    data class OnEditNote(
        val note: Note
    ) : ReaderEvent()

    data object OnDismissNoteEditor : ReaderEvent()

    data object OnShowNotesBottomSheet : ReaderEvent()

    /** Jump to the note location and close the notes sheet. */
    data class OnSelectNote(
        val note: Note
    ) : ReaderEvent()

    data object OnShowSettingsBottomSheet : ReaderEvent()

    data object OnDismissBottomSheet : ReaderEvent()

    data object OnShowChaptersDrawer : ReaderEvent()

    data object OnDismissDrawer : ReaderEvent()

    data object OnNavigateBack : ReaderEvent()

    data class OnNavigateToBookInfo(
        val changePath: Boolean
    ) : ReaderEvent()
}