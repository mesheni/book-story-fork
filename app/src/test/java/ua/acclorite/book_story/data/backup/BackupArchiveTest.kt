package ua.acclorite.book_story.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupArchiveTest {
    private fun archive(vararg entries: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { buffer ->
        ZipOutputStream(buffer).use { zip -> entries.forEach { (name, data) ->
            zip.putNextEntry(ZipEntry(name)); zip.write(data); zip.closeEntry()
        } }
    }.toByteArray()

    @Test fun readsKnownMetadataEntry() {
        val data = "{}".toByteArray()
        assertArrayEquals(data, readBackupEntries(ByteArrayInputStream(archive("metadata.json" to data)))["metadata.json"])
    }

    @Test fun writesAndReadsMetadataArchive() {
        val metadata = "{\"version\":1}".toByteArray()
        val output = ByteArrayOutputStream()
        writeBackupMetadata(output, metadata)
        assertArrayEquals(metadata, readBackupEntries(ByteArrayInputStream(output.toByteArray())).getValue("metadata.json"))
    }

    @Test fun rejectsTraversalAndUnknownEntries() {
        assertThrows(IllegalArgumentException::class.java) { readBackupEntries(ByteArrayInputStream(archive("../metadata.json" to byteArrayOf()))) }
        assertThrows(IllegalArgumentException::class.java) { readBackupEntries(ByteArrayInputStream(archive("unknown" to byteArrayOf()))) }
    }

    @Test fun rejectsOversizedMetadata() {
        assertThrows(IllegalArgumentException::class.java) {
            readBackupEntries(ByteArrayInputStream(archive("metadata.json" to ByteArray(1025))), maxMetadataBytes = 1024)
        }
    }

    @Test fun streamsBookFilesAndMetadata() {
        val saved = mutableMapOf<Long, ByteArrayOutputStream>()
        val bytes = archive("metadata.json" to "{}".toByteArray(), "books/42" to "book".toByteArray())
        val metadata = readBackupBundle(ByteArrayInputStream(bytes)) { id -> ByteArrayOutputStream().also { saved[id] = it } }
        assertArrayEquals("{}".toByteArray(), metadata)
        assertArrayEquals("book".toByteArray(), saved.getValue(42).toByteArray())
    }

    @Test fun writesBundleWithoutBufferingBooksInMetadata() {
        val output = ByteArrayOutputStream()
        writeBackupBundle(output, "{}".toByteArray(), listOf(7L)) { ByteArrayInputStream("contents".toByteArray()) }
        val books = mutableMapOf<Long, ByteArrayOutputStream>()
        assertArrayEquals("{}".toByteArray(), readBackupBundle(ByteArrayInputStream(output.toByteArray())) { ByteArrayOutputStream().also { stream -> books[it] = stream } })
        assertArrayEquals("contents".toByteArray(), books.getValue(7).toByteArray())
    }

    @Test fun rejectsUnsafeBookNames() {
        assertThrows(IllegalArgumentException::class.java) {
            readBackupBundle(ByteArrayInputStream(archive("books/../evil" to byteArrayOf()))) { ByteArrayOutputStream() }
        }
    }
}
