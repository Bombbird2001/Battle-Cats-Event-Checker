package com.bombbird.battlecatseventchecker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.*

class RequestReceiver: BroadcastReceiver() {
    private lateinit var htmlRequest: HtmlRequest
    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent

    override fun onReceive(context: Context?, intent: Intent?) {
        htmlRequest = HtmlRequest()
        if (context != null) {
            htmlRequest.checkEvent(context, false)
        }
        Log.w("RequestReceiver", "Task ran")
        if (context != null) {
            scheduleRequest(context)
            Log.w("RequestReceiver", "Task scheduled")
        }
    }

    fun scheduleRequest(context: Context) {
        val intent = Intent(context, RequestReceiver::class.java)
        intent.action = "android.intent.action.NOTIFY"
        pendingIntent = PendingIntent.getBroadcast(context, 7776, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val min = Calendar.getInstance().get(Calendar.MINUTE)
        val next: Int
        next = when {
            min < 1 -> 2
            min < 16 -> 17
            min < 31 -> 32
            min < 46 -> 47
            else -> 62
        }
        val minutesRemaining = next - min
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, minutesRemaining)
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
    }

    fun cancelRequests() {
        if (::alarmManager.isInitialized) alarmManager.cancel(pendingIntent)
        if (::pendingIntent.isInitialized) pendingIntent.cancel()
    }
}