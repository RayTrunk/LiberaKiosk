package com.axon.kiosk

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import java.util.Locale

class AxonKioskApp : Application() {

    companion object {
        const val TAG = "AxonKiosk"
        lateinit var instance: AxonKioskApp
            private set
        
        fun setLocale(context: Context, languageCode: String): Context {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            
            return context.createConfigurationContext(config)
        }
    }

    lateinit var prefsManager: PrefsManager
        private set

    override fun attachBaseContext(base: Context) {
        // Initialize prefs early to get language setting
        val prefs = base.getSharedPreferences("axon_kiosk_prefs", Context.MODE_PRIVATE)
        val language = prefs.getString("app_language", "en") ?: "en"
        super.attachBaseContext(setLocale(base, language))
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefsManager = PrefsManager(this)
        
        Log.i(TAG, "AXON KIOSK starting...")
        Log.i(TAG, "Language: ${prefsManager.appLanguage}")
        Log.i(TAG, "WebServer enabled: ${prefsManager.webServerEnabled}")
        Log.i(TAG, "WebServer port: ${prefsManager.webServerPort}")
        
        // Start web server if enabled
        if (prefsManager.webServerEnabled) {
            Log.i(TAG, "Starting WebServer...")
            startWebServer()
        } else {
            Log.i(TAG, "WebServer disabled, not starting")
        }

        // Start MQTT if enabled
        if (prefsManager.mqttEnabled) {
            Log.i(TAG, "Starting MQTT...")
            startMqtt()
        } else {
            Log.i(TAG, "MQTT disabled, not starting")
        }
    }

    fun startWebServer() {
        try {
            val intent = Intent(this, WebServerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WebServer", e)
        }
    }

    fun stopWebServer() {
        try {
            stopService(Intent(this, WebServerService::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop WebServer", e)
        }
    }

    fun startMqtt() {
        try {
            val intent = Intent(this, MqttService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MQTT", e)
        }
    }

    fun stopMqtt() {
        try {
            stopService(Intent(this, MqttService::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop MQTT", e)
        }
    }

    fun startScreenCapture() {
        if (!ScreenCaptureService.hasPermission()) {
            Log.w(TAG, "Screen capture permission not granted")
            return
        }
        try {
            val intent = Intent(this, ScreenCaptureService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ScreenCapture", e)
        }
    }

    fun stopScreenCapture() {
        try {
            stopService(Intent(this, ScreenCaptureService::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop ScreenCapture", e)
        }
    }
}
