package com.axon.kiosk

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.*

class RebootScheduler {

    companion object {
        private const val TAG = "RebootScheduler"
        private const val REQUEST_CODE = 9999

        fun scheduleDaily(context: Context, timeString: String) {
            val prefsManager = PrefsManager(context)
            if (!prefsManager.dailyRebootEnabled) {
                cancel(context)
                return
            }

            try {
                val parts = timeString.split(":")
                val hour = parts[0].toInt()
                val minute = parts.getOrNull(1)?.toInt() ?: 0

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    
                    // If time already passed today, schedule for tomorrow
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, RebootReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Cancel existing alarm
                alarmManager.cancel(pendingIntent)

                // Schedule new alarm
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }

                Log.i(TAG, "Daily reboot scheduled for ${calendar.time}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule daily reboot", e)
            }
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, RebootReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.i(TAG, "Daily reboot cancelled")
        }
    }
}

class RebootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "RebootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.i(TAG, "Daily reboot triggered")
        
        val prefsManager = PrefsManager(context)
        if (!prefsManager.dailyRebootEnabled) {
            Log.i(TAG, "Daily reboot disabled, skipping")
            return
        }

        val kioskManager = KioskManager(context)
        val success = kioskManager.rebootDevice()
        
        if (!success) {
            Log.e(TAG, "Reboot failed, rescheduling for tomorrow")
            // Reschedule for tomorrow
            RebootScheduler.scheduleDaily(context, prefsManager.dailyRebootTime)
        }
    }
}
