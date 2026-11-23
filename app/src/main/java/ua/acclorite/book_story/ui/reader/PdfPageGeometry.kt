package ua.acclorite.book_story.ui.reader

/** The visible page rectangle in the viewer's pixel coordinates. */
internal data class PdfPageGeometry(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    fun normalize(x: Float, y: Float): Pair<Float, Float>? {
        if (width <= 0f || height <= 0f) return null
        val nx = (x - left) / width
        val ny = (y - top) / height
        return if (nx in 0f..1f && ny in 0f..1f) nx to ny else null
    }
}
