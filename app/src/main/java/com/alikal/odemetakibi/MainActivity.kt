package com.alikal.odemetakibi

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var content: LinearLayout
    private val INK = Color.parseColor("#2B2B2B")
    private val MUTED = Color.parseColor("#7A7270")
    private val RED = Color.parseColor("#F2453D")
    private val GREEN = Color.parseColor("#2E9E5B")
    private val LINE = Color.parseColor("#E3DEDC")

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun shape(fill: Int, stroke: Int? = null, radius: Int = 16, oval: Boolean = false) =
        GradientDrawable().apply {
            if (oval) this.shape = GradientDrawable.OVAL else cornerRadius = dp(radius).toFloat()
            setColor(fill)
            if (stroke != null) setStroke(dp(1), stroke)
        }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Color.parseColor("#F6F4F3")
        if (Build.VERSION.SDK_INT >= 23) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        scroll.addView(content)
        setContentView(scroll)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    override fun onResume() {
        super.onResume()
        Scheduler.schedule(this)
        render()
        askExactAlarm()
    }

    private fun toggle(i: Int) {
        Store.setPaid(this, i, i !in Store.paid(this))
        if (Store.pending(this).isEmpty()) Notifier.cancel(this)
        Scheduler.schedule(this)
        render()
    }

    private fun tv(text: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color)
            if (bold) setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
        }

    private fun render() {
        content.removeAllViews()
        val paid = Store.paid(this)
        val total = Store.ITEMS.size
        val monthName = SimpleDateFormat("LLLL yyyy", Locale("tr")).format(Date())

        // Başlık
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(tv(monthName.replaceFirstChar { it.uppercase() }, 13f, MUTED))
        titles.addView(tv("Ödeme Takibi", 28f, INK, true))
        head.addView(titles, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        head.addView(ImageButton(this).apply {
            background = shape(Color.WHITE, LINE, oval = true)
            setImageResource(R.drawable.ic_sliders)
            contentDescription = "Ayarlar"
            setOnClickListener { showSettings() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        content.addView(head)

        // Özet kartı
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(Color.WHITE, LINE, 20)
            setPadding(dp(20), dp(18), dp(20), dp(18))
        }
        card.addView(tv("${paid.size} / $total ödendi", 24f, INK, true))
        val bar = LinearLayout(this).apply {
            background = shape(Color.parseColor("#EFE9E7"), null, 6)
            weightSum = total.toFloat()
        }
        bar.addView(View(this).apply { background = shape(GREEN, null, 6) },
            LinearLayout.LayoutParams(0, dp(10), paid.size.toFloat()))
        card.addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(10)).apply {
            topMargin = dp(12)
        })
        val next = SimpleDateFormat("d MMMM, HH:mm", Locale("tr")).format(Date(Scheduler.nextTrigger(this)))
        val note = if (Store.pending(this).isEmpty()) "Bu ayın tüm ödemeleri tamamlandı 🎉 · Sonraki: $next"
        else if (!Store.isActive(this)) "Hatırlatmalar ayın ${Store.payDay(this)}. gününde başlar · $next"
        else "Sıradaki hatırlatma: $next"
        card.addView(tv(note, 13f, MUTED).apply { setPadding(0, dp(12), 0, 0) })
        content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(22) })

        // Ödeme satırları
        content.addView(tv("Bu ayın ödemeleri", 13f, MUTED).apply { setPadding(0, dp(26), 0, dp(8)) })
        Store.ITEMS.forEachIndexed { i, name ->
            val done = i in paid
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                background = shape(Color.WHITE, LINE, 16)
                setPadding(dp(16), dp(16), dp(16), dp(16))
                setOnClickListener { toggle(i) }
            }
            row.addView(TextView(this).apply {
                text = if (done) "✓" else ""
                setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                background = if (done) shape(GREEN, null, oval = true) else shape(Color.WHITE, Color.parseColor("#BDB5B2"), oval = true)
            }, LinearLayout.LayoutParams(dp(28), dp(28)))
            row.addView(tv(name, 16f, if (done) MUTED else INK, true).apply {
                if (done) paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(14) })
            row.addView(tv(if (done) "Ödendi" else "Bekliyor", 13f, if (done) GREEN else RED, true))
            content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }

        content.addView(tv("Ödenmeyenler için her gün hatırlatılır. Satıra dokunarak ödendi işaretini aç/kapat.", 12f, MUTED)
            .apply { setPadding(0, dp(14), 0, 0) })
    }

    private fun askExactAlarm() {
        if (Build.VERSION.SDK_INT < 31) return
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val sp = getSharedPreferences("odeme", Context.MODE_PRIVATE)
        if (am.canScheduleExactAlarms() || sp.getBoolean("asked_exact", false)) return
        sp.edit().putBoolean("asked_exact", true).apply()
        AlertDialog.Builder(this)
            .setTitle("Tam saatinde hatırlatma")
            .setMessage("Hatırlatmaların tam saatinde gelmesi için \"Alarmlar ve hatırlatıcılar\" iznini açın.")
            .setPositiveButton("İzin ver") { _, _ ->
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
            }
            .setNegativeButton("Şimdi değil", null).show()
    }

    private fun showSettings() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
        }
        fun field(label: String, value: Int): EditText {
            box.addView(TextView(this).apply { text = label; textSize = 13f; setPadding(0, dp(12), 0, 0) })
            return EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER; setText(value.toString())
            }.also { box.addView(it) }
        }
        val day = field("Hatırlatma başlangıç günü (1-28)", Store.payDay(this))
        val hour = field("Günlük hatırlatma saati (0-23)", Store.hour(this))
        val min = field("Dakika (0-59)", Store.minute(this))
        AlertDialog.Builder(this)
            .setTitle("Ayarlar")
            .setView(box)
            .setPositiveButton("Kaydet") { _, _ ->
                val d = day.text.toString().toIntOrNull() ?: 5
                val h = hour.text.toString().toIntOrNull() ?: 9
                val m = min.text.toString().toIntOrNull() ?: 0
                if (d !in 1..28 || h !in 0..23 || m !in 0..59) {
                    Toast.makeText(this, "Geçersiz değer, kaydedilmedi", Toast.LENGTH_LONG).show()
                } else {
                    Store.saveSettings(this, d, h, m)
                    Scheduler.schedule(this)
                    render()
                }
            }
            .setNegativeButton("Vazgeç", null).show()
    }
}
