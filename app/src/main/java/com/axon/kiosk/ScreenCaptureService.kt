package com.axon.kiosk

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "axon_kiosk_capture"
        
        // Static access to latest screenshot
        @Volatile
        var latestScreenshot: ByteArray? = null
            private set
        
        @Volatile
        var isCapturing: Boolean = false
            private set

        private var resultCode: Int = Activity.RESULT_CANCELED
        private var resultData: Intent? = null

        fun setMediaProjectionResult(code: Int, data: Intent?) {
            resultCode = code
            resultData = data
        }

        fun hasPermission(): Boolean {
            return resultCode == Activity.RESULT_OK && resultData != null
        }
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handler: Handler? = null
    
    private var screenWidth = 720
    private var screenHeight = 1280
    private var screenDensity = 1
    
    private val isRunning = AtomicBoolean(false)
    private var captureInterval = 1000L // 1 second default

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Get screen metrics
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)
        
        // Use reduced resolution for bandwidth
        screenWidth = (metrics.widthPixels * 0.5).toInt()
        screenHeight = (metrics.heightPixels * 0.5).toInt()
        screenDensity = metrics.densityDpi
        
        handler = Handler(Looper.getMainLooper())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground", e)
        }
        
        intent?.let {
            captureInterval = it.getLongExtra("interval", 1000L)
        }
        
        if (!isRunning.get() && hasPermission()) {
            startCapture()
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopCapture()
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
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Screen capture for remote viewing"
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Libera Kiosk")
            .setContentText("Screen capture active")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startCapture() {
        if (isRunning.get()) return
        
        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData!!)
            
            if (mediaProjection == null) {
                Log.e(TAG, "Failed to get MediaProjection")
                return
            }

            // Register callback for projection stop
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.i(TAG, "MediaProjection stopped")
                    stopCapture()
                }
            }, handler)

            // Create ImageReader
            imageReader = ImageReader.newInstance(
                screenWidth, screenHeight,
                PixelFormat.RGBA_8888, 2
            )

            // Create VirtualDisplay
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "AxonKioskCapture",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, handler
            )

            isRunning.set(true)
            isCapturing = true
            
            // Start capture loop
            startCaptureLoop()
            
            Log.i(TAG, "Screen capture started: ${screenWidth}x${screenHeight}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting capture", e)
            stopCapture()
        }
    }

    private fun startCaptureLoop() {
        handler?.postDelayed(object : Runnable {
            override fun run() {
                if (isRunning.get()) {
                    captureScreen()
                    handler?.postDelayed(this, captureInterval)
                }
            }
        }, captureInterval)
    }

    private fun captureScreen() {
        try {
            val image = imageReader?.acquireLatestImage() ?: return
            
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth

            // Create bitmap
            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()

            // Crop to actual screen size if needed
            val croppedBitmap = if (rowPadding > 0) {
                Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight).also {
                    bitmap.recycle()
                }
            } else {
                bitmap
            }

            // Convert to JPEG
            val outputStream = ByteArrayOutputStream()
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            croppedBitmap.recycle()
            
            latestScreenshot = outputStream.toByteArray()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing screen", e)
        }
    }

    private fun stopCapture() {
        isRunning.set(false)
        isCapturing = false
        
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            
            imageReader?.close()
            imageReader = null
            
            mediaProjection?.stop()
            mediaProjection = null
            
            latestScreenshot = null
            
            Log.i(TAG, "Screen capture stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping capture", e)
        }
    }

    fun updateInterval(interval: Long) {
        captureInterval = interval.coerceIn(500, 5000)
    }
}
