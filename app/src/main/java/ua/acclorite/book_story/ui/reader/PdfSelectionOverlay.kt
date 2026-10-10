/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.note.Note
import ua.acclorite.book_story.domain.model.note.NoteRect
import ua.acclorite.book_story.domain.model.reader.PdfWord
import ua.acclorite.book_story.ui.common.helpers.showToast

/**
 * Selection layer over a rendered PDF page.
 *
 * Long-press starts a word-snapped selection, dragging extends it by whole
 * words; after release a floating toolbar offers copy/translate/share and
 * adding the quote to notes. Saved note highlights of the page are drawn
 * underneath the active selection.
 */
@Composable
fun PdfSelectionOverlay(
    words: List<PdfWord>?,
    pageNotes: List<Note>,
    onAddNote: (selectedText: String, rects: List<NoteRect>) -> Unit,
    onTranslate: (String) -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var selectionRange by remember { mutableStateOf<IntRange?>(null) }
    var anchorIndex by remember { mutableStateOf(-1) }
    var committed by remember { mutableStateOf(false) }

    fun wordRect(index: Int): Rect? {
        if (overlaySize.width == 0 || overlaySize.height == 0) return null
        val word = words?.getOrNull(index) ?: return null
        return Rect(
            left = word.left * overlaySize.width,
            top = word.top * overlaySize.height,
            right = word.right * overlaySize.width,
            bottom = word.bottom * overlaySize.height
        )
    }

    fun hitTest(position: Offset): Int {
        if (words.isNullOrEmpty()) return -1
        val tolerance = with(density) { 8.dp.toPx() }
        // With the tolerance the padded rectangles of adjacent lines
        // overlap; prefer the smallest word containing the point.
        var best = -1
        var bestArea = Float.MAX_VALUE
        words.indices.forEach { index ->
            val rect = wordRect(index) ?: return@forEach
            if (
                position.x >= rect.left - tolerance &&
                position.x <= rect.right + tolerance &&
                position.y >= rect.top - tolerance &&
                position.y <= rect.bottom + tolerance
            ) {
                val area = rect.width * rect.height
                if (area < bestArea) {
                    bestArea = area
                    best = index
                }
            }
        }
        return best
    }

    fun clearSelection() {
        selectionRange = null
        anchorIndex = -1
        committed = false
    }

    Box(
        modifier
            .onSizeChanged { overlaySize = it }
            // While a selection is committed, a tap clears it instead of
            // toggling the reader menu; otherwise taps pass through to
            // the LazyColumn tap handler.
            .then(
                if (committed) {
                    Modifier.pointerInput(words) {
                        detectTapGestures { clearSelection() }
                    }
                } else Modifier
            )
            .pointerInput(words) {
                if (words.isNullOrEmpty()) return@pointerInput
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        committed = false
                        val hit = hitTest(offset)
                        if (hit >= 0) {
                            anchorIndex = hit
                            selectionRange = hit..hit
                        } else {
                            anchorIndex = -1
                            selectionRange = null
                        }
                    },
                    onDrag = { change, _ ->
                        val hit = hitTest(change.position)
                        if (hit >= 0 && anchorIndex >= 0) {
                            selectionRange = min(anchorIndex, hit)..max(anchorIndex, hit)
                        }
                    },
                    onDragEnd = {
                        selectionRange
                            ?.takeIf { !it.isEmpty() && !words.isNullOrEmpty() }
                            ?.let { committed = true }
                    },
                    onDragCancel = { clearSelection() }
                )
            }
    ) {
        Canvas(Modifier.matchParentSize()) {
            pageNotes.forEach { note ->
                note.rects.forEach { rect ->
                    drawRoundRect(
                        color = note.color.copy(alpha = 0.35f),
                        topLeft = Offset(rect.left * size.width, rect.top * size.height),
                        size = Size(
                            (rect.right - rect.left) * size.width,
                            (rect.bottom - rect.top) * size.height
                        ),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }
            selectionRange?.let { range ->
                for (index in range) {
                    val word = words?.getOrNull(index) ?: continue
                    drawRoundRect(
                        color = Note.DEFAULT_COLOR.copy(alpha = 0.45f),
                        topLeft = Offset(word.left * size.width, word.top * size.height),
                        size = Size(
                            (word.right - word.left) * size.width,
                            (word.bottom - word.top) * size.height
                        ),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }
            }
        }

        val activeRange = selectionRange
        if (committed && activeRange != null && !words.isNullOrEmpty()) {
            val selectedWords = words.subList(activeRange.first, activeRange.last + 1)
            val selectionText = selectedWords.joinToString(" ") { it.text }
            val selectionRects = selectedWords.map {
                NoteRect(left = it.left, top = it.top, right = it.right, bottom = it.bottom)
            }

            val normalizedBounds = selectedWords.fold(
                Rect(Float.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)
            ) { acc, word ->
                Rect(
                    left = min(acc.left, word.left),
                    top = min(acc.top, word.top),
                    right = max(acc.right, word.right),
                    bottom = max(acc.bottom, word.bottom)
                )
            }

            PdfSelectionToolbar(
                selectionBounds = Rect(
                    left = normalizedBounds.left * overlaySize.width,
                    top = normalizedBounds.top * overlaySize.height,
                    right = normalizedBounds.right * overlaySize.width,
                    bottom = normalizedBounds.bottom * overlaySize.height
                ),
                containerWidth = overlaySize.width,
                onCopy = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("pdf_selection", selectionText))
                    context.getString(R.string.copied)
                        .showToast(context = context, longToast = false)
                    clearSelection()
                },
                onAddNote = {
                    onAddNote(selectionText, selectionRects)
                    clearSelection()
                },
                onTranslate = {
                    onTranslate(selectionText)
                    clearSelection()
                },
                onShare = {
                    onShare(selectionText)
                    clearSelection()
                }
            )
        }
    }
}

@Composable
private fun PdfSelectionToolbar(
    selectionBounds: Rect,
    containerWidth: Int,
    onCopy: () -> Unit,
    onAddNote: () -> Unit,
    onTranslate: () -> Unit,
    onShare: () -> Unit
) {
    val density = LocalDensity.current
    val barWidthPx = with(density) { 216.dp.toPx() }
    val barHeightPx = with(density) { 48.dp.toPx() }
    val marginPx = with(density) { 8.dp.toPx() }

    val x = (selectionBounds.center.x - barWidthPx / 2f)
        .roundToInt()
        .coerceIn(0, (containerWidth - barWidthPx).roundToInt().coerceAtLeast(0))
    val y = if (selectionBounds.top - barHeightPx - marginPx >= 0f) {
        (selectionBounds.top - barHeightPx - marginPx).roundToInt()
    } else {
        (selectionBounds.bottom + marginPx).roundToInt()
    }

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(x, y),
        properties = PopupProperties(focusable = false),
        onDismissRequest = {}
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row {
                IconButton(onClick = onCopy) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy)
                    )
                }
                IconButton(onClick = onAddNote) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Notes,
                        contentDescription = stringResource(R.string.add_to_notes)
                    )
                }
                IconButton(onClick = onTranslate) {
                    Icon(
                        Icons.Default.Translate,
                        contentDescription = stringResource(R.string.translate)
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = stringResource(R.string.share)
                    )
                }
            }
        }
    }
}
