package com.axon.kiosk

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import java.util.*
import javax.net.ssl.SSLServerSocketFactory

class WebServerService : Service() {

    companion object {
        private const val TAG = "WebServerService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "axon_kiosk_webserver"
        private const val SESSION_TIMEOUT_MS = 3600000L
    }

    private var server: KioskWebServer? = null
    private val sessionTokens = mutableMapOf<String, Long>()
    private val gson = Gson()
    private lateinit var prefsManager: PrefsManager
    private lateinit var certificateManager: CertificateManager

    override fun onCreate() {
        super.onCreate()
        prefsManager = AxonKioskApp.instance.prefsManager
        certificateManager = CertificateManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "WebServerService onStartCommand called")
        try {
            startForeground(NOTIFICATION_ID, createNotification())
            Log.i(TAG, "Foreground started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground", e)
        }
        startServer()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Delete old channel if exists (Fire OS fix)
            try {
                manager.deleteNotificationChannel(CHANNEL_ID)
            } catch (e: Exception) { }
            
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Libera Kiosk Web Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Web administration server"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val protocol = if (prefsManager.httpsEnabled) "HTTPS" else "HTTP"
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Libera Kiosk")
            .setContentText("$protocol server on port ${prefsManager.webServerPort}")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startServer() {
        try {
            server?.stop()
            val port = prefsManager.webServerPort
            val useHttps = prefsManager.httpsEnabled
            
            server = KioskWebServer(port)
            
            if (useHttps) {
                val sslFactory = certificateManager.getSSLServerSocketFactory()
                if (sslFactory != null) {
                    server?.makeSecure(sslFactory, null)
                    Log.i(TAG, "HTTPS enabled for web server")
                } else {
                    Log.w(TAG, "Failed to enable HTTPS, falling back to HTTP")
                }
            }
            
            server?.start()
            val protocol = if (useHttps && certificateManager.hasCertificate()) "HTTPS" else "HTTP"
            Log.i(TAG, "$protocol web server started on port $port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start web server", e)
        }
    }

    private fun stopServer() {
        server?.stop()
        server = null
        Log.i(TAG, "Web server stopped")
    }

    inner class KioskWebServer(port: Int) : NanoHTTPD(port) {

        override fun serve(session: IHTTPSession): Response {
            val uri = session.uri
            val method = session.method

            val corsHeaders = mapOf(
                "Access-Control-Allow-Origin" to "*",
                "Access-Control-Allow-Methods" to "GET, POST, OPTIONS",
                "Access-Control-Allow-Headers" to "Content-Type, Authorization"
            )

            if (method == Method.OPTIONS) {
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "").apply {
                    corsHeaders.forEach { (k, v) -> addHeader(k, v) }
                }
            }

            return when {
                uri == "/" || uri == "/index.html" -> serveLoginPage(corsHeaders)
                uri == "/admin" -> serveAdminPage(session, corsHeaders)
                uri == "/api/login" && method == Method.POST -> handleLogin(session, corsHeaders)
                uri == "/api/logout" && method == Method.POST -> handleLogout(session, corsHeaders)
                uri == "/api/settings" && method == Method.GET -> handleGetSettings(session, corsHeaders)
                uri == "/api/settings" && method == Method.POST -> handleSetSettings(session, corsHeaders)
                uri == "/api/brightness" && method == Method.POST -> handleSetBrightness(session, corsHeaders)
                uri == "/api/status" && method == Method.GET -> handleGetStatus(session, corsHeaders)
                uri == "/api/reload" && method == Method.POST -> handleReload(session, corsHeaders)
                uri == "/api/reboot" && method == Method.POST -> handleReboot(session, corsHeaders)
                uri == "/api/screenshot" && method == Method.GET -> handleScreenshot(session, corsHeaders)
                uri == "/api/apps" && method == Method.GET -> handleGetApps(session, corsHeaders)
                uri == "/api/app/start" && method == Method.POST -> handleStartApp(session, corsHeaders)
                uri == "/api/app/stop" && method == Method.POST -> handleStopApp(session, corsHeaders)
                uri == "/api/app/restart" && method == Method.POST -> handleRestartApp(session, corsHeaders)
                uri == "/api/upload-apk" && method == Method.POST -> handleUploadApk(session, corsHeaders)
                uri == "/api/app/uninstall" && method == Method.POST -> handleUninstallApp(session, corsHeaders)
                uri == "/api/certificate/info" && method == Method.GET -> handleGetCertInfo(session, corsHeaders)
                uri == "/api/certificate/generate" && method == Method.POST -> handleGenerateCert(session, corsHeaders)
                uri == "/api/certificate/delete" && method == Method.POST -> handleDeleteCert(session, corsHeaders)
                uri == "/live" -> serveLiveViewPage(session, corsHeaders)
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
            }
        }

        private fun isValidSession(token: String?): Boolean {
            if (token == null) return false
            val expiry = sessionTokens[token] ?: return false
            if (System.currentTimeMillis() > expiry) {
                sessionTokens.remove(token)
                return false
            }
            return true
        }

        private fun getAuthToken(session: IHTTPSession): String? {
            return session.headers["authorization"]?.removePrefix("Bearer ") ?: session.parms["token"]
        }

        private fun readBody(session: IHTTPSession): String {
            val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
            val body = ByteArray(contentLength)
            session.inputStream.read(body, 0, contentLength)
            return String(body)
        }

        private fun jsonResponse(status: Response.Status, data: Any, headers: Map<String, String>): Response {
            return newFixedLengthResponse(status, "application/json", gson.toJson(data)).apply {
                headers.forEach { (k, v) -> addHeader(k, v) }
            }
        }

        private fun handleLogin(session: IHTTPSession, headers: Map<String, String>): Response {
            return try {
                val body = readBody(session)
                val request = gson.fromJson(body, LoginRequest::class.java)
                if (prefsManager.verifyWebPassword(request.password)) {
                    val token = UUID.randomUUID().toString()
                    sessionTokens[token] = System.currentTimeMillis() + SESSION_TIMEOUT_MS
                    jsonResponse(Response.Status.OK, mapOf("success" to true, "token" to token), headers)
                } else {
                    jsonResponse(Response.Status.UNAUTHORIZED, mapOf("success" to false, "error" to "Invalid password"), headers)
                }
            } catch (e: Exception) {
                jsonResponse(Response.Status.BAD_REQUEST, mapOf("error" to e.message), headers)
            }
        }

        private fun handleLogout(session: IHTTPSession, headers: Map<String, String>): Response {
            getAuthToken(session)?.let { sessionTokens.remove(it) }
            return jsonResponse(Response.Status.OK, mapOf("success" to true), headers)
        }

        private fun handleGetSettings(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return jsonResponse(Response.Status.UNAUTHORIZED, mapOf("error" to "Unauthorized"), headers)
            }
            return jsonResponse(Response.Status.OK, prefsManager.toMap(), headers)
        }

        private fun handleSetSettings(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return jsonResponse(Response.Status.UNAUTHORIZED, mapOf("error" to "Unauthorized"), headers)
            }
            return try {
                val body = readBody(session)
                @Suppress("UNCHECKED_CAST")
                val settings = gson.fromJson(body, Map::class.java) as Map<String, Any>
                prefsManager.fromMap(settings)
                val intent = Intent("com.axon.kiosk.SETTINGS_CHANGED")
                intent.setPackage(packageName)
                sendBroadcast(intent)
                jsonResponse(Response.Status.OK, mapOf("success" to true, "message" to "Settings saved"), headers)
            } catch (e: Exception) {
                jsonResponse(Response.Status.BAD_REQUEST, mapOf("error" to e.message), headers)
            }
        }

        private fun handleSetBrightness(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return jsonResponse(Response.Status.UNAUTHORIZED, mapOf("error" to "Unauthorized"), headers)
            }
            return try {
                val body = readBody(session)
                @Suppress("UNCHECKED_CAST")
                val data = gson.fromJson(body, Map::class.java) as Map<String, Any>
                val brightness = (data["brightness"] as? Number)?.toInt() ?: 100
                
                // Send broadcast to MainActivity for live brightness change
                val intent = Intent("com.axon.kiosk.SET_BRIGHTNESS")
                intent.setPackage(packageName)
                intent.putExtra("brightness", brightness)
                sendBroadcast(intent)
                
                jsonResponse(Response.Status.OK, mapOf("success" to true, "brightness" to brightness), headers)
            } catch (e: Exception) {
                jsonResponse(Response.Status.BAD_REQUEST, mapOf("error" to e.message), headers)
            }
        }

        private fun handleGetStatus(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return jsonResponse(Response.Status.UNAUTHORIZED, mapOf("error" to "Unauthorized"), headers)
            }
            val status = mapOf(
                "serverRunning" to true,
                "deviceOwner" to KioskManager(this@WebServerService).isDeviceOwner,
                "uptime" to SystemClock.elapsedRealtime(),
                "batteryLevel" to getBatteryLevel(),
                "wifiConnected" to isWifiConnected(),
                "ipAddress" to getDeviceIP()
            )
            return jsonResponse(Response.Status.OK, status, headers)
        }

        private fun handleReload(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return jsonResponse(Response.Status.UNAUTHORIZED, mapOf("error" to "Unauthorized"), headers)
            }
            Log.i(TAG, "Reload requested via web API")
            val intent = Intent("com.axon.kiosk.RELOAD_WEBVIEW")
            intent.setPackage(packageName)
            sendBroadcast(intent)
            return jsonResponse(Response.Status.OK, mapOf("success" to true, "message" to "Reloading..."), headers)
        }

        private fun handleReboot(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return jsonResponse(Response.Status.UNAUTHORIZED, mapOf("error" to "Unauthorized"), headers)
            }
            Log.i(TAG, "Reboot requested via web API")
            
            val kioskManager = KioskManager(this@WebServerService)
            
            // Try reboot (returns success/failure)
            val success = kioskManager.rebootDevice()
            
            return if (success) {
                jsonResponse(Response.Status.OK, mapOf("success" to true, "message" to "Rebooting..."), headers)
            } else {
                // Try via broadcast as fallback
                val intent = Intent("com.axon.kiosk.REBOOT_DEVICE")
                intent.setPackage(packageName)
                sendBroadcast(intent)
                jsonResponse(Response.Status.OK, mapOf(
                    "success" to true, 
                    "message" to "Reboot initiated",
                    "note" to "If device doesn't reboot, Device Owner or root required"
                ), headers)
            }
        }

        private fun handleScreenshot(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return jsonResponse(Response.Status.UNAUTHORIZED, mapOf("error" to "Unauthorized"), headers)
            }
            
            val screenshot = ScreenCaptureService.latestScreenshot
            return if (screenshot != null) {
                newFixedLengthResponse(
                    Response.Status.OK,
                    "image/jpeg",
                    java.io.ByteArrayInputStream(screenshot),
                    screenshot.size.toLong()
                ).apply {
                    headers.forEach { (k, v) -> addHeader(k, v) }
                    addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                }
            } else {
                jsonResponse(
                    Response.Status.SERVICE_UNAVAILABLE, 
                    mapOf("error" to "Screen capture not available", "capturing" to ScreenCaptureService.isCapturing),
                    headers
                )
            }
        }

        private fun serveLiveViewPage(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return newFixedLengthResponse(Response.Status.REDIRECT, "text/html", "").apply { addHeader("Location", "/") }
            }
            return newFixedLengthResponse(Response.Status.OK, "text/html", HtmlTemplates.LIVE_VIEW_HTML).apply {
                headers.forEach { (k, v) -> addHeader(k, v) }
            }
        }

        private fun getBatteryLevel(): Int {
            val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }

        private fun isWifiConnected(): Boolean {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }

        private fun getDeviceIP(): String {
            return try {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val ipAddress = wifiManager.connectionInfo.ipAddress
                String.format("%d.%d.%d.%d", ipAddress and 0xff, (ipAddress shr 8) and 0xff, (ipAddress shr 16) and 0xff, (ipAddress shr 24) and 0xff)
            } catch (e: Exception) { "0.0.0.0" }
        }

        private fun serveLoginPage(headers: Map<String, String>): Response {
            return newFixedLengthResponse(Response.Status.OK, "text/html", HtmlTemplates.LOGIN_HTML).apply {
                headers.forEach { (k, v) -> addHeader(k, v) }
            }
        }

        private fun serveAdminPage(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return newFixedLengthResponse(Response.Status.REDIRECT, "text/html", "").apply { addHeader("Location", "/") }
            }
            return newFixedLengthResponse(Response.Status.OK, "text/html", HtmlTemplates.ADMIN_HTML).apply {
                headers.forEach { (k, v) -> addHeader(k, v) }
            }
        }

        private fun handleGetApps(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return jsonResponse(Response.Status.UNAUTHORIZED, mapOf("error" to "Unauthorized"), headers)
            }
            val pm = packageManager
            
            // Get all installed packages including system apps
            val allApps = mutableListOf<Map<String, String>>()
            
            // Method 1: Get apps with launcher intent
            val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
            mainIntent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            val launcherApps = pm.queryIntentActivities(mainIntent, 0)
            
            for (resolveInfo in launcherApps) {
                val packageName = resolveInfo.activityInfo.packageName
                if (packageName != getPackageName()) {
                    val appName = resolveInfo.loadLabel(pm).toString()
                    allApps.add(mapOf(
                        "packageName" to packageName,
                        "appName" to appName,
                        "activityName" to resolveInfo.activityInfo.name
                    ))
                }
            }
            
            // Method 2: Also check installed applications directly
            val installedApps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            for (appInfo in installedApps) {
                // Check if already added
                if (allApps.none { it["packageName"] == appInfo.packageName }) {
                    // Try to get launch intent
                    val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                    if (launchIntent != null && appInfo.packageName != getPackageName()) {
                        val appName = pm.getApplicationLabel(appInfo).toString()
                        allApps.add(mapOf(
                            "packageName" to appInfo.packageName,
                            "appName" to appName,
                            "activityName" to ""
                        ))
                    }
                }
            }
            
            // Sort by app name
            val sortedApps = allApps.sortedBy { it["appName"]?.lowercase() }
            
            Log.i(TAG, "Found ${sortedApps.size} apps")
            return jsonResponse(Response.Status.OK, mapOf("apps" to sortedApps, "count" to sortedApps.size), headers)
        }

        private fun handleStartApp(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return jsonResponse(Response.Status.UNAUTHORIZED, mapOf("error" to "Unauthorized"), headers)
            }
            return try {
                val body = readBody(session)
                @Suppress("UNCHECKED_CAST")
                val data = gson.fromJson(body, Map::class.java) as Map<String, Any>
                val packageName = data["packageName"]?.toString() ?: ""
                
                if (packageName.isEmpty()) {
                    return jsonResponse(Response.Status.BAD_REQUEST, mapOf("error" to "Package name required"), headers)
                }
                
                val intent = Intent("com.axon.kiosk.START_EXTERNAL_APP")
                intent.setPackage(getPackageName())
                intent.putExtra("packageName", packageName)
                sendBroadcast(intent)
                
                jsonResponse(Response.Status.OK, mapOf("success" to true, "message" to "Starting $packageName"), headers)
            } catch (e: Exception) {
                jsonResponse(Response.Status.BAD_REQUEST, mapOf("error" to e.message), headers)
            }
        }

        private fun handleStopApp(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return jsonResponse(Response.Status.UNAUTHORIZED, mapOf("error" to "Unauthorized"), headers)
            }
            val intent = Intent("com.axon.kiosk.STOP_EXTERNAL_APP")
            intent.setPackage(packageName)
            sendBroadcast(intent)
            return jsonResponse(Response.Status.OK, mapOf("success" to true, "message" to "Stopping app"), headers)
        }

        private fun handleRestartApp(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return jsonResponse(Response.Status.UNAUTHORIZED, mapOf("error" to "Unauthorized"), headers)
            }
            val intent = Intent("com.axon.kiosk.RESTART_EXTERNAL_APP")
            intent.setPackage(packageName)
            sendBroadcast(intent)
            return jsonResponse(Response.Status.OK, mapOf("success" to true, "message" to "Restarting app"), headers)
        }

        private fun handleUninstallApp(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return jsonResponse(Response.Status.UNAUTHORIZED, mapOf("error" to "Unauthorized"), headers)
            }
            return try {
                val body = readBody(session)
                @Suppress("UNCHECKED_CAST")
                val data = gson.fromJson(body, Map::class.java) as Map<String, Any>
                val pkgName = data["packageName"]?.toString() ?: ""
                
                if (pkgName.isEmpty()) {
                    return jsonResponse(Response.Status.BAD_REQUEST, mapOf("error" to "Package name required"), headers)
                }
                
                // Send broadcast to MainActivity to handle uninstall
                val intent = Intent("com.axon.kiosk.UNINSTALL_APP")
                intent.setPackage(getPackageName())
                intent.putExtra("packageName", pkgName)
                sendBroadcast(intent)
                
                jsonResponse(Response.Status.OK, mapOf("success" to true, "message" to "Uninstall initiated for $pkgName"), headers)
            } catch (e: Exception) {
                Log.e(TAG, "Uninstall failed", e)
                jsonResponse(Response.Status.BAD_REQUEST, mapOf("error" to e.message), headers)
            }
        }

        private fun handleUploadApk(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return jsonResponse(Response.Status.UNAUTHORIZED, mapOf("error" to "Unauthorized"), headers)
            }
            return try {
                val files = mutableMapOf<String, String>()
                session.parseBody(files)
                
                val tempFile = files["file"]
                if (tempFile == null) {
                    return jsonResponse(Response.Status.BAD_REQUEST, mapOf("error" to "No file uploaded"), headers)
                }
                
                val sourceFile = java.io.File(tempFile)
                val apkDir = java.io.File(getExternalFilesDir(null), "apk")
                apkDir.mkdirs()
                val apkFile = java.io.File(apkDir, "upload_${System.currentTimeMillis()}.apk")
                sourceFile.copyTo(apkFile, overwrite = true)
                
                // Trigger installation via Intent
                val intent = Intent("com.axon.kiosk.INSTALL_APK")
                intent.setPackage(packageName)
                intent.putExtra("apkPath", apkFile.absolutePath)
                sendBroadcast(intent)
                
                jsonResponse(Response.Status.OK, mapOf(
                    "success" to true, 
                    "message" to "APK uploaded, installation started",
                    "path" to apkFile.absolutePath
                ), headers)
            } catch (e: Exception) {
                Log.e(TAG, "APK upload failed", e)
                jsonResponse(Response.Status.INTERNAL_ERROR, mapOf("error" to e.message), headers)
            }
        }

        private fun handleGetCertInfo(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return jsonResponse(Response.Status.UNAUTHORIZED, mapOf("error" to "Unauthorized"), headers)
            }
            val info = certificateManager.getCertificateInfo()
            val hasCert = certificateManager.hasCertificate()
            val httpsEnabled = prefsManager.httpsEnabled
            
            return jsonResponse(Response.Status.OK, mapOf(
                "hasCertificate" to hasCert,
                "httpsEnabled" to httpsEnabled,
                "info" to info
            ), headers)
        }

        private fun handleGenerateCert(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return jsonResponse(Response.Status.UNAUTHORIZED, mapOf("error" to "Unauthorized"), headers)
            }
            return try {
                val success = certificateManager.generateCertificate()
                if (success) {
                    jsonResponse(Response.Status.OK, mapOf(
                        "success" to true,
                        "message" to "Certificate generated successfully. Restart server to apply.",
                        "info" to certificateManager.getCertificateInfo()
                    ), headers)
                } else {
                    jsonResponse(Response.Status.INTERNAL_ERROR, mapOf(
                        "success" to false,
                        "error" to "Failed to generate certificate"
                    ), headers)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Certificate generation failed", e)
                jsonResponse(Response.Status.INTERNAL_ERROR, mapOf("error" to e.message), headers)
            }
        }

        private fun handleDeleteCert(session: IHTTPSession, headers: Map<String, String>): Response {
            if (!isValidSession(getAuthToken(session))) {
                return jsonResponse(Response.Status.UNAUTHORIZED, mapOf("error" to "Unauthorized"), headers)
            }
            return try {
                certificateManager.deleteCertificate()
                prefsManager.httpsEnabled = false
                jsonResponse(Response.Status.OK, mapOf(
                    "success" to true,
                    "message" to "Certificate deleted"
                ), headers)
            } catch (e: Exception) {
                Log.e(TAG, "Certificate deletion failed", e)
                jsonResponse(Response.Status.INTERNAL_ERROR, mapOf("error" to e.message), headers)
            }
        }
    }

    data class LoginRequest(val password: String = "")
}
