package ua.acclorite.book_story.data.backup

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

suspend fun restoreBackupSafely(
    target: BackupSnapshot,
    readCurrent: suspend () -> BackupSnapshot,
    validatePreferences: suspend (Map<String, BackupValue>) -> Unit,
    restoreTables: suspend (Map<String, List<Map<String, BackupValue>>>) -> Unit,
    restorePreferences: suspend (Map<String, BackupValue>) -> Unit
) {
    validatePreferences(target.preferences)
    val previous = readCurrent()
    withContext(NonCancellable) {
        restoreTables(target.tables)
        try {
            restorePreferences(target.preferences)
        } catch (failure: Throwable) {
            try { restoreTables(previous.tables) } catch (rollback: Throwable) { failure.addSuppressed(rollback) }
            try { restorePreferences(previous.preferences) } catch (rollback: Throwable) { failure.addSuppressed(rollback) }
            throw failure
        }
    }
}
