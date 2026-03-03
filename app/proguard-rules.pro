# Bouncy Castle — only keep classes actually used by KyberVault
-keep class org.bouncycastle.jce.provider.BouncyCastleProvider { *; }
-keep class org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider { *; }
-keep class org.bouncycastle.pqc.jcajce.provider.kyber.** { *; }
-keep class org.bouncycastle.pqc.crypto.crystals.kyber.** { *; }
-keep class org.bouncycastle.jcajce.provider.asymmetric.x25519.** { *; }
-keep class org.bouncycastle.jcajce.provider.asymmetric.edec.** { *; }
-keep class org.bouncycastle.jce.spec.** { *; }
-keep class org.bouncycastle.asn1.** { *; }
-dontwarn org.bouncycastle.**

# ZXing QR — only keep encoding classes
-keep class com.google.zxing.BarcodeFormat { *; }
-keep class com.google.zxing.EncodeHintType { *; }
-keep class com.google.zxing.WriterException { *; }
-keep class com.google.zxing.common.BitMatrix { *; }
-keep class com.google.zxing.qrcode.QRCodeWriter { *; }
-keep class com.google.zxing.qrcode.decoder.ErrorCorrectionLevel { *; }
-dontwarn com.google.zxing.**

# KyberVault crypto
-keep class com.kybervault.crypto.** { *; }

# Key vault
-keepclassmembers class com.kybervault.data.EphemeralKeyVault { *; }

# Google Tink (pulled by androidx.security:security-crypto) — optional deps not used at runtime
-dontwarn com.google.api.client.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.joda.time.**

# ── ANTI-SNIFFER / ANTI-LOGGER HARDENING ──

# Strip ALL Log calls in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Strip System.out/err (prevent accidental println leaks)
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# Strip Throwable.printStackTrace (stack trace leak prevention)
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
    public void printStackTrace(java.io.PrintStream);
    public void printStackTrace(java.io.PrintWriter);
}

# Obfuscate everything not explicitly kept
-repackageclasses ''
-allowaccessmodification
-overloadaggressively

# Remove source file and line number info (prevents stack trace leaks)
-renamesourcefileattribute ''
-keepattributes Exceptions

# Keep security hardening (needs reflection for some checks)
-keep class com.kybervault.security.SecurityHardening { *; }
-keep class com.kybervault.security.SecurityHardening$ThreatReport { *; }
-keep class com.kybervault.security.SecurityHardening$ThreatLevel { *; }

# Biometric
-keep class com.kybervault.security.BiometricHelper { *; }