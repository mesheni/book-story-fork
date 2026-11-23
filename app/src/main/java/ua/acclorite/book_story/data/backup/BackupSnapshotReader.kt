package ua.acclorite.book_story.data.backup

import android.database.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.data.local.data_store.DataStore
import ua.acclorite.book_story.data.local.room.BookDatabase
import java.util.concurrent.Callable
import javax.inject.Inject

class BackupSnapshotReader @Inject constructor(
    private val database: BookDatabase,
    private val dataStore: DataStore
) {
    suspend fun read(): BackupSnapshot = withContext(Dispatchers.IO) {
        val preferences = dataStore.snapshot().mapValues { (_, value) ->
            when (value) {
                is Boolean -> BackupValue.Flag(value)
                is Int -> BackupValue.Int32(value)
                is Long -> BackupValue.Integer(value)
                is Float -> BackupValue.Float32(value)
                is Double -> BackupValue.Decimal(value)
                is String -> BackupValue.Text(value)
                is Set<*> -> BackupValue.StringSet(value.map { it as? String ?: error("Invalid preference set") }.toSet())
                else -> error("Unsupported preference type")
            }
        }
        val tables = database.runInTransaction(Callable {
            TABLES.associateWith { name ->
                database.openHelper.readableDatabase.query("SELECT * FROM `$name`").use { cursor ->
                    val rows = mutableListOf<Map<String, BackupValue>>()
                    while (cursor.moveToNext()) {
                        rows.add(cursor.columnNames.mapIndexed { index, column -> column to when (cursor.getType(index)) {
                            Cursor.FIELD_TYPE_NULL -> BackupValue.Null
                            Cursor.FIELD_TYPE_INTEGER -> BackupValue.Integer(cursor.getLong(index))
                            Cursor.FIELD_TYPE_FLOAT -> BackupValue.Decimal(cursor.getDouble(index))
                            Cursor.FIELD_TYPE_STRING -> BackupValue.Text(cursor.getString(index))
                            else -> error("Unsupported database value")
                        } }.toMap())
                    }
                    rows
                }
            }
        })
        BackupSnapshot(tables, preferences)
    }

    companion object {
        val TABLES = listOf("BookEntity", "HistoryEntity", "CategoryEntity", "ColorPresetEntity", "ReadingSessionEntity", "NoteEntity", "BookmarkEntity")
    }
}
