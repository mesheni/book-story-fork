package ua.acclorite.book_story.presentation.reader.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ReaderGenerationTest {
    @Test fun closingDuringInitializationInvalidatesThatInitialization() {
        val generation = ReaderGeneration()
        val opened = generation.next()
        assertTrue(generation.close(opened))
        assertFalse(generation.isCurrent(opened))
    }

    @Test fun oldDisposeCannotInvalidateNewReader() {
        val generation = ReaderGeneration()
        val old = generation.next()
        val current = generation.next()
        assertFalse(generation.close(old))
        assertTrue(generation.isCurrent(current))
    }

    @Test fun lateInitializationDoesNotPublishAfterClose() {
        val generation = ReaderGeneration()
        val token = generation.next()
        val started = CountDownLatch(1)
        val resume = CountDownLatch(1)
        val published = AtomicBoolean(false)
        val worker = Thread {
            started.countDown()
            if (resume.await(2, TimeUnit.SECONDS) && generation.isCurrent(token)) published.set(true)
        }
        worker.start()
        assertTrue(started.await(2, TimeUnit.SECONDS))
        assertTrue(generation.close(token))
        resume.countDown()
        worker.join(2000)
        assertFalse(worker.isAlive)
        assertFalse(published.get())
    }
}
