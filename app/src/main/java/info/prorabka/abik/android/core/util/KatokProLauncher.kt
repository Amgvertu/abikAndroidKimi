package info.prorabka.abik.android.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** Запускает Katok.pro, а при его отсутствии открывает страницу приложения в RuStore. */
object KatokProLauncher {
    private const val PACKAGE_NAME = "com.katok.pro"
    private const val RUSTORE_URL = "https://www.rustore.ru/catalog/app/$PACKAGE_NAME"

    fun open(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
        if (launchIntent != null && runCatching { context.startActivity(launchIntent) }.isSuccess) {
            return
        }

        val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(RUSTORE_URL))
        if (runCatching { context.startActivity(storeIntent) }.isFailure) {
            Toast.makeText(context, "Не удалось открыть Katok.pro", Toast.LENGTH_SHORT).show()
        }
    }
}
