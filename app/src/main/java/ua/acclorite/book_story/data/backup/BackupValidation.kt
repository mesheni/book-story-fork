package ua.acclorite.book_story.data.backup

data class BackupColumn(val affinity: String, val required: Boolean)

fun validateBackupTables(
    tables: Map<String, List<Map<String, BackupValue>>>,
    schema: Map<String, Map<String, BackupColumn>>
) {
    require(tables.keys == schema.keys) { "Backup table set does not match database" }
    tables.forEach { (table, rows) ->
        val columns = schema.getValue(table)
        val ids = mutableSetOf<Long>()
        rows.forEach { row ->
            require(row.keys == columns.keys) { "Invalid columns in $table" }
            row.forEach { (name, value) ->
                val column = columns.getValue(name)
                require(when (value) {
                    BackupValue.Null -> !column.required
                    is BackupValue.Integer -> column.affinity == "INTEGER"
                    is BackupValue.Decimal -> column.affinity == "REAL"
                    is BackupValue.Text -> column.affinity == "TEXT"
                    else -> false
                }) { "Invalid value for $table.$name" }
            }
            val id = (row["id"] as? BackupValue.Integer)?.value
            require(id != null && ids.add(id)) { "Duplicate or invalid id in $table" }
        }
    }
    val books = tables["BookEntity"].orEmpty().map { (it.getValue("id") as BackupValue.Integer).value }.toSet()
    listOf("NoteEntity", "BookmarkEntity").forEach { name ->
        tables[name].orEmpty().forEach { row ->
            require((row["bookId"] as? BackupValue.Integer)?.value in books) { "Orphaned $name" }
        }
    }
}
