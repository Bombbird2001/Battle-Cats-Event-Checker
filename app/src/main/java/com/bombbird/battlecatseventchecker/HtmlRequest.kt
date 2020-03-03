package com.bombbird.battlecatseventchecker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import okhttp3.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class HtmlRequest {
    class MyCallback(private val context: Context, private val htmlRequest: HtmlRequest, private val forceShow: Boolean): Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
        }

        override fun onResponse(call: Call, response: Response) {
            if (!response.isSuccessful) {
                println("Unsuccessful response")
                response.close()
                return
            }
            val map = ArrayList<Int>(7)
            map.add(Calendar.MONDAY)
            map.add(Calendar.TUESDAY)
            map.add(Calendar.WEDNESDAY)
            map.add(Calendar.THURSDAY)
            map.add(Calendar.FRIDAY)
            map.add(Calendar.SATURDAY)
            map.add(Calendar.SUNDAY)

            val worlds = ArrayList<String>()
            var hour = 0

            val doc = Jsoup.parse(response.body?.string())
            response.close()
            val tableDivs = doc.select("div[class=cld_box01]")
            var index = -1
            for (box: Element in tableDivs) {
                index++
                if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) != map[index]) {
                    continue
                }
                //Correct day, continue checking
                val rows = box.selectFirst("table").select("tr")
                for (row: Element in rows) {
                    val hourP = row.selectFirst("td[class=hour]")?.selectFirst("p") ?: continue
                    val str = hourP.text()
                    hour = str.substring(0, str.length - 4).toInt()
                    if (hour == 24) hour = 0
                    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    if (hour != currentHour) {
                        //Not correct hour, continue
                        continue
                    }
                    for (td: Element in row.select("td")) {
                        if (td.hasAttr("class")) continue
                        //Loop thru <span>s in <p> in <td>
                        for (span: Element in td.selectFirst("p").select("span")) {
                            val txt = span.text()
                            if (!txt.contains("Treasure Festival")) continue
                            worlds.add(txt.split("(")[1].split(")")[0])
                        }
                    }
                    break
                }
                break
            }

            val worldString = worlds.joinToString(", ")
            println(worldString)

            val sharedPref = context.getSharedPreferences("EventTime", Context.MODE_PRIVATE) as SharedPreferences
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            if (!forceShow && sharedPref.contains("year") && sharedPref.contains("month") && sharedPref.contains("day") && sharedPref.contains("hour")) {
                if (sharedPref.getInt("year", -1) == year && sharedPref.getInt("month", -1) == month && sharedPref.getInt("day", -1) == day && sharedPref.getInt("hour", -1) == hour) return
            }

            var endHour = hour + 1
            if (endHour >= 24) endHour -= 24
            val title: String
            val toDisplay: String
            val id: Int
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (worlds.size == 0) {
                title = "No treasure for now..."
                toDisplay = "No ongoing treasure fests from $hour:00 to $endHour:00"
                id = 777
                notificationManager.cancel(776)
            } else {
                title = "Treasure fest is ongoing!"
                toDisplay = "Ongoing treasure fest in " + worlds.joinToString(", ") + " from $hour:00 to $endHour:00"
                id = 776
                notificationManager.cancel(777)
            }

            htmlRequest.createNotificationChannel(context)
            val builder = NotificationCompat.Builder(context, "Bombbird")
                .setSmallIcon(R.drawable.ic_announcement_black_24dp)
                .setContentTitle(title)
                .setContentText(toDisplay)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(toDisplay))
            with (NotificationManagerCompat.from(context)) {
                // notificationId is a unique int for each notification that you must define
                notify(id, builder.build())
            }

            //Shown notification, update the preferences to latest check date & time
            val editor = sharedPref.edit()
            editor.putInt("year", year)
            editor.putInt("month", month)
            editor.putInt("day", day)
            editor.putInt("hour", hour)
            editor.putString("worlds", worldString)
            editor.apply()

            if (context is MainActivity) {
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    context.updateLabels()
                }
            }
        }
    }

    fun createNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Bombbird"
            val descriptionText = "Notification channel for Bombbird apps"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("Bombbird", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val client = OkHttpClient()

    fun checkEvent(context: Context, forceShow: Boolean) {
        val request = Request.Builder()
            .url("https://ponos.s3.amazonaws.com/information/appli/battlecats/calendar/en/index.html")
            .build()
        client.newCall(request).enqueue(MyCallback(context, this, forceShow))
        Log.w("HtmlRequest", "Checked event")
    }
}
