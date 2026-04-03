package com.axon.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.i(TAG, "Boot completed, checking auto-launch settings")
            
            val prefs = PrefsManager(context)
            
            // Start web server if enabled
            if (prefs.webServerEnabled) {
                AxonKioskApp.instance.startWebServer()
            }

            // Start MQTT if enabled
            if (prefs.mqttEnabled) {
                AxonKioskApp.instance.startMqtt()
            }
            
            // Auto-launch main activity if enabled
            if (prefs.autoLaunch) {
                Log.i(TAG, "Auto-launching kiosk")
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(launchIntent)
            }
        }
    }
}
