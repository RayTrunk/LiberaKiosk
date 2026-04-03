package com.axon.kiosk

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefsManager: PrefsManager
    private lateinit var kioskManager: KioskManager

    private var isAuthenticated = false

    // Views
    private lateinit var mainContainer: ScrollView
    private lateinit var pinContainer: LinearLayout
    private lateinit var pinInput: TextInputEditText
    private lateinit var pinError: TextView
    private lateinit var pinSubmitBtn: Button

    // Settings views
    private lateinit var urlInput: TextInputEditText
    private lateinit var pinCodeInput: TextInputEditText
    private lateinit var webPasswordInput: TextInputEditText
    private lateinit var maxAttemptsInput: TextInputEditText
    private lateinit var brightnessSeekBar: SeekBar
    private lateinit var brightnessValue: TextView
    private lateinit var screensaverInput: TextInputEditText
    private lateinit var webPortInput: TextInputEditText
    private lateinit var mqttBrokerUrlInput: TextInputEditText
    private lateinit var mqttPortInput: TextInputEditText
    private lateinit var mqttUsernameInput: TextInputEditText
    private lateinit var mqttPasswordInput: TextInputEditText
    private lateinit var mqttDeviceNameInput: TextInputEditText

    private lateinit var switchPinToScreen: SwitchMaterial
    private lateinit var switchAutoReload: SwitchMaterial
    private lateinit var switchStatusBar: SwitchMaterial
    private lateinit var switchAutoLaunch: SwitchMaterial
    private lateinit var switchScreenOn: SwitchMaterial
    private lateinit var switchTestMode: SwitchMaterial
    private lateinit var switchWebServer: SwitchMaterial
    private lateinit var switchMqtt: SwitchMaterial
    private lateinit var switchMqttControl: SwitchMaterial

    // External App
    private lateinit var switchExternalApp: SwitchMaterial
    private lateinit var externalAppPackageInput: TextInputEditText
    private lateinit var switchShowOverlay: SwitchMaterial
    private lateinit var selectAppBtn: Button

    // Daily Reboot
    private lateinit var switchDailyReboot: SwitchMaterial
    private lateinit var dailyRebootTimeInput: TextInputEditText
    private lateinit var switch24hFormat: SwitchMaterial
    private lateinit var autoReloadMinutesInput: TextInputEditText
    private lateinit var switchShowNavButtons: SwitchMaterial
    private lateinit var switchDesktopMode: SwitchMaterial
    private lateinit var switchScreenOffEnabled: SwitchMaterial
    private lateinit var screenOffStartInput: TextInputEditText
    private lateinit var screenOffEndInput: TextInputEditText
    private lateinit var screensaverUrlInput: TextInputEditText
    private lateinit var switchScreenOffUse24h: SwitchMaterial
    private lateinit var switchKioskMode: SwitchMaterial
    private lateinit var kioskExitTapsInput: TextInputEditText
    private lateinit var deviceIpText: TextView
    private lateinit var adminUrlText: TextView

    private lateinit var serverStatusText: TextView
    private lateinit var serverUrlText: TextView
    private lateinit var deviceOwnerStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefsManager = AxonKioskApp.instance.prefsManager
        kioskManager = KioskManager(this)

        initViews()
        showPinDialog()
    }

    private fun initViews() {
        // Containers
        mainContainer = findViewById(R.id.mainContainer)
        pinContainer = findViewById(R.id.pinContainer)
        pinInput = findViewById(R.id.pinInput)
        pinError = findViewById(R.id.pinError)
        pinSubmitBtn = findViewById(R.id.pinSubmitBtn)

        // URL Settings
        urlInput = findViewById(R.id.urlInput)

        // Security
        pinCodeInput = findViewById(R.id.pinCodeInput)
        webPasswordInput = findViewById(R.id.webPasswordInput)
        maxAttemptsInput = findViewById(R.id.maxAttemptsInput)

        // Display
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar)
        brightnessValue = findViewById(R.id.brightnessValue)
        screensaverInput = findViewById(R.id.screensaverInput)

        // Web Server
        webPortInput = findViewById(R.id.webPortInput)
        serverStatusText = findViewById(R.id.serverStatusText)
        serverUrlText = findViewById(R.id.serverUrlText)

        // MQTT
        mqttBrokerUrlInput = findViewById(R.id.mqttBrokerUrlInput)
        mqttPortInput = findViewById(R.id.mqttPortInput)
        mqttUsernameInput = findViewById(R.id.mqttUsernameInput)
        mqttPasswordInput = findViewById(R.id.mqttPasswordInput)
        mqttDeviceNameInput = findViewById(R.id.mqttDeviceNameInput)
        switchMqtt = findViewById(R.id.switchMqtt)
        switchMqttControl = findViewById(R.id.switchMqttControl)

        // Switches
        switchPinToScreen = findViewById(R.id.switchPinToScreen)
        switchAutoReload = findViewById(R.id.switchAutoReload)
        switchStatusBar = findViewById(R.id.switchStatusBar)
        switchAutoLaunch = findViewById(R.id.switchAutoLaunch)
        switchScreenOn = findViewById(R.id.switchScreenOn)
        switchTestMode = findViewById(R.id.switchTestMode)
        switchWebServer = findViewById(R.id.switchWebServer)

        // External App
        switchExternalApp = findViewById(R.id.switchExternalApp)
        externalAppPackageInput = findViewById(R.id.externalAppPackageInput)
        switchShowOverlay = findViewById(R.id.switchShowOverlay)
        selectAppBtn = findViewById(R.id.selectAppBtn)

        // Daily Reboot & Auto-Reload
        switchDailyReboot = findViewById(R.id.switchDailyReboot)
        dailyRebootTimeInput = findViewById(R.id.dailyRebootTimeInput)
        switch24hFormat = findViewById(R.id.switch24hFormat)
        autoReloadMinutesInput = findViewById(R.id.autoReloadMinutesInput)
        switchShowNavButtons = findViewById(R.id.switchShowNavButtons)
        switchDesktopMode = findViewById(R.id.switchDesktopMode)
        switchScreenOffEnabled = findViewById(R.id.switchScreenOffEnabled)
        screenOffStartInput = findViewById(R.id.screenOffStartInput)
        screenOffEndInput = findViewById(R.id.screenOffEndInput)
        screensaverUrlInput = findViewById(R.id.screensaverUrlInput)
        switchScreenOffUse24h = findViewById(R.id.switchScreenOffUse24h)
        switchKioskMode = findViewById(R.id.switchKioskMode)
        kioskExitTapsInput = findViewById(R.id.kioskExitTapsInput)
        deviceIpText = findViewById(R.id.deviceIpText)
        adminUrlText = findViewById(R.id.adminUrlText)

        // Status
        deviceOwnerStatus = findViewById(R.id.deviceOwnerStatus)

        // Setup listeners
        setupListeners()
    }

    private fun showPinDialog() {
        mainContainer.visibility = View.GONE
        pinContainer.visibility = View.VISIBLE
        pinError.visibility = View.GONE

        // Check lockout
        if (prefsManager.isPinLockedOut()) {
            val remaining = prefsManager.getLockoutRemainingSeconds()
            pinError.text = "Too many attempts. Try again in ${remaining / 60}:${String.format("%02d", remaining % 60)}"
            pinError.visibility = View.VISIBLE
            pinInput.isEnabled = false
            pinSubmitBtn.isEnabled = false
            return
        }

        pinSubmitBtn.setOnClickListener {
            val enteredPin = pinInput.text.toString()
            
            if (enteredPin == prefsManager.pinCode) {
                prefsManager.resetPinAttempts()
                isAuthenticated = true
                showSettings()
            } else {
                val lockedOut = prefsManager.recordFailedPinAttempt()
                if (lockedOut) {
                    pinError.text = "Too many attempts. Locked for 15 minutes."
                    pinInput.isEnabled = false
                    pinSubmitBtn.isEnabled = false
                } else {
                    val remaining = prefsManager.maxPinAttempts - prefsManager.failedPinAttempts
                    pinError.text = "Incorrect PIN. $remaining attempts remaining."
                }
                pinError.visibility = View.VISIBLE
                pinInput.text?.clear()
            }
        }
    }

    private fun showSettings() {
        pinContainer.visibility = View.GONE
        mainContainer.visibility = View.VISIBLE
        loadSettings()
        updateServerStatus()
    }

    private fun loadSettings() {
        // URL
        urlInput.setText(prefsManager.kioskUrl)

        // Security
        pinCodeInput.setText(prefsManager.pinCode)
        maxAttemptsInput.setText(prefsManager.maxPinAttempts.toString())

        // Display
        brightnessSeekBar.progress = prefsManager.brightness
        brightnessValue.text = "${prefsManager.brightness}%"
        screensaverInput.setText(prefsManager.screensaverTimeout.toString())

        // Web Server
        webPortInput.setText(prefsManager.webServerPort.toString())

        // MQTT
        switchMqtt.isChecked = prefsManager.mqttEnabled
        mqttBrokerUrlInput.setText(prefsManager.mqttBrokerUrl)
        mqttPortInput.setText(prefsManager.mqttPort.toString())
        mqttUsernameInput.setText(prefsManager.mqttUsername)
        mqttPasswordInput.setText(prefsManager.mqttPassword)
        mqttDeviceNameInput.setText(prefsManager.mqttDeviceName)
        switchMqttControl.isChecked = prefsManager.mqttAllowControl

        // Switches
        switchPinToScreen.isChecked = prefsManager.pinAppToScreen
        switchAutoReload.isChecked = prefsManager.autoReload
        switchStatusBar.isChecked = prefsManager.showStatusBar
        switchAutoLaunch.isChecked = prefsManager.autoLaunch
        switchScreenOn.isChecked = prefsManager.screenAlwaysOn
        switchTestMode.isChecked = prefsManager.testMode
        switchWebServer.isChecked = prefsManager.webServerEnabled

        // External App
        switchExternalApp.isChecked = prefsManager.externalAppMode
        externalAppPackageInput.setText(prefsManager.externalAppPackage)
        switchShowOverlay.isChecked = prefsManager.showOverlayButton

        // Daily Reboot & Auto-Reload
        switchDailyReboot.isChecked = prefsManager.dailyRebootEnabled
        dailyRebootTimeInput.setText(prefsManager.dailyRebootTime)
        switch24hFormat.isChecked = prefsManager.use24hFormat
        autoReloadMinutesInput.setText(prefsManager.autoReloadMinutes.toString())
        switchShowNavButtons.isChecked = prefsManager.showNavButtons
        switchDesktopMode.isChecked = prefsManager.desktopMode
        switchScreenOffEnabled.isChecked = prefsManager.screenOffEnabled
        screenOffStartInput.setText(prefsManager.screenOffStart)
        screenOffEndInput.setText(prefsManager.screenOffEnd)
        switchScreenOffUse24h.isChecked = prefsManager.screenOffUse24h
        screensaverUrlInput.setText(prefsManager.screensaverUrl)
        switchKioskMode.isChecked = prefsManager.kioskMode
        kioskExitTapsInput.setText(prefsManager.kioskExitTaps.toString())
        
        // Device Info
        val ip = getDeviceIP()
        deviceIpText.text = ip
        adminUrlText.text = "http://$ip:${prefsManager.webServerPort}"

        // Device Owner Status
        deviceOwnerStatus.text = if (kioskManager.isDeviceOwner) {
            "✓ Active - Full kiosk mode available"
        } else {
            "✗ Not set - Limited functionality"
        }
    }

    private fun setupListeners() {
        // Brightness seekbar
        brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                brightnessValue.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Web server switch
        switchWebServer.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AxonKioskApp.instance.startWebServer()
            } else {
                AxonKioskApp.instance.stopWebServer()
            }
            updateServerStatus()
        }

        // Select App button
        selectAppBtn.setOnClickListener {
            showAppPicker()
        }

        // Copy URL button
        findViewById<Button>(R.id.copyUrlBtn).setOnClickListener {
            val url = serverUrlText.text.toString()
            if (url.isNotEmpty() && url != "Server not running") {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Server URL", url))
                Toast.makeText(this, "URL copied!", Toast.LENGTH_SHORT).show()
            }
        }

        // Save button
        findViewById<Button>(R.id.saveBtn).setOnClickListener {
            saveSettings()
        }

        // Reset button
        findViewById<Button>(R.id.resetBtn).setOnClickListener {
            showResetConfirmation()
        }

        // Clear certs button
        findViewById<Button>(R.id.clearCertsBtn).setOnClickListener {
            prefsManager.clearTrustedCerts()
            Toast.makeText(this, "Trusted certificates cleared", Toast.LENGTH_SHORT).show()
        }
        
        // WiFi Settings button
        findViewById<Button>(R.id.wifiSettingsBtn).setOnClickListener {
            openWifiSettings()
        }

        // Exit kiosk button
        findViewById<Button>(R.id.exitKioskBtn).setOnClickListener {
            showExitConfirmation()
        }

        // Remove device owner button
        findViewById<Button>(R.id.removeDeviceOwnerBtn).apply {
            visibility = if (kioskManager.isDeviceOwner) View.VISIBLE else View.GONE
            setOnClickListener {
                showRemoveDeviceOwnerConfirmation()
            }
        }
    }

    private fun saveSettings() {
        // Validate URL
        val url = urlInput.text.toString().trim()
        if (url.isEmpty()) {
            urlInput.error = "URL is required"
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            urlInput.error = "URL must start with http:// or https://"
            return
        }

        // Validate PIN
        val pin = pinCodeInput.text.toString()
        if (pin.length < 4 || pin.length > 6) {
            pinCodeInput.error = "PIN must be 4-6 digits"
            return
        }

        // Save all settings
        prefsManager.kioskUrl = url
        prefsManager.pinCode = pin
        prefsManager.maxPinAttempts = maxAttemptsInput.text.toString().toIntOrNull() ?: 5
        prefsManager.brightness = brightnessSeekBar.progress
        prefsManager.screensaverTimeout = screensaverInput.text.toString().toIntOrNull() ?: 0
        prefsManager.webServerPort = webPortInput.text.toString().toIntOrNull() ?: 2424

        prefsManager.pinAppToScreen = switchPinToScreen.isChecked
        prefsManager.autoReload = switchAutoReload.isChecked
        prefsManager.showStatusBar = switchStatusBar.isChecked
        prefsManager.autoLaunch = switchAutoLaunch.isChecked
        prefsManager.screenAlwaysOn = switchScreenOn.isChecked
        prefsManager.testMode = switchTestMode.isChecked
        prefsManager.webServerEnabled = switchWebServer.isChecked

        // MQTT
        prefsManager.mqttEnabled = switchMqtt.isChecked
        prefsManager.mqttBrokerUrl = mqttBrokerUrlInput.text.toString().trim()
        prefsManager.mqttPort = mqttPortInput.text.toString().toIntOrNull() ?: 1883
        prefsManager.mqttUsername = mqttUsernameInput.text.toString().trim()
        prefsManager.mqttPassword = mqttPasswordInput.text.toString().trim()
        prefsManager.mqttDeviceName = mqttDeviceNameInput.text.toString().trim()
        prefsManager.mqttAllowControl = switchMqttControl.isChecked

        // External App
        prefsManager.externalAppMode = switchExternalApp.isChecked
        prefsManager.externalAppPackage = externalAppPackageInput.text.toString().trim()
        prefsManager.showOverlayButton = switchShowOverlay.isChecked

        // Daily Reboot & Auto-Reload
        prefsManager.dailyRebootEnabled = switchDailyReboot.isChecked
        prefsManager.dailyRebootTime = dailyRebootTimeInput.text.toString().trim().ifEmpty { "03:00" }
        prefsManager.use24hFormat = switch24hFormat.isChecked
        prefsManager.autoReloadMinutes = autoReloadMinutesInput.text.toString().toIntOrNull() ?: 0
        prefsManager.showNavButtons = switchShowNavButtons.isChecked
        prefsManager.desktopMode = switchDesktopMode.isChecked
        prefsManager.screenOffEnabled = switchScreenOffEnabled.isChecked
        prefsManager.screenOffStart = screenOffStartInput.text.toString().trim().ifEmpty { "22:00" }
        prefsManager.screenOffEnd = screenOffEndInput.text.toString().trim().ifEmpty { "07:00" }
        prefsManager.screenOffUse24h = switchScreenOffUse24h.isChecked
        prefsManager.screensaverUrl = screensaverUrlInput.text.toString().trim()
        if (prefsManager.screensaverUrl.isNotEmpty()) {
            prefsManager.screensaverType = "url"
        }
        prefsManager.kioskMode = switchKioskMode.isChecked
        prefsManager.kioskExitTaps = kioskExitTapsInput.text.toString().toIntOrNull() ?: 7

        // Schedule or cancel daily reboot
        if (prefsManager.dailyRebootEnabled) {
            RebootScheduler.scheduleDaily(this, prefsManager.dailyRebootTime)
        } else {
            RebootScheduler.cancel(this)
        }

        // Update web password if provided
        val webPass = webPasswordInput.text.toString()
        if (webPass.isNotEmpty()) {
            prefsManager.setWebPassword(webPass)
            webPasswordInput.text?.clear()
        }

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateServerStatus() {
        if (prefsManager.webServerEnabled) {
            serverStatusText.text = "● Running"
            serverStatusText.setTextColor(getColor(R.color.success))
            serverUrlText.text = "http://${getDeviceIP()}:${prefsManager.webServerPort}"
        } else {
            serverStatusText.text = "○ Stopped"
            serverStatusText.setTextColor(getColor(R.color.text_secondary))
            serverUrlText.text = "Server not running"
        }
    }

    private fun getDeviceIP(): String {
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                (ipAddress shr 8) and 0xff,
                (ipAddress shr 16) and 0xff,
                (ipAddress shr 24) and 0xff
            )
        } catch (e: Exception) {
            "0.0.0.0"
        }
    }

    private fun showResetConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Reset Settings")
            .setMessage("This will reset all settings to defaults. Continue?")
            .setPositiveButton("Reset") { _, _ ->
                prefsManager.resetAll()
                Toast.makeText(this, "Settings reset", Toast.LENGTH_SHORT).show()
                loadSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit Kiosk Mode")
            .setMessage("This will exit the kiosk and return to normal device usage.")
            .setPositiveButton("Exit") { _, _ ->
                if (kioskManager.isDeviceOwner) {
                    kioskManager.stopLockTask(this)
                }
                finishAffinity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun openWifiSettings() {
        try {
            // Try to open WiFi settings directly
            val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general wireless settings
            try {
                val intent = Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(this, "Could not open WiFi settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRemoveDeviceOwnerConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Remove Device Owner")
            .setMessage(
                "This will remove Device Owner privileges. The app will lose kiosk lockdown capabilities.\n\n" +
                "You will need to factory reset to re-enable Device Owner.\n\n" +
                "Continue?"
            )
            .setPositiveButton("Remove") { _, _ ->
                kioskManager.clearDeviceOwner()
                Toast.makeText(this, "Device Owner removed", Toast.LENGTH_LONG).show()
                loadSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAppPicker() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        
        val apps = pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName } // Exclude our own app
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        val appNames = apps.map { 
            val label = it.loadLabel(pm).toString()
            val pkg = it.activityInfo.packageName
            "$label\n($pkg)"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select App")
            .setItems(appNames) { _, which ->
                val selected = apps[which]
                val pkg = selected.activityInfo.packageName
                val activity = selected.activityInfo.name
                
                externalAppPackageInput.setText(pkg)
                prefsManager.externalAppActivity = activity
                
                Toast.makeText(this, "Selected: ${selected.loadLabel(pm)}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (isAuthenticated) {
            super.onBackPressed()
        }
    }
}
