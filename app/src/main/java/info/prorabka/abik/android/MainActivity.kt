package info.prorabka.abik.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.prefs.SessionManager
import info.prorabka.abik.android.core.prefs.SettingsStore
import info.prorabka.abik.android.core.prefs.ThemeMode
import info.prorabka.abik.android.core.ui.theme.AbikTheme
import info.prorabka.abik.android.presentation.navigation.AppNavHost
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Приложение русскоязычное: принудительно локаль ru для всего дерева
     * экрана (календарь выбора даты — неделя с понедельника, месяцы и дни
     * по-русски), независимо от системного языка устройства.
     */
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(java.util.Locale("ru"))
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    @Inject
    lateinit var settingsStore: SettingsStore

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var teamManager: CurrentTeamManager

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            val themeMode by settingsStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            AbikTheme(dark = dark) {
                Surface(modifier = Modifier.fillMaxSize().imePadding()) {
                    AppNavHost(
                        navController = rememberNavController(),
                        sessionManager = sessionManager,
                        teamManager = teamManager,
                    )
                }
            }
        }
    }

    /** Android 13+: без runtime-разрешения push-уведомления не показываются. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
