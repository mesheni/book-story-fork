/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.mapper.note

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject
import ua.acclorite.book_story.data.local.dto.NoteEntity
import ua.acclorite.book_story.domain.model.note.Note
import ua.acclorite.book_story.domain.model.note.NoteRect
import ua.acclorite.book_story.domain.model.note.NoteSegment
import javax.inject.Inject

interface NoteMapper {
    fun toNoteEntity(note: Note): NoteEntity
    fun toNote(entity: NoteEntity): Note
}

/**
 * Anchor payloads ([Note.segments], [Note.rects]) are stored as JSON built
 * with the platform org.json classes — the project does not apply the
 * kotlinx.serialization compiler plugin, only the runtime library.
 */
class NoteMapperImpl @Inject constructor() : NoteMapper {

    override fun toNoteEntity(note: Note): NoteEntity {
        return NoteEntity(
            id = note.id,
            bookId = note.bookId,
            selectedText = note.selectedText,
            note = note.note,
            color = note.color.toArgb(),
            chapterIndex = note.chapterIndex,
            segmentsJson = if (note.segments.isEmpty()) null else encodeSegments(note.segments),
            pageIndex = note.pageIndex,
            rectsJson = if (note.rects.isEmpty()) null else encodeRects(note.rects),
            createdAt = note.createdAt
        )
    }

    override fun toNote(entity: NoteEntity): Note {
        return Note(
            id = entity.id,
            bookId = entity.bookId,
            selectedText = entity.selectedText,
            note = entity.note,
            color = Color(entity.color),
            chapterIndex = entity.chapterIndex,
            segments = decodeSegments(entity.segmentsJson),
            pageIndex = entity.pageIndex,
            rects = decodeRects(entity.rectsJson),
            createdAt = entity.createdAt
        )
    }

    private fun encodeSegments(segments: List<NoteSegment>): String {
        val array = JSONArray()
        segments.forEach { segment ->
            array.put(
                JSONObject()
                    .put("i", segment.listItemIndex)
                    .put("s", segment.start)
                    .put("e", segment.end)
            )
        }
        return array.toString()
    }

    private fun decodeSegments(json: String?): List<NoteSegment> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                NoteSegment(
                    listItemIndex = item.getInt("i"),
                    start = item.getInt("s"),
                    end = item.getInt("e")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeRects(rects: List<NoteRect>): String {
        val array = JSONArray()
        rects.forEach { rect ->
            array.put(
                JSONObject()
                    .put("l", rect.left.toDouble())
                    .put("t", rect.top.toDouble())
                    .put("r", rect.right.toDouble())
                    .put("b", rect.bottom.toDouble())
            )
        }
        return array.toString()
    }

    private fun decodeRects(json: String?): List<NoteRect> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                NoteRect(
                    left = item.getDouble("l").toFloat(),
                    top = item.getDouble("t").toFloat(),
                    right = item.getDouble("r").toFloat(),
                    bottom = item.getDouble("b").toFloat()
                )
            }
        }.getOrDefault(emptyList())
    }
}
