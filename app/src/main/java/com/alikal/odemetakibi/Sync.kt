package com.alikal.odemetakibi

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Firebase Realtime Database (REST) ile cihazlar arası eşitleme. Yol: /odeme/<yyyy-MM>/<anahtar> */
object Sync {
    private val KEYS = listOf("kira", "kredi_karti", "aidat", "emlak_aidat", "emlak_taksit")

    private fun url(c: Context, sub: String = "") =
        "${Store.dbUrl(c).trim().trimEnd('/')}/odeme/${Store.monthKey()}$sub.json"

    private fun http(method: String, url: String, body: String? = null): String? = try {
        val con = URL(url).openConnection() as HttpURLConnection
        con.requestMethod = method
        con.connectTimeout = 4000
        con.readTimeout = 4000
        if (body != null) {
            con.doOutput = true
            con.setRequestProperty("Content-Type", "application/json")
            con.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = con.responseCode
        val text = (if (code in 200..299) con.inputStream else con.errorStream)
            ?.bufferedReader()?.use { it.readText() }
        con.disconnect()
        if (code in 200..299) text ?: "" else null
    } catch (e: Exception) {
        null
    }

    /** Tek bir ödemenin durumunu buluta yazar. */
    fun pushOne(c: Context, index: Int, value: Boolean): Boolean {
        val ok = http("PUT", url(c, "/${KEYS[index]}"), value.toString()) != null
        Store.setOnline(c, ok)
        if (!ok) Store.setDirty(c, true)
        return ok
    }

    private fun pushAll(c: Context): Boolean {
        val paid = Store.paid(c)
        for (i in KEYS.indices) {
            if (http("PUT", url(c, "/${KEYS[i]}"), (i in paid).toString()) == null) return false
        }
        return true
    }

    /** Buluttaki durumu çeker. Yerel durum değiştiyse true döner. */
    fun syncNow(c: Context): Boolean {
        if (Store.dirty(c)) {
            if (pushAll(c)) Store.setDirty(c, false) else { Store.setOnline(c, false); return false }
        }
        val txt = http("GET", url(c))
        if (txt == null) { Store.setOnline(c, false); return false }
        Store.setOnline(c, true)
        val before = Store.paid(c)
        val t = txt.trim()
        if (t.isEmpty() || t == "null") {
            if (before.isNotEmpty()) pushAll(c)
            return false
        }
        return try {
            val o = JSONObject(t)
            val s = mutableSetOf<Int>()
            KEYS.forEachIndexed { i, k -> if (o.optBoolean(k, false)) s.add(i) }
            if (s != before) { Store.replacePaid(c, s); true } else false
        } catch (e: Exception) {
            false
        }
    }
}
