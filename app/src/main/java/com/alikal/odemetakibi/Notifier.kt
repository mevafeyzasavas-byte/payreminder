package com.alikal.odemetakibi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifier {
    private const val CH = "odeme_hatirlatma"
    private const val ID = 1

    private fun actionIntent(c: Context, index: Int): PendingIntent {
        val i = Intent(c, ActionReceiver::class.java).putExtra("item", index)
        return PendingIntent.getBroadcast(
            c, 100 + index, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun show(c: Context, silent: Boolean = false) {
        val pending = Store.pending(c)
        if (pending.isEmpty()) { cancel(c); return }
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CH, "Ödeme hatırlatma", NotificationManager.IMPORTANCE_HIGH)
            ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            (c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
        val names = pending.joinToString(", ") { Store.ITEMS[it] }
        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(c, CH)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Ödeme zamanı · ${pending.size} bekleyen")
            .setContentText(names)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Ödenmeyenler: $names"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOnlyAlertOnce(silent)
            .setAutoCancel(false)
            .setContentIntent(open)
        // En fazla 2 ödemeyi bildirimden direkt "ödendi" yapabilirsin
        pending.take(2).forEach { b.addAction(0, "✓ ${Store.ITEMS[it]}", actionIntent(c, it)) }
        try {
            NotificationManagerCompat.from(c).notify(ID, b.build())
        } catch (e: SecurityException) {
        }
    }

    fun cancel(c: Context) = NotificationManagerCompat.from(c).cancel(ID)
}
