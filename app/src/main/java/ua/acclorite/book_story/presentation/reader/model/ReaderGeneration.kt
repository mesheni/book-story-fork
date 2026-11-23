package ua.acclorite.book_story.presentation.reader.model

import java.util.concurrent.atomic.AtomicInteger

/** Rejects results and disposal requests belonging to an older reader. */
internal class ReaderGeneration {
    private val value = AtomicInteger(0)

    fun next(): Int = value.incrementAndGet()
    fun isCurrent(generation: Int): Boolean = generation > 0 && value.get() == generation
    fun close(generation: Int): Boolean = generation > 0 && value.compareAndSet(generation, generation + 1)
}
