package com.example.keydates

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission", "NotificationPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("eventTitle") ?: "Event Reminder"
        val notification = NotificationCompat.Builder(context, "eventChannel")
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .setContentTitle("Event Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(title.hashCode(), notification)
    }
}