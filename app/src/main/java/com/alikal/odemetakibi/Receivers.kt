package com.alikal.odemetakibi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val app = c.applicationContext
        val pr = goAsync()
        Thread {
            try {
                // Başka cihazdan ödendi işaretlendiyse burada tekrar hatırlatma
                Sync.syncNow(app)
                if (Store.isActive(app) && Store.pending(app).isNotEmpty()) Notifier.show(app)
                else Notifier.cancel(app)
            } finally {
                Scheduler.schedule(app)
                pr.finish()
            }
        }.start()
    }
}

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val idx = i.getIntExtra("item", -1)
        if (idx !in Store.ITEMS.indices) return
        val app = c.applicationContext
        Store.setPaid(app, idx, true)
        Scheduler.schedule(app)
        if (Store.pending(app).isEmpty()) {
            Notifier.cancel(app)
            Toast.makeText(app, "Bu ayın tüm ödemeleri tamamlandı 🎉", Toast.LENGTH_SHORT).show()
        } else {
            Notifier.show(app, silent = true)
            Toast.makeText(app, "${Store.ITEMS[idx]} ödendi", Toast.LENGTH_SHORT).show()
        }
        val pr = goAsync()
        Thread {
            try { Sync.pushOne(app, idx, true) } finally { pr.finish() }
        }.start()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Scheduler.schedule(c)
    }
}
