package ua.acclorite.book_story.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalPdfSnapshotTest {
    @Test fun filenameIsStableAndCannotEscapeItsDirectory() {
        val name = externalPdfSnapshotName("content://provider/../file?name=../../test.pdf")
        assertEquals(name, externalPdfSnapshotName("content://provider/../file?name=../../test.pdf"))
        assertTrue(name.matches(Regex("[0-9a-f]{64}\\.pdf")))
        assertNotEquals(name, externalPdfSnapshotName("content://provider/other"))
    }
}
