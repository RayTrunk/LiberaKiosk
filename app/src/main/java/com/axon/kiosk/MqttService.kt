package com.axon.kiosk

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.nio.charset.StandardCharsets
import java.util.*

class MqttService : Service() {

    companion object {
        private const val TAG = "MqttService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "libera_kiosk_mqtt"
    }

    private var client: Mqtt5AsyncClient? = null
    private lateinit var prefsManager: PrefsManager
    private val gson = Gson()
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var statusUpdateJob: Job? = null
    private var discoveryPublished = false

    override fun onCreate() {
        super.onCreate()
        prefsManager = AxonKioskApp.instance.prefsManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        connect()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceJob.cancel()
        disconnect()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Libera Kiosk MQTT Client",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Libera Kiosk")
            .setContentText("MQTT client connected to ${prefsManager.mqttBrokerUrl}")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun connect() {
        if (client != null) return
        
        val broker = prefsManager.mqttBrokerUrl
        if (broker.isEmpty()) {
            Log.w(TAG, "MQTT Broker URL is empty, cannot connect")
            return
        }

        val clientId = if (prefsManager.mqttClientId.isNotEmpty()) 
            prefsManager.mqttClientId 
        else 
            "axon_kiosk_${getAndroidId()}"

        Log.i(TAG, "Connecting to MQTT broker: $broker:$clientId")

        try {
            val clientBuilder = MqttClient.builder()
                .useMqttVersion5()
                .identifier(clientId)
                .serverHost(broker)
                .serverPort(prefsManager.mqttPort)
                .automaticReconnectWithDefaultConfig()

            if (prefsManager.mqttUsername.isNotEmpty()) {
                clientBuilder.simpleAuth()
                    .username(prefsManager.mqttUsername)
                    .password(prefsManager.mqttPassword.toByteArray(StandardCharsets.UTF_8))
                    .applySimpleAuth()
            }

            client = clientBuilder.buildAsync()

            client?.connect()?.whenComplete { ack, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "MQTT connection failed", throwable)
                } else {
                    Log.i(TAG, "MQTT connected successfully")
                    onConnected()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build MQTT client", e)
        }
    }

    private fun onConnected() {
        discoveryPublished = false
        publishDiscovery()
        subscribeToCommands()
        startStatusUpdates()
    }

    private fun disconnect() {
        statusUpdateJob?.cancel()
        client?.disconnect()
        client = null
        Log.i(TAG, "MQTT disconnected")
    }

    private fun subscribeToCommands() {
        if (!prefsManager.mqttAllowControl) return

        val commandTopic = "${prefsManager.mqttBaseTopic}/${getTopicId()}/command"
        client?.subscribeWith()
            ?.topicFilter(commandTopic)
            ?.qos(MqttQos.AT_LEAST_ONCE)
            ?.callback { publish -> handleCommand(publish) }
            ?.send()
        
        Log.i(TAG, "Subscribed to commands on: $commandTopic")
    }

    private fun handleCommand(publish: Mqtt5Publish) {
        val payload = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
        Log.i(TAG, "Received MQTT command: $payload")

        try {
            val command = gson.fromJson(payload, MqttCommand::class.java)
            when (command.action.lowercase()) {
                "reload" -> sendAppBroadcast("com.axon.kiosk.RELOAD_WEBVIEW")
                "reboot" -> sendAppBroadcast("com.axon.kiosk.REBOOT_DEVICE")
                "restart_app" -> sendAppBroadcast("com.axon.kiosk.RESTART_EXTERNAL_APP")
                "brightness" -> {
                    val brightness = command.value?.toIntOrNull() ?: 100
                    val intent = Intent("com.axon.kiosk.SET_BRIGHTNESS")
                    intent.setPackage(packageName)
                    intent.putExtra("brightness", brightness)
                    sendBroadcast(intent)
                }
                "url" -> {
                    val url = command.value ?: ""
                    if (url.isNotEmpty()) {
                        prefsManager.kioskUrl = url
                        sendAppBroadcast("com.axon.kiosk.SETTINGS_CHANGED")
                    }
                }
                "screenshot" -> {
                    // Notify that we want a screenshot, it will be published via status or a separate topic
                    Log.i(TAG, "Screenshot requested via MQTT")
                    // Implementation depends on how we want to return the image
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse MQTT command", e)
        }
    }

    private fun sendAppBroadcast(action: String) {
        val intent = Intent(action)
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    private fun startStatusUpdates() {
        statusUpdateJob?.cancel()
        statusUpdateJob = serviceScope.launch {
            while (isActive) {
                publishStatus()
                delay(prefsManager.mqttStatusInterval * 1000L)
            }
        }
    }

    private fun publishStatus() {
        if (client?.state?.isConnected == false) return

        val status = mapOf(
            "online" to true,
            "uptime" to SystemClock.elapsedRealtime() / 1000,
            "battery_level" to getBatteryLevel(),
            "battery_status" to getBatteryStatus(),
            "wifi_rssi" to getWifiRssi(),
            "ip_address" to getDeviceIP(),
            "brightness" to prefsManager.brightness,
            "kiosk_url" to prefsManager.kioskUrl,
            "external_app" to prefsManager.externalAppPackage,
            "kiosk_mode" to prefsManager.kioskMode,
            "last_update" to System.currentTimeMillis()
        )

        val topic = "${prefsManager.mqttBaseTopic}/${getTopicId()}/status"
        publishJson(topic, status, true)
    }

    private fun publishDiscovery() {
        if (discoveryPublished) return
        
        val prefix = prefsManager.mqttDiscoveryPrefix
        val topicId = getTopicId()
        val deviceName = if (prefsManager.mqttDeviceName.isNotEmpty()) 
            prefsManager.mqttDeviceName 
        else 
            "Libera Kiosk $topicId"

        val device = mapOf(
            "identifiers" to listOf(topicId),
            "name" to deviceName,
            "model" to Build.MODEL,
            "manufacturer" to Build.MANUFACTURER,
            "sw_version" to "1.0.0"
        )

        val baseTopic = "${prefsManager.mqttBaseTopic}/$topicId"

        // Battery Sensor
        publishDiscoveryEntity(prefix, "sensor", topicId, "battery", mapOf(
            "name" to "Battery",
            "stat_t" to "$baseTopic/status",
            "val_tpl" to "{{ value_json.battery_level }}",
            "unit_of_meas" to "%",
            "dev_cla" to "battery",
            "unique_id" to "${topicId}_battery",
            "device" to device
        ))

        // Uptime Sensor
        publishDiscoveryEntity(prefix, "sensor", topicId, "uptime", mapOf(
            "name" to "Uptime",
            "stat_t" to "$baseTopic/status",
            "val_tpl" to "{{ value_json.uptime }}",
            "unit_of_meas" to "s",
            "dev_cla" to "duration",
            "unique_id" to "${topicId}_uptime",
            "device" to device
        ))

        // Brightness Number
        publishDiscoveryEntity(prefix, "number", topicId, "brightness", mapOf(
            "name" to "Brightness",
            "stat_t" to "$baseTopic/status",
            "val_tpl" to "{{ value_json.brightness }}",
            "cmd_t" to "$baseTopic/command",
            "cmd_tpl" to "{\"action\": \"brightness\", \"value\": \"{{ value }}\"}",
            "min" to 0,
            "max" to 255,
            "unique_id" to "${topicId}_brightness",
            "device" to device
        ))

        // Reload Button
        publishDiscoveryEntity(prefix, "button", topicId, "reload", mapOf(
            "name" to "Reload WebView",
            "cmd_t" to "$baseTopic/command",
            "payload_press" to "{\"action\": \"reload\"}",
            "unique_id" to "${topicId}_reload",
            "device" to device
        ))

        // Reboot Button
        publishDiscoveryEntity(prefix, "button", topicId, "reboot", mapOf(
            "name" to "Reboot Device",
            "cmd_t" to "$baseTopic/command",
            "payload_press" to "{\"action\": \"reboot\"}",
            "unique_id" to "${topicId}_reboot",
            "device" to device
        ))

        discoveryPublished = true
        Log.i(TAG, "Home Assistant discovery published")
    }

    private fun publishDiscoveryEntity(prefix: String, component: String, topicId: String, objectId: String, config: Any) {
        val topic = "$prefix/$component/$topicId/$objectId/config"
        publishJson(topic, config, true)
    }

    private fun publishJson(topic: String, data: Any, retain: Boolean = false) {
        val payload = gson.toJson(data)
        client?.publishWith()
            ?.topic(topic)
            ?.payload(payload.toByteArray(StandardCharsets.UTF_8))
            ?.retain(retain)
            ?.qos(MqttQos.AT_MOST_ONCE)
            ?.send()
    }

    private fun getTopicId(): String {
        return if (prefsManager.mqttDeviceName.isNotEmpty())
            prefsManager.mqttDeviceName.lowercase().replace(" ", "_")
        else
            getAndroidId()
    }

    private fun getAndroidId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    private fun getBatteryLevel(): Int {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun getBatteryStatus(): String {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
            else -> "unknown"
        }
    }

    private fun getWifiRssi(): Int {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return 0
        val capabilities = cm.getNetworkCapabilities(network) ?: return 0
        return if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            // RSSI retrieval is more complex in newer Android, returning a placeholder for now
            -50 
        } else 0
    }

    private fun getDeviceIP(): String {
        // Implementation from WebServerService
        return "0.0.0.0" 
    }

    data class MqttCommand(val action: String, val value: String? = null)
}
