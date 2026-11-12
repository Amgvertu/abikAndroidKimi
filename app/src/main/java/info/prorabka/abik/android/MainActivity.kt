package info.prorabka.abik.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.prefs.SessionManager
import info.prorabka.abik.android.core.prefs.SettingsStore
import info.prorabka.abik.android.core.prefs.ThemeMode
import info.prorabka.abik.android.core.prefs.TokenStore
import info.prorabka.abik.android.core.security.BiometricAuthManager
import info.prorabka.abik.android.core.ui.theme.AbikTheme
import info.prorabka.abik.android.presentation.auth.BiometricLockScreen
import info.prorabka.abik.android.presentation.navigation.AppNavHost
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

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

    @Inject
    lateinit var tokenStore: TokenStore

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    private var biometricUnlocked by mutableStateOf(true)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        biometricUnlocked = !requiresBiometric()
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
                if (biometricUnlocked) {
                    info.prorabka.abik.android.presentation.system.AppSystemEffects()
                    Surface(modifier = Modifier.fillMaxSize().imePadding()) {
                        AppNavHost(
                            navController = rememberNavController(),
                            sessionManager = sessionManager,
                            teamManager = teamManager,
                        )
                    }
                } else {
                    BiometricLockScreen(
                        biometricAuthManager = biometricAuthManager,
                        onUnlocked = { biometricUnlocked = true },
                        onPasswordLogin = {
                            sessionManager.logoutLocal()
                            teamManager.clear()
                            biometricUnlocked = true
                        },
                    )
                }
            }
        }
    }

    override fun onStop() {
        // При возврате из фона сессия снова защищается системной биометрией.
        if (requiresBiometric()) biometricUnlocked = false
        super.onStop()
    }

    private fun requiresBiometric(): Boolean =
        tokenStore.isLoggedIn && tokenStore.biometricEnabled && biometricAuthManager.canAuthenticate()

    /** Android 13+: без runtime-разрешения push-уведомления не показываются. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
