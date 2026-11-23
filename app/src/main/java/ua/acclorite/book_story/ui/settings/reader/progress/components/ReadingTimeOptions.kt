package ua.acclorite.book_story.ui.settings.reader.progress.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ua.acclorite.book_story.R
import ua.acclorite.book_story.ui.common.components.settings.SliderWithTitle
import ua.acclorite.book_story.ui.common.components.settings.SwitchWithTitle
import ua.acclorite.book_story.ui.common.helpers.LocalSettings
import ua.acclorite.book_story.ui.theme.ExpandingTransition

@Composable
fun ReadingTimeOptions() {
    val settings = LocalSettings.current
    SwitchWithTitle(
        selected = settings.showReadingTime.value,
        title = stringResource(R.string.reading_time_option),
        description = stringResource(R.string.reading_time_option_desc),
        onClick = { settings.showReadingTime.update(!settings.showReadingTime.lastValue) }
    )
    ExpandingTransition(visible = settings.showReadingTime.value) {
        SwitchWithTitle(
            selected = settings.autoReadingPace.value,
            title = stringResource(R.string.auto_reading_pace),
            description = stringResource(R.string.auto_reading_pace_desc),
            onClick = { settings.autoReadingPace.update(!settings.autoReadingPace.lastValue) }
        )
    }
    ExpandingTransition(visible = settings.showReadingTime.value) {
        SliderWithTitle(
            value = settings.readingWordsPerMinute.value to stringResource(R.string.words_per_minute_unit),
            fromValue = 50,
            toValue = 600,
            title = stringResource(R.string.reading_speed_option),
            onValueChange = { settings.readingWordsPerMinute.update(it) }
        )
    }
}
