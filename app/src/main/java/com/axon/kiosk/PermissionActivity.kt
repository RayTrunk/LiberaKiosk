package com.axon.kiosk

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PermissionActivity"
        private const val REQUEST_MEDIA_PROJECTION = 1001
        private const val REQUEST_OVERLAY = 1002
        private const val REQUEST_WRITE_SETTINGS = 1003
        private const val REQUEST_DEVICE_ADMIN = 1004
        private const val REQUEST_BATTERY_OPTIMIZATION = 1005
        private const val REQUEST_RUNTIME_PERMISSIONS = 1006

        private val RUNTIME_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        fun needsSetup(context: Context): Boolean {
            val prefs = PrefsManager(context)
            if (prefs.isFirstRun) return true
            
            // Check critical permissions
            if (!Settings.canDrawOverlays(context)) return true
            if (!ScreenCaptureService.hasPermission()) return true
            
            return false
        }
    }

    private lateinit var prefsManager: PrefsManager
    private lateinit var kioskManager: KioskManager

    // UI Elements
    private lateinit var statusOverlay: ImageView
    private lateinit var statusWriteSettings: ImageView
    private lateinit var statusScreenCapture: ImageView
    private lateinit var statusBattery: ImageView
    private lateinit var statusDeviceAdmin: ImageView
    private lateinit var statusRuntime: ImageView

    private lateinit var btnOverlay: Button
    private lateinit var btnWriteSettings: Button
    private lateinit var btnScreenCapture: Button
    private lateinit var btnBattery: Button
    private lateinit var btnDeviceAdmin: Button
    private lateinit var btnRuntime: Button
    private lateinit var btnContinue: Button
    private lateinit var btnSkip: Button

    private lateinit var deviceAdminSection: LinearLayout
    private lateinit var adbHint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefsManager = AxonKioskApp.instance.prefsManager
        kioskManager = KioskManager(this)

        setContentView(R.layout.activity_permission)

        initViews()
        updateAllStatus()
    }

    private fun initViews() {
        // Status icons
        statusOverlay = findViewById(R.id.statusOverlay)
        statusWriteSettings = findViewById(R.id.statusWriteSettings)
        statusScreenCapture = findViewById(R.id.statusScreenCapture)
        statusBattery = findViewById(R.id.statusBattery)
        statusDeviceAdmin = findViewById(R.id.statusDeviceAdmin)
        statusRuntime = findViewById(R.id.statusRuntime)

        // Buttons
        btnOverlay = findViewById(R.id.btnOverlay)
        btnWriteSettings = findViewById(R.id.btnWriteSettings)
        btnScreenCapture = findViewById(R.id.btnScreenCapture)
        btnBattery = findViewById(R.id.btnBattery)
        btnDeviceAdmin = findViewById(R.id.btnDeviceAdmin)
        btnRuntime = findViewById(R.id.btnRuntime)
        btnContinue = findViewById(R.id.btnContinue)
        btnSkip = findViewById(R.id.btnSkip)

        deviceAdminSection = findViewById(R.id.deviceAdminSection)
        adbHint = findViewById(R.id.adbHint)

        // Click listeners
        btnOverlay.setOnClickListener { requestOverlayPermission() }
        btnWriteSettings.setOnClickListener { requestWriteSettingsPermission() }
        btnScreenCapture.setOnClickListener { requestScreenCapturePermission() }
        btnBattery.setOnClickListener { requestBatteryOptimization() }
        btnDeviceAdmin.setOnClickListener { requestDeviceAdmin() }
        btnRuntime.setOnClickListener { requestRuntimePermissions() }
        
        btnContinue.setOnClickListener { finishSetup() }
        btnSkip.setOnClickListener { finishSetup() }
    }

    private fun updateAllStatus() {
        // Overlay
        val hasOverlay = Settings.canDrawOverlays(this)
        updateStatus(statusOverlay, btnOverlay, hasOverlay)

        // Write Settings
        val hasWriteSettings = Settings.System.canWrite(this)
        updateStatus(statusWriteSettings, btnWriteSettings, hasWriteSettings)

        // Screen Capture
        val hasScreenCapture = ScreenCaptureService.hasPermission()
        updateStatus(statusScreenCapture, btnScreenCapture, hasScreenCapture)

        // Battery Optimization
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val hasBattery = pm.isIgnoringBatteryOptimizations(packageName)
        updateStatus(statusBattery, btnBattery, hasBattery)

        // Device Admin
        val hasDeviceAdmin = kioskManager.isDeviceOwner
        updateStatus(statusDeviceAdmin, btnDeviceAdmin, hasDeviceAdmin)
        
        // Show/hide device admin section based on status
        if (hasDeviceAdmin) {
            adbHint.visibility = View.GONE
        } else {
            adbHint.visibility = View.VISIBLE
        }

        // Runtime Permissions
        val hasRuntime = hasAllRuntimePermissions()
        updateStatus(statusRuntime, btnRuntime, hasRuntime)

        // Update continue button
        val criticalPermissionsGranted = hasOverlay && hasScreenCapture
        btnContinue.isEnabled = criticalPermissionsGranted
        btnContinue.alpha = if (criticalPermissionsGranted) 1f else 0.5f
    }

    private fun updateStatus(icon: ImageView, button: Button, granted: Boolean) {
        if (granted) {
            icon.setImageResource(R.drawable.ic_check)
            icon.setColorFilter(ContextCompat.getColor(this, R.color.success))
            button.isEnabled = false
            button.alpha = 0.5f
        } else {
            icon.setImageResource(R.drawable.ic_close)
            icon.setColorFilter(ContextCompat.getColor(this, R.color.danger))
            button.isEnabled = true
            button.alpha = 1f
        }
    }

    private fun hasAllRuntimePermissions(): Boolean {
        return RUNTIME_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Permission Requests
    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, REQUEST_OVERLAY)
    }

    private fun requestWriteSettingsPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, REQUEST_WRITE_SETTINGS)
    }

    private fun requestScreenCapturePermission() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
            projectionManager.createScreenCaptureIntent(),
            REQUEST_MEDIA_PROJECTION
        )
    }

    private fun requestBatteryOptimization() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivityForResult(intent, REQUEST_BATTERY_OPTIMIZATION)
    }

    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                ComponentName(this@PermissionActivity, DeviceAdminReceiver::class.java)
            )
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Libera Kiosk needs Device Admin for full kiosk lockdown."
            )
        }
        startActivityForResult(intent, REQUEST_DEVICE_ADMIN)
    }

    private fun requestRuntimePermissions() {
        val permissionsToRequest = RUNTIME_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_RUNTIME_PERMISSIONS)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_MEDIA_PROJECTION -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    ScreenCaptureService.setMediaProjectionResult(resultCode, data)
                    Log.i(TAG, "Screen capture permission granted")
                    
                    // Start capture service
                    startScreenCaptureService()
                }
            }
            REQUEST_OVERLAY, REQUEST_WRITE_SETTINGS, REQUEST_BATTERY_OPTIMIZATION, REQUEST_DEVICE_ADMIN -> {
                // Just update status
            }
        }
        
        updateAllStatus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RUNTIME_PERMISSIONS) {
            updateAllStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        updateAllStatus()
    }

    private fun startScreenCaptureService() {
        val intent = Intent(this, ScreenCaptureService::class.java).apply {
            putExtra("interval", 1000L)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun finishSetup() {
        prefsManager.isFirstRun = false
        
        // Start screen capture if permission granted
        if (ScreenCaptureService.hasPermission()) {
            startScreenCaptureService()
        }
        
        // Always start web server
        prefsManager.webServerEnabled = true
        AxonKioskApp.instance.startWebServer()
        
        // Start main activity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Don't allow back during setup
    }
}
