package ua.acclorite.book_story.domain.use_case.book

import ua.acclorite.book_story.domain.model.reader.PdfPageText
import kotlin.math.ceil

private val words = Regex("[\\p{L}\\p{N}]+(?:['’\\-][\\p{L}\\p{N}]+)*")

fun estimatePdfReadingTime(pages: List<PdfPageText>, chapters: List<Int>, page: Int, speed: Int): ReadingTimeEstimate? {
    if (speed <= 0 || pages.none { it.pageIndex == page && words.containsMatchIn(it.text) }) return null
    val chapter = chapters.any { it in 0..page }
    val end = if (chapter) chapters.filter { it > page }.minOrNull() ?: Int.MAX_VALUE else Int.MAX_VALUE
    val count = pages.asSequence().filter { it.pageIndex >= page && it.pageIndex < end }.sumOf { words.findAll(it.text).count() }
    if (count == 0) return null
    return ReadingTimeEstimate(ceil(count.toDouble() / speed).toInt(), chapter)
}
