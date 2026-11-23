package ua.acclorite.book_story.domain.use_case.book

import ua.acclorite.book_story.domain.model.reader.ReaderText
import kotlin.math.ceil

data class ReadingTimeEstimate(val minutes: Int, val isChapter: Boolean)

fun estimateReadingTime(text: List<ReaderText>, index: Int, wordsPerMinute: Int): ReadingTimeEstimate? {
    if (text.isEmpty() || index !in text.indices || wordsPerMinute <= 0) return null
    val hasChapter = (index downTo 0).any { text[it] is ReaderText.Chapter }
    val end = if (hasChapter) (index + 1 until text.size).firstOrNull { text[it] is ReaderText.Chapter } ?: text.size else text.size
    val wordPattern = Regex("[\\p{L}\\p{N}]+(?:['’\\-][\\p{L}\\p{N}]+)*")
    val words = (index until end).sumOf { i ->
        val item = text[i]
        if (item is ReaderText.Text) wordPattern.findAll(item.line.text).count() else 0
    }
    if (words == 0) return null
    return ReadingTimeEstimate(ceil(words.toDouble() / wordsPerMinute).toInt(), hasChapter)
}
