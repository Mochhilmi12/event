package com.example.keydates

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.keydates.databinding.ActivityMainBinding
import java.util.*

data class Event(
    val id: String,
    var title: String,
    var description: String,
    var day: Int,
    var month: Int,
    var year: Int,
    var hour: Int,
    var minute: Int
)

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: EventAdapter
    private val sharedPreferences by lazy { getSharedPreferences("events", MODE_PRIVATE) }
    private val eventList = mutableListOf<Event>()

    // Register the launcher for requesting the exact alarm permission
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadEvents()

        adapter = EventAdapter(eventList, this::onEditEvent, this::onDeleteEvent)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        val space = resources.getDimensionPixelSize(R.dimen.recyclerViewItemSpacing)
        binding.recyclerView.addItemDecoration(ItemSpacingDecoration(space))

        binding.addEventButton.setOnClickListener {
            EventDialogFragment { event -> addOrUpdateEvent(event) }.show(supportFragmentManager, "EventDialog")
        }

        // Initialize the ActivityResultLauncher
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Permission granted, initialize the app
                initializeApp()
            } else {
                // Permission denied, show a toast
                Toast.makeText(this, "Permission to set exact alarms is required", Toast.LENGTH_SHORT).show()
            }
        }

        // Check for permission if the Android version is Android 12 (API level 31) or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!isExactAlarmPermissionGranted()) {
                // If permission is not granted, request it
                requestExactAlarmPermission()
            } else {
                // If permission is granted, continue with app initialization
                initializeApp()
            }
        } else {
            // For versions below API 31, no need for runtime permission
            initializeApp()
        }
    }

    private fun isExactAlarmPermissionGranted(): Boolean {
        // Check if the app has permission to schedule exact alarms
        return Settings.canDrawOverlays(this)
    }

    private fun requestExactAlarmPermission() {
        // Request the exact alarm permission from the user
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        requestPermissionLauncher.launch(intent) // Use the launcher to request the permission
    }

    private fun initializeApp() {
        // Perform any initialization tasks after permission is granted
    }

    private fun loadEvents() {
        val json = sharedPreferences.getString("eventList", null)
        if (json != null) {
            val jsonArray = org.json.JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val event = Event(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getString("title"),
                    description = jsonObject.getString("description"),
                    day = jsonObject.getInt("day"),
                    month = jsonObject.getInt("month"),
                    year = jsonObject.getInt("year"),
                    hour = jsonObject.getInt("hour"),
                    minute = jsonObject.getInt("minute")
                )
                eventList.add(event)
            }
        }
    }

    private fun saveEvents() {
        try {
            val jsonArray = org.json.JSONArray()
            for (event in eventList) {
                val jsonObject = org.json.JSONObject().apply {
                    put("id", event.id)
                    put("title", event.title)
                    put("description", event.description)
                    put("day", event.day)
                    put("month", event.month)
                    put("year", event.year)
                    put("hour", event.hour)
                    put("minute", event.minute)
                }
                jsonArray.put(jsonObject)
            }
            sharedPreferences.edit().putString("eventList", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving events: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addOrUpdateEvent(event: Event) {
        val index = eventList.indexOfFirst { it.id == event.id }
        if (index != -1) {
            eventList[index] = event
        } else {
            eventList.add(event)
        }
        adapter.notifyDataSetChanged()
        saveEvents()
        scheduleNotification(event)
    }

    private fun onEditEvent(event: Event) {
        EventDialogFragment(event) { updatedEvent -> addOrUpdateEvent(updatedEvent) }
            .show(supportFragmentManager, "EventDialog")
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onDeleteEvent(event: Event) {
        eventList.remove(event)
        adapter.notifyDataSetChanged()
        saveEvents()
    }

    @SuppressLint("MissingPermission", "ScheduleExactAlarm")
    private fun scheduleNotification(event: Event) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("eventTitle", event.title)
        }

        // Set PendingIntent with FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            this, event.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set the exact time for the alarm
        val calendar = Calendar.getInstance().apply {
            set(event.year, event.month - 1, event.day, event.hour, event.minute, 0)
        }

        // Schedule the alarm using the exact alarm manager
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }
}
