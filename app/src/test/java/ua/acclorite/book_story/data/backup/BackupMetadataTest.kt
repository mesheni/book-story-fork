package ua.acclorite.book_story.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupMetadataTest {
    @Test fun roundTripsDatabaseAndSettingsWithoutLosingTypes() {
        val snapshot = BackupSnapshot(
            tables = mapOf("BookEntity" to listOf(mapOf("id" to BackupValue.Integer(12), "title" to BackupValue.Text("Книга"), "image" to BackupValue.Null))),
            preferences = mapOf("dark" to BackupValue.Flag(true), "font" to BackupValue.Decimal(1.25), "filters" to BackupValue.StringSet(setOf("epub", "pdf")))
        )
        assertEquals(snapshot, decodeBackupMetadata(encodeBackupMetadata(snapshot)))
    }

    @Test fun rejectsUnsupportedVersionAndInvalidType() {
        assertThrows(IllegalArgumentException::class.java) { decodeBackupMetadata("""{"version":2,"tables":{},"preferences":{}}""") }
        assertThrows(IllegalArgumentException::class.java) { decodeBackupMetadata("""{"version":1,"tables":{},"preferences":{"x":{"type":"unknown"}}}""") }
    }
}
