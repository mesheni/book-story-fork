/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.domain.model.note.Note
import ua.acclorite.book_story.domain.model.reader.PdfWord
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.R

private const val MAX_RENDER_DIMENSION = 4096

/** Rendered pages cache limit: 1/8 of the heap, capped at 128 MB. */
private fun bitmapCacheLimitBytes(): Int =
    (Runtime.getRuntime().maxMemory() / 8).coerceAtMost(128L * 1024 * 1024).toInt()

/**
 * PdfRenderer is single-threaded and opening a page is expensive,
 * so every load goes through one lock and an LRU cache of rendered pages.
 * Pages are rendered at screen resolution: the native page size is ~72 DPI
 * and would be visibly blurry when stretched to the screen.
 * Bitmaps are never recycled — they may still be drawn after cache eviction,
 * on API 26+ pixel data lives on the heap and is reclaimed by GC.
 */
private class PdfPageLoader(
    private val renderer: PdfRenderer,
    private val lock: Mutex,
    private val cache: LruCache<Int, Bitmap>,
    private val targetWidthPx: Int
) {
    fun peek(pageIndex: Int): Bitmap? = cache.get(pageIndex)

    suspend fun load(pageIndex: Int): Bitmap = lock.withLock {
        cache.get(pageIndex)?.let { return it }
        val rendered = withContext(Dispatchers.IO) {
            renderer.openPage(pageIndex).use { page ->
                val scale = targetWidthPx.toFloat() / page.width
                var width = (page.width * scale).toInt().coerceAtLeast(1)
                var height = (page.height * scale).toInt().coerceAtLeast(1)
                if (width > MAX_RENDER_DIMENSION || height > MAX_RENDER_DIMENSION) {
                    val shrink = MAX_RENDER_DIMENSION.toFloat() / maxOf(width, height)
                    width = (width * shrink).toInt().coerceAtLeast(1)
                    height = (height * shrink).toInt().coerceAtLeast(1)
                }
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                    // Some PDFs never paint the page background; without an opaque
                    // backdrop such pages composite as black-on-black over the
                    // dark theme surface. Standard viewers use a white backdrop.
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(
                        bitmap,
                        null,
                        Matrix().apply {
                            setScale(width.toFloat() / page.width, height.toFloat() / page.height)
                        },
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )
                }
            }
        }
        currentCoroutineContext().ensureActive()
        cache.put(pageIndex, rendered)
        rendered
    }
}

@Composable
fun PdfViewerContent(
    uriString: String?,
    initialPage: Int,
    navigationRequest: Int,
    onPageChanged: (ReaderEvent.OnPdfPageChanged) -> Unit,
    isLoading: Boolean,
    viewerError: String?,
    onViewerInitialized: (ReaderEvent.OnPdfViewerInitialized) -> Unit,
    onToggleMenu: () -> Unit,
    notes: List<Note>,
    loadWordBoxes: suspend (Int) -> List<PdfWord>?,
    addPdfNote: (ReaderEvent.OnAddPdfNote) -> Unit,
    openTranslator: (ReaderEvent.OnOpenTranslator) -> Unit,
    openShareApp: (ReaderEvent.OnOpenShareApp) -> Unit
) {
    val context = LocalContext.current
    val currentOnToggleMenu by rememberUpdatedState(onToggleMenu)
    val rendererState = remember { mutableStateOf<PdfRenderer?>(null) }
    val descriptorState = remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember(uriString) { mutableStateOf(0) }
    var pageSizes by remember(uriString) { mutableStateOf<Array<Pair<Int, Int>>?>(null) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)
    val renderLock = remember { Mutex() }
    val closeScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    DisposableEffect(uriString) {
        onDispose {
            val renderer = rendererState.value
            val descriptor = descriptorState.value
            rendererState.value = null
            descriptorState.value = null
            // PdfRenderer single-threaded: close only after in-flight renders release the lock
            closeScope.launch {
                renderLock.withLock {
                    runCatching { renderer?.close() }
                    runCatching { descriptor?.close() }
                }
            }
        }
    }

    LaunchedEffect(uriString) {
        runCatching {
            withContext(Dispatchers.IO) {
                val uri = uriString?.toUri() ?: error("Missing PDF URI")
                val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: error("Unable to open PDF")
                try {
                    val renderer = PdfRenderer(descriptor)
                    // Page aspect ratios are needed before any bitmap is ready,
                    // so placeholders get real heights and the list is scrollable at once
                    val sizes = runCatching {
                        Array(renderer.pageCount) { index ->
                            renderer.openPage(index).use { page -> page.width to page.height }
                        }
                    }.getOrNull()
                    rendererState.value = renderer
                    descriptorState.value = descriptor
                    pageSizes = sizes
                    pageCount = renderer.pageCount
                    onViewerInitialized(ReaderEvent.OnPdfViewerInitialized(renderer.pageCount))
                } catch (error: Exception) {
                    descriptor.close()
                    throw error
                }
            }
        }.onFailure { error ->
            if (error is CancellationException) throw error
            onViewerInitialized(
                ReaderEvent.OnPdfViewerInitialized(
                    pageCount = 0,
                    error = error.message ?: "PDF open failed"
                )
            )
        }
    }

    LaunchedEffect(listState, pageCount) {
        if (pageCount <= 0) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { page ->
                onPageChanged(
                    ReaderEvent.OnPdfPageChanged(
                        page = page.coerceIn(0, pageCount - 1),
                        pageCount = pageCount
                    )
                )
            }
    }

    LaunchedEffect(navigationRequest, pageCount) {
        if (pageCount > 0) {
            listState.scrollToItem(initialPage.coerceIn(0, pageCount - 1))
        }
    }

    if (viewerError != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.pdf_viewer_error))
        }
        return
    }

    val activeRenderer = rendererState.value
    if (isLoading || pageCount == 0 || activeRenderer == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val targetWidthPx = with(LocalDensity.current) {
        (LocalConfiguration.current.screenWidthDp - 16).dp.toPx().toInt()
    }.coerceAtLeast(1)

    val pageLoader = remember(activeRenderer, targetWidthPx) {
        PdfPageLoader(
            renderer = activeRenderer,
            lock = renderLock,
            cache = object : LruCache<Int, Bitmap>(bitmapCacheLimitBytes()) {
                override fun sizeOf(key: Int, value: Bitmap): Int = value.byteCount
            },
            targetWidthPx = targetWidthPx
        )
    }

    LaunchedEffect(pageLoader, pageCount) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collectLatest { page ->
                // Mutex is FIFO: the visible page must enqueue before its
                // neighbors, otherwise it waits a whole render behind a prefetch
                if (pageLoader.peek(page) == null) {
                    runCatching { pageLoader.load(page) }
                }
                val next = page + 1
                if (next in 0 until pageCount && pageLoader.peek(next) == null) {
                    runCatching { pageLoader.load(next) }
                }
                val previous = page - 1
                if (previous in 0 until pageCount && pageLoader.peek(previous) == null) {
                    runCatching { pageLoader.load(previous) }
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { currentOnToggleMenu() })
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(pageCount) { pageIndex ->
            PdfPage(
                pageIndex = pageIndex,
                pageSize = pageSizes?.getOrNull(pageIndex),
                pageLoader = pageLoader,
                notes = notes,
                loadWordBoxes = loadWordBoxes,
                addPdfNote = addPdfNote,
                openTranslator = openTranslator,
                openShareApp = openShareApp
            )
        }
    }
}

@Composable
private fun PdfPage(
    pageIndex: Int,
    pageSize: Pair<Int, Int>?,
    pageLoader: PdfPageLoader,
    notes: List<Note>,
    loadWordBoxes: suspend (Int) -> List<PdfWord>?,
    addPdfNote: (ReaderEvent.OnAddPdfNote) -> Unit,
    openTranslator: (ReaderEvent.OnOpenTranslator) -> Unit,
    openShareApp: (ReaderEvent.OnOpenShareApp) -> Unit
) {
    var bitmap by remember(pageLoader, pageIndex) { mutableStateOf(pageLoader.peek(pageIndex)) }
    var words by remember(pageIndex) { mutableStateOf<List<PdfWord>?>(null) }

    LaunchedEffect(pageLoader, pageIndex) {
        bitmap = pageLoader.load(pageIndex)
    }

    LaunchedEffect(pageIndex) {
        words = loadWordBoxes(pageIndex)
    }

    val pageNotes = remember(notes, pageIndex) {
        notes.filter { it.pageIndex == pageIndex && it.rects.isNotEmpty() }
    }

    // Padding lives on the wrapper so the selection overlay maps 1:1
    // onto the rendered bitmap area.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        if (bitmap == null && pageSize != null && pageSize.first > 0 && pageSize.second > 0) {
            // Placeholder with the final aspect ratio: keeps LazyColumn extents
            // correct so the document is scrollable before bitmaps are ready
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(pageSize.first.toFloat() / pageSize.second)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        } else {
            bitmap?.let { renderedPage ->
                Image(
                    bitmap = renderedPage.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }
        }

        PdfSelectionOverlay(
            words = words,
            pageNotes = pageNotes,
            onAddNote = { selectedText, rects ->
                addPdfNote(
                    ReaderEvent.OnAddPdfNote(
                        pageIndex = pageIndex,
                        selectedText = selectedText,
                        rects = rects
                    )
                )
            },
            onTranslate = { text ->
                openTranslator(
                    ReaderEvent.OnOpenTranslator(
                        textToTranslate = text,
                        translateWholeParagraph = false
                    )
                )
            },
            onShare = { text ->
                openShareApp(
                    ReaderEvent.OnOpenShareApp(
                        textToShare = text
                    )
                )
            },
            modifier = Modifier.matchParentSize()
        )
    }
}
