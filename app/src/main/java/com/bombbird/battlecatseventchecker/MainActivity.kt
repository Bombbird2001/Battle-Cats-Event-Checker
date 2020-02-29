package com.bombbird.battlecatseventchecker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {
    private val htmlRequest = HtmlRequest()
    private val requestReceiver = RequestReceiver()
    private var selected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val intent = Intent(this, RequestReceiver::class.java)
        intent.action = "android.intent.action.NOTIFY"
        val testIntent = PendingIntent.getBroadcast(this, 7776, intent, PendingIntent.FLAG_NO_CREATE)
        selected = testIntent != null
        if (selected) {
            fab_on.visibility = View.INVISIBLE
            fab_off.visibility = View.VISIBLE
            textView.setText(R.string.notify_on)
        }

        registerReceiver(RequestReceiver(), IntentFilter("android.intent.action.NOTIFY"))

        fab_on.setOnClickListener {
            if (!selected) {
                htmlRequest.checkEvent(this, true)
                requestReceiver.scheduleRequest(this)
                selected = true
                println("Alarm activated")
                textView.setText(R.string.notify_on)
                it.visibility = View.INVISIBLE
                fab_off.visibility = View.VISIBLE
            }
        }
        fab_off.setOnClickListener {
            if (selected) {
                requestReceiver.cancelRequests()
                selected = false
                println("Alarm deactivated")
                textView.setText(R.string.notify_off)
                it.visibility = View.INVISIBLE
                fab_on.visibility = View.VISIBLE
            }
        }

        updateLabels()
    }

    fun updateLabels() {
        Log.w("MainActivity", "Updated!")
        if (textView == null) return
        val sharedPrefs = getSharedPreferences("EventTime", Context.MODE_PRIVATE)
        if (!sharedPrefs.contains("worlds")) {
            textView2.setText(R.string.not_checked)
        } else {
            val str = sharedPrefs.getString("worlds", "")
            val year = sharedPrefs.getInt("year", -1)
            val month = sharedPrefs.getInt("month", -1) + 1
            val day = sharedPrefs.getInt("day", -1)
            val hour = sharedPrefs.getInt("hour", -1)
            var nextHour = hour + 1
            if (nextHour >= 24) nextHour -= 24
            textView2.text = resources.getString(R.string.check_time, year, month, day)
            if (str.isNullOrEmpty()) {
                textView3.text = resources.getString(R.string.checked_no_treasure, hour, nextHour)
            } else {
                textView3.text = resources.getString(R.string.checked_treasure, str, hour, nextHour)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
