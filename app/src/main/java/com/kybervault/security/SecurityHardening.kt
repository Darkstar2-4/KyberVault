package com.kybervault.security

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Debug
import android.provider.Settings
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Anti-sniffer, anti-logger, anti-debug hardening.
 *
 * Threat model:
 * - Keyloggers (accessibility-based or IME-based)
 * - Screen capture / overlay attacks (tapjacking)
 * - Debugger attachment (Frida, LLDB, gdb)
 * - Root-level memory dumping
 * - Logcat sniffing of exception traces
 * - Emulator-based analysis
 */
object SecurityHardening {

    // ── DEBUGGER DETECTION ──

    /** Check if a debugger is currently attached or waiting. */
    fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    /** Check if the app was built with debuggable flag (should be false in release). */
    fun isDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /** Detect common instrumentation frameworks (Frida, Xposed, etc). */
    fun detectInstrumentation(): List<String> {
        val threats = mutableListOf<String>()

        // Frida detection: check for frida-server in running processes
        try {
            val process = Runtime.getRuntime().exec("ps -A")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.useLines { lines ->
                lines.forEach { line ->
                    val lower = line.lowercase()
                    if ("frida" in lower) threats.add("frida")
                    if ("xposed" in lower) threats.add("xposed")
                    if ("magisk" in lower) threats.add("magisk")
                    if ("lsposed" in lower) threats.add("lsposed")
                }
            }
        } catch (_: Exception) {}

        // Frida detection: check default frida port
        try {
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress("127.0.0.1", 27042), 100)
            socket.close()
            threats.add("frida-port")
        } catch (_: Exception) {}

        // Xposed detection: check for Xposed classes
        try {
            Class.forName("de.robv.android.xposed.XposedBridge")
            threats.add("xposed-bridge")
        } catch (_: ClassNotFoundException) {}

        // Substrate / Cydia detection
        try {
            Class.forName("com.saurik.substrate.MS")
            threats.add("substrate")
        } catch (_: ClassNotFoundException) {}

        return threats
    }

    // ── ROOT DETECTION ──

    fun isRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
            "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su",
            "/su/bin/su", "/system/xbin/daemonsu",
            "/system/etc/init.d/99SuperSUDaemon",
            "/dev/com.koushikdutta.superuser.daemon/"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }

        // Check su binary accessibility
        try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            if (!result.isNullOrBlank()) return true
        } catch (_: Exception) {}

        // Check build tags
        val tags = Build.TAGS
        return tags != null && tags.contains("test-keys")
    }

    // ── EMULATOR DETECTION ──

    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.BOARD == "QC_Reference_Phone"
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HOST.startsWith("Build")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator"))
    }

    // ── OVERLAY / TAPJACKING DETECTION ──

    /** Check if any overlay / draw-over-apps permission is active. */
    fun hasActiveOverlays(context: Context): Boolean {
        return try {
            Settings.canDrawOverlays(context)
        } catch (_: Exception) { false }
    }

    // ── ACCESSIBILITY SNOOPING ──

    /** Get list of active accessibility services (potential keyloggers). */
    fun getActiveAccessibilityServices(context: Context): List<String> {
        val services = mutableListOf<String>()
        try {
            val setting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (!setting.isNullOrBlank()) {
                services.addAll(setting.split(":").filter { it.isNotBlank() })
            }
        } catch (_: Exception) {}
        return services
    }

    // ── WINDOW HARDENING ──

    /** Apply all window-level hardening to an Activity. */
    fun hardenWindow(activity: Activity) {
        // Prevent tapjacking — reject touches when another app's view is on top
        activity.window.decorView.filterTouchesWhenObscured = true
    }

    // ── ERROR SANITIZATION ──

    /**
     * Strip internal details from exception messages before showing to UI.
     * Prevents leaking class names, stack frames, internal paths to screen
     * or to accessibility-based loggers.
     */
    fun sanitizeError(prefix: String, e: Exception): String {
        val msg = e.message ?: "unknown error"
        val cleaned = msg
            .replace(Regex("""[a-z]+(\.[a-z]+){2,}\.[A-Z]\w+"""), "[internal]")
            .replace(Regex("""\([\w.]+:\d+\)"""), "")
            .replace(Regex("""/[\w/.-]+"""), "[path]")
            .replace(Regex("""\bat [^\s]+"""), "")
            .trim()
        val capped = if (cleaned.length > 120) cleaned.take(117) + "..." else cleaned
        return "$prefix: $capped"
    }

    // ── COMPREHENSIVE CHECK ──

    data class ThreatReport(
        val isDebuggerAttached: Boolean,
        val isDebuggable: Boolean,
        val isRooted: Boolean,
        val isEmulator: Boolean,
        val instrumentationThreats: List<String>,
        val overlaysActive: Boolean,
        val accessibilityServices: List<String>
    ) {
        val threatLevel: ThreatLevel get() {
            if (isDebuggerAttached || instrumentationThreats.isNotEmpty()) return ThreatLevel.CRITICAL
            if (isRooted || isDebuggable) return ThreatLevel.HIGH
            if (isEmulator) return ThreatLevel.MEDIUM
            if (overlaysActive || accessibilityServices.isNotEmpty()) return ThreatLevel.LOW
            return ThreatLevel.NONE
        }

        val hasCriticalThreats get() = threatLevel == ThreatLevel.CRITICAL
        val hasHighThreats get() = threatLevel <= ThreatLevel.HIGH && threatLevel != ThreatLevel.NONE

        fun summary(): String = buildString {
            if (isDebuggerAttached) appendLine("\u2022 Debugger attached")
            if (isDebuggable) appendLine("\u2022 Debug build detected")
            if (isRooted) appendLine("\u2022 Root access detected")
            if (isEmulator) appendLine("\u2022 Emulator detected")
            instrumentationThreats.forEach { appendLine("\u2022 Instrumentation: $it") }
            if (overlaysActive) appendLine("\u2022 Screen overlay permission active")
            accessibilityServices.forEach { svc ->
                val short = svc.substringAfterLast("/").substringAfterLast(".")
                appendLine("\u2022 Accessibility: $short")
            }
        }.trimEnd()
    }

    enum class ThreatLevel { NONE, LOW, MEDIUM, HIGH, CRITICAL }

    /** Run all checks and return a threat report. */
    fun scan(context: Context): ThreatReport {
        return ThreatReport(
            isDebuggerAttached = isDebuggerAttached(),
            isDebuggable = isDebuggable(context),
            isRooted = isRooted(),
            isEmulator = isEmulator(),
            instrumentationThreats = detectInstrumentation(),
            overlaysActive = hasActiveOverlays(context),
            accessibilityServices = getActiveAccessibilityServices(context)
        )
    }
}
