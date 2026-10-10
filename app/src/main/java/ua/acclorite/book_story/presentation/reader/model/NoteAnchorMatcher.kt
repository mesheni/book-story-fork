/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader.model

import ua.acclorite.book_story.domain.model.note.NoteSegment
import ua.acclorite.book_story.domain.model.reader.ReaderText

/**
 * A paragraph with its whitespace-normalized form. [indexMap][i] is the
 * original string index of the i-th normalized character; a whitespace run
 * collapses into a single space mapped to the run's first character.
 */
private class Paragraph(val listItemIndex: Int, original: String) {
    val normalized: String
    val indexMap: IntArray

    init {
        val chars = StringBuilder()
        val map = mutableListOf<Int>()
        var runStart = -1
        original.forEachIndexed { index, char ->
            if (char.isWhitespace()) {
                if (runStart == -1 && chars.isNotEmpty()) runStart = index
            } else {
                if (runStart != -1) {
                    chars.append(' ')
                    map.add(runStart)
                    runStart = -1
                }
                chars.append(char)
                map.add(index)
            }
        }
        normalized = chars.toString()
        indexMap = map.toIntArray()
    }
}

/**
 * Resolves a text selection to char ranges inside the loaded book text.
 *
 * Compose's SelectionContainer only hands out the selected string, so the
 * anchor is recovered by matching that string against the paragraphs.
 * Matching is whitespace-insensitive: runs of whitespace collapse into a
 * single space and a paragraph boundary counts as one space, which covers
 * selections spanning several paragraphs (the selection string joins them
 * with a newline).
 */
object NoteAnchorMatcher {

    /** How many paragraphs past the start one to scan while matching. */
    private const val MAX_LOOKAHEAD_PARAGRAPHS = 200

    /**
     * @param selection the selected string.
     * @param text the loaded reader text (the LazyColumn items).
     * @param fromIndex list index to start the search from (usually the
     * first visible item); on miss the search wraps around to the start.
     * @return the char segments of the first match, or an empty list when
     * the selection could not be located (the note is then saved unanchored).
     */
    fun findSegments(
        selection: String,
        text: List<ReaderText>,
        fromIndex: Int
    ): List<NoteSegment> {
        val target = Paragraph(-1, selection).normalized
        if (target.isEmpty()) return emptyList()

        val paragraphs = mutableListOf<Paragraph>()
        text.forEachIndexed { index, entry ->
            if (entry is ReaderText.Text) {
                paragraphs += Paragraph(index, entry.line.text)
            }
        }
        if (paragraphs.isEmpty()) return emptyList()

        // Past the last paragraph (the first visible item is an image or
        // a separator at the end of the book) start from the last paragraph
        // instead of the beginning, so repeated phrases anchor nearby.
        val startPos = paragraphs.indexOfFirst { it.listItemIndex >= fromIndex }
            .let { if (it == -1) paragraphs.lastIndex else it }

        for (offset in paragraphs.indices) {
            val start = (startPos + offset) % paragraphs.size
            val first = paragraphs[start]
            for (position in first.normalized.indices) {
                val segments = tryMatch(target, paragraphs, start, position)
                if (segments != null) return segments
            }
        }
        return emptyList()
    }

    private fun tryMatch(
        target: String,
        paragraphs: List<Paragraph>,
        startParagraph: Int,
        startPosition: Int
    ): List<NoteSegment>? {
        val segments = mutableListOf<NoteSegment>()
        var targetIndex = 0
        var paragraphIndex = startParagraph
        var charIndex = startPosition
        var segmentStart = -1
        var segmentEnd = -1

        fun closeSegment() {
            if (segmentStart != -1) {
                segments += NoteSegment(
                    listItemIndex = paragraphs[paragraphIndex].listItemIndex,
                    start = segmentStart,
                    end = segmentEnd + 1
                )
                segmentStart = -1
                segmentEnd = -1
            }
        }

        while (targetIndex < target.length) {
            if (paragraphIndex - startParagraph > MAX_LOOKAHEAD_PARAGRAPHS) return null

            val paragraph = paragraphs.getOrNull(paragraphIndex) ?: return null

            if (charIndex >= paragraph.normalized.length) {
                // Paragraph boundary: a space in the target is consumed by
                // the boundary itself, then matching continues on the next one.
                closeSegment()
                if (target[targetIndex] == ' ') targetIndex++
                paragraphIndex++
                charIndex = 0
                continue
            }

            if (paragraph.normalized[charIndex] != target[targetIndex]) return null

            if (segmentStart == -1) segmentStart = paragraph.indexMap[charIndex]
            segmentEnd = paragraph.indexMap[charIndex]

            targetIndex++
            charIndex++
        }

        closeSegment()
        return segments
    }
}
