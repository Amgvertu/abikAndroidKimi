package info.prorabka.abik.android

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.HiltAndroidApp
import info.prorabka.abik.android.push.RuStorePushInitializer
import ru.rustore.sdk.pushclient.RuStorePushClient

@HiltAndroidApp
class AbikApp : Application() {

    override fun onCreate() {
        super.onCreate()
        forceRussianLocale()
        initRuStorePush()
    }

    /**
     * Приложение русскоязычное: принудительно локаль ru независимо от
     * системного языка. В т.ч. календарь выбора даты — неделя с понедельника,
     * месяцы и дни по-русски.
     */
    private fun forceRussianLocale() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"))
    }

    /** RuStore Push включается только когда задан project ID из консоли RuStore. */
    private fun initRuStorePush() {
        val projectId = BuildConfig.RUSTORE_PROJECT_ID
        if (projectId.isBlank()) return
        runCatching {
            RuStorePushClient.init(
                application = this,
                projectId = projectId,
            )
            RuStorePushInitializer.initialized = true
        }.onFailure { Log.e("AbikApp", "RuStore Push: ошибка инициализации", it) }
    }
}

