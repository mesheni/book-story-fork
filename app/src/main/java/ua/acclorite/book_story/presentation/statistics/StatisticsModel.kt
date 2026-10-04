/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.statistics

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.domain.use_case.statistics.ExportReadingStatisticsUseCase
import ua.acclorite.book_story.domain.use_case.statistics.GetStatisticsSummaryUseCase
import ua.acclorite.book_story.domain.service.ReadingSessionTracker
import java.io.File
import javax.inject.Inject

@HiltViewModel
class StatisticsModel @Inject constructor(
    private val getSummary: GetStatisticsSummaryUseCase,
    private val exportStatistics: ExportReadingStatisticsUseCase,
    private val readingSessionTracker: ReadingSessionTracker,
    private val application: Application
) : ViewModel() {
    private val _state = MutableStateFlow(StatisticsState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching { getSummary() }.onSuccess { summary ->
                _state.update { it.copy(summary = summary, isLoading = false) }
            }.onFailure {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun requestClear() {
        _state.update { it.copy(showClearDialog = true) }
    }

    fun updateCollectionEnabled(enabled: Boolean) {
        viewModelScope.launch { readingSessionTracker.setEnabled(enabled) }
    }

    fun dismissClear() {
        _state.update { it.copy(showClearDialog = false) }
    }

    fun confirmClear() {
        viewModelScope.launch {
            val result = runCatching { readingSessionTracker.clearStatistics() }
            _state.update { it.copy(showClearDialog = false) }
            if (result.isSuccess) refresh()
        }
    }

    suspend fun exportCsv(): Uri = withContext(Dispatchers.IO) {
        val csv = exportStatistics()
        val directory = File(application.cacheDir, "statistics").apply { mkdirs() }
        directory.listFiles()?.forEach { it.delete() }
        val file = File(directory, "reading-statistics.csv").apply { writeText(csv, Charsets.UTF_8) }
        FileProvider.getUriForFile(
            application,
            "${application.packageName}.fileprovider",
            file
        )
    }

    fun shareCsv(onReady: (Uri) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            runCatching { exportCsv() }
                .onSuccess(onReady)
                .onFailure { onError() }
        }
    }
}