package ua.acclorite.book_story.data.backup

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupRestoreCoordinatorTest {
    @Test fun restoresOldDatabaseIfPreferencesFail() {
        val original = BackupSnapshot(mapOf("old" to emptyList()), emptyMap())
        val target = BackupSnapshot(mapOf("new" to emptyList()), emptyMap())
        val writes = mutableListOf<Map<String, List<Map<String, BackupValue>>>>()
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                restoreBackupSafely(target, { original }, {}, { writes.add(it) }, { throw IllegalStateException("settings failure") })
            }
        }
        assertEquals(listOf(target.tables, original.tables), writes)
    }

    @Test fun rejectsInvalidPreferencesBeforeWritingDatabase() {
        var writes = 0
        val snapshot = BackupSnapshot(emptyMap(), emptyMap())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { restoreBackupSafely(snapshot, { snapshot }, { throw IllegalArgumentException() }, { writes++ }, {}) }
        }
        assertEquals(0, writes)
    }
}
