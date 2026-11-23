package ua.acclorite.book_story.data.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.data.local.room.BookDatabase
import javax.inject.Inject

/** Replaces database rows atomically after validating the entire snapshot. */
class BackupDatabaseRestorer @Inject constructor(private val database: BookDatabase) {
    private fun schema(): Map<String, Map<String, BackupColumn>> {
        val db = database.openHelper.readableDatabase
        return BackupSnapshotReader.TABLES.associateWith { table ->
            db.query("PRAGMA table_info(`$table`)").use { cursor ->
                val columns = linkedMapOf<String, BackupColumn>()
                while (cursor.moveToNext()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    val type = cursor.getString(cursor.getColumnIndexOrThrow("type")).uppercase()
                    val required = cursor.getInt(cursor.getColumnIndexOrThrow("notnull")) != 0
                    columns[name] = BackupColumn(type, required)
                }
                columns
            }
        }
    }

    suspend fun validate(tables: Map<String, List<Map<String, BackupValue>>>) = withContext(Dispatchers.IO) {
        validateBackupTables(tables, schema())
    }

    suspend fun restore(tables: Map<String, List<Map<String, BackupValue>>>) = withContext(Dispatchers.IO) {
        database.runInTransaction {
            val db = database.openHelper.writableDatabase
            val schema = schema()
            validateBackupTables(tables, schema)
            db.execSQL("DELETE FROM `PdfPageTextEntity`")
            BackupSnapshotReader.TABLES.asReversed().forEach { db.execSQL("DELETE FROM `$it`") }
            BackupSnapshotReader.TABLES.forEach { table ->
                val columns = schema.getValue(table).keys.toList()
                val names = columns.joinToString(",") { "`$it`" }
                val placeholders = columns.joinToString(",") { "?" }
                val statement = db.compileStatement("INSERT INTO `$table` ($names) VALUES ($placeholders)")
                try {
                    tables.getValue(table).forEach { row ->
                        statement.clearBindings()
                        columns.forEachIndexed { index, column ->
                            when (val value = row.getValue(column)) {
                                BackupValue.Null -> statement.bindNull(index + 1)
                                is BackupValue.Integer -> statement.bindLong(index + 1, value.value)
                                is BackupValue.Decimal -> statement.bindDouble(index + 1, value.value)
                                is BackupValue.Text -> statement.bindString(index + 1, value.value)
                                else -> error("Invalid database value")
                            }
                        }
                        statement.executeInsert()
                    }
                } finally { statement.close() }
            }
        }
    }
}
