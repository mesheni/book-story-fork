/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
sealed class ReaderEffect {
    data class OnSystemBarsVisibility(
        val show: Boolean?
    ) : ReaderEffect()

    data object OnResetBrightness : ReaderEffect()

    /** Share the exported notes file (FileProvider uri). */
    data class OnShareNotes(
        val uri: Uri
    ) : ReaderEffect()

    /** Notes export failed (the file could not be written). */
    data object OnNotesExportError : ReaderEffect()

    data class OnOpenTranslator(
        val textToTranslate: String,
        val translateWholeParagraph: Boolean
    ) : ReaderEffect()

    data class OnOpenShareApp(
        val textToShare: String
    ) : ReaderEffect()

    data class OnOpenWebBrowser(
        val textToSearch: String
    ) : ReaderEffect()

    data class OnOpenDictionary(
        val textToDefine: String
    ) : ReaderEffect()

    data object OnNavigateBack : ReaderEffect()

    data class OnNavigateToBookInfo(
        val changePath: Boolean
    ) : ReaderEffect()
}