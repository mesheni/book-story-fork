package ua.acclorite.book_story.presentation.reader.model

import ua.acclorite.book_story.domain.model.bookmark.Bookmark
import ua.acclorite.book_story.domain.model.reader.ReaderText

private fun bookmarkPreview(text: List<ReaderText>, index: Int): String = when (val item = text.getOrNull(index)) {
    is ReaderText.Text -> item.line.text.take(120)
    is ReaderText.Chapter -> item.title.take(120)
    else -> ""
}

internal fun pdfBookmarkPosition(text: List<ReaderText>, index: Int, offset: Int): Pair<Int, Int>? {
    if (index in text.indices && bookmarkPreview(text, index).isNotBlank()) return index to offset
    val nearest = text.indices.filter { bookmarkPreview(text, it).isNotBlank() }
        .minWithOrNull(compareBy<Int> { kotlin.math.abs(it - index) }.thenByDescending { it })
    return nearest?.let { it to 0 }
}

internal fun resolvePdfBookmark(bookmark: Bookmark, text: List<ReaderText>): Pair<Int, Int>? {
    if (bookmark.previewText.isBlank()) return null
    if (bookmarkPreview(text, bookmark.scrollIndex) == bookmark.previewText) {
        return bookmark.scrollIndex to bookmark.scrollOffset
    }
    val matches = text.indices.filter { bookmarkPreview(text, it) == bookmark.previewText }
    return matches.singleOrNull()?.let { it to 0 }
}
