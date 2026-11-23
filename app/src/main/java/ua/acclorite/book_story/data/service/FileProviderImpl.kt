/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.service

import android.app.Application
import androidx.core.net.toUri
import androidx.core.content.FileProvider as AndroidFileProvider
import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.data.model.file.CachedFileCompat
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.service.FileProvider
import javax.inject.Inject

class FileProviderImpl @Inject constructor(
    private val application: Application,
    private val snapshots: ExternalPdfSnapshotStore
) : FileProvider {

    override fun getFileFromBook(book: Book): Result<CachedFile> = runCatching {
        book.sourceUri?.let { sourceUri ->
            val uri = sourceUri.toUri()
            if (uri.authority == "${application.packageName}.fileprovider" && uri.pathSegments.firstOrNull() == "restored_books") {
                val size = application.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
                require(size > 0L) { "Restored book is unavailable" }
                return@runCatching CachedFileCompat.fromUri(application, uri, CachedFileCompat.build(
                    name = book.filePath.substringAfterLast('/'), path = book.filePath,
                    size = size, lastModified = 0L, isDirectory = false
                ))
            }
            val sourceFile = CachedFileCompat.fromUri(application, sourceUri.toUri())
            if (runCatching { sourceFile.canAccess() && !sourceFile.isDirectory }.getOrDefault(false)) {
                return@runCatching sourceFile
            }
            snapshots.snapshot(sourceUri)?.let { file ->
                val localUri = AndroidFileProvider.getUriForFile(application, "${application.packageName}.fileprovider", file)
                val builder = CachedFileCompat.build(
                    name = book.filePath.substringAfterLast('/').ifBlank { "${book.title}.pdf" },
                    path = book.filePath,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    isDirectory = false
                )
                return@runCatching CachedFileCompat.fromUri(application, localUri, builder)
            }
        }

        application.contentResolver.persistedUriPermissions.forEach { storage ->
            val storageFile = CachedFileCompat.fromUri(
                application,
                storage.uri
            )

            if (!storageFile.isDirectory) return@forEach
            if (!book.filePath.startsWith(storageFile.path, ignoreCase = true)) return@forEach

            storageFile.walk().forEach { file ->
                if (book.filePath.equals(file.path, ignoreCase = true)) {
                    return@runCatching file
                }
            }
        }

        throw NoSuchElementException("Could not find file from book.")
    }

    override fun getStorageFiles(): Result<List<CachedFile>> = runCatching {
        application.contentResolver.persistedUriPermissions.mapNotNull { permission ->
            val storage = CachedFileCompat.fromUri(
                application,
                permission.uri
            )
            if (!storage.isDirectory) return@mapNotNull null

            storage
        }.let { storages ->
            storages.filter { storage ->
                storages.none {
                    it.path != storage.path && storage.path.startsWith(
                        it.path,
                        ignoreCase = true
                    )
                }
            }
        }
    }
}
