package com.axon.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager

class KioskManager(private val context: Context) {

    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    
    private val adminComponentName: ComponentName =
        ComponentName(context, DeviceAdminReceiver::class.java)

    companion object {
        private const val TAG = "KioskManager"
    }

    val isDeviceOwner: Boolean
        get() = devicePolicyManager.isDeviceOwnerApp(context.packageName)

    val isAdminActive: Boolean
        get() = devicePolicyManager.isAdminActive(adminComponentName)

    /**
     * Set up kiosk policies when device owner
     */
    fun setupKioskPolicies() {
        if (!isDeviceOwner) {
            Log.w(TAG, "Not device owner, cannot set kiosk policies")
            return
        }

        try {
            // Set this app as lock task package
            devicePolicyManager.setLockTaskPackages(adminComponentName, arrayOf(context.packageName))
            
            // Set as home app
            setAsHomeApp()
            
            // Disable keyguard
            devicePolicyManager.setKeyguardDisabled(adminComponentName, true)
            
            // Set user restrictions
            setUserRestrictions(true)
            
            Log.i(TAG, "Kiosk policies configured successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting kiosk policies", e)
        }
    }

    /**
     * Set app as default home/launcher
     */
    private fun setAsHomeApp() {
        if (!isDeviceOwner) return
        
        try {
            val intentFilter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            
            val activity = ComponentName(context, MainActivity::class.java)
            devicePolicyManager.addPersistentPreferredActivity(adminComponentName, intentFilter, activity)
            
            Log.i(TAG, "Set as home app")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting as home app", e)
        }
    }

    /**
     * Set user restrictions
     */
    private fun setUserRestrictions(enable: Boolean) {
        if (!isDeviceOwner) return
        
        val restrictions = arrayOf(
            UserManager.DISALLOW_SAFE_BOOT,
            UserManager.DISALLOW_FACTORY_RESET,
            UserManager.DISALLOW_ADD_USER,
            UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA,
            UserManager.DISALLOW_USB_FILE_TRANSFER
        )
        
        restrictions.forEach { restriction ->
            try {
                if (enable) {
                    devicePolicyManager.addUserRestriction(adminComponentName, restriction)
                } else {
                    devicePolicyManager.clearUserRestriction(adminComponentName, restriction)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not set restriction: $restriction", e)
            }
        }
    }

    /**
     * Start lock task mode
     */
    fun startLockTask(activity: Activity) {
        if (!isDeviceOwner) {
            Log.w(TAG, "Not device owner, using screen pinning instead")
            activity.startLockTask()
            return
        }
        
        try {
            val packages = devicePolicyManager.getLockTaskPackages(adminComponentName)
            if (!packages.contains(context.packageName)) {
                devicePolicyManager.setLockTaskPackages(adminComponentName, arrayOf(context.packageName))
            }
            
            // Set lock task features
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                devicePolicyManager.setLockTaskFeatures(
                    adminComponentName,
                    DevicePolicyManager.LOCK_TASK_FEATURE_NONE
                )
            }
            
            activity.startLockTask()
            Log.i(TAG, "Lock task started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting lock task", e)
        }
    }

    /**
     * Stop lock task mode
     */
    fun stopLockTask(activity: Activity) {
        try {
            activity.stopLockTask()
            Log.i(TAG, "Lock task stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping lock task", e)
        }
    }

    /**
     * Add package to lock task whitelist
     */
    fun addPackageToLockTask(packageName: String) {
        if (!isDeviceOwner) return
        
        try {
            val currentPackages = devicePolicyManager.getLockTaskPackages(adminComponentName)
            if (!currentPackages.contains(packageName)) {
                val newPackages = currentPackages + packageName
                devicePolicyManager.setLockTaskPackages(adminComponentName, newPackages)
                Log.i(TAG, "Added $packageName to lock task whitelist")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding package to lock task", e)
        }
    }

    /**
     * Enter immersive fullscreen mode
     */
    fun enterFullscreen(activity: Activity, showStatusBar: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = activity.window.insetsController
            if (controller != null) {
                if (showStatusBar) {
                    controller.hide(WindowInsets.Type.navigationBars())
                    controller.show(WindowInsets.Type.statusBars())
                } else {
                    controller.hide(WindowInsets.Type.systemBars())
                }
                controller.systemBarsBehavior = 
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = if (showStatusBar) {
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            } else {
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }
    }

    /**
     * Keep screen on
     */
    fun setScreenAlwaysOn(activity: Activity, enabled: Boolean) {
        if (enabled) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * Set screen brightness
     */
    fun setBrightness(activity: Activity, brightness: Int) {
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = brightness / 100f
        activity.window.attributes = layoutParams
    }

    /**
     * Reboot device (requires device owner or root)
     */
    fun rebootDevice(): Boolean {
        Log.i(TAG, "Attempting to reboot device...")
        
        // Method 1: Device Owner
        if (isDeviceOwner) {
            try {
                devicePolicyManager.reboot(adminComponentName)
                Log.i(TAG, "Reboot initiated via Device Owner")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Device Owner reboot failed", e)
            }
        }
        
        // Method 2: PowerManager (requires REBOOT permission)
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            pm.reboot(null)
            Log.i(TAG, "Reboot initiated via PowerManager")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "PowerManager reboot failed", e)
        }
        
        // Method 3: Shell command (requires root)
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot"))
            Log.i(TAG, "Reboot initiated via root shell")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Root shell reboot failed", e)
        }
        
        Log.e(TAG, "All reboot methods failed")
        return false
    }

    /**
     * Clear device owner (for uninstall)
     */
    fun clearDeviceOwner() {
        if (!isDeviceOwner) return
        
        try {
            setUserRestrictions(false)
            devicePolicyManager.clearDeviceOwnerApp(context.packageName)
            Log.i(TAG, "Device owner cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing device owner", e)
        }
    }

    /**
     * Check if we can write system settings
     */
    fun canWriteSettings(): Boolean {
        return Settings.System.canWrite(context)
    }

    /**
     * Request write settings permission
     */
    fun requestWriteSettingsPermission(activity: Activity) {
        if (!canWriteSettings()) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
            activity.startActivity(intent)
        }
    }
    
    /**
     * Enable full Kiosk Mode with Lock Task
     */
    fun enableKioskMode(activity: Activity) {
        if (!isDeviceOwner) {
            Log.w(TAG, "Cannot enable kiosk mode - not device owner")
            return
        }
        
        try {
            // Add this app to lock task packages
            devicePolicyManager.setLockTaskPackages(adminComponentName, arrayOf(context.packageName))
            
            // Start lock task mode
            activity.startLockTask()
            
            Log.i(TAG, "Kiosk mode enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling kiosk mode", e)
        }
    }
    
    /**
     * Disable Kiosk Mode and exit Lock Task
     */
    fun disableKioskMode(activity: Activity) {
        try {
            activity.stopLockTask()
            Log.i(TAG, "Kiosk mode disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling kiosk mode", e)
        }
    }
}
