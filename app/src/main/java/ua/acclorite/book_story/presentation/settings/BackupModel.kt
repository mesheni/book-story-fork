package ua.acclorite.book_story.presentation.settings

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.data.backup.BackupSnapshotReader
import ua.acclorite.book_story.data.backup.encodeBackupMetadata
import ua.acclorite.book_story.data.backup.writeBackupBundle
import ua.acclorite.book_story.data.backup.BackupSnapshot
import ua.acclorite.book_story.data.backup.BackupDatabaseRestorer
import ua.acclorite.book_story.data.backup.decodeBackupMetadata
import ua.acclorite.book_story.data.backup.readBackupBundle
import ua.acclorite.book_story.data.backup.BackupValue
import ua.acclorite.book_story.data.backup.restoreBackupSafely
import ua.acclorite.book_story.data.settings.SettingsManager
import ua.acclorite.book_story.presentation.library.LibraryScreen
import ua.acclorite.book_story.presentation.history.HistoryScreen
import ua.acclorite.book_story.domain.repository.BookRepository
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BackupModel @Inject constructor(
    private val app: Application,
    private val snapshotReader: BackupSnapshotReader,
    private val restorer: BackupDatabaseRestorer,
    private val settings: SettingsManager,
    private val books: BookRepository
) : ViewModel() {
    data class PreparedBackup(val snapshot: BackupSnapshot, val bookCount: Int, val directory: File)
    private var staged: PreparedBackup? = null

    suspend fun export(uri: Uri, includeBooks: Boolean) = withContext(Dispatchers.IO) {
        val snapshot = snapshotReader.read()
        val metadata = encodeBackupMetadata(snapshot).toByteArray(Charsets.UTF_8)
        require(metadata.size <= 32 * 1024 * 1024)
        val ids = if (includeBooks) snapshot.tables.getValue("BookEntity").map { (it.getValue("id") as BackupValue.Integer).value } else emptyList()
        val files = ids.associateWith { id -> books.getFileFromBook(Math.toIntExact(id)).getOrThrow().uri }
        val output = app.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot open backup file")
        output.use { writeBackupBundle(it, metadata, ids) { id -> app.contentResolver.openInputStream(Uri.parse(files.getValue(id))) ?: error("Cannot read book") } }
    }

    suspend fun prepareImport(uri: Uri): PreparedBackup = withContext(Dispatchers.IO) {
        discard()
        val directory = File(app.filesDir, "restored-books/${UUID.randomUUID()}")
        check(directory.mkdirs())
        try {
            val files = mutableMapOf<Long, File>()
            val input = app.contentResolver.openInputStream(uri) ?: error("Cannot open backup file")
            val bytes = input.use { readBackupBundle(it) { id -> File(directory, id.toString()).also { file -> files[id] = file }.outputStream().buffered() } }
            val snapshot = decodeBackupMetadata(bytes.toString(Charsets.UTF_8))
            restorer.validate(snapshot.tables)
            settings.validateRestorePreferences(snapshot.preferences)
            val rows = snapshot.tables.getValue("BookEntity")
            val bookIds = rows.map { (it.getValue("id") as BackupValue.Integer).value }.toSet()
            require(files.keys.all { it in bookIds }) { "Book missing from metadata" }
            val updated = rows.map { row ->
                val id = (row.getValue("id") as BackupValue.Integer).value
                val file = files[id] ?: return@map row
                require(file.length() > 0) { "Empty book file" }
                val path = (row.getValue("filePath") as BackupValue.Text).value
                val ext = path.substringAfterLast('.', "").lowercase().takeIf { it in setOf("pdf", "epub", "fb2", "txt", "html", "htm", "md") } ?: error("Unsupported book format")
                val named = File(directory, "$id.$ext")
                check(file.renameTo(named))
                val localUri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", named)
                row + ("sourceUri" to BackupValue.Text(localUri.toString()))
            }
            val prepared = PreparedBackup(snapshot.copy(tables = snapshot.tables + ("BookEntity" to updated)), files.size, directory)
            staged = prepared
            prepared
        } catch (error: Throwable) { directory.deleteRecursively(); throw error }
    }

    suspend fun restore(prepared: PreparedBackup) = withContext(NonCancellable) {
        require(staged == prepared)
        // Once restoration starts, retain the files even on failure: an
        // unsuccessful rollback must never leave restored rows pointing to deleted files.
        staged = null
        restoreBackupSafely(prepared.snapshot, snapshotReader::read, settings::validateRestorePreferences, restorer::restore, settings::restorePreferences)
        if (prepared.bookCount == 0) prepared.directory.deleteRecursively()
        LibraryScreen.refreshListChannel.trySend(0)
        HistoryScreen.refreshListChannel.trySend(0)
    }

    fun discard() { staged?.directory?.deleteRecursively(); staged = null }
    override fun onCleared() { discard(); super.onCleared() }
}
