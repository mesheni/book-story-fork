package ua.acclorite.book_story.domain.use_case.book

class ReadingPaceTracker {
    private var lastIndex: Int? = null
    private var lastTime = 0L
    private var words = 0
    private var elapsed = 0L
    private var pendingWords = 0
    private var pendingElapsed = 0L

    fun reset() { lastIndex = null; clearSamples() }

    private fun clearSamples() { words = 0; elapsed = 0; pendingWords = 0; pendingElapsed = 0 }

    fun observe(index: Int, crossedWords: Int, now: Long): Int? {
        val previous = lastIndex
        if (previous == index) return null
        val duration = now - lastTime
        lastIndex = index
        lastTime = now
        if (previous == null) return null
        if (index <= previous || index - previous > 5 || duration <= 0 || duration > 120_000 || crossedWords <= 0) {
            clearSamples()
            return null
        }
        pendingWords += crossedWords
        pendingElapsed += duration
        if (pendingElapsed < 2_000) return null
        val pace = pendingWords * 60_000.0 / pendingElapsed
        if (pace !in 50.0..600.0) { clearSamples(); return null }
        words += pendingWords
        elapsed += pendingElapsed
        pendingWords = 0
        pendingElapsed = 0
        if (words < 80 || elapsed < 20_000) return null
        val result = (words * 60_000.0 / elapsed).toInt().coerceIn(50, 600)
        clearSamples()
        return result
    }
}
