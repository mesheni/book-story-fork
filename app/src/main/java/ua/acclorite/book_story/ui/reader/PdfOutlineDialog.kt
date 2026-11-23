package ua.acclorite.book_story.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.R
import ua.acclorite.book_story.data.parser.file.PdfOutlineEntry
import ua.acclorite.book_story.data.parser.file.readPdfOutline

@Composable
fun PdfOutlineDialog(uri: String, dismiss: () -> Unit, select: (Int) -> Unit) {
    val context = LocalContext.current
    val outline by produceState<Result<List<PdfOutlineEntry>>?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                PDFBoxResourceLoader.init(context.applicationContext)
                val input = context.contentResolver.openInputStream(uri.toUri()) ?: error("Unable to read PDF")
                input.use { stream -> PDDocument.load(stream).use(::readPdfOutline) }
            }
        }
    }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.pdf_outline)) },
        text = {
            val result = outline
            when {
                result == null -> Text(stringResource(R.string.pdf_outline_loading))
                result.isFailure -> Text(stringResource(R.string.pdf_outline_error))
                result.getOrNull().isNullOrEmpty() -> Text(stringResource(R.string.pdf_outline_empty))
                else -> LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(result.getOrThrow()) { entry ->
                        Column(Modifier.fillMaxWidth().clickable(enabled = entry.page != null) { entry.page?.let(select) }
                            .padding(start = (entry.depth.coerceAtMost(6) * 16).dp, top = 8.dp, bottom = 8.dp)) {
                            Text(entry.title, color = if (entry.page == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                            entry.page?.let { Text(stringResource(R.string.note_pdf_page, it + 1), style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
