package ua.acclorite.book_story.ui.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.R
import kotlin.math.roundToInt

private val thumbnailSemaphore = Semaphore(2)

internal fun pdfThumbnailSize(width: Int, height: Int, maxEdge: Int): Pair<Int, Int>? {
    if (width <= 0 || height <= 0 || maxEdge <= 0) return null
    val scale = maxEdge.toDouble() / maxOf(width, height)
    return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
}

@Composable
fun PdfThumbnailsDialog(uri: String, pageCount: Int, dismiss: () -> Unit, select: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.pdf_thumbnails)) },
        text = {
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items((0 until pageCount).toList(), key = { it }) { page ->
                    PdfThumbnail(uri, page, select)
                }
            }
        },
        confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun PdfThumbnail(uri: String, index: Int, select: (Int) -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, uri, index) {
        value = withContext(Dispatchers.IO) {
            thumbnailSemaphore.withPermit {
                runCatching {
                    val descriptor = context.contentResolver.openFileDescriptor(uri.toUri(), "r") ?: return@runCatching null
                    val renderer = try { PdfRenderer(descriptor) } catch (error: Exception) { descriptor.close(); throw error }
                    renderer.use { pdf ->
                        pdf.openPage(index).use { page ->
                            val (width, height) = pdfThumbnailSize(page.width, page.height, 240) ?: return@use null
                            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { image ->
                                image.eraseColor(android.graphics.Color.WHITE)
                                page.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            }
                        }
                    }
                }.getOrNull()
            }
        }
    }
    Column(Modifier.padding(4.dp).clickable { select(index) }) {
        bitmap?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth().height(110.dp)) }
            ?: Text(stringResource(R.string.pdf_thumbnail_unavailable), Modifier.height(110.dp))
        Text(stringResource(R.string.note_pdf_page, index + 1))
    }
}
