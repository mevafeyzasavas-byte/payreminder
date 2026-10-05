package com.alikal.odemetakibi

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object Scheduler {

    /** Sıradaki hatırlatma: ayın ödeme gününden itibaren, ödenmeyen varsa her gün. */
    fun nextTrigger(c: Context): Long {
        val now = Calendar.getInstance()
        val curMonth = now.get(Calendar.MONTH)
        val payDay = Store.payDay(c)
        val hasPending = Store.pending(c).isNotEmpty()
        for (d in 0..70) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, d)
                set(Calendar.HOUR_OF_DAY, Store.hour(c)); set(Calendar.MINUTE, Store.minute(c))
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= now.timeInMillis) continue
            val day = cal.get(Calendar.DAY_OF_MONTH)
            if (cal.get(Calendar.MONTH) == curMonth) {
                if (hasPending && day >= payDay) return cal.timeInMillis
            } else if (day >= payDay) {
                return cal.timeInMillis // yeni ay: hepsi tekrar bekliyor
            }
        }
        return now.timeInMillis + 24L * 3600 * 1000
    }

    fun schedule(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            c, 1, Intent(c, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val t = nextTrigger(c)
        try {
            when {
                Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms() ->
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
                Build.VERSION.SDK_INT >= 23 ->
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
                else -> am.setExact(AlarmManager.RTC_WAKEUP, t, pi)
            }
        } catch (e: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, t, pi)
        }
    }
}
