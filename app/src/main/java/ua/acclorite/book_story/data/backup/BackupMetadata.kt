package ua.acclorite.book_story.data.backup

import kotlinx.serialization.json.*

sealed interface BackupValue {
    data object Null : BackupValue
    data class Integer(val value: Long) : BackupValue
    data class Int32(val value: Int) : BackupValue
    data class Decimal(val value: Double) : BackupValue
    data class Float32(val value: Float) : BackupValue
    data class Text(val value: String) : BackupValue
    data class Flag(val value: Boolean) : BackupValue
    data class StringSet(val value: Set<String>) : BackupValue
}

data class BackupSnapshot(
    val tables: Map<String, List<Map<String, BackupValue>>>,
    val preferences: Map<String, BackupValue>
)

private fun encodeValue(value: BackupValue): JsonObject = buildJsonObject {
    when (value) {
        BackupValue.Null -> put("type", "null")
        is BackupValue.Integer -> { put("type", "long"); put("value", value.value) }
        is BackupValue.Int32 -> { put("type", "int"); put("value", value.value) }
        is BackupValue.Decimal -> { require(value.value.isFinite()); put("type", "double"); put("value", value.value) }
        is BackupValue.Float32 -> { require(value.value.isFinite()); put("type", "float"); put("value", value.value) }
        is BackupValue.Text -> { put("type", "string"); put("value", value.value) }
        is BackupValue.Flag -> { put("type", "bool"); put("value", value.value) }
        is BackupValue.StringSet -> { put("type", "strings"); put("value", JsonArray(value.value.sorted().map(::JsonPrimitive))) }
    }
}

private fun decodeValue(value: JsonElement): BackupValue {
    val obj = value.jsonObject
    return when (obj.getValue("type").jsonPrimitive.content) {
        "null" -> BackupValue.Null
        "long" -> BackupValue.Integer(obj.getValue("value").jsonPrimitive.long)
        "int" -> BackupValue.Int32(obj.getValue("value").jsonPrimitive.int)
        "double" -> BackupValue.Decimal(obj.getValue("value").jsonPrimitive.double.also { require(it.isFinite()) })
        "float" -> BackupValue.Float32(obj.getValue("value").jsonPrimitive.float.also { require(it.isFinite()) })
        "string" -> BackupValue.Text(obj.getValue("value").jsonPrimitive.content)
        "bool" -> BackupValue.Flag(obj.getValue("value").jsonPrimitive.boolean)
        "strings" -> BackupValue.StringSet(obj.getValue("value").jsonArray.map { it.jsonPrimitive.content }.toSet())
        else -> throw IllegalArgumentException("Unsupported backup value")
    }
}

fun encodeBackupMetadata(snapshot: BackupSnapshot): String = buildJsonObject {
    put("version", 1)
    put("tables", buildJsonObject {
        snapshot.tables.forEach { (name, rows) -> put(name, JsonArray(rows.map { row -> JsonObject(row.mapValues { encodeValue(it.value) }) })) }
    })
    put("preferences", JsonObject(snapshot.preferences.mapValues { encodeValue(it.value) }))
}.toString()

fun decodeBackupMetadata(json: String): BackupSnapshot {
    val root = Json.parseToJsonElement(json).jsonObject
    require(root.getValue("version").jsonPrimitive.int == 1) { "Unsupported backup version" }
    return BackupSnapshot(
        root.getValue("tables").jsonObject.mapValues { (_, rows) -> rows.jsonArray.map { row -> row.jsonObject.mapValues { decodeValue(it.value) } } },
        root.getValue("preferences").jsonObject.mapValues { decodeValue(it.value) }
    )
}
