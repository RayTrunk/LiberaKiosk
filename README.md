# Libera Kiosk 🚀

**Libera Kiosk** is a professional-grade Android application designed to turn any tablet or smartphone into a dedicated, locked-down kiosk device. Perfect for digital signage, dashboards, point-of-sale (POS) systems, and industrial control panels.

## 🌟 Key Features

*   **🔒 Full Kiosk Lockdown:** Uses Android's Device Policy Manager (DPC) to provide true "Device Owner" lockdown.
    *   Disables Status Bar, Recent Apps, and Power Menu.
    *   Locks the device into a single function.
*   **📡 Remote Management:**
    *   **MQTT v5 Integration:** Real-time status reporting and remote control.
    *   **Home Assistant Discovery:** Automatically appears as a device in Home Assistant with entities for battery, uptime, brightness, and commands.
    *   **REST API:** Local HTTP server for programmatic administration and status monitoring.
*   **🛠️ Two Operating Modes:**
    *   **WebView Mode:** Renders any website or dashboard in full-screen with high performance.
    *   **External App Mode:** Launches and monitors a specific third-party app (e.g., Square, Shopify POS).
*   **🌙 Smart Scheduling:**
    *   Configurable Screen Off/On schedule to save power and screen life.
    *   Daily auto-reboot to ensure long-term stability.
*   **⚡ Auto-Start:** Launches automatically on device boot.

## 🚀 Getting Started

### Installation
1. Install the APK on your Android device (Android 9.0+ recommended).
2. Follow the setup wizard to grant necessary permissions.

### Enabling Full Lockdown (Device Owner)
To enable the most secure lockdown features, the app must be set as the **Device Owner** via ADB:
```bash
adb shell dpm set-device-owner com.axon.kiosk/.DeviceAdminReceiver
```

### Configuration
1. Tap **5 times** in the bottom-right corner to access settings.
2. Default PIN: `1234`
3. Configure your URL, MQTT broker, and lockdown preferences.

## 📡 MQTT Integration

Libera Kiosk supports Home Assistant MQTT Discovery. Once connected to your broker, the following entities will be created:

| Entity | Type | Description |
| :--- | :--- | :--- |
| `sensor.battery` | Sensor | Current battery percentage. |
| `sensor.uptime` | Sensor | Device uptime in seconds. |
| `number.brightness` | Number | Adjust screen brightness (0-255). |
| `button.reload` | Button | Force reload the WebView. |
| `button.reboot` | Button | Remotely reboot the tablet. |

## 🛡️ Security
*   **PIN Protection:** Local settings access is protected by a configurable PIN.
*   **Brute Force Protection:** Automatic lockout after 5 failed PIN attempts.
*   **Web Admin Password:** The remote REST API is protected by a separate password.

## 🏗️ Technical Stack
*   **Language:** Kotlin 1.9
*   **Networking:** Ktor (Client/Server), HiveMQ MQTT Client
*   **Encryption:** BouncyCastle (for SSL/TLS certificates)
*   **UI:** Material Components for Android

---
Developed with ❤️ for the community.
