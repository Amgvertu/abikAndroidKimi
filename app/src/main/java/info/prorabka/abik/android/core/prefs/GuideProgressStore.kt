package info.prorabka.abik.android.core.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Локально запоминает показанную справку отдельно для каждого пользователя. */
@Singleton
class GuideProgressStore @Inject constructor(
    @ApplicationContext context: Context,
    private val tokenStore: TokenStore,
) {
    private val prefs = context.getSharedPreferences("abik_guide_progress", Context.MODE_PRIVATE)

    fun completed(): Set<String> =
        prefs.getStringSet(key(), emptySet()).orEmpty().toSet()

    fun complete(guideKey: String) {
        prefs.edit().putStringSet(key(), completed() + guideKey).apply()
    }

    private fun key() = "completed:${tokenStore.userId}"
}
