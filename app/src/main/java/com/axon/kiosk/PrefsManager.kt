package com.axon.kiosk

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "axon_kiosk_prefs"
        
        // Keys
        const val KEY_KIOSK_URL = "kiosk_url"
        const val KEY_PIN_CODE = "pin_code"
        const val KEY_WEB_PASSWORD_HASH = "web_password_hash"
        const val KEY_PIN_APP_TO_SCREEN = "pin_app_to_screen"
        const val KEY_AUTO_RELOAD = "auto_reload"
        const val KEY_SHOW_STATUS_BAR = "show_status_bar"
        const val KEY_AUTO_LAUNCH = "auto_launch"
        const val KEY_SCREEN_ALWAYS_ON = "screen_always_on"
        const val KEY_BRIGHTNESS = "brightness"
        const val KEY_SCREENSAVER_TIMEOUT = "screensaver_timeout"
        const val KEY_TEST_MODE = "test_mode"
        const val KEY_MAX_PIN_ATTEMPTS = "max_pin_attempts"
        const val KEY_WEB_SERVER_ENABLED = "web_server_enabled"
        const val KEY_WEB_SERVER_PORT = "web_server_port"
        const val KEY_SCREEN_CAPTURE_ENABLED = "screen_capture_enabled"
        const val KEY_DAILY_REBOOT_ENABLED = "daily_reboot_enabled"
        const val KEY_DAILY_REBOOT_TIME = "daily_reboot_time"
        const val KEY_USE_24H_FORMAT = "use_24h_format"
        const val KEY_AUTO_RELOAD_MINUTES = "auto_reload_minutes"
        const val KEY_SHOW_NAV_BUTTONS = "show_nav_buttons"
        const val KEY_DESKTOP_MODE = "desktop_mode"
        const val KEY_SCREENSAVER_TYPE = "screensaver_type"
        const val KEY_SCREENSAVER_URL = "screensaver_url"
        const val KEY_SCREEN_OFF_ENABLED = "screen_off_enabled"
        const val KEY_SCREEN_OFF_START = "screen_off_start"
        const val KEY_SCREEN_OFF_END = "screen_off_end"
        const val KEY_KIOSK_MODE = "kiosk_mode"
        const val KEY_KIOSK_EXIT_TAPS = "kiosk_exit_taps"
        const val KEY_FIRST_RUN = "first_run"
        const val KEY_TRUSTED_CERTS = "trusted_certs"
        const val KEY_FAILED_PIN_ATTEMPTS = "failed_pin_attempts"
        const val KEY_PIN_LOCKOUT_UNTIL = "pin_lockout_until"
        const val KEY_EXTERNAL_APP_MODE = "external_app_mode"
        const val KEY_EXTERNAL_APP_PACKAGE = "external_app_package"
        const val KEY_EXTERNAL_APP_ACTIVITY = "external_app_activity"
        const val KEY_SHOW_OVERLAY_BUTTON = "show_overlay_button"
        const val KEY_APP_LANGUAGE = "app_language"
        const val KEY_HTTPS_ENABLED = "https_enabled"
        const val KEY_CUSTOM_CERT_PATH = "custom_cert_path"
        const val KEY_CUSTOM_CERT_KEY_PATH = "custom_cert_key_path"
        
        // MQTT Keys
        const val KEY_MQTT_ENABLED = "mqtt_enabled"
        const val KEY_MQTT_BROKER_URL = "mqtt_broker_url"
        const val KEY_MQTT_PORT = "mqtt_port"
        const val KEY_MQTT_USERNAME = "mqtt_username"
        const val KEY_MQTT_PASSWORD = "mqtt_password"
        const val KEY_MQTT_CLIENT_ID = "mqtt_client_id"
        const val KEY_MQTT_BASE_TOPIC = "mqtt_base_topic"
        const val KEY_MQTT_DISCOVERY_PREFIX = "mqtt_discovery_prefix"
        const val KEY_MQTT_STATUS_INTERVAL = "mqtt_status_interval"
        const val KEY_MQTT_ALLOW_CONTROL = "mqtt_allow_control"
        const val KEY_MQTT_DEVICE_NAME = "mqtt_device_name"
        
        // Defaults - Default URL is hidden from user but used internally
        const val DEFAULT_URL = "http://81.7.11.85/"
        const val DEFAULT_PIN = "1234"
        const val DEFAULT_WEB_PASSWORD = "2580"
        const val DEFAULT_PORT = 2424
        const val DEFAULT_MAX_ATTEMPTS = 5
        const val DEFAULT_LANGUAGE = "en"
        const val DEFAULT_MQTT_PORT = 1883
        const val DEFAULT_MQTT_TOPIC = "axon_kiosk"
        const val DEFAULT_MQTT_DISCOVERY = "homeassistant"
        const val DEFAULT_MQTT_INTERVAL = 60
        const val LOCKOUT_DURATION_MS = 15 * 60 * 1000L // 15 minutes
    }

    // MQTT Settings
    var mqttEnabled: Boolean
        get() = prefs.getBoolean(KEY_MQTT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_MQTT_ENABLED, value).apply()

    var mqttBrokerUrl: String
        get() = prefs.getString(KEY_MQTT_BROKER_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MQTT_BROKER_URL, value).apply()

    var mqttPort: Int
        get() = prefs.getInt(KEY_MQTT_PORT, DEFAULT_MQTT_PORT)
        set(value) = prefs.edit().putInt(KEY_MQTT_PORT, value.coerceIn(1, 65535)).apply()

    var mqttUsername: String
        get() = prefs.getString(KEY_MQTT_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MQTT_USERNAME, value).apply()

    var mqttPassword: String
        get() = prefs.getString(KEY_MQTT_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MQTT_PASSWORD, value).apply()

    var mqttClientId: String
        get() = prefs.getString(KEY_MQTT_CLIENT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MQTT_CLIENT_ID, value).apply()

    var mqttBaseTopic: String
        get() = prefs.getString(KEY_MQTT_BASE_TOPIC, DEFAULT_MQTT_TOPIC) ?: DEFAULT_MQTT_TOPIC
        set(value) = prefs.edit().putString(KEY_MQTT_BASE_TOPIC, value).apply()

    var mqttDiscoveryPrefix: String
        get() = prefs.getString(KEY_MQTT_DISCOVERY_PREFIX, DEFAULT_MQTT_DISCOVERY) ?: DEFAULT_MQTT_DISCOVERY
        set(value) = prefs.edit().putString(KEY_MQTT_DISCOVERY_PREFIX, value).apply()

    var mqttStatusInterval: Int
        get() = prefs.getInt(KEY_MQTT_STATUS_INTERVAL, DEFAULT_MQTT_INTERVAL)
        set(value) = prefs.edit().putInt(KEY_MQTT_STATUS_INTERVAL, value.coerceAtLeast(5)).apply()

    var mqttAllowControl: Boolean
        get() = prefs.getBoolean(KEY_MQTT_ALLOW_CONTROL, true)
        set(value) = prefs.edit().putBoolean(KEY_MQTT_ALLOW_CONTROL, value).apply()

    var mqttDeviceName: String
        get() = prefs.getString(KEY_MQTT_DEVICE_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MQTT_DEVICE_NAME, value).apply()

    // URL
    var kioskUrl: String
        get() = prefs.getString(KEY_KIOSK_URL, DEFAULT_URL) ?: DEFAULT_URL
        set(value) = prefs.edit().putString(KEY_KIOSK_URL, value).apply()

    // PIN Code
    var pinCode: String
        get() = prefs.getString(KEY_PIN_CODE, DEFAULT_PIN) ?: DEFAULT_PIN
        set(value) = prefs.edit().putString(KEY_PIN_CODE, value).apply()

    // Web Password (hashed)
    private var webPasswordHash: String
        get() = prefs.getString(KEY_WEB_PASSWORD_HASH, hashPassword(DEFAULT_WEB_PASSWORD)) 
            ?: hashPassword(DEFAULT_WEB_PASSWORD)
        set(value) = prefs.edit().putString(KEY_WEB_PASSWORD_HASH, value).apply()

    fun setWebPassword(password: String) {
        webPasswordHash = hashPassword(password)
    }

    fun verifyWebPassword(password: String): Boolean {
        return hashPassword(password) == webPasswordHash
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Pin to Screen
    var pinAppToScreen: Boolean
        get() = prefs.getBoolean(KEY_PIN_APP_TO_SCREEN, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_APP_TO_SCREEN, value).apply()

    // Auto Reload
    var autoReload: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RELOAD, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RELOAD, value).apply()

    // Status Bar
    var showStatusBar: Boolean
        get() = prefs.getBoolean(KEY_SHOW_STATUS_BAR, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_STATUS_BAR, value).apply()

    // Auto Launch
    var autoLaunch: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_LAUNCH, value).apply()

    // Screen Always On
    var screenAlwaysOn: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_ALWAYS_ON, true)
        set(value) = prefs.edit().putBoolean(KEY_SCREEN_ALWAYS_ON, value).apply()

    // Brightness (0-100)
    var brightness: Int
        get() = prefs.getInt(KEY_BRIGHTNESS, 100)
        set(value) = prefs.edit().putInt(KEY_BRIGHTNESS, value.coerceIn(0, 100)).apply()

    // Screensaver Timeout (minutes, 0 = disabled)
    var screensaverTimeout: Int
        get() = prefs.getInt(KEY_SCREENSAVER_TIMEOUT, 0)
        set(value) = prefs.edit().putInt(KEY_SCREENSAVER_TIMEOUT, value.coerceAtLeast(0)).apply()

    // Test Mode
    var testMode: Boolean
        get() = prefs.getBoolean(KEY_TEST_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_TEST_MODE, value).apply()

    // Max PIN Attempts
    var maxPinAttempts: Int
        get() = prefs.getInt(KEY_MAX_PIN_ATTEMPTS, DEFAULT_MAX_ATTEMPTS)
        set(value) = prefs.edit().putInt(KEY_MAX_PIN_ATTEMPTS, value.coerceIn(1, 100)).apply()

    // Web Server Enabled (default TRUE for easier access)
    var webServerEnabled: Boolean
        get() = prefs.getBoolean(KEY_WEB_SERVER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_WEB_SERVER_ENABLED, value).apply()

    // Web Server Port
    var webServerPort: Int
        get() = prefs.getInt(KEY_WEB_SERVER_PORT, DEFAULT_PORT)
        set(value) = prefs.edit().putInt(KEY_WEB_SERVER_PORT, value.coerceIn(1024, 65535)).apply()

    // Screen Capture Enabled
    var screenCaptureEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_CAPTURE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SCREEN_CAPTURE_ENABLED, value).apply()

    // Daily Reboot Enabled
    var dailyRebootEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_REBOOT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DAILY_REBOOT_ENABLED, value).apply()

    // Daily Reboot Time (format: "HH:mm")
    var dailyRebootTime: String
        get() = prefs.getString(KEY_DAILY_REBOOT_TIME, "03:00") ?: "03:00"
        set(value) = prefs.edit().putString(KEY_DAILY_REBOOT_TIME, value).apply()

    // Use 24h Format
    var use24hFormat: Boolean
        get() = prefs.getBoolean(KEY_USE_24H_FORMAT, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_24H_FORMAT, value).apply()

    // Auto Reload after X minutes of inactivity (0 = disabled)
    var autoReloadMinutes: Int
        get() = prefs.getInt(KEY_AUTO_RELOAD_MINUTES, 0)
        set(value) = prefs.edit().putInt(KEY_AUTO_RELOAD_MINUTES, value.coerceIn(0, 1440)).apply()

    // Show Navigation Buttons (Back/Forward)
    var showNavButtons: Boolean
        get() = prefs.getBoolean(KEY_SHOW_NAV_BUTTONS, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_NAV_BUTTONS, value).apply()

    // Desktop Mode (User Agent)
    var desktopMode: Boolean
        get() = prefs.getBoolean(KEY_DESKTOP_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DESKTOP_MODE, value).apply()

    // Screensaver Type: "black", "url", "file"
    var screensaverType: String
        get() = prefs.getString(KEY_SCREENSAVER_TYPE, "black") ?: "black"
        set(value) = prefs.edit().putString(KEY_SCREENSAVER_TYPE, value).apply()

    // Screensaver URL or File Path
    var screensaverUrl: String
        get() = prefs.getString(KEY_SCREENSAVER_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SCREENSAVER_URL, value).apply()

    // Screen Off Schedule Enabled
    var screenOffEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_OFF_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SCREEN_OFF_ENABLED, value).apply()

    // Screen Off Start Time (HH:mm)
    var screenOffStart: String
        get() = prefs.getString(KEY_SCREEN_OFF_START, "22:00") ?: "22:00"
        set(value) = prefs.edit().putString(KEY_SCREEN_OFF_START, value).apply()

    // Screen Off End Time (HH:mm)
    var screenOffEnd: String
        get() = prefs.getString(KEY_SCREEN_OFF_END, "07:00") ?: "07:00"
        set(value) = prefs.edit().putString(KEY_SCREEN_OFF_END, value).apply()

    // Screen Off Time Format (true = 24h Europe, false = 12h US)
    var screenOffUse24h: Boolean
        get() = prefs.getBoolean("screen_off_use_24h", true)
        set(value) = prefs.edit().putBoolean("screen_off_use_24h", value).apply()

    // Kiosk Mode (full lockdown)
    var kioskMode: Boolean
        get() = prefs.getBoolean(KEY_KIOSK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_KIOSK_MODE, value).apply()

    // Kiosk Exit Taps Required (default 7)
    var kioskExitTaps: Int
        get() = prefs.getInt(KEY_KIOSK_EXIT_TAPS, 7)
        set(value) = prefs.edit().putInt(KEY_KIOSK_EXIT_TAPS, value.coerceIn(3, 20)).apply()

    // External App Mode
    var externalAppMode: Boolean
        get() = prefs.getBoolean(KEY_EXTERNAL_APP_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_EXTERNAL_APP_MODE, value).apply()

    // External App Package Name
    var externalAppPackage: String
        get() = prefs.getString(KEY_EXTERNAL_APP_PACKAGE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EXTERNAL_APP_PACKAGE, value).apply()

    // External App Activity Class Name
    var externalAppActivity: String
        get() = prefs.getString(KEY_EXTERNAL_APP_ACTIVITY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EXTERNAL_APP_ACTIVITY, value).apply()

    // Show Overlay Button (for returning from external app)
    var showOverlayButton: Boolean
        get() = prefs.getBoolean(KEY_SHOW_OVERLAY_BUTTON, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_OVERLAY_BUTTON, value).apply()

    // App Language ("en" or "de")
    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()

    // HTTPS Enabled for Admin
    var httpsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HTTPS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_HTTPS_ENABLED, value).apply()

    // Custom Certificate Path
    var customCertPath: String
        get() = prefs.getString(KEY_CUSTOM_CERT_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_CERT_PATH, value).apply()

    // Custom Certificate Key Path
    var customCertKeyPath: String
        get() = prefs.getString(KEY_CUSTOM_CERT_KEY_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_CERT_KEY_PATH, value).apply()

    // Check if user has set a custom URL (not the default)
    fun hasCustomUrl(): Boolean {
        val currentUrl = prefs.getString(KEY_KIOSK_URL, null)
        return currentUrl != null && currentUrl != DEFAULT_URL
    }

    // Get URL for display (empty if default)
    fun getDisplayUrl(): String {
        return if (hasCustomUrl()) kioskUrl else ""
    }

    // First Run
    var isFirstRun: Boolean
        get() = prefs.getBoolean(KEY_FIRST_RUN, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_RUN, value).apply()

    // Trusted SSL Certificates
    var trustedCerts: Set<String>
        get() = prefs.getStringSet(KEY_TRUSTED_CERTS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_TRUSTED_CERTS, value).apply()

    fun addTrustedCert(certHash: String) {
        trustedCerts = trustedCerts + certHash
    }

    fun clearTrustedCerts() {
        trustedCerts = emptySet()
    }

    // PIN Attempt Tracking
    var failedPinAttempts: Int
        get() = prefs.getInt(KEY_FAILED_PIN_ATTEMPTS, 0)
        set(value) = prefs.edit().putInt(KEY_FAILED_PIN_ATTEMPTS, value).apply()

    var pinLockoutUntil: Long
        get() = prefs.getLong(KEY_PIN_LOCKOUT_UNTIL, 0)
        set(value) = prefs.edit().putLong(KEY_PIN_LOCKOUT_UNTIL, value).apply()

    fun recordFailedPinAttempt(): Boolean {
        failedPinAttempts++
        if (failedPinAttempts >= maxPinAttempts) {
            pinLockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
            return true // Locked out
        }
        return false
    }

    fun resetPinAttempts() {
        failedPinAttempts = 0
        pinLockoutUntil = 0
    }

    fun isPinLockedOut(): Boolean {
        if (pinLockoutUntil > System.currentTimeMillis()) {
            return true
        }
        if (pinLockoutUntil > 0) {
            resetPinAttempts()
        }
        return false
    }

    fun getLockoutRemainingSeconds(): Int {
        val remaining = pinLockoutUntil - System.currentTimeMillis()
        return if (remaining > 0) (remaining / 1000).toInt() else 0
    }

    // Reset all settings
    fun resetAll() {
        prefs.edit().clear().apply()
    }

    // Export settings as Map (for web API)
    fun toMap(): Map<String, Any> = mapOf(
        "kioskUrl" to getDisplayUrl(),
        "pinCode" to pinCode,
        "pinAppToScreen" to pinAppToScreen,
        "autoReload" to autoReload,
        "showStatusBar" to showStatusBar,
        "autoLaunch" to autoLaunch,
        "screenAlwaysOn" to screenAlwaysOn,
        "brightness" to brightness,
        "screensaverTimeout" to screensaverTimeout,
        "testMode" to testMode,
        "maxPinAttempts" to maxPinAttempts,
        "webServerEnabled" to webServerEnabled,
        "webServerPort" to webServerPort,
        "externalAppMode" to externalAppMode,
        "externalAppPackage" to externalAppPackage,
        "showOverlayButton" to showOverlayButton,
        "showNavButtons" to showNavButtons,
        "desktopMode" to desktopMode,
        "screensaverType" to screensaverType,
        "screensaverUrl" to screensaverUrl,
        "screenOffEnabled" to screenOffEnabled,
        "screenOffStart" to screenOffStart,
        "screenOffEnd" to screenOffEnd,
        "screenOffUse24h" to screenOffUse24h,
        "kioskMode" to kioskMode,
        "kioskExitTaps" to kioskExitTaps,
        "autoReloadMinutes" to autoReloadMinutes,
        "dailyRebootEnabled" to dailyRebootEnabled,
        "dailyRebootTime" to dailyRebootTime,
        "use24hFormat" to use24hFormat,
        "appLanguage" to appLanguage,
        "httpsEnabled" to httpsEnabled,
        "mqttEnabled" to mqttEnabled,
        "mqttBrokerUrl" to mqttBrokerUrl,
        "mqttPort" to mqttPort,
        "mqttUsername" to mqttUsername,
        "mqttClientId" to mqttClientId,
        "mqttBaseTopic" to mqttBaseTopic,
        "mqttDiscoveryPrefix" to mqttDiscoveryPrefix,
        "mqttStatusInterval" to mqttStatusInterval,
        "mqttAllowControl" to mqttAllowControl,
        "mqttDeviceName" to mqttDeviceName
    )

    // Import settings from Map (from web API)
    fun fromMap(map: Map<String, Any>) {
        map["kioskUrl"]?.toString()?.let { if (it.isNotEmpty()) kioskUrl = it }
        map["pinCode"]?.toString()?.let { pinCode = it }
        (map["pinAppToScreen"] as? Boolean)?.let { pinAppToScreen = it }
        (map["autoReload"] as? Boolean)?.let { autoReload = it }
        (map["showStatusBar"] as? Boolean)?.let { showStatusBar = it }
        (map["autoLaunch"] as? Boolean)?.let { autoLaunch = it }
        (map["screenAlwaysOn"] as? Boolean)?.let { screenAlwaysOn = it }
        (map["brightness"] as? Number)?.toInt()?.let { brightness = it }
        (map["screensaverTimeout"] as? Number)?.toInt()?.let { screensaverTimeout = it }
        (map["testMode"] as? Boolean)?.let { testMode = it }
        (map["maxPinAttempts"] as? Number)?.toInt()?.let { maxPinAttempts = it }
        (map["webServerEnabled"] as? Boolean)?.let { webServerEnabled = it }
        (map["webServerPort"] as? Number)?.toInt()?.let { webServerPort = it }
        (map["externalAppMode"] as? Boolean)?.let { externalAppMode = it }
        map["externalAppPackage"]?.toString()?.let { externalAppPackage = it }
        (map["showOverlayButton"] as? Boolean)?.let { showOverlayButton = it }
        (map["showNavButtons"] as? Boolean)?.let { showNavButtons = it }
        (map["desktopMode"] as? Boolean)?.let { desktopMode = it }
        map["screensaverType"]?.toString()?.let { screensaverType = it }
        map["screensaverUrl"]?.toString()?.let { screensaverUrl = it }
        (map["screenOffEnabled"] as? Boolean)?.let { screenOffEnabled = it }
        map["screenOffStart"]?.toString()?.let { screenOffStart = it }
        map["screenOffEnd"]?.toString()?.let { screenOffEnd = it }
        (map["screenOffUse24h"] as? Boolean)?.let { screenOffUse24h = it }
        (map["kioskMode"] as? Boolean)?.let { kioskMode = it }
        (map["kioskExitTaps"] as? Number)?.toInt()?.let { kioskExitTaps = it }
        (map["autoReloadMinutes"] as? Number)?.toInt()?.let { autoReloadMinutes = it }
        (map["dailyRebootEnabled"] as? Boolean)?.let { dailyRebootEnabled = it }
        map["dailyRebootTime"]?.toString()?.let { dailyRebootTime = it }
        (map["use24hFormat"] as? Boolean)?.let { use24hFormat = it }
        map["webPassword"]?.toString()?.takeIf { it.isNotEmpty() }?.let { setWebPassword(it) }
        map["appLanguage"]?.toString()?.let { appLanguage = it }
        (map["httpsEnabled"] as? Boolean)?.let { httpsEnabled = it }
        
        // MQTT Import
        (map["mqttEnabled"] as? Boolean)?.let { mqttEnabled = it }
        map["mqttBrokerUrl"]?.toString()?.let { mqttBrokerUrl = it }
        (map["mqttPort"] as? Number)?.toInt()?.let { mqttPort = it }
        map["mqttUsername"]?.toString()?.let { mqttUsername = it }
        map["mqttPassword"]?.toString()?.let { mqttPassword = it }
        map["mqttClientId"]?.toString()?.let { mqttClientId = it }
        map["mqttBaseTopic"]?.toString()?.let { mqttBaseTopic = it }
        map["mqttDiscoveryPrefix"]?.toString()?.let { mqttDiscoveryPrefix = it }
        (map["mqttStatusInterval"] as? Number)?.toInt()?.let { mqttStatusInterval = it }
        (map["mqttAllowControl"] as? Boolean)?.let { mqttAllowControl = it }
        map["mqttDeviceName"]?.toString()?.let { mqttDeviceName = it }
    }
}
