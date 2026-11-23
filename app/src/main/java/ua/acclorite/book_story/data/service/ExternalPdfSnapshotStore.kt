/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */
package ua.acclorite.book_story.data.service

import android.app.Application
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

internal fun externalPdfSnapshotName(sourceUri: String): String =
    MessageDigest.getInstance("SHA-256").digest(sourceUri.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) } + ".pdf"

/** Private durable copies for providers that grant only temporary access. */
@Singleton
class ExternalPdfSnapshotStore @Inject constructor(private val application: Application) {
    private val mutex = Mutex()
    private val directory get() = File(application.filesDir, "external-pdfs")

    fun snapshot(sourceUri: String): File? = File(directory, externalPdfSnapshotName(sourceUri))
        .takeIf { it.isFile && it.length() > 0L }

    /**
     * Whether an existing snapshot already matches the source of [sourceSize] bytes
     * (unknown size, <= 0, never matches so the file is re-copied).
     */
    fun isUpToDate(sourceUri: String, sourceSize: Long): Boolean =
        sourceSize > 0L && snapshot(sourceUri)?.length() == sourceSize

    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }

    /** Call inside [withLock] so deletion and concurrent imports cannot race. */
    suspend fun save(uri: Uri) = withContext(Dispatchers.IO) {
        val parent = directory
        check(parent.isDirectory || parent.mkdirs()) { "Unable to create PDF storage" }
        val destination = File(parent, externalPdfSnapshotName(uri.toString()))
        val temporary = File.createTempFile("import-", ".tmp", parent)
        try {
            val input = application.contentResolver.openInputStream(uri) ?: error("Unable to read PDF")
            input.use { source ->
                temporary.outputStream().buffered().use { target ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = source.read(buffer)
                        if (count < 0) break
                        target.write(buffer, 0, count)
                    }
                }
            }
            coroutineContext.ensureActive()
            check(temporary.length() > 0L) { "Empty PDF" }
            check(temporary.renameTo(destination)) { "Unable to save PDF" }
        } finally {
            temporary.delete()
        }
    }

    /** Call inside [withLock] after checking whether another book still uses it. */
    fun delete(sourceUri: String) {
        File(directory, externalPdfSnapshotName(sourceUri)).delete()
    }
}
