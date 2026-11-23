package ua.acclorite.book_story.presentation.library

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.acclorite.book_story.domain.model.library.Book

class RecentBooksTest {
    @Test fun returnsRecentlyOpenedBooksInDescendingOrder() {
        val books = listOf(
            Book.default.copy(id = 1, lastOpened = 10),
            Book.default.copy(id = 2, lastOpened = null),
            Book.default.copy(id = 3, lastOpened = 30),
            Book.default.copy(id = 4, lastOpened = 20),
            Book.default.copy(id = 5, lastOpened = 5)
        )
        assertEquals(listOf(3, 4, 1), recentBooks(books).map { it.id })
    }

    @Test fun emptyWhenNoBookWasOpened() {
        assertEquals(emptyList<Book>(), recentBooks(listOf(Book.default)))
    }
}
