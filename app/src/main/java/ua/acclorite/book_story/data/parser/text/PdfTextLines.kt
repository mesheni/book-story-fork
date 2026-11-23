package ua.acclorite.book_story.data.parser.text

internal fun collapsePdfSpaces(text: String, onChunk: (Int) -> Unit = {}): String = buildString(text.length) {
    text.forEachIndexed { index, char ->
        if (index % 8192 == 0) onChunk(index)
        if (char != ' ' || index == 0 || text[index - 1] != ' ') append(char)
    }
}

internal fun appendPdfLine(lines: MutableList<String>, raw: String) {
    val line = raw.trim()
    if (line.isEmpty()) return
    if (lines.isEmpty() || !line.first().isLetter() || line.first().isUpperCase()) {
        lines.add(line)
        return
    }
    val previous = lines.last()
    lines[lines.lastIndex] = if (line.first().isLowerCase() && previous.length > 1 &&
        previous.last() == '-' && previous[previous.lastIndex - 1].isLowerCase()
    ) previous.dropLast(1) + line else "$previous $line"
}
