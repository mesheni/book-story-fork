package ua.acclorite.book_story.data.backup

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun writeBackupBundle(output: OutputStream, metadata: ByteArray, bookIds: List<Long>, openBook: (Long) -> InputStream) {
    require(metadata.size <= 32 * 1024 * 1024 && bookIds.size <= 100_000 && bookIds.all { it > 0 } && bookIds.toSet().size == bookIds.size)
    var totalBytes = metadata.size.toLong()
    ZipOutputStream(output).use { zip ->
        zip.putNextEntry(ZipEntry("metadata.json"))
        zip.write(metadata)
        zip.closeEntry()
        bookIds.forEach { id ->
            zip.putNextEntry(ZipEntry("books/$id"))
            openBook(id).use { input ->
                val buffer = ByteArray(8192)
                var size = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    size += count
                    totalBytes += count
                    require(size <= 512L * 1024 * 1024 && totalBytes <= 4L * 1024 * 1024 * 1024) { "Backup is too large" }
                    zip.write(buffer, 0, count)
                }
                require(size > 0) { "Book file is empty" }
            }
            zip.closeEntry()
        }
    }
}

/** Streams validated book entries to caller-owned destinations, never using archive paths. */
fun readBackupBundle(input: InputStream, openBook: (Long) -> OutputStream): ByteArray {
    var metadata: ByteArray? = null
    val seenBooks = mutableSetOf<Long>()
    var totalBytes = 0L
    ZipInputStream(input).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            require(!entry.isDirectory) { "Directories are not allowed" }
            val id = Regex("books/([1-9][0-9]*)").matchEntire(entry.name)?.groupValues?.get(1)?.toLongOrNull()
            val buffer: ByteArrayOutputStream?
            val output: OutputStream
            val limit: Long
            if (entry.name == "metadata.json") {
                require(metadata == null) { "Duplicate metadata" }
                buffer = ByteArrayOutputStream()
                output = buffer
                limit = 32L * 1024 * 1024
            } else {
                require(id != null && seenBooks.add(id) && seenBooks.size <= 100_000) { "Invalid book entry" }
                buffer = null
                output = openBook(id)
                limit = 512L * 1024 * 1024
            }
            output.use { target ->
                val bytes = ByteArray(8192)
                var entryBytes = 0L
                while (true) {
                    val count = zip.read(bytes)
                    if (count < 0) break
                    entryBytes += count
                    totalBytes += count
                    require(entryBytes <= limit && totalBytes <= 4L * 1024 * 1024 * 1024) { "Backup is too large" }
                    target.write(bytes, 0, count)
                }
            }
            if (buffer != null) metadata = buffer.toByteArray()
            zip.closeEntry()
        }
    }
    return requireNotNull(metadata) { "Missing backup metadata" }
}

fun writeBackupMetadata(output: OutputStream, metadata: ByteArray) {
    require(metadata.size <= 32 * 1024 * 1024) { "Backup metadata is too large" }
    ZipOutputStream(output).use { zip ->
        zip.putNextEntry(ZipEntry("metadata.json"))
        zip.write(metadata)
        zip.closeEntry()
    }
}

/** Reads the metadata-only backup format without extracting paths to disk. */
fun readBackupEntries(input: InputStream, maxMetadataBytes: Int = 32 * 1024 * 1024): Map<String, ByteArray> {
    require(maxMetadataBytes > 0)
    val result = mutableMapOf<String, ByteArray>()
    ZipInputStream(input).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            require(!entry.isDirectory && entry.name == "metadata.json" && entry.name !in result) { "Invalid backup entry" }
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = zip.read(buffer)
                if (count < 0) break
                require(output.size().toLong() + count <= maxMetadataBytes.toLong()) { "Backup metadata is too large" }
                output.write(buffer, 0, count)
            }
            result[entry.name] = output.toByteArray()
            zip.closeEntry()
        }
    }
    require("metadata.json" in result) { "Missing backup metadata" }
    return result
}
