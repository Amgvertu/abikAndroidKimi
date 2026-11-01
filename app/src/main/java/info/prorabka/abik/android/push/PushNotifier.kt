package info.prorabka.abik.android.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import info.prorabka.abik.android.MainActivity
import info.prorabka.abik.android.R

/** Имена провайдеров push — отправляются на сервер при регистрации устройства. */
object PushProviders {
    const val FCM = "FCM"
    const val RUSTORE = "RUSTORE"
    const val HUAWEI = "HUAWEI"
}

/** Единый показ уведомлений для всех провайдеров (FCM, RuStore, Huawei). */
object PushNotifier {

    private const val CHANNEL_ID = "abik_notifications"

    fun show(context: Context, title: String, body: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Уведомления abik",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(intent)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
