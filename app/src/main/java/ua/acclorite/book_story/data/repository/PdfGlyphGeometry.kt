package ua.acclorite.book_story.data.repository

internal data class PdfGlyphRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** Map PDFBox's text-direction coordinates to the displayed, cropped page. */
internal fun mapPdfGlyph(
    x: Float, baseline: Float, width: Float, height: Float,
    cropWidth: Float, cropHeight: Float, textDirection: Int, pageRotation: Int
): PdfGlyphRect {
    fun unrotate(px: Float, py: Float): Pair<Float, Float> = when ((textDirection % 360 + 360) % 360) {
        90 -> py to cropHeight - px
        180 -> cropWidth - px to cropHeight - py
        270 -> cropWidth - py to px
        else -> px to py
    }
    val rotation = (pageRotation % 360 + 360) % 360
    fun rotate(px: Float, py: Float): Pair<Float, Float> = when (rotation) {
        90 -> cropHeight - py to px
        180 -> cropWidth - px to cropHeight - py
        270 -> py to cropWidth - px
        else -> px to py
    }
    val points = listOf(x to baseline - height, x + width to baseline - height,
        x to baseline, x + width to baseline).map { (px, py) ->
        val (ux, uy) = unrotate(px, py)
        rotate(ux, uy)
    }
    val visualWidth = if (rotation == 90 || rotation == 270) cropHeight else cropWidth
    val visualHeight = if (rotation == 90 || rotation == 270) cropWidth else cropHeight
    return PdfGlyphRect(
        (points.minOf { it.first } / visualWidth).coerceIn(0f, 1f),
        (points.minOf { it.second } / visualHeight).coerceIn(0f, 1f),
        (points.maxOf { it.first } / visualWidth).coerceIn(0f, 1f),
        (points.maxOf { it.second } / visualHeight).coerceIn(0f, 1f)
    )
}
