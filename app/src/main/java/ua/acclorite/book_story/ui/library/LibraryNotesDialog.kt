package ua.acclorite.book_story.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.model.note.Note
import ua.acclorite.book_story.domain.use_case.note.filterLibraryNotes

@Composable
fun LibraryNotesDialog(notes: List<Note>, books: List<Book>, dismiss: () -> Unit, open: (Note) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedBook by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedColorIndex by rememberSaveable { mutableStateOf(-1) }
    var bookMenu by remember { mutableStateOf(false) }
    var colorMenu by remember { mutableStateOf(false) }
    val titles = remember(books) { books.associate { it.id to it.title } }
    val colors = Note.COLORS
    val filtered = remember(notes, query, titles, selectedBook, selectedColorIndex) {
        filterLibraryNotes(notes, query, selectedBook, colors.getOrNull(selectedColorIndex)).filter { it.bookId in titles }
    }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.library_notes)) },
        text = {
            Column {
                OutlinedTextField(query, { query = it }, singleLine = true, label = { Text(stringResource(R.string.search_field_empty)) })
                Row {
                    Box(Modifier.weight(1f)) {
                        TextButton(onClick = { bookMenu = true }) {
                            Text(titles[selectedBook] ?: stringResource(R.string.library_notes_all_books), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.heightIn(max = 48.dp))
                        }
                        DropdownMenu(expanded = bookMenu, onDismissRequest = { bookMenu = false }, modifier = Modifier.heightIn(max = 300.dp)) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.library_notes_all_books)) }, onClick = { selectedBook = null; bookMenu = false })
                            books.filter { book -> notes.any { it.bookId == book.id } }.sortedBy { it.title }.forEach { book ->
                                DropdownMenuItem(text = { Text(book.title) }, onClick = { selectedBook = book.id; bookMenu = false })
                            }
                        }
                    }
                    Box {
                        TextButton(onClick = { colorMenu = true }) {
                            colors.getOrNull(selectedColorIndex)?.let { Box(Modifier.padding(end = 4.dp).size(16.dp).background(it, CircleShape)) }
                            Text(stringResource(R.string.library_notes_color))
                        }
                        DropdownMenu(expanded = colorMenu, onDismissRequest = { colorMenu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.library_notes_all_colors)) }, onClick = { selectedColorIndex = -1; colorMenu = false })
                            colors.forEachIndexed { index, color ->
                                DropdownMenuItem(text = {
                                    Row {
                                        Box(Modifier.size(20.dp).background(color, CircleShape))
                                        Text(stringResource(R.string.library_notes_color_number, index + 1), Modifier.padding(start = 8.dp))
                                    }
                                }, onClick = { selectedColorIndex = index; colorMenu = false })
                            }
                        }
                    }
                }
                if (filtered.isEmpty()) Text(stringResource(R.string.library_notes_empty), Modifier.padding(vertical = 16.dp))
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(filtered, key = { it.id }) { note ->
                        Column(Modifier.fillMaxWidth().clickable { open(note) }.padding(vertical = 10.dp)) {
                            Text(titles[note.bookId].orEmpty(), style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                            Text(note.selectedText, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            if (note.note.isNotBlank()) Text(note.note, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
