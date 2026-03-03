package com.kybervault.ui

import android.app.Application
import android.content.Context
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kybervault.KyberVaultApp
import com.kybervault.crypto.AesEngine
import com.kybervault.crypto.CryptoFacade
import com.kybervault.crypto.EncryptedPayload
import com.kybervault.crypto.EncryptionMode
import com.kybervault.i18n.getStrings
import com.kybervault.security.SecurityHardening
import com.kybervault.security.TotpEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import androidx.core.content.edit

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val vault get() = (getApplication<KyberVaultApp>()).keyVault
    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    /** Get current i18n strings based on current language setting. */
    private val s get() = getStrings(_uiState.value.language)

    private var recipientKyberPubKey: PublicKey? = null
    private var recipientX25519PubKey: PublicKey? = null

    private val prefs get() = getApplication<KyberVaultApp>()
        .getSharedPreferences("kybervault_prefs", Context.MODE_PRIVATE)

    init {
        _uiState.update {
            it.copy(
                hideCopyWarning = prefs.getBoolean("hide_copy_warning", false),
                flagSecure = prefs.getBoolean("flag_secure", true),
                wipeOnExit = prefs.getBoolean("wipe_on_exit", true),
                displayEncoding = prefs.getString("display_encoding", "base64") ?: "base64",
                dyslexicFont = prefs.getBoolean("dyslexic_font", false),
                useWireFormat = prefs.getBoolean("use_wire_format", true),
                language = prefs.getString("language", "en") ?: "en",
                requireBiometric = prefs.getBoolean("require_biometric", false),
                requirePin = prefs.getBoolean("require_pin", false),
                hasPin = prefs.getString("pin_hash", null) != null,
                requireTotp = prefs.getBoolean("require_totp", false),
                hasTotp = prefs.getString("totp_secret", null) != null
            )
        }
        // Restore state from temp if vault has keys from auto-restore
        refreshStateFromVault()
        // Set default status if still empty
        if (_uiState.value.statusMessage.isBlank()) {
            _uiState.update { it.copy(statusMessage = s.noKeysInMemory) }
        }
    }

    private fun refreshStateFromVault() {
        val alias = _uiState.value.activeAlias
        val bundle = vault.get(alias) ?: return
        val sessionKeyB64 = vault.getSessionKey(alias)?.let {
            val b = Base64.encodeToString(it, Base64.NO_WRAP)
            com.kybervault.util.SecureWipe.wipe(it); b
        } ?: ""
        _uiState.update {
            it.copy(
                hasActiveKey = true, activeAlias = alias,
                kyberPubKeyFull = Base64.encodeToString(bundle.kyberKeyPair.public.encoded, Base64.NO_WRAP),
                kyberPrivKeyFull = Base64.encodeToString(bundle.kyberKeyPair.private.encoded, Base64.NO_WRAP),
                x25519PubKeyFull = Base64.encodeToString(bundle.x25519KeyPair.public.encoded, Base64.NO_WRAP),
                x25519PrivKeyFull = Base64.encodeToString(bundle.x25519KeyPair.private.encoded, Base64.NO_WRAP),
                keyCount = vault.size, hasSessionKey = vault.hasSessionKey(alias),
                generatedAesKey = sessionKeyB64,
                statusMessage = s.statusRecalled
            )
        }
    }

    // ==================== SETTINGS ====================

    fun setFlagSecure(enabled: Boolean) {
        prefs.edit { putBoolean("flag_secure", enabled)}
        _uiState.update { it.copy(flagSecure = enabled) }
    }
    fun setWipeOnExit(enabled: Boolean) {
        prefs.edit { putBoolean("wipe_on_exit", enabled)}
        _uiState.update { it.copy(wipeOnExit = enabled) }
    }
    fun setHideCopyWarning(hide: Boolean) {
        prefs.edit { putBoolean("hide_copy_warning", hide)}
        _uiState.update { it.copy(hideCopyWarning = hide) }
    }
    fun setDisplayEncoding(encoding: String) {
        prefs.edit { putString("display_encoding", encoding)}
        _uiState.update { it.copy(displayEncoding = encoding) }
    }
    fun setDyslexicFont(enabled: Boolean) {
        prefs.edit { putBoolean("dyslexic_font", enabled)}
        _uiState.update { it.copy(dyslexicFont = enabled) }
    }
    fun setUseWireFormat(enabled: Boolean) {
        prefs.edit { putBoolean("use_wire_format", enabled)}
        _uiState.update { it.copy(useWireFormat = enabled) }
    }
    fun setLanguage(lang: String) {
        prefs.edit {putString("language", lang)}
        _uiState.update { it.copy(language = lang) }
    }
    fun setRequireBiometric(enabled: Boolean) {
        if (!enabled) {
            // Disabling biometric = delete all persisted keys (RAM keys survive)
            vault.deleteAllPersisted(getApplication())
        }
        prefs.edit { putBoolean("require_biometric", enabled)}
        _uiState.update { it.copy(requireBiometric = enabled) }
    }

    // ── PIN CODE ──

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun setPin(pin: String) {
        prefs.edit { putString("pin_hash", hashPin(pin)) }
        prefs.edit {putBoolean("require_pin", true)}
        _uiState.update { it.copy(requirePin = true, hasPin = true) }
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString("pin_hash", null) ?: return false
        return hashPin(pin) == stored
    }

    fun disablePin() {
        vault.deleteAllPersisted(getApplication())
        prefs.edit { remove("pin_hash").remove("require_pin")}
        _uiState.update { it.copy(requirePin = false, hasPin = false) }
    }

    // ── TOTP ──

    fun enableTotp(secret: ByteArray) {
        prefs.edit {
            putString("totp_secret", TotpEngine.secretToStorage(secret))
                .putBoolean("require_totp", true)
            }
        _uiState.update { it.copy(requireTotp = true, hasTotp = true) }
    }

    fun verifyTotp(code: String): Boolean {
        val stored = prefs.getString("totp_secret", null) ?: return false
        val secret = TotpEngine.secretFromStorage(stored)
        return TotpEngine.verifyCode(secret, code)
    }

    fun disableTotp() {
        vault.deleteAllPersisted(getApplication())
        prefs.edit { remove("totp_secret").remove("require_totp") }
        _uiState.update { it.copy(requireTotp = false, hasTotp = false) }
    }

    // ==================== TAB 1: KEY GENERATION ====================

    fun generateKeyPair(alias: String = "default") {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            try {
                val bundle = vault.generateAndStore(alias)
                _uiState.update {
                    it.copy(isGenerating = false, hasActiveKey = true, activeAlias = alias,
                        kyberPubKeyFull = Base64.encodeToString(bundle.kyberKeyPair.public.encoded, Base64.NO_WRAP),
                        kyberPrivKeyFull = Base64.encodeToString(bundle.kyberKeyPair.private.encoded, Base64.NO_WRAP),
                        x25519PubKeyFull = Base64.encodeToString(bundle.x25519KeyPair.public.encoded, Base64.NO_WRAP),
                        x25519PrivKeyFull = Base64.encodeToString(bundle.x25519KeyPair.private.encoded, Base64.NO_WRAP),
                        keyCount = vault.size, statusMessage = s.statusKeypairGenerated)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, error = SecurityHardening.sanitizeError("Keygen failed", e)) }
            }
        }
    }

    fun setEncryptionMode(mode: EncryptionMode) {
        if (mode == EncryptionMode.HYBRID || mode == EncryptionMode.KYBER)
            _uiState.update { it.copy(encryptionMode = mode) }
    }

    // ==================== TAB 2: KEY EXCHANGE ====================

    fun importRecipientKey(pastedBlock: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val lines = pastedBlock.trim().lines().map { it.trim() }
                var kyberB64: String? = null; var x25519B64: String? = null
                for (line in lines) {
                    when {
                        line.startsWith("Kyber-1024:") -> kyberB64 = line.substringAfter("Kyber-1024:").trim()
                        line.startsWith("X25519:") -> x25519B64 = line.substringAfter("X25519:").trim()
                    }
                }
                if (kyberB64 == null && !pastedBlock.contains("---")) kyberB64 = pastedBlock.trim()
                if (kyberB64 == null) throw IllegalArgumentException("No Kyber-1024 public key found")

                val pqcProvider = BouncyCastlePQCProvider()
                recipientKyberPubKey = KeyFactory.getInstance("Kyber", pqcProvider)
                    .generatePublic(X509EncodedKeySpec(Base64.decode(kyberB64, Base64.NO_WRAP)))
                if (x25519B64 != null) {
                    recipientX25519PubKey = KeyFactory.getInstance("X25519")
                        .generatePublic(X509EncodedKeySpec(Base64.decode(x25519B64, Base64.NO_WRAP)))
                }
                _uiState.update {
                    it.copy(hasRecipientKey = true, error = null,
                        recipientKeyPreview = "Kyber: ${kyberB64.take(24)}\u2026" +
                                if (x25519B64 != null) " + X25519" else "",
                        statusMessage = s.statusRecipientImported)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(hasRecipientKey = false, error = SecurityHardening.sanitizeError("Import failed", e)) }
            }
        }
    }

    fun sendKemExchange(message: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val mode = _uiState.value.encryptionMode
                val recKyberPub = recipientKyberPubKey ?: throw IllegalStateException("Import recipient key first")
                val kemCiphertext: ByteArray; val x25519EphPub: ByteArray?; val kemSharedSecret: ByteArray

                when (mode) {
                    EncryptionMode.HYBRID -> {
                        val recX25519Pub = recipientX25519PubKey ?: throw IllegalStateException("Recipient X25519 key missing")
                        val r = com.kybervault.crypto.HybridKem.encapsulate(recKyberPub, recX25519Pub)
                        kemCiphertext = r.kyberCiphertext; x25519EphPub = r.x25519EphemeralPubKey
                        kemSharedSecret = r.combinedSecret.copyOf(); r.wipeCombinedSecret()
                    }
                    EncryptionMode.KYBER -> {
                        val r = com.kybervault.crypto.KyberEngine.encapsulate(recKyberPub)
                        kemCiphertext = r.ciphertext; x25519EphPub = null
                        kemSharedSecret = r.sharedSecret.copyOf(); r.wipeSecret()
                    }
                    EncryptionMode.AES -> throw IllegalStateException("AES is for messaging")
                }

                val currentAesKey = _uiState.value.generatedAesKey
                val plaintextToEncrypt = when {
                    message.isNotBlank() -> message
                    currentAesKey.isNotBlank() -> "AES256_SESSION_KEY:$currentAesKey"
                    else -> "[KEY_EXCHANGE_ONLY]"
                }

                val aad = when (mode) {
                    EncryptionMode.HYBRID -> "KYBER1024-X25519-HYBRID-AES256GCM"
                    EncryptionMode.KYBER -> "KYBER1024-AES256GCM"; else -> ""
                }.toByteArray(Charsets.UTF_8)
                val encData = AesEngine.encrypt(plaintextToEncrypt.toByteArray(Charsets.UTF_8), kemSharedSecret, aad)
                val alias = _uiState.value.activeAlias
                if (!vault.hasSessionKey(alias)) vault.storeSessionKey(alias, kemSharedSecret)
                com.kybervault.util.SecureWipe.wipe(kemSharedSecret)

                val payload = EncryptedPayload(kemCiphertext, x25519EphPub, encData, mode, 2)
                val aesKeyB64 = if (currentAesKey.isBlank()) {
                    vault.getSessionKey(alias)?.let { val b = Base64.encodeToString(it, Base64.NO_WRAP); com.kybervault.util.SecureWipe.wipe(it); b } ?: ""
                } else currentAesKey

                _uiState.update {
                    it.copy(isProcessing = false, hasSessionKey = true, lastCiphertext = payload.serialize(it.useWireFormat),
                        generatedAesKey = aesKeyB64, statusMessage = s.statusSessionEstablished)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = SecurityHardening.sanitizeError("Send failed", e)) }
            }
        }
    }

    fun receiveKemExchange(ciphertextSerialized: String, customKyberPrivB64: String = "", customX25519PrivB64: String = "") {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val alias = _uiState.value.activeAlias
                val payload = EncryptedPayload.deserialize(ciphertextSerialized.trim())
                val kyberPriv: PrivateKey; val x25519Priv: PrivateKey?

                if (customKyberPrivB64.isNotBlank()) {
                    val pqcProvider = BouncyCastlePQCProvider()
                    kyberPriv = KeyFactory.getInstance("Kyber", pqcProvider)
                        .generatePrivate(PKCS8EncodedKeySpec(Base64.decode(customKyberPrivB64.trim(), Base64.NO_WRAP)))
                    x25519Priv = if (customX25519PrivB64.isNotBlank()) KeyFactory.getInstance("X25519")
                        .generatePrivate(PKCS8EncodedKeySpec(Base64.decode(customX25519PrivB64.trim(), Base64.NO_WRAP)))
                    else vault.getX25519PrivateKey(alias)
                } else {
                    kyberPriv = vault.getKyberPrivateKey(alias) ?: throw IllegalStateException("No private key — generate or paste one")
                    x25519Priv = vault.getX25519PrivateKey(alias)
                }

                val sessionKey: ByteArray = when (payload.mode) {
                    EncryptionMode.HYBRID -> {
                        val xPriv = x25519Priv ?: throw IllegalStateException("Hybrid requires X25519 private key")
                        val xEph = payload.x25519EphemeralPubKey ?: throw IllegalStateException("Missing ephemeral key")
                        com.kybervault.crypto.HybridKem.decapsulate(payload.kemCiphertext, xEph, kyberPriv, xPriv)
                    }
                    EncryptionMode.KYBER -> com.kybervault.crypto.KyberEngine.decapsulate(kyberPriv, payload.kemCiphertext)
                    EncryptionMode.AES -> throw IllegalStateException("AES payload — use Message tab")
                }

                var decryptedMsg = ""; var finalSessionKey = sessionKey
                if (payload.encryptedData.isNotEmpty()) {
                    val aad = when (payload.mode) {
                        EncryptionMode.HYBRID -> "KYBER1024-X25519-HYBRID-AES256GCM"
                        EncryptionMode.KYBER -> "KYBER1024-AES256GCM"; else -> ""
                    }.toByteArray(Charsets.UTF_8)
                    val raw = String(AesEngine.decrypt(payload.encryptedData, sessionKey, aad), Charsets.UTF_8)
                    when {
                        raw == "[KEY_EXCHANGE_ONLY]" -> {}
                        raw.startsWith("AES256_SESSION_KEY:") -> {
                            decryptedMsg = "(Received AES-256 session key)"
                            finalSessionKey = Base64.decode(raw.substringAfter("AES256_SESSION_KEY:"), Base64.NO_WRAP)
                        }
                        else -> decryptedMsg = raw
                    }
                }

                vault.storeSessionKey(alias, finalSessionKey)
                val aesKeyB64 = Base64.encodeToString(vault.getSessionKey(alias)!!, Base64.NO_WRAP)
                com.kybervault.util.SecureWipe.wipe(sessionKey)
                if (finalSessionKey !== sessionKey) com.kybervault.util.SecureWipe.wipe(finalSessionKey)

                _uiState.update {
                    it.copy(isProcessing = false, hasSessionKey = true,
                        lastDecryptedText = decryptedMsg.ifEmpty { "(Key exchange only)" },
                        generatedAesKey = aesKeyB64, statusMessage = s.statusSessionDerived)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = SecurityHardening.sanitizeError("Receive failed", e)) }
            }
        }
    }

    // ==================== SESSION KEY ====================

    fun generateSessionKey() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val alias = _uiState.value.activeAlias
                val key = ByteArray(32); java.security.SecureRandom().nextBytes(key)
                vault.storeSessionKey(alias, key)
                val keyB64 = Base64.encodeToString(key, Base64.NO_WRAP)
                com.kybervault.util.SecureWipe.wipe(key)
                _uiState.update { it.copy(hasSessionKey = true, generatedAesKey = keyB64, statusMessage = s.statusAesGenerated) }
            } catch (e: Exception) { _uiState.update { it.copy(error = SecurityHardening.sanitizeError("Key gen failed", e)) } }
        }
    }

    fun setSessionKeyManual(keyB64: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val alias = _uiState.value.activeAlias
                val keyBytes = Base64.decode(keyB64.trim(), Base64.NO_WRAP)
                if (keyBytes.size != 32) throw IllegalArgumentException("Must be 32 bytes (got ${keyBytes.size})")
                vault.storeSessionKey(alias, keyBytes)
                com.kybervault.util.SecureWipe.wipe(keyBytes)
                _uiState.update { it.copy(hasSessionKey = true, generatedAesKey = keyB64.trim(), statusMessage = s.statusCustomKeySet) }
            } catch (e: Exception) { _uiState.update { it.copy(error = SecurityHardening.sanitizeError("Invalid key", e)) } }
        }
    }

    // ==================== TAB 3: AES MESSAGING ====================

    fun encryptAes(plaintext: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val key = vault.getSessionKey(_uiState.value.activeAlias) ?: throw IllegalStateException("No session key")
                val payload = CryptoFacade.encryptAes(plaintext, key)
                com.kybervault.util.SecureWipe.wipe(key)
                _uiState.update { it.copy(isProcessing = false, lastCiphertext = payload.serialize(it.useWireFormat), statusMessage = s.statusEncrypted) }
            } catch (e: Exception) { _uiState.update { it.copy(isProcessing = false, error = SecurityHardening.sanitizeError("Encrypt", e)) } }
        }
    }

    fun decryptAes(ciphertextSerialized: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val key = vault.getSessionKey(_uiState.value.activeAlias) ?: throw IllegalStateException("No session key")
                val input = ciphertextSerialized.trim()
                val payload = try { EncryptedPayload.deserialize(input) } catch (_: Exception) {
                    // Fallback: treat as raw Base64 AES ciphertext (no wire format header)
                    EncryptedPayload(ByteArray(0), null, Base64.decode(input, Base64.NO_WRAP), EncryptionMode.AES, 2)
                }
                val plaintext = CryptoFacade.decryptAes(payload, key)
                com.kybervault.util.SecureWipe.wipe(key)
                _uiState.update { it.copy(isProcessing = false, lastDecryptedText = plaintext, statusMessage = s.statusDecrypted) }
            } catch (e: Exception) { _uiState.update { it.copy(isProcessing = false, error = SecurityHardening.sanitizeError("Decrypt", e)) } }
        }
    }

    // ==================== MANAGEMENT ====================

    fun togglePrivateKeyVisibility() { _uiState.update { it.copy(showPrivateKeys = !it.showPrivateKeys) } }

    fun wipeRamOnly() {
        vault.destroyAll(); recipientKyberPubKey = null; recipientX25519PubKey = null
        _uiState.update { VaultUiState(statusMessage = s.statusRamWiped,
            hideCopyWarning = it.hideCopyWarning, flagSecure = it.flagSecure, wipeOnExit = it.wipeOnExit,
            displayEncoding = it.displayEncoding, dyslexicFont = it.dyslexicFont, useWireFormat = it.useWireFormat, language = it.language, requireBiometric = it.requireBiometric, requirePin = it.requirePin, hasPin = it.hasPin, requireTotp = it.requireTotp, hasTotp = it.hasTotp) }
    }

    fun wipeRamAndStorage() {
        vault.destroyAll(); recipientKyberPubKey = null; recipientX25519PubKey = null
        vault.deleteAllPersisted(getApplication())
        vault.deleteTempState(getApplication())
        _uiState.update { VaultUiState(statusMessage = s.statusAllDestroyed,
            hideCopyWarning = it.hideCopyWarning, flagSecure = it.flagSecure, wipeOnExit = it.wipeOnExit,
            displayEncoding = it.displayEncoding, dyslexicFont = it.dyslexicFont, useWireFormat = it.useWireFormat, language = it.language, requireBiometric = it.requireBiometric, requirePin = it.requirePin, hasPin = it.hasPin, requireTotp = it.requireTotp, hasTotp = it.hasTotp) }
    }

    fun showSaveWarning() { _uiState.update { it.copy(showSaveWarningDialog = true) } }
    fun dismissSaveWarning() { _uiState.update { it.copy(showSaveWarningDialog = false) } }

    fun saveKeyToStorage(alias: String = "default") {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val ok = vault.persistEncrypted(getApplication(), alias)
                _uiState.update {
                    if (ok) it.copy(statusMessage = s.statusSaved)
                    else it.copy(error = "Save failed")
                }
            } catch (e: Exception) { _uiState.update { it.copy(error = SecurityHardening.sanitizeError("Save crashed", e)) } }
        }
    }

    fun loadKeyFromStorage(alias: String = "default") {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val ok = vault.loadEncrypted(getApplication(), alias)
                if (ok) {
                    val bundle = vault.get(alias)
                    val sessionKeyB64 = vault.getSessionKey(alias)?.let {
                        val b = Base64.encodeToString(it, Base64.NO_WRAP); com.kybervault.util.SecureWipe.wipe(it); b
                    } ?: ""
                    _uiState.update {
                        it.copy(hasActiveKey = true, activeAlias = alias,
                            kyberPubKeyFull = bundle?.kyberKeyPair?.public?.encoded?.let { b -> Base64.encodeToString(b, Base64.NO_WRAP) } ?: "",
                            kyberPrivKeyFull = bundle?.kyberKeyPair?.private?.encoded?.let { b -> Base64.encodeToString(b, Base64.NO_WRAP) } ?: "",
                            x25519PubKeyFull = bundle?.x25519KeyPair?.public?.encoded?.let { b -> Base64.encodeToString(b, Base64.NO_WRAP) } ?: "",
                            x25519PrivKeyFull = bundle?.x25519KeyPair?.private?.encoded?.let { b -> Base64.encodeToString(b, Base64.NO_WRAP) } ?: "",
                            keyCount = vault.size, hasSessionKey = vault.hasSessionKey(alias),
                            generatedAesKey = sessionKeyB64, statusMessage = s.statusRecalled)
                    }
                } else {
                    _uiState.update { it.copy(error = "No saved keys for '$alias'") }
                }
            } catch (e: Exception) { _uiState.update { it.copy(error = SecurityHardening.sanitizeError("Load failed", e)) } }
        }
    }

    // Do NOT wipe on onCleared — ViewModel survives activity recreation.
    // Wipe is handled by WipeService.onTaskRemoved when user swipes from recents.
}

data class VaultUiState(
    val isGenerating: Boolean = false, val isProcessing: Boolean = false,
    val hasActiveKey: Boolean = false, val hasSessionKey: Boolean = false,
    val hasRecipientKey: Boolean = false, val recipientKeyPreview: String = "",
    val activeAlias: String = "default",
    val kyberPubKeyFull: String = "", val kyberPrivKeyFull: String = "",
    val x25519PubKeyFull: String = "", val x25519PrivKeyFull: String = "",
    val showPrivateKeys: Boolean = false,
    val encryptionMode: EncryptionMode = EncryptionMode.HYBRID,
    val keyCount: Int = 0,
    val lastCiphertext: String = "", val lastDecryptedText: String = "",
    val generatedAesKey: String = "",
    val statusMessage: String = "", val error: String? = null,
    val showSaveWarningDialog: Boolean = false,
    val hideCopyWarning: Boolean = false, val flagSecure: Boolean = true,
    val wipeOnExit: Boolean = true, val displayEncoding: String = "base64",
    val dyslexicFont: Boolean = false, val useWireFormat: Boolean = true,
    val language: String = "en",
    val requireBiometric: Boolean = false,
    val requirePin: Boolean = false, val hasPin: Boolean = false,
    val requireTotp: Boolean = false, val hasTotp: Boolean = false
)
