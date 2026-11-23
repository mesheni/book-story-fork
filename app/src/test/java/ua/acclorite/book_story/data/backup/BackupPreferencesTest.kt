package ua.acclorite.book_story.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupPreferencesTest {
    @Test fun acceptsMatchingTypesAndPreservesUnmentionedSettings() {
        val current = mapOf<String, Any>("font" to 10, "dark" to false)
        assertEquals(mapOf("font" to 14, "dark" to false), mergeBackupPreferences(current, mapOf("font" to BackupValue.Int32(14))))
    }

    @Test fun rejectsUnknownKeysAndWrongTypes() {
        val current = mapOf<String, Any>("font" to 10)
        assertThrows(IllegalArgumentException::class.java) { mergeBackupPreferences(current, mapOf("font" to BackupValue.Text("large"))) }
        assertThrows(IllegalArgumentException::class.java) { mergeBackupPreferences(current, mapOf("other" to BackupValue.Flag(true))) }
    }
}
