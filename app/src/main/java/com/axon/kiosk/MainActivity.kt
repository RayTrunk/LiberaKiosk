package com.axon.kiosk

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.net.http.SslCertificate
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.*
import android.webkit.*
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val TAP_COUNT_THRESHOLD = 5
        private const val TAP_TIMEOUT_MS = 3000L
        private const val TAP_AREA_SIZE = 150
        private const val OVERLAY_PERMISSION_REQUEST = 1001
        private const val SWIPE_THRESHOLD = 150
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    private lateinit var webViewContainer: FrameLayout
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var navButtonsContainer: LinearLayout
    private lateinit var btnNavBack: ImageButton
    private lateinit var btnNavForward: ImageButton
    private lateinit var prefsManager: PrefsManager
    private lateinit var kioskManager: KioskManager
    private lateinit var gestureDetector: GestureDetector

    private var tapCount = 0
    private var lastTapTime = 0L
    private var tapAreaSize = 0
    
    // Kiosk Mode Exit
    private var kioskExitTapCount = 0
    private var kioskExitLastTapTime = 0L

    private val handler = Handler(Looper.getMainLooper())
    private var screensaverRunnable: Runnable? = null
    private var isScreensaverActive = false
    private var autoReloadRunnable: Runnable? = null
    private var lastActivityTime = System.currentTimeMillis()

    // External App Mode
    private var overlayButton: ImageButton? = null
    private var windowManager: WindowManager? = null
    private var isExternalAppRunning = false
    private var externalAppTapCount = 0
    private var externalAppLastTapTime = 0L

    // Broadcast Receiver für Settings-Änderungen
    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.axon.kiosk.SETTINGS_CHANGED" -> {
                    // Re-read prefs from storage to get latest values
                    val newPrefs = PrefsManager(this@MainActivity)
                    val previousExternalAppMode = isExternalAppRunning
                    val newExternalAppMode = newPrefs.externalAppMode
                    
                    // Store current URL before applying new settings
                    val previousUrl = prefsManager.kioskUrl
                    val newUrl = newPrefs.kioskUrl
                    
                    applySettings()
                    
                    // Check if switching from External App to URL mode
                    if (previousExternalAppMode && !newExternalAppMode) {
                        Log.i(TAG, "Switching from External App to URL mode")
                        stopExternalApp()
                        return
                    }
                    
                    // Check if switching from URL to External App mode
                    if (!previousExternalAppMode && newExternalAppMode && newPrefs.externalAppPackage.isNotEmpty()) {
                        Log.i(TAG, "Switching from URL to External App mode")
                        prefsManager.externalAppMode = true
                        prefsManager.externalAppPackage = newPrefs.externalAppPackage
                        startExternalAppMode()
                        return
                    }
                    
                    // Check if URL changed and reload (only in URL mode)
                    if (previousUrl != newUrl && !prefsManager.externalAppMode && !isExternalAppRunning) {
                        Log.i(TAG, "URL changed from $previousUrl to $newUrl, loading new URL")
                        handler.postDelayed({
                            webView.loadUrl(newUrl)
                        }, 100)
                    }
                    
                    // Update desktop mode if changed
                    if (newPrefs.desktopMode != prefsManager.desktopMode) {
                        setupWebView()
                    }
                    
                    // MQTT Service Control
                    if (newPrefs.mqttEnabled != prefsManager.mqttEnabled) {
                        if (newPrefs.mqttEnabled) {
                            Log.i(TAG, "Starting MQTT Service from Settings change")
                            AxonKioskApp.instance.startMqtt()
                        } else {
                            Log.i(TAG, "Stopping MQTT Service from Settings change")
                            AxonKioskApp.instance.stopMqtt()
                        }
                    } else if (newPrefs.mqttEnabled) {
                        // Restart MQTT service if configuration changed (host, port, etc)
                        if (newPrefs.mqttBrokerUrl != prefsManager.mqttBrokerUrl ||
                            newPrefs.mqttPort != prefsManager.mqttPort ||
                            newPrefs.mqttUsername != prefsManager.mqttUsername ||
                            newPrefs.mqttPassword != prefsManager.mqttPassword ||
                            newPrefs.mqttClientId != prefsManager.mqttClientId) {
                            Log.i(TAG, "Restarting MQTT Service due to config change")
                            AxonKioskApp.instance.stopMqtt()
                            handler.postDelayed({ AxonKioskApp.instance.startMqtt() }, 1000)
                        }
                    }
                    
                    updateNavButtonsVisibility()
                }
                "com.axon.kiosk.RELOAD_WEBVIEW" -> {
                    Log.i(TAG, "Reload broadcast received")
                    // Force reload the current URL from prefs
                    val currentUrl = prefsManager.kioskUrl
                    Log.i(TAG, "Reloading URL: $currentUrl")
                    webView.loadUrl(currentUrl)
                }
                "com.axon.kiosk.REBOOT_DEVICE" -> {
                    Log.i(TAG, "Reboot broadcast received")
                    kioskManager.rebootDevice()
                }
                "com.axon.kiosk.SET_BRIGHTNESS" -> {
                    val brightness = intent.getIntExtra("brightness", -1)
                    if (brightness >= 0) {
                        Log.i(TAG, "Setting brightness to $brightness")
                        kioskManager.setBrightness(this@MainActivity, brightness)
                    }
                }
                "com.axon.kiosk.START_EXTERNAL_APP" -> {
                    val packageName = intent.getStringExtra("packageName") ?: ""
                    if (packageName.isNotEmpty()) {
                        Log.i(TAG, "Starting external app: $packageName")
                        prefsManager.externalAppPackage = packageName
                        prefsManager.externalAppMode = true
                        startExternalAppMode()
                    }
                }
                "com.axon.kiosk.STOP_EXTERNAL_APP" -> {
                    Log.i(TAG, "Stopping external app")
                    stopExternalApp()
                }
                "com.axon.kiosk.RESTART_EXTERNAL_APP" -> {
                    Log.i(TAG, "Restarting external app")
                    restartExternalApp()
                }
                "com.axon.kiosk.INSTALL_APK" -> {
                    val apkPath = intent.getStringExtra("apkPath") ?: ""
                    if (apkPath.isNotEmpty()) {
                        Log.i(TAG, "Installing APK: $apkPath")
                        installApk(apkPath)
                    }
                }
                "com.axon.kiosk.UNINSTALL_APP" -> {
                    val packageName = intent.getStringExtra("packageName") ?: ""
                    if (packageName.isNotEmpty()) {
                        Log.i(TAG, "Uninstalling app: $packageName")
                        uninstallApp(packageName)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefsManager = AxonKioskApp.instance.prefsManager
        kioskManager = KioskManager(this)
        
        // Redirect to PermissionActivity on first run
        if (prefsManager.isFirstRun) {
            Log.i(TAG, "First run detected, redirecting to PermissionActivity")
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
            return
        }
        
        setContentView(R.layout.activity_main)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        tapAreaSize = (TAP_AREA_SIZE * resources.displayMetrics.density).toInt()

        initViews()
        setupGestureDetector()
        setupWebView()
        applySettings()
        
        // Apply Kiosk Mode if enabled
        if (prefsManager.kioskMode && kioskManager.isDeviceOwner) {
            kioskManager.enableKioskMode(this)
        }

        if (kioskManager.isDeviceOwner) {
            kioskManager.setupKioskPolicies()
        }

        // Register broadcast receiver
        val filter = IntentFilter().apply {
            addAction("com.axon.kiosk.SETTINGS_CHANGED")
            addAction("com.axon.kiosk.RELOAD_WEBVIEW")
            addAction("com.axon.kiosk.REBOOT_DEVICE")
            addAction("com.axon.kiosk.SET_BRIGHTNESS")
            addAction("com.axon.kiosk.START_EXTERNAL_APP")
            addAction("com.axon.kiosk.STOP_EXTERNAL_APP")
            addAction("com.axon.kiosk.RESTART_EXTERNAL_APP")
            addAction("com.axon.kiosk.INSTALL_APK")
            addAction("com.axon.kiosk.UNINSTALL_APP")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(settingsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(settingsReceiver, filter)
        }

        // Check mode and start
        if (prefsManager.externalAppMode && prefsManager.externalAppPackage.isNotEmpty()) {
            startExternalAppMode()
        } else {
            loadKioskUrl()
        }

        if (prefsManager.isFirstRun) {
            prefsManager.isFirstRun = false
            showWelcomeDialog()
        }

        // Start WebServer if enabled
        if (prefsManager.webServerEnabled) {
            AxonKioskApp.instance.startWebServer()
        }
    }

    private fun initViews() {
        webViewContainer = findViewById(R.id.webViewContainer)
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        navButtonsContainer = findViewById(R.id.navButtonsContainer)
        btnNavBack = findViewById(R.id.btnNavBack)
        btnNavForward = findViewById(R.id.btnNavForward)
        
        // Navigation button listeners
        btnNavBack.setOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }
        btnNavForward.setOnClickListener {
            if (webView.canGoForward()) {
                webView.goForward()
            }
        }
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                
                // Swipe from right to left (open settings)
                if (Math.abs(diffX) > Math.abs(diffY) && 
                    Math.abs(diffX) > SWIPE_THRESHOLD && 
                    Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD &&
                    diffX < 0 && 
                    e1.x > resources.displayMetrics.widthPixels - 100) {
                    
                    Log.i(TAG, "Swipe detected from right edge")
                    handleSettingsAccess()
                    return true
                }
                return false
            }
            
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                handleKioskExitTap()
                return false
            }
        })
        
        // Add touch listener to root view
        webViewContainer.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            resetScreensaverTimer()
            false
        }
    }
    
    private fun handleKioskExitTap() {
        if (!prefsManager.kioskMode) return
        
        val now = System.currentTimeMillis()
        if (now - kioskExitLastTapTime > TAP_TIMEOUT_MS) {
            kioskExitTapCount = 0
        }
        
        kioskExitTapCount++
        kioskExitLastTapTime = now
        
        val requiredTaps = prefsManager.kioskExitTaps
        
        if (kioskExitTapCount >= requiredTaps) {
            kioskExitTapCount = 0
            showPinDialogForKioskExit()
        }
    }
    
    private fun handleSettingsAccess() {
        if (prefsManager.kioskMode) {
            showPinDialogForSettings()
        } else {
            openSettings()
        }
    }
    
    private fun showPinDialogForSettings() {
        val pinInput = EditText(this).apply {
            hint = "Enter PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        
        AlertDialog.Builder(this)
            .setTitle("🔐 Enter PIN")
            .setView(pinInput)
            .setPositiveButton("OK") { _, _ ->
                if (pinInput.text.toString() == prefsManager.pinCode) {
                    openSettings()
                } else {
                    showToast("Invalid PIN")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showPinDialogForKioskExit() {
        val pinInput = EditText(this).apply {
            hint = "Enter PIN to exit Kiosk Mode"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        
        AlertDialog.Builder(this)
            .setTitle("🔓 Exit Kiosk Mode")
            .setMessage("Enter PIN to disable Kiosk Mode")
            .setView(pinInput)
            .setPositiveButton("Exit") { _, _ ->
                if (pinInput.text.toString() == prefsManager.pinCode) {
                    prefsManager.kioskMode = false
                    kioskManager.disableKioskMode(this)
                    showToast("Kiosk Mode disabled")
                } else {
                    showToast("Invalid PIN")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    private fun updateNavButtonsVisibility() {
        navButtonsContainer.visibility = if (prefsManager.showNavButtons && !prefsManager.externalAppMode) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            mediaPlaybackRequiresUserGesture = false
            setGeolocationEnabled(true)
            
            // Desktop/Mobile mode
            if (prefsManager.desktopMode) {
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true && prefsManager.autoReload) {
                    Log.w(TAG, "Page load error, scheduling reload")
                    handler.postDelayed({ loadKioskUrl() }, 5000)
                }
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                val cert = error?.certificate
                val certHash = cert?.let { getCertificateHash(it) } ?: ""

                if (certHash.isNotEmpty() && prefsManager.trustedCerts.contains(certHash)) {
                    handler?.proceed()
                    return
                }

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("SSL Certificate Warning")
                    .setMessage("The server's SSL certificate is not trusted. Continue?\n\nHost: ${error?.url}")
                    .setPositiveButton("Trust Always") { _, _ ->
                        if (certHash.isNotEmpty()) {
                            prefsManager.addTrustedCert(certHash)
                        }
                        handler?.proceed()
                    }
                    .setNeutralButton("Trust Once") { _, _ ->
                        handler?.proceed()
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        handler?.cancel()
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                callback?.invoke(origin, true, true)
            }
        }

        webView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                handleTap(event)
                resetScreensaverTimer()
            }
            false
        }
    }

    private fun getCertificateHash(cert: SslCertificate): String {
        return try {
            val x509 = cert.x509Certificate
            if (x509 != null) {
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(x509.encoded)
                digest.joinToString("") { "%02x".format(it) }
            } else ""
        } catch (e: Exception) { "" }
    }

    private val SslCertificate.x509Certificate: X509Certificate?
        get() = try {
            val bundle = SslCertificate.saveState(this)
            val bytes = bundle.getByteArray("x509-certificate")
            if (bytes != null) {
                val certFactory = CertificateFactory.getInstance("X.509")
                certFactory.generateCertificate(bytes.inputStream()) as X509Certificate
            } else null
        } catch (e: Exception) { null }

    private fun handleTap(event: MotionEvent) {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val isInCorner = event.x > screenWidth - tapAreaSize && event.y > screenHeight - tapAreaSize

        if (isInCorner) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTapTime > TAP_TIMEOUT_MS) {
                tapCount = 0
            }
            tapCount++
            lastTapTime = currentTime

            if (tapCount >= TAP_COUNT_THRESHOLD) {
                tapCount = 0
                openSettings()
            }
        }
    }

    // ==================== EXTERNAL APP MODE ====================

    private fun startExternalAppMode() {
        val packageName = prefsManager.externalAppPackage
        if (packageName.isEmpty()) {
            Log.e(TAG, "No external app package configured")
            return
        }

        // Try specific activity first, then fallback to launch intent
        val launchIntent: Intent? = try {
            val activityName = prefsManager.externalAppActivity
            if (activityName.isNotEmpty()) {
                Intent().apply {
                    setClassName(packageName, activityName)
                }
            } else {
                packageManager.getLaunchIntentForPackage(packageName)
            }
        } catch (e: Exception) {
            packageManager.getLaunchIntentForPackage(packageName)
        }

        if (launchIntent == null) {
            AlertDialog.Builder(this)
                .setTitle("App Not Found")
                .setMessage("The configured app '$packageName' is not installed.")
                .setPositiveButton("OK") { _, _ -> loadKioskUrl() }
                .show()
            return
        }

        // Stop any active lock task first
        if (kioskManager.isDeviceOwner) {
            try {
                stopLockTask()
                Log.i(TAG, "Stopped lock task before launching external app")
            } catch (e: Exception) {
                Log.w(TAG, "Could not stop lock task: ${e.message}")
            }
        }

        // Hide WebView
        webViewContainer.visibility = View.GONE
        isExternalAppRunning = true

        // Add external app to lock task whitelist if device owner
        if (kioskManager.isDeviceOwner) {
            kioskManager.addPackageToLockTask(packageName)
        }

        // Show overlay button if enabled
        if (prefsManager.showOverlayButton) {
            showOverlayButton()
        }

        // Launch external app with proper flags
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(launchIntent)
        
        // Move kiosk app to background
        moveTaskToBack(true)

        Log.i(TAG, "Started external app: $packageName")
    }

    private fun stopExternalAppMode() {
        isExternalAppRunning = false
        removeOverlayButton()
        webViewContainer.visibility = View.VISIBLE
        
        // Bring MainActivity back to foreground
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        
        loadKioskUrl()
        Log.i(TAG, "Stopped external app mode")
    }

    private fun stopExternalApp() {
        val packageName = prefsManager.externalAppPackage
        if (packageName.isNotEmpty()) {
            try {
                // Force stop app using Activity Manager
                val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                am.killBackgroundProcesses(packageName)
                
                // Also try to force stop via Device Policy Manager if device owner
                if (kioskManager.isDeviceOwner) {
                    try {
                        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                        val adminComponent = android.content.ComponentName(this, DeviceAdminReceiver::class.java)
                        dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), true)
                        handler.postDelayed({
                            dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), false)
                        }, 100)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not suspend package: ${e.message}")
                    }
                }
                
                Log.i(TAG, "Force stopped app: $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop app: $packageName", e)
            }
        }
        prefsManager.externalAppMode = false
        stopExternalAppMode()
    }

    private fun restartExternalApp() {
        val packageName = prefsManager.externalAppPackage
        if (packageName.isEmpty()) return
        
        // Stop app first
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.killBackgroundProcesses(packageName)
            
            // Force close activity
            if (kioskManager.isDeviceOwner) {
                try {
                    val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                    val adminComponent = android.content.ComponentName(this, DeviceAdminReceiver::class.java)
                    dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), true)
                    handler.postDelayed({
                        dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), false)
                    }, 100)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not suspend package: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not kill app: $packageName", e)
        }
        
        // Short delay then restart
        handler.postDelayed({
            startExternalAppMode()
        }, 500)
    }

    private fun installApk(apkPath: String) {
        try {
            val apkFile = java.io.File(apkPath)
            if (!apkFile.exists()) {
                Log.e(TAG, "APK file not found: $apkPath")
                return
            }

            val intent = Intent(Intent.ACTION_VIEW)
            val apkUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                androidx.core.content.FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    apkFile
                )
            } else {
                android.net.Uri.fromFile(apkFile)
            }
            
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            
            Log.i(TAG, "Started APK installation: $apkPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install APK: $apkPath", e)
        }
    }

    private fun uninstallApp(pkgName: String) {
        try {
            // Try Device Owner silent uninstall first
            if (kioskManager.isDeviceOwner) {
                try {
                    val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                    val adminComponent = android.content.ComponentName(this, DeviceAdminReceiver::class.java)
                    
                    // Use PackageInstaller for silent uninstall
                    val packageInstaller = packageManager.packageInstaller
                    packageInstaller.uninstall(pkgName, android.app.PendingIntent.getBroadcast(
                        this, 0,
                        Intent("com.axon.kiosk.UNINSTALL_RESULT"),
                        android.app.PendingIntent.FLAG_IMMUTABLE
                    ).intentSender)
                    
                    Log.i(TAG, "Silent uninstall initiated for: $pkgName")
                    return
                } catch (e: Exception) {
                    Log.w(TAG, "Silent uninstall failed, falling back to user prompt", e)
                }
            }
            
            // Fall back to user-interactive uninstall
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = android.net.Uri.parse("package:$pkgName")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            
            Log.i(TAG, "Started user uninstall dialog for: $pkgName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to uninstall: $pkgName", e)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlayButton() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }

        if (overlayButton != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 20
            y = 20
        }

        overlayButton = ImageButton(this).apply {
            setImageResource(R.drawable.ic_settings_overlay)
            setBackgroundResource(R.drawable.overlay_button_bg)
            alpha = 0.7f
            setPadding(24, 24, 24, 24)

            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - externalAppLastTapTime > TAP_TIMEOUT_MS) {
                        externalAppTapCount = 0
                    }
                    externalAppTapCount++
                    externalAppLastTapTime = currentTime

                    if (externalAppTapCount >= TAP_COUNT_THRESHOLD) {
                        externalAppTapCount = 0
                        openSettings()
                    }
                }
                false
            }
        }

        try {
            windowManager?.addView(overlayButton, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay button", e)
        }
    }

    private fun removeOverlayButton() {
        overlayButton?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay button", e)
            }
            overlayButton = null
        }
    }

    private fun requestOverlayPermission() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Overlay permission is needed to show the settings button over other apps.")
            .setPositiveButton("Grant") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                @Suppress("DEPRECATION")
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST)
            }
            .setNegativeButton("Skip", null)
            .show()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            if (Settings.canDrawOverlays(this) && isExternalAppRunning) {
                showOverlayButton()
            }
        }
    }

    // ==================== COMMON METHODS ====================

    private fun openSettings() {
        if (prefsManager.pinAppToScreen && kioskManager.isDeviceOwner) {
            kioskManager.stopLockTask(this)
        }
        removeOverlayButton()
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun applySettings() {
        kioskManager.enterFullscreen(this, prefsManager.showStatusBar)
        kioskManager.setScreenAlwaysOn(this, prefsManager.screenAlwaysOn)
        kioskManager.setBrightness(this, prefsManager.brightness)

        if (prefsManager.pinAppToScreen) {
            kioskManager.startLockTask(this)
        }

        setupScreensaverTimer()
        updateNavButtonsVisibility()
    }

    private fun loadKioskUrl() {
        if (prefsManager.externalAppMode) return
        val url = prefsManager.kioskUrl
        Log.i(TAG, "Loading URL: $url")
        webView.loadUrl(url)
    }

    private fun loadUrl(url: String) {
        if (prefsManager.externalAppMode) return
        Log.i(TAG, "Loading new URL: $url")
        webView.loadUrl(url)
    }

    private fun setupScreensaverTimer() {
        screensaverRunnable?.let { handler.removeCallbacks(it) }

        val timeout = prefsManager.screensaverTimeout
        if (timeout > 0) {
            screensaverRunnable = Runnable { activateScreensaver() }
            handler.postDelayed(screensaverRunnable!!, timeout * 60 * 1000L)
        }
    }

    private fun resetScreensaverTimer() {
        if (isScreensaverActive) deactivateScreensaver()
        setupScreensaverTimer()
        resetAutoReloadTimer()
    }

    private fun setupAutoReloadTimer() {
        autoReloadRunnable?.let { handler.removeCallbacks(it) }

        val minutes = prefsManager.autoReloadMinutes
        if (minutes > 0 && !prefsManager.externalAppMode) {
            autoReloadRunnable = Runnable { 
                Log.i(TAG, "Auto-reload triggered after $minutes minutes of inactivity")
                reloadWebView()
                setupAutoReloadTimer() // Reschedule
            }
            handler.postDelayed(autoReloadRunnable!!, minutes * 60 * 1000L)
            Log.d(TAG, "Auto-reload scheduled in $minutes minutes")
        }
    }

    private fun resetAutoReloadTimer() {
        lastActivityTime = System.currentTimeMillis()
        setupAutoReloadTimer()
    }

    private fun reloadWebView() {
        if (!prefsManager.externalAppMode) {
            Log.i(TAG, "Reloading WebView: ${prefsManager.kioskUrl}")
            webView.reload()
        }
    }

    private var screensaverView: WebView? = null
    private var originalUrl: String = ""

    private fun activateScreensaver() {
        isScreensaverActive = true
        
        when (prefsManager.screensaverType) {
            "black" -> {
                kioskManager.setBrightness(this, 5)
            }
            "url", "file" -> {
                val screensaverUrl = prefsManager.screensaverUrl
                if (screensaverUrl.isNotEmpty()) {
                    originalUrl = webView.url ?: prefsManager.kioskUrl
                    webView.loadUrl(screensaverUrl)
                }
                kioskManager.setBrightness(this, prefsManager.brightness)
            }
            else -> {
                kioskManager.setBrightness(this, 5)
            }
        }
    }

    private fun deactivateScreensaver() {
        isScreensaverActive = false
        kioskManager.setBrightness(this, prefsManager.brightness)
        
        // Restore original URL if screensaver was URL/file
        if (prefsManager.screensaverType in listOf("url", "file") && originalUrl.isNotEmpty()) {
            webView.loadUrl(originalUrl)
            originalUrl = ""
        }
    }

    private fun showWelcomeDialog() {
        AlertDialog.Builder(this)
            .setTitle("Welcome to Libera Kiosk")
            .setMessage(
                "To access settings:\n" +
                "• Tap 5 times in the bottom-right corner\n" +
                "• Enter PIN (default: 1234)\n\n" +
                "For full kiosk mode, set as Device Owner via ADB:\n" +
                "adb shell dpm set-device-owner com.axon.kiosk/.DeviceAdminReceiver"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        applySettings()

        // Re-check external app mode
        if (prefsManager.externalAppMode && prefsManager.externalAppPackage.isNotEmpty()) {
            if (!isExternalAppRunning) {
                startExternalAppMode()
            } else if (prefsManager.showOverlayButton) {
                showOverlayButton()
            }
        } else {
            if (isExternalAppRunning) {
                stopExternalAppMode()
            } else if (webView.url != prefsManager.kioskUrl) {
                loadKioskUrl()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        screensaverRunnable?.let { handler.removeCallbacks(it) }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (prefsManager.testMode) {
            if (!prefsManager.externalAppMode && webView.canGoBack()) {
                webView.goBack()
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(settingsReceiver)
        } catch (e: Exception) { }
        removeOverlayButton()
        webView.destroy()
        super.onDestroy()
    }
}
