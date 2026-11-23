/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */
package ua.acclorite.book_story.ui.reader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.ahmer.pdfviewer.PDFView
import com.ahmer.pdfviewer.listener.OnDrawListener
import com.ahmer.pdfviewer.listener.OnErrorListener
import com.ahmer.pdfviewer.listener.OnLoadCompleteListener
import com.ahmer.pdfviewer.listener.OnLongPressListener
import com.ahmer.pdfviewer.listener.OnPageChangeListener
import com.ahmer.pdfviewer.listener.OnPageErrorListener
import com.ahmer.pdfviewer.listener.OnPageScrollListener
import com.ahmer.pdfviewer.listener.OnTapListener
import com.ahmer.pdfviewer.util.FitPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.note.Note
import ua.acclorite.book_story.domain.model.note.NoteRect
import ua.acclorite.book_story.domain.model.reader.PdfWord
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import kotlin.math.max
import kotlin.math.min
import java.io.File
import kotlin.coroutines.coroutineContext

/** Intercepts a gesture only after PDFView has recognized a long press. */
private class PdfGestureHost(context: Context) : FrameLayout(context) {
    var selecting = false
    var cancelSent = false
    var onMove: ((Float, Float) -> Unit)? = null
    var onRelease: (() -> Unit)? = null

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (selecting) {
            if (!cancelSent) {
                val cancel = MotionEvent.obtain(event)
                cancel.action = MotionEvent.ACTION_CANCEL
                super.dispatchTouchEvent(cancel)
                cancel.recycle()
                cancelSent = true
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> onMove?.invoke(event.x, event.y)
                MotionEvent.ACTION_UP -> { selecting = false; onRelease?.invoke() }
                MotionEvent.ACTION_CANCEL -> { selecting = false; onRelease?.invoke() }
            }
            return true
        }
        return super.dispatchTouchEvent(event)
    }
}

private class PdfSelectionController(
    private val host: PdfGestureHost,
    private val view: PDFView,
    private val scope: CoroutineScope,
    private val loadWords: suspend (Int) -> List<PdfWord>?,
    private val addNote: (Int, String, List<NoteRect>) -> Unit,
    private val translate: (String) -> Unit,
    private val share: (String) -> Unit
) {
    private var page = -1
    private var words: List<PdfWord> = emptyList()
    private var first = -1
    private var last = -1
    private var released = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var loadJob: Job? = null
    private var popup: PopupMenu? = null
    private var popupAnchor: View? = null

    init {
        host.onMove = ::move
        host.onRelease = { released = true; if (first >= 0) showMenu() }
    }

    fun clear() {
        val openPopup = popup
        popup = null
        openPopup?.dismiss()
        popupAnchor?.let { host.removeView(it) }
        popupAnchor = null
        loadJob?.cancel()
        page = -1
        words = emptyList()
        first = -1
        last = -1
        host.selecting = false
        view.invalidate()
    }

    private fun geometry(pageIndex: Int): PdfPageGeometry? {
        val file = view.pdfFile ?: return null
        if (pageIndex !in 0 until file.pagesCount) return null
        val zoom = view.zoom
        val size = file.getScaledPageSize(pageIndex, zoom)
        return PdfPageGeometry(
            view.currentXOffset + file.getSecondaryPageOffset(pageIndex, zoom),
            view.currentYOffset + file.getPageOffset(pageIndex, zoom),
            size.width, size.height
        )
    }

    fun start(x: Float, y: Float) {
        clear()
        val file = view.pdfFile ?: return
        val pageIndex = file.getPageAtOffset(y - view.currentYOffset, view.zoom)
        val initialPoint = geometry(pageIndex)?.normalize(x, y) ?: return
        page = pageIndex
        lastTouchX = x
        lastTouchY = y
        released = false
        host.cancelSent = false
        host.selecting = true
        loadJob = scope.launch {
            words = loadWords(pageIndex).orEmpty()
            if (page != pageIndex) return@launch
            first = hit(initialPoint.first, initialPoint.second)
            last = first
            if (first < 0) {
                Toast.makeText(host.context, R.string.pdf_no_text_layer, Toast.LENGTH_SHORT).show()
                clear()
            } else {
                move(lastTouchX, lastTouchY)
                view.invalidate()
                if (released) showMenu()
            }
        }
    }

    private fun hit(x: Float, y: Float): Int = words.indices
        .filter { index ->
            val w = words[index]
            x >= w.left - .01f && x <= w.right + .01f && y >= w.top - .01f && y <= w.bottom + .01f
        }
        .minByOrNull { index -> (words[index].right - words[index].left) * (words[index].bottom - words[index].top) }
        ?: -1

    private fun move(x: Float, y: Float) {
        lastTouchX = x
        lastTouchY = y
        if (first < 0) return
        val point = geometry(page)?.normalize(x, y) ?: return
        val index = hit(point.first, point.second)
        if (index >= 0) { last = index; view.invalidate() }
    }

    fun draw(canvas: Canvas, pageIndex: Int, width: Float, height: Float) {
        if (page != pageIndex || first < 0 || last < 0) return
        val paint = Paint().apply { color = Note.DEFAULT_COLOR.copy(alpha = .45f).toArgb() }
        for (index in min(first, last)..max(first, last)) {
            val word = words[index]
            canvas.drawRect(word.left * width, word.top * height, word.right * width, word.bottom * height, paint)
        }
    }

    private fun showMenu() {
        if (first < 0 || last < 0) return
        val selection = words.subList(min(first, last), max(first, last) + 1)
        val text = selection.joinToString(" ") { it.text }
        val rects = selection.map { NoteRect(it.left, it.top, it.right, it.bottom) }
        val selectedPage = page
        val bounds = geometry(page) ?: return
        val anchor = View(host.context)
        val params = FrameLayout.LayoutParams(1, 1).apply {
            leftMargin = (bounds.left + selection.first().left * bounds.width).toInt().coerceIn(0, max(0, host.width - 1))
            topMargin = (bounds.top + selection.first().bottom * bounds.height).toInt().coerceIn(0, max(0, host.height - 1))
        }
        host.addView(anchor, params)
        popupAnchor = anchor
        anchor.post {
        if (page != selectedPage || !host.isAttachedToWindow || popupAnchor !== anchor) return@post
        PopupMenu(host.context, anchor).apply {
            popup = this
            menu.add(0, 1, 0, R.string.copy)
            menu.add(0, 2, 1, R.string.add_to_notes)
            menu.add(0, 3, 2, R.string.translate)
            menu.add(0, 4, 3, R.string.share)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    1 -> (host.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("pdf_selection", text))
                    2 -> addNote(selectedPage, text, rects)
                    3 -> translate(text)
                    4 -> share(text)
                }
                clear()
                true
            }
            setOnDismissListener { clear() }
            show()
        } }
    }
}

@Composable
internal fun PdfNativeViewer(
    uriString: String?,
    initialPage: Int,
    navigationRequest: Int,
    onPageChanged: (ReaderEvent.OnPdfPageChanged) -> Unit,
    onViewerInitialized: (ReaderEvent.OnPdfViewerInitialized) -> Unit,
    onToggleMenu: () -> Unit,
    notes: List<Note>,
    loadWordBoxes: suspend (Int) -> List<PdfWord>?,
    addPdfNote: (ReaderEvent.OnAddPdfNote) -> Unit,
    openTranslator: (ReaderEvent.OnOpenTranslator) -> Unit,
    openShareApp: (ReaderEvent.OnOpenShareApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val currentNotes by rememberUpdatedState(notes)
    val currentLoadWords by rememberUpdatedState(loadWordBoxes)
    val currentPageChanged by rememberUpdatedState(onPageChanged)
    val currentInitialized by rememberUpdatedState(onViewerInitialized)
    val currentToggle by rememberUpdatedState(onToggleMenu)
    val currentAdd by rememberUpdatedState(addPdfNote)
    val currentTranslate by rememberUpdatedState(openTranslator)
    val currentShare by rememberUpdatedState(openShareApp)
    val latestPage by rememberUpdatedState(initialPage)
    var view by remember(uriString) { mutableStateOf<PDFView?>(null) }
    var controller by remember(uriString) { mutableStateOf<PdfSelectionController?>(null) }
    var loading by remember(uriString) { mutableStateOf(true) }
    val navigation = remember(uriString) { PdfNavigationState(initialPage) }
    var disposed by remember(uriString) { mutableStateOf(false) }
    var savedZoom by rememberSaveable(uriString) { mutableStateOf(1f) }
    var temporaryFile by remember(uriString) { mutableStateOf<File?>(null) }
    var fallbackJob by remember(uriString) { mutableStateOf<Job?>(null) }
    var pageError by remember(uriString) { mutableStateOf<Int?>(null) }
    var retryPage by remember(uriString) { mutableStateOf<(() -> Unit)?>(null) }

    DisposableEffect(uriString) {
        disposed = false
        onDispose {
            disposed = true
            fallbackJob?.cancel()
            controller?.clear()
            view?.recycle()
            temporaryFile?.delete()
        }
    }

    LaunchedEffect(navigationRequest, view) {
        val activeView = view
        val target = navigation.request(initialPage)
        if (activeView?.pdfFile != null && target != null) {
            controller?.clear()
            activeView.jumpTo(target.coerceIn(0, activeView.pdfFile!!.pagesCount - 1))
        }
    }

    Box(modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PdfGestureHost(context).also { host ->
                    val restoredZoom = savedZoom
                    val pdf = PDFView(context, null)
                    host.addView(pdf, FrameLayout.LayoutParams(-1, -1))
                    view = pdf
                    pdf.setMinZoom(1f); pdf.setMidZoom(2f); pdf.setMaxZoom(4f)
                    val selection = PdfSelectionController(
                        host, pdf, scope,
                        loadWords = { currentLoadWords(it) },
                        addNote = { page, text, rects -> currentAdd(ReaderEvent.OnAddPdfNote(page, text, rects)) },
                        translate = { currentTranslate(ReaderEvent.OnOpenTranslator(it, false)) },
                        share = { currentShare(ReaderEvent.OnOpenShareApp(it)) }
                    )
                    controller = selection
                    val uri = uriString?.toUri()
                    if (uri == null) {
                        loading = false
                        currentInitialized(ReaderEvent.OnPdfViewerInitialized(0, "Missing PDF URI"))
                    } else {
                        var fallbackStarted = false
                        lateinit var openDocument: (File?) -> Unit
                        fun fail(error: Throwable?) {
                            if (!disposed) {
                                loading = false
                                currentInitialized(ReaderEvent.OnPdfViewerInitialized(0, error?.message ?: "PDF open failed"))
                            }
                        }
                        openDocument = { file ->
                        (if (file == null) pdf.fromUri(uri) else pdf.fromFile(file))
                        .defaultPage(latestPage.coerceAtLeast(0))
                        .pageFitPolicy(FitPolicy.WIDTH).fitEachPage(true)
                        .pageSnap(false).pageFling(false).enableDoubleTap(true)
                        .onLoad(object : OnLoadCompleteListener {
                            override fun loadComplete(totalPages: Int) {
                                if (!disposed) {
                                    if (totalPages <= 0) {
                                        fail(IllegalStateException("PDF has no pages"))
                                        return
                                    }
                                    currentInitialized(ReaderEvent.OnPdfViewerInitialized(totalPages))
                                    // The library applies its default page after this callback.
                                    // Apply any newer request after that initial jump.
                                    pdf.post {
                                        if (!disposed) {
                                            if (restoredZoom > 1f) pdf.zoomTo(restoredZoom.coerceIn(1f, 4f))
                                            val target = navigation.finishLoading(totalPages)
                                            pdf.jumpTo(target)
                                            loading = false
                                        }
                                    }
                                }
                            }
                        })
                        .onPageChange(object : OnPageChangeListener {
                            override fun onPageChanged(page: Int, totalPages: Int) {
                                if (!disposed && navigation.ready) {
                                    selection.clear()
                                    currentPageChanged(ReaderEvent.OnPdfPageChanged(page, totalPages))
                                }
                            }
                        })
                        .onPageScroll(object : OnPageScrollListener {
                            override fun onPageScrolled(page: Int, positionOffset: Float) {
                                if (!disposed && !loading) savedZoom = pdf.zoom.coerceIn(1f, 4f)
                            }
                        })
                        .onError(object : OnErrorListener {
                            override fun onError(t: Throwable?) {
                                if (disposed) return
                                if (fallbackStarted) {
                                    fail(t)
                                    return
                                }
                                fallbackStarted = true
                                fallbackJob = scope.launch {
                                    try {
                                        val copy = File.createTempFile("pdf-view-", ".pdf", context.cacheDir)
                                        temporaryFile = copy
                                        withContext(Dispatchers.IO) {
                                            val input = context.contentResolver.openInputStream(uri)
                                                ?: error("Unable to read PDF")
                                            input.use { source ->
                                                copy.outputStream().use { target ->
                                                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                                    while (true) {
                                                        coroutineContext.ensureActive()
                                                        val count = source.read(buffer)
                                                        if (count < 0) break
                                                        target.write(buffer, 0, count)
                                                    }
                                                }
                                            }
                                        }
                                        if (!disposed) openDocument(copy)
                                    } catch (cancel: CancellationException) {
                                        throw cancel
                                    } catch (error: Exception) {
                                        fail(error)
                                    }
                                }
                            }
                        })
                        .onPageError(object : OnPageErrorListener {
                            override fun onPageError(page: Int, t: Throwable?) {
                                if (!disposed) pageError = page
                            }
                        })
                        .onTap(object : OnTapListener {
                            override fun onTap(e: MotionEvent?): Boolean { selection.clear(); currentToggle(); return true }
                        })
                        .onLongPress(object : OnLongPressListener {
                            override fun onLongPress(e: MotionEvent?) { e?.let { selection.start(it.x, it.y) } }
                        })
                        .onDrawAll(object : OnDrawListener {
                            override fun onLayerDrawn(canvas: Canvas?, pageWidth: Float, pageHeight: Float, currentPage: Int) {
                                val c = canvas ?: return
                                val file = pdf.pdfFile ?: return
                                c.save()
                                c.translate(pdf.currentXOffset + file.getSecondaryPageOffset(currentPage, pdf.zoom), pdf.currentYOffset)
                                currentNotes.filter { it.pageIndex == currentPage }.forEach { note ->
                                    val paint = Paint().apply { color = note.color.copy(alpha = .35f).toArgb() }
                                    note.rects.forEach { c.drawRect(it.left * pageWidth, it.top * pageHeight, it.right * pageWidth, it.bottom * pageHeight, paint) }
                                }
                                selection.draw(c, currentPage, pageWidth, pageHeight)
                                c.restore()
                            }
                        }).load()
                        }
                        retryPage = {
                            pageError = null
                            loading = true
                            navigation.startLoading(latestPage)
                            selection.clear()
                            openDocument(temporaryFile)
                        }
                        openDocument(null)
                    }
                }
            },
            update = { view?.invalidate() }
        )
        if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
        pageError?.let { page ->
            Surface(Modifier.align(Alignment.BottomCenter).padding(12.dp), tonalElevation = 4.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.pdf_page_error, page + 1), Modifier.padding(start = 12.dp))
                    TextButton(onClick = { retryPage?.invoke() }) { Text(stringResource(R.string.pdf_retry)) }
                    TextButton(onClick = { pageError = null }) { Text(stringResource(android.R.string.cancel)) }
                }
            }
        }
    }
}
