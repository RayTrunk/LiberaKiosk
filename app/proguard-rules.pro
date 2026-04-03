# Squizz Kiosk ProGuard Rules

# Keep NanoHTTPD
-keep class fi.iki.elonen.** { *; }

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.squizz.kiosk.data.** { *; }

# Keep Device Admin
-keep class com.squizz.kiosk.DeviceAdminReceiver { *; }
