package ua.acclorite.book_story.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.model.library.PdfOpenMode
import ua.acclorite.book_story.ui.common.components.common.AsyncCoverImage
import kotlin.math.roundToInt

@Composable
fun RecentBooksRow(books: List<Book>, open: (Book) -> Unit) {
    if (books.isEmpty()) return
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(stringResource(R.string.recent_reading), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            books.forEach { book ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.width(200.dp).clickable { open(book) }) {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        book.coverImage?.let { AsyncCoverImage(uri = it, modifier = Modifier.size(width = 42.dp, height = 60.dp), contentDescription = null) }
                        Column {
                            Text(book.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                            val progress = if (book.pdfOpenMode == PdfOpenMode.NATIVE_PDF) book.pdfProgress else book.progress
                            Text("${(progress.coerceIn(0f, 1f) * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
