package com.alikal.odemetakibi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (Store.isActive(c) && Store.pending(c).isNotEmpty()) Notifier.show(c)
        Scheduler.schedule(c)
    }
}

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val idx = i.getIntExtra("item", -1)
        if (idx !in Store.ITEMS.indices) return
        Store.setPaid(c, idx, true)
        Scheduler.schedule(c)
        if (Store.pending(c).isEmpty()) {
            Notifier.cancel(c)
            Toast.makeText(c, "Bu ayın tüm ödemeleri tamamlandı 🎉", Toast.LENGTH_SHORT).show()
        } else {
            Notifier.show(c, silent = true)
            Toast.makeText(c, "${Store.ITEMS[idx]} ödendi", Toast.LENGTH_SHORT).show()
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Scheduler.schedule(c)
    }
}
