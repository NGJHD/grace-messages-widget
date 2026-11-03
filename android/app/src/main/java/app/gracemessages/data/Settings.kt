package app.gracemessages.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class WidgetSettings(
    val fontSizeSp: Int = 18,
    val gradient: Boolean = true,
    val color1: Int = Presets.gradients[0].c1,
    val color2: Int = Presets.gradients[0].c2,
    val category: CategoryFilter = CategoryFilter.ALL,
    /** 0 = daily at midnight, otherwise every N hours. */
    val refreshHours: Int = 0,
) {
    companion object {
        const val MIN_FONT = 12
        const val MAX_FONT = 36
        val REFRESH_OPTIONS = listOf(0, 1, 2, 3, 4, 6, 12)
    }
}

internal object Keys {
    val FONT_SIZE = intPreferencesKey("font_size")
    val GRADIENT = booleanPreferencesKey("gradient")
    val COLOR1 = intPreferencesKey("color1")
    val COLOR2 = intPreferencesKey("color2")
    val CATEGORY = stringPreferencesKey("category")
    val REFRESH_HOURS = intPreferencesKey("refresh_hours")

    // Selection state
    val CURRENT_ID = stringPreferencesKey("current_id")
    val PERIOD_KEY = stringPreferencesKey("period_key")
    val SHOWN = stringSetPreferencesKey("shown_ids")
}

fun Preferences.toSettings(): WidgetSettings {
    val d = WidgetSettings()
    return WidgetSettings(
        fontSizeSp = this[Keys.FONT_SIZE] ?: d.fontSizeSp,
        gradient = this[Keys.GRADIENT] ?: d.gradient,
        color1 = this[Keys.COLOR1] ?: d.color1,
        color2 = this[Keys.COLOR2] ?: d.color2,
        category = this[Keys.CATEGORY]
            ?.let { runCatching { CategoryFilter.valueOf(it) }.getOrNull() } ?: d.category,
        refreshHours = this[Keys.REFRESH_HOURS] ?: d.refreshHours,
    )
}

suspend fun Context.readSettings(): WidgetSettings = dataStore.data.first().toSettings()

suspend fun Context.updateSettings(transform: (WidgetSettings) -> WidgetSettings) {
    dataStore.edit { p ->
        val n = transform(p.toSettings())
        p[Keys.FONT_SIZE] = n.fontSizeSp
        p[Keys.GRADIENT] = n.gradient
        p[Keys.COLOR1] = n.color1
        p[Keys.COLOR2] = n.color2
        p[Keys.CATEGORY] = n.category.name
        p[Keys.REFRESH_HOURS] = n.refreshHours
    }
}
