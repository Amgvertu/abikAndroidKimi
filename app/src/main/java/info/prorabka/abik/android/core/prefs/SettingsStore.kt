package info.prorabka.abik.android.core.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import info.prorabka.abik.android.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "abik_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Настройки приложения: тема оформления и переопределение base URL (debug).
 */
@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val keyTheme = stringPreferencesKey("theme_mode")
    private val keyBaseUrl = stringPreferencesKey("base_url_override")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[keyTheme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[keyTheme] = mode.name }
    }

    /** null — использовать URL по умолчанию (BuildConfig.DEFAULT_BASE_URL). */
    val baseUrlOverride: Flow<String?> = context.dataStore.data.map { it[keyBaseUrl] }

    suspend fun setBaseUrlOverride(url: String?) {
        context.dataStore.edit { prefs ->
            if (url == null) prefs.remove(keyBaseUrl) else prefs[keyBaseUrl] = url
        }
    }

    val defaultBaseUrl: String get() = BuildConfig.DEFAULT_BASE_URL
    val localBaseUrl: String get() = BuildConfig.LOCAL_BASE_URL

    private val keyTeamId = longPreferencesKey("selected_team_id")

    /** Последняя выбранная команда (0 — не выбрана). */
    val selectedTeamId: Flow<Long> = context.dataStore.data.map { it[keyTeamId] ?: 0L }

    suspend fun setSelectedTeamId(id: Long) {
        context.dataStore.edit { it[keyTeamId] = id }
    }
}
