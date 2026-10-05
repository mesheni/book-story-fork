/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.R

@Composable
fun PdfViewerContent(
    uriString: String?,
    initialPage: Int,
    navigationRequest: Int,
    onPageChanged: (ReaderEvent.OnPdfPageChanged) -> Unit,
    isLoading: Boolean,
    viewerError: String?,
    onViewerInitialized: (ReaderEvent.OnPdfViewerInitialized) -> Unit,
    onToggleMenu: () -> Unit
) {
    val context = LocalContext.current
    var rendererState by remember(uriString) { mutableStateOf<PdfRenderer?>(null) }
    var descriptorState by remember(uriString) { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember(uriString) { mutableStateOf(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)

    DisposableEffect(uriString) {
        onDispose {
            rendererState?.close()
            descriptorState?.close()
            rendererState = null
            descriptorState = null
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
                    descriptorState = descriptor
                    rendererState = renderer
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

    if (isLoading || rendererState == null || pageCount == 0) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onToggleMenu() })
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(pageCount) { pageIndex ->
            PdfPage(
                renderer = rendererState!!,
                pageIndex = pageIndex
            )
        }
    }
}

@Composable
private fun PdfPage(
    renderer: PdfRenderer,
    pageIndex: Int
) {
    var bitmap by remember(renderer, pageIndex) { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(renderer, pageIndex) {
        onDispose {
            bitmap?.recycle()
            bitmap = null
        }
    }

    LaunchedEffect(renderer, pageIndex) {
        val renderedBitmap = withContext(Dispatchers.IO) {
            renderer.openPage(pageIndex).use { page ->
                Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888).also {
                    page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            }
        }
        bitmap?.recycle()
        bitmap = renderedBitmap
    }

    bitmap?.let { renderedPage ->
        Image(
            bitmap = renderedPage.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentScale = ContentScale.FillWidth
        )
    }
}
