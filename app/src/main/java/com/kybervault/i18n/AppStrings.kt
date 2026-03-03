package com.kybervault.i18n

import androidx.compose.runtime.compositionLocalOf

/** Every user-facing string in the app. Each language file implements this. */
data class AppStrings(
    // ── App / Nav ──
    val appName: String,
    val tabKeygen: String,
    val tabExchange: String,
    val tabMessage: String,
    val info: String,
    val settings: String,
    val close: String,
    val done: String,
    val cancel: String,
    val save: String,
    val copy: String,
    val share: String,
    val expand: String,

    // ── KeyGen Screen ──
    val keyGenerator: String,
    val keyAlias: String,
    val keyAliasPlaceholder: String,
    val exchangeMode: String,
    val hybridTitle: String,
    val hybridSubtitle: String,
    val hybridBadge: String,
    val kyberTitle: String,
    val kyberSubtitle: String,
    val kyberBadge: String,
    val generateHybridKeypair: String,
    val generateKyberKeypair: String,
    val generateKeypair: String,
    val generating: String,
    val recallSavedKeys: String,
    val recallDescription: String,
    val aliasToLoad: String,
    val recallKeys: String,
    val activeAlias: (String) -> String,
    val publicKeysSafe: String,
    val privateKeys: String,
    val hide: String,
    val reveal: String,
    val kyberPublicKey: String,
    val x25519PublicKey: String,
    val kyberPrivateKey: String,
    val x25519PrivateKey: String,
    val wipe: String,
    val wipeStorage: String,
    val keygenTip: String,

    // ── Exchange Screen ──
    val keyExchange: String,
    val kemSubtitle: String,
    val sessionKeyActive: String,
    val sessionKey: String,
    val sessionKeyDescription: String,
    val generateAes256: String,
    val pasteAesKey: String,
    val setCustomKey: String,
    val kemExchange: String,
    val send: String,
    val receive: String,
    val sendInstructions: String,
    val hybrid: String,
    val kyberOnly: String,
    val recipientKyberPub: String,
    val recipientX25519Pub: String,
    val importRecipientKey: String,
    val messageOptional: String,
    val leaveBlankForSession: String,
    val establishSession: String,
    val sendEncrypted: String,
    val encrypting: String,
    val kemCiphertext: String,
    val receiveInstructions: String,
    val kemCiphertextPlaceholder: String,
    val generatedKey: String,
    val customKey: String,
    val kyberPrivBase64: String,
    val x25519PrivBase64: String,
    val decryptDeriveKey: String,
    val decrypting: String,
    val decryptedMessage: String,
    val sessionKeySaved: String,

    // ── Message Screen ──
    val secureMessage: String,
    val aesSubtitle: String,
    val noSessionKey: String,
    val sessionReady: String,
    val encrypt: String,
    val decrypt: String,
    val messagePlaceholder: String,
    val ciphertext: String,
    val ciphertextPlaceholder: String,
    val encrypted: String,
    val decrypted: String,
    val messageTip: String,

    // ── Settings ──
    val security: String,
    val screenshotProtection: String,
    val screenshotDescription: String,
    val wipeOnClose: String,
    val wipeOnCloseDescription: String,
    val encryption: String,
    val wireFormatHeader: String,
    val wireFormatDescription: String,
    val wireFormatNote: String,
    val display: String,
    val skipCopyWarning: String,
    val skipCopyDescription: String,
    val keyEncoding: String,
    val base64: String,
    val hex: String,
    val accessibility: String,
    val openDyslexicFont: String,
    val openDyslexicDescription: String,
    val language: String,

    // ── Info Dialog ──
    val whatIsKyberVault: String,
    val whatIsDescription: String,
    val whyPostQuantum: String,
    val whyPqDescription: String,
    val hybridMode: String,
    val hybridDescription: String,
    val exchangeFlow: String,
    val exchangeFlowDescription: String,
    val wireFormat: String,
    val wireFormatInfo: String,
    val securityModel: String,
    val securityModelInfo: String,
    val keySizes: String,
    val keySizesInfo: String,
    val clipboardSafety: String,
    val clipboardSafetyInfo: String,
    val supportDevelopment: String,
    val supportDescription: String,
    val btcLabel: String,
    val freeOpenSource: String,

    // ── Wipe Dialog ──
    val wipeKeys: String,
    val ramOnly: String,
    val ramOnlyDescription: String,
    val ramAndStorage: String,
    val ramAndStorageDescription: String,

    // ── Save Warning ──
    val saveToDevice: String,
    val saveWarningBody: String,
    val saveRecallNote: String,

    // ── Clipboard Warning ──
    val clipboardWarning: String,
    val clipboardWarningBody: String,
    val clipboardBullet1: String,
    val clipboardBullet2: String,
    val clipboardBullet3: String,
    val clipboardAdvice: String,
    val dontShowAgain: String,
    val copying: (String) -> String,
    val copied: String,
    val copiedTip: String,

    // ── QR Dialog ──
    val qrCode: String,
    val qrScanTip: String,
    val qrTooLarge: (Int) -> String,

    // ── KeyStatusBar ──
    val aliasesInRam: (Int) -> String,

    // ── ViewModel status messages ──
    val statusKeypairGenerated: String,
    val statusRecipientImported: String,
    val statusSessionEstablished: String,
    val statusSessionDerived: String,
    val statusAesGenerated: String,
    val statusCustomKeySet: String,
    val statusEncrypted: String,
    val statusDecrypted: String,
    val statusSaved: String,
    val statusRecalled: String,
    val statusRamWiped: String,
    val statusAllDestroyed: String,
    val noKeysInMemory: String,
)

val LocalStrings = compositionLocalOf { EnStrings }

/** All available languages: code → display name. */
val AvailableLanguages = listOf(
    "en" to "English",
    "es" to "Español",
    "fr" to "Français",
    "de" to "Deutsch",
    "it" to "Italiano",
    "ro" to "Română",
    "pl" to "Polski",
    "fi" to "Suomi",
    "sv" to "Svenska",
    "ru" to "Русский",
    "ja" to "日本語",
    "ko" to "한국어",
    "hi" to "हिन्दी",
    "vi" to "Tiếng Việt",
    "he" to "עברית",
    "ar" to "العربية",
    "fa" to "فارسی",
    "br" to "British Posh",
    "rn" to "Redneck",
)

/** Resolve language code to AppStrings instance. */
fun getStrings(lang: String): AppStrings = when (lang) {
    "es" -> EsStrings
    "fr" -> FrStrings
    "de" -> DeStrings
    "it" -> ItStrings
    "ro" -> RoStrings
    "pl" -> PlStrings
    "fi" -> FiStrings
    "sv" -> SvStrings
    "ru" -> RuStrings
    "ja" -> JaStrings
    "ko" -> KoStrings
    "hi" -> HiStrings
    "vi" -> ViStrings
    "he" -> HeStrings
    "ar" -> ArStrings
    "fa" -> FaStrings
    "br" -> BrStrings
    "rn" -> RnStrings
    else -> EnStrings
}
