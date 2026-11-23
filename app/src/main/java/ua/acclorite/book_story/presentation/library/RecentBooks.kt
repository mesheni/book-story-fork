package ua.acclorite.book_story.presentation.library

import ua.acclorite.book_story.domain.model.library.Book

fun recentBooks(books: List<Book>): List<Book> = books
    .filter { it.lastOpened != null }
    .sortedByDescending { it.lastOpened }
    .take(3)
