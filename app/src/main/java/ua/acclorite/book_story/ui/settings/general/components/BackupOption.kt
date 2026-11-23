package ua.acclorite.book_story.ui.settings.general.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import ua.acclorite.book_story.R
import ua.acclorite.book_story.presentation.settings.BackupModel
import ua.acclorite.book_story.presentation.settings.SettingsModel

@Composable
fun BackupOption() {
    val model = hiltViewModel<BackupModel>()
    val settingsModel = hiltViewModel<SettingsModel>()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var includeBooks by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<BackupModel.PreparedBackup?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                model.export(uri, includeBooks)
                Toast.makeText(context, R.string.backup_export_success, Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                Toast.makeText(context, R.string.backup_export_error, Toast.LENGTH_LONG).show()
            } finally { busy = false }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try { pending = model.prepareImport(uri) }
            catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                Toast.makeText(context, R.string.backup_import_error, Toast.LENGTH_LONG).show()
            } finally { busy = false }
        }
    }
    pending?.let { prepared ->
        AlertDialog(
            onDismissRequest = { pending = null; model.discard() },
            title = { Text(stringResource(R.string.backup_import)) },
            text = { Text(stringResource(R.string.backup_import_confirm, prepared.snapshot.tables["BookEntity"].orEmpty().size, prepared.bookCount)) },
            confirmButton = { TextButton(onClick = {
                pending = null
                scope.launch {
                    busy = true
                    try {
                        model.restore(prepared)
                        settingsModel.refreshAfterRestore()
                        Toast.makeText(context, R.string.backup_import_success, Toast.LENGTH_LONG).show()
                    } catch (error: Exception) {
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        model.discard()
                        Toast.makeText(context, R.string.backup_import_error, Toast.LENGTH_LONG).show()
                    } finally { busy = false }
                }
            }) { Text(stringResource(R.string.backup_import)) } },
            dismissButton = { TextButton(onClick = { pending = null; model.discard() }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.backup_description), style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = includeBooks, onCheckedChange = { includeBooks = it }, enabled = !busy)
            Text(stringResource(R.string.backup_include_books), Modifier.padding(start = 8.dp))
        }
        Button(enabled = !busy, onClick = { launcher.launch("bookstory-backup.zip") }) {
            Text(stringResource(if (busy) R.string.backup_working else R.string.backup_export))
        }
        Button(enabled = !busy, onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }) {
            Text(stringResource(R.string.backup_import))
        }
    }
}
