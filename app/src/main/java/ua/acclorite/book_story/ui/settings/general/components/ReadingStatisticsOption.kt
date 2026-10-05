/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.settings.general.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ua.acclorite.book_story.R
import androidx.hilt.navigation.compose.hiltViewModel
import ua.acclorite.book_story.presentation.settings.SettingsEvent
import ua.acclorite.book_story.presentation.settings.SettingsModel
import ua.acclorite.book_story.ui.common.components.settings.SwitchWithTitle
import ua.acclorite.book_story.ui.common.helpers.LocalSettings

@Composable
fun ReadingStatisticsOption() {
    val settings = LocalSettings.current
    val settingsModel = hiltViewModel<SettingsModel>()
    SwitchWithTitle(
        selected = settings.readingStatisticsEnabled.value,
        title = stringResource(R.string.reading_statistics_collect),
        description = stringResource(R.string.reading_statistics_collect_desc)
    ) {
        val enabled = !settings.readingStatisticsEnabled.lastValue
        settings.readingStatisticsEnabled.update(enabled)
        settingsModel.onEvent(SettingsEvent.OnUpdateReadingStatistics(enabled))
    }
}