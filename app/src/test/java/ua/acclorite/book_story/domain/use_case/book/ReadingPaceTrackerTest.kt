package ua.acclorite.book_story.domain.use_case.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingPaceTrackerTest {
    @Test fun learnsFromPlausibleReadingIntervals() {
        val tracker = ReadingPaceTracker()
        assertNull(tracker.observe(0, 0, 0))
        assertNull(tracker.observe(1, 40, 12000))
        assertEquals(200, tracker.observe(2, 40, 24000))
    }

    @Test fun excludesJumpsBacktrackingAndLongPauses() {
        val tracker = ReadingPaceTracker()
        tracker.observe(0, 0, 0)
        assertNull(tracker.observe(20, 300, 10000))
        assertNull(tracker.observe(19, 30, 20000))
        assertNull(tracker.observe(20, 30, 150000))
        assertNull(tracker.observe(21, 20, 151000))
    }

    @Test fun learnsFromShortParagraphsReadAtNormalPace() {
        val tracker = ReadingPaceTracker()
        tracker.observe(0, 0, 0)
        var estimate: Int? = null
        for (index in 1..16) estimate = tracker.observe(index, 5, index * 1500L) ?: estimate
        assertEquals(200, estimate)
    }
}
