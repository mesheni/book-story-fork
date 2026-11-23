package ua.acclorite.book_story.data.backup

fun mergeBackupPreferences(current: Map<String, Any>, backup: Map<String, BackupValue>): Map<String, Any> {
    val result = current.toMutableMap()
    backup.forEach { (key, item) ->
        val old = current[key] ?: throw IllegalArgumentException("Unknown preference $key")
        val value: Any = when (item) {
            is BackupValue.Flag -> item.value
            is BackupValue.Int32 -> item.value
            is BackupValue.Integer -> item.value
            is BackupValue.Float32 -> item.value
            is BackupValue.Decimal -> item.value
            is BackupValue.Text -> item.value
            is BackupValue.StringSet -> item.value
            BackupValue.Null -> throw IllegalArgumentException("Null preference $key")
        }
        require(old::class == value::class || old is Set<*> && value is Set<*>) { "Invalid preference type for $key" }
        result[key] = value
    }
    return result
}
