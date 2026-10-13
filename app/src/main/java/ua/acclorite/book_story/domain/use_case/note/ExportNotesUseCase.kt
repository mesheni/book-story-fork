/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.note

import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.model.note.Note
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.domain.model.reader.ReaderText.Chapter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Renders the book's notes (quotes + comments) as a Markdown
 * document for sharing. [pdfPageLabel] formats a 0-based PDF
 * page index into a localized label.
 */
class ExportNotesUseCase @Inject constructor() {

    operator fun invoke(
        book: Book,
        author: String,
        notes: List<Note>,
        text: List<ReaderText>,
        pdfPageLabel: (Int) -> String
    ): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())

        return buildString {
            appendLine("# ${book.title}")
            if (author.isNotBlank()) {
                appendLine()
                appendLine("_${author}_")
            }
            appendLine()

            notes.forEach { note ->
                appendLine("---")
                appendLine()
                appendLine("> ${note.selectedText.replace("\n", "\n> ")}")
                appendLine()

                noteLocation(note, text, pdfPageLabel)?.let { location ->
                    appendLine("*$location*")
                    appendLine()
                }

                if (note.note.isNotBlank()) {
                    appendLine(note.note)
                    appendLine()
                }

                appendLine(
                    "<sub>${formatter.format(Instant.ofEpochMilli(note.createdAt))}</sub>"
                )
                appendLine()
            }
        }
    }

    private fun noteLocation(
        note: Note,
        text: List<ReaderText>,
        pdfPageLabel: (Int) -> String
    ): String? {
        note.pageIndex?.let { return pdfPageLabel(it) }

        val anchor = note.segments.firstOrNull()?.listItemIndex ?: return null
        for (i in anchor.coerceIn(text.indices) downTo 0) {
            val title = (text[i] as? Chapter)?.title
            if (title != null) return title
        }
        return null
    }
}
