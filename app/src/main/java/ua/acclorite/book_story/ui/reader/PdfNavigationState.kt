package ua.acclorite.book_story.ui.reader

/** Retains the latest navigation request until the renderer has opened the document. */
internal class PdfNavigationState(initialPage: Int) {
    var ready: Boolean = false
        private set
    private var pendingPage: Int = initialPage

    fun request(page: Int): Int? {
        pendingPage = page
        return if (ready) page else null
    }

    fun startLoading(page: Int) {
        ready = false
        pendingPage = page
    }

    fun finishLoading(pageCount: Int): Int {
        require(pageCount > 0)
        ready = true
        return pendingPage.coerceIn(0, pageCount - 1)
    }
}
