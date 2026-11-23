package ua.acclorite.book_story.data.settings

import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import ua.acclorite.book_story.data.settings.model.Setting

class SettingRestoreTest {
    private enum class Mode { FIRST, SECOND }

    @Test fun validatesBeforeApplyingAndDoesNotWriteAgain() {
        var writes = 0
        val setting = Setting(stringPreferencesKey("mode"), Mode.FIRST, { _: String -> writes++ }, { it.name }, { Mode.valueOf(it) })
        assertThrows(IllegalArgumentException::class.java) { setting.validateRestored("INVALID") }
        assertEquals(Mode.FIRST, setting.lastValue)
        setting.validateRestored("SECOND")
        setting.applyRestored("SECOND")
        assertEquals(Mode.SECOND, setting.lastValue)
        assertEquals(0, writes)
    }
}
