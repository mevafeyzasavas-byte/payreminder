package com.alikal.odemetakibi

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Store {
    val ITEMS = listOf("Kira", "Kredi Kartı", "Aidat", "Emlak Aidat", "Emlak Taksit")

    private fun p(c: Context) = c.getSharedPreferences("odeme", Context.MODE_PRIVATE)

    fun payDay(c: Context) = p(c).getInt("day", 5)
    fun hour(c: Context) = p(c).getInt("hour", 9)
    fun minute(c: Context) = p(c).getInt("minute", 0)

    fun saveSettings(c: Context, day: Int, hour: Int, minute: Int) {
        p(c).edit().putInt("day", day).putInt("hour", hour).putInt("minute", minute).apply()
    }

    private fun month() = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    /** Bu ay ödenmiş olarak işaretlenenler. Ay değişince otomatik sıfırlanır. */
    fun paid(c: Context): MutableSet<Int> {
        val sp = p(c)
        if (sp.getString("month", "") != month()) {
            sp.edit().putString("month", month()).putString("paid", "").apply()
            return mutableSetOf()
        }
        return (sp.getString("paid", "") ?: "").split(",")
            .mapNotNull { it.toIntOrNull() }.toMutableSet()
    }

    fun setPaid(c: Context, index: Int, value: Boolean) {
        val s = paid(c)
        if (value) s.add(index) else s.remove(index)
        p(c).edit().putString("month", month()).putString("paid", s.joinToString(",")).apply()
    }

    fun pending(c: Context): List<Int> {
        val s = paid(c)
        return ITEMS.indices.filter { it !in s }
    }

    /** Bu ay hatırlatma günü geldi mi? */
    fun isActive(c: Context) = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) >= payDay(c)
}
