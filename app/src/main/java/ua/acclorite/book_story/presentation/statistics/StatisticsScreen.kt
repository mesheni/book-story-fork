/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.statistics

import android.content.Intent
import android.os.Parcelable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.Switch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import ua.acclorite.book_story.R
import ua.acclorite.book_story.presentation.navigator.Screen
import ua.acclorite.book_story.ui.navigator.LocalNavigator
import ua.acclorite.book_story.ui.common.helpers.LocalSettings
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext

@Parcelize
object StatisticsScreen : Screen, Parcelable {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val model = hiltViewModel<StatisticsModel>()
        val state = model.state.collectAsStateWithLifecycle().value
        val settings = LocalSettings.current
        val navigator = LocalNavigator.current
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
        val dateFormatter = remember(settings.language.value) {
            DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        }

        LaunchedEffect(Unit) { model.refresh() }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.reading_statistics)) },
                    navigationIcon = {
                            androidx.compose.material3.IconButton(onClick = navigator::pop) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                            }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.isLoading && state.summary == null) {
                    item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                }
                item {
                    state.summary?.let { summary ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.statistics_today), style = MaterialTheme.typography.titleMedium)
                                Text(formatDuration(summary.todayMillis), style = MaterialTheme.typography.headlineMedium)
                                Text(stringResource(R.string.statistics_sessions, summary.todaySessions))
                            }
                        }
                    }
                }
                item {
                    state.summary?.let { summary ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.statistics_week), style = MaterialTheme.typography.titleMedium)
                                Text(formatDuration(summary.weekMillis), style = MaterialTheme.typography.headlineSmall)
                                Text(stringResource(R.string.statistics_sessions, summary.weekSessions))
                                val barColor = MaterialTheme.colorScheme.primary
                                Spacer(Modifier.height(12.dp))
                                Canvas(Modifier.fillMaxWidth().height(110.dp)) {
                                    val max = (summary.days.maxOfOrNull { it.durationMillis } ?: 0L).coerceAtLeast(1L)
                                    val barWidth = size.width / (summary.days.size * 2f)
                                    summary.days.forEachIndexed { index, day ->
                                        val barHeight = size.height * day.durationMillis / max
                                        drawRoundRect(
                                            color = barColor,
                                            topLeft = androidx.compose.ui.geometry.Offset(
                                                x = index * size.width / summary.days.size + barWidth / 2,
                                                y = size.height - barHeight
                                            ),
                                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                                        )
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    summary.days.forEach { day ->
                                        Text(
                                            LocalDate.ofEpochDay(day.dayEpoch).format(dateFormatter),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                Text(stringResource(R.string.statistics_streak, summary.streakDays))
                            }
                        }
                    }
                }
                if (state.summary?.sessions.isNullOrEmpty()) {
                    item { Text(stringResource(R.string.statistics_empty)) }
                } else if (state.summary?.books.isNullOrEmpty()) {
                    item { Text(stringResource(R.string.statistics_books_empty)) }
                } else {
                    item {
                        Text(stringResource(R.string.statistics_books), style = MaterialTheme.typography.titleLarge)
                    }
                    items(state.summary!!.books) { book ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(book.title, style = MaterialTheme.typography.titleMedium)
                                Text(formatDuration(book.durationMillis))
                                Text(stringResource(R.string.statistics_sessions, book.sessionCount))
                                Text(stringResource(R.string.statistics_progress, (book.progress * 100).toInt()))
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.reading_statistics_collect))
                        Switch(
                            checked = settings.readingStatisticsEnabled.value,
                            onCheckedChange = { enabled ->
                                settings.readingStatisticsEnabled.update(enabled)
                                model.updateCollectionEnabled(enabled)
                            }
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = state.summary?.sessions?.isNotEmpty() == true,
                            onClick = {
                            model.shareCsv(onReady = { uri ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                            }, onError = {
                                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.statistics_export_error)) }
                            })
                        }) { Text(stringResource(R.string.statistics_export)) }
                        TextButton(
                            enabled = state.summary?.sessions?.isNotEmpty() == true || state.summary?.books?.isNotEmpty() == true,
                            onClick = model::requestClear
                        ) {
                            Text(stringResource(R.string.statistics_clear))
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }

        if (state.showClearDialog) {
            AlertDialog(
                onDismissRequest = model::dismissClear,
                title = { Text(stringResource(R.string.statistics_clear)) },
                text = { Text(stringResource(R.string.statistics_clear_confirm)) },
                confirmButton = {
                    TextButton(onClick = model::confirmClear) { Text(stringResource(R.string.delete)) }
                },
                dismissButton = {
                    TextButton(onClick = model::dismissClear) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }
}

@Composable
private fun formatDuration(millis: Long): String {
    val minutes = millis / 60_000
    val hours = minutes / 60
    return if (hours > 0) stringResource(R.string.statistics_duration_hours_minutes, hours, minutes % 60)
    else stringResource(R.string.statistics_duration_minutes, minutes)
}