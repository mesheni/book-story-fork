package ua.acclorite.book_story.data.backup

import org.junit.Assert.assertThrows
import org.junit.Test

class BackupValidationTest {
    private val schema = mapOf(
        "BookEntity" to mapOf("id" to BackupColumn("INTEGER", true)),
        "NoteEntity" to mapOf("id" to BackupColumn("INTEGER", true), "bookId" to BackupColumn("INTEGER", true))
    )
    private fun row(id: Long) = mapOf("id" to BackupValue.Integer(id))

    @Test fun acceptsValidReferences() {
        validateBackupTables(mapOf("BookEntity" to listOf(row(1)), "NoteEntity" to listOf(row(2) + ("bookId" to BackupValue.Integer(1)))), schema)
    }

    @Test fun rejectsUnknownColumnsAndMissingBooks() {
        assertThrows(IllegalArgumentException::class.java) {
            validateBackupTables(mapOf("BookEntity" to listOf(row(1) + ("extra" to BackupValue.Text("x"))), "NoteEntity" to emptyList()), schema)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateBackupTables(mapOf("BookEntity" to listOf(row(1)), "NoteEntity" to listOf(row(2) + ("bookId" to BackupValue.Integer(99)))), schema)
        }
    }

    @Test fun rejectsWrongValueTypesAndDuplicateIds() {
        assertThrows(IllegalArgumentException::class.java) {
            validateBackupTables(mapOf("BookEntity" to listOf(mapOf("id" to BackupValue.Text("1"))), "NoteEntity" to emptyList()), schema)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateBackupTables(mapOf("BookEntity" to listOf(row(1), row(1)), "NoteEntity" to emptyList()), schema)
        }
    }
}
