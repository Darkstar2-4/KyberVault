package com.kybervault.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.kybervault.crypto.HybridKeyBundle
import com.kybervault.crypto.KyberEngine
import com.kybervault.crypto.X25519Engine
import com.kybervault.util.SecureWipe
import org.json.JSONObject
import java.io.File
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ConcurrentHashMap

class EphemeralKeyVault {

    private val keySlots = ConcurrentHashMap<String, KeySlot>()
    private val sessionKeys = ConcurrentHashMap<String, ByteArray>()
    var lastError: String? = null; private set

    fun generateAndStore(alias: String): HybridKeyBundle {
        val bundle = HybridKeyBundle(KyberEngine.generateKeyPair(), X25519Engine.generateKeyPair())
        keySlots.put(alias, KeySlot(bundle))?.bundle?.wipe()
        return bundle
    }

    fun store(alias: String, bundle: HybridKeyBundle) {
        keySlots.put(alias, KeySlot(bundle))?.bundle?.wipe()
    }

    fun get(alias: String): HybridKeyBundle? = keySlots[alias]?.bundle
    fun getKyberPrivateKey(alias: String): PrivateKey? = keySlots[alias]?.bundle?.kyberKeyPair?.private
    fun getX25519PrivateKey(alias: String): PrivateKey? = keySlots[alias]?.bundle?.x25519KeyPair?.private

    fun storeSessionKey(alias: String, key: ByteArray) {
        sessionKeys.put(alias, key.copyOf())?.let { SecureWipe.wipe(it) }
    }
    fun getSessionKey(alias: String): ByteArray? = sessionKeys[alias]?.copyOf()
    fun hasSessionKey(alias: String): Boolean = sessionKeys.containsKey(alias)
    fun hasAnySessionKey(): Boolean = sessionKeys.isNotEmpty()
    fun destroySessionKey(alias: String) { sessionKeys.remove(alias)?.let { SecureWipe.wipe(it) } }

    val size: Int get() = keySlots.size

    fun destroy(alias: String) { keySlots.remove(alias)?.bundle?.wipe(); destroySessionKey(alias) }
    fun destroyAll() {
        keySlots.keys.toList().forEach { destroy(it) }; keySlots.clear()
        sessionKeys.keys.toList().forEach { destroySessionKey(it) }; sessionKeys.clear()
    }

    fun deleteAllPersisted(context: Context) {
        context.filesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("kv_") && file.name.endsWith(".key")) file.delete()
        }
    }

    // ==================== TEMP STATE (background/foreground) ====================

    fun persistTemp(context: Context) {
        try {
            val mk = buildMasterKey(context)
            val aliases = keySlots.keys.toList()
            val meta = JSONObject().apply {
                put("aliases", aliases.joinToString(","))
                aliases.forEach { alias ->
                    sessionKeys[alias]?.let { put("session_$alias", Base64.encodeToString(it, Base64.NO_WRAP)) }
                }
            }
            // Save each alias's keys
            aliases.forEach { alias ->
                val bundle = keySlots[alias]?.bundle ?: return@forEach
                writeTempFile(context, mk, "kvtmp_${alias}_kpub", bundle.kyberKeyPair.public.encoded)
                writeTempFile(context, mk, "kvtmp_${alias}_kprv", bundle.kyberKeyPair.private.encoded)
                writeTempFile(context, mk, "kvtmp_${alias}_xpub", bundle.x25519KeyPair.public.encoded)
                writeTempFile(context, mk, "kvtmp_${alias}_xprv", bundle.x25519KeyPair.private.encoded)
            }
            writeTempFile(context, mk, "kvtmp_meta", meta.toString().toByteArray())
        } catch (_: Exception) {}
    }

    fun restoreTemp(context: Context) {
        try {
            val mk = buildMasterKey(context)
            val metaBytes = readTempFile(context, mk, "kvtmp_meta") ?: return
            val meta = JSONObject(String(metaBytes))
            val aliases = meta.getString("aliases").split(",").filter { it.isNotBlank() }

            val pqcProvider = org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider()
            aliases.forEach { alias ->
                val kpub = readTempFile(context, mk, "kvtmp_${alias}_kpub") ?: return@forEach
                val kprv = readTempFile(context, mk, "kvtmp_${alias}_kprv") ?: return@forEach
                val xpub = readTempFile(context, mk, "kvtmp_${alias}_xpub") ?: return@forEach
                val xprv = readTempFile(context, mk, "kvtmp_${alias}_xprv") ?: return@forEach

                val kf = KeyFactory.getInstance("Kyber", pqcProvider)
                val xf = KeyFactory.getInstance("X25519")
                store(alias, HybridKeyBundle(
                    KeyPair(kf.generatePublic(X509EncodedKeySpec(kpub)), kf.generatePrivate(PKCS8EncodedKeySpec(kprv))),
                    KeyPair(xf.generatePublic(X509EncodedKeySpec(xpub)), xf.generatePrivate(PKCS8EncodedKeySpec(xprv)))
                ))
                SecureWipe.wipeAll(kpub, kprv, xpub, xprv)

                if (meta.has("session_$alias")) {
                    val sk = Base64.decode(meta.getString("session_$alias"), Base64.NO_WRAP)
                    storeSessionKey(alias, sk); SecureWipe.wipe(sk)
                }
            }
            SecureWipe.wipe(metaBytes)
        } catch (_: Exception) {}
    }

    fun deleteTempState(context: Context) {
        context.filesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("kvtmp_")) file.delete()
        }
    }

    // ==================== PERSISTENT STORAGE ====================

    fun persistEncrypted(context: Context, alias: String): Boolean {
        val bundle = keySlots[alias]?.bundle
        if (bundle == null) { lastError = "No keypair for alias '$alias'"; return false }
        return try {
            val mk = buildMasterKey(context)
            listOf("kyber_pub", "kyber_priv", "x25519_pub", "x25519_priv", "session").forEach {
                File(context.filesDir, "kv_${alias}_${it}.key").delete()
            }
            writeEncrypted(context, mk, "kv_${alias}_kyber_pub.key", bundle.kyberKeyPair.public.encoded)
            writeEncrypted(context, mk, "kv_${alias}_kyber_priv.key", bundle.kyberKeyPair.private.encoded)
            writeEncrypted(context, mk, "kv_${alias}_x25519_pub.key", bundle.x25519KeyPair.public.encoded)
            writeEncrypted(context, mk, "kv_${alias}_x25519_priv.key", bundle.x25519KeyPair.private.encoded)
            sessionKeys[alias]?.let { writeEncrypted(context, mk, "kv_${alias}_session.key", it) }
            lastError = null; true
        } catch (e: Exception) {
            lastError = "${e.javaClass.simpleName}: ${e.message}"; false
        }
    }

    fun loadEncrypted(context: Context, alias: String): Boolean {
        return try {
            val mk = buildMasterKey(context)
            val kyberPubBytes = readEncrypted(context, mk, "kv_${alias}_kyber_pub.key") ?: return false
            val kyberPrivBytes = readEncrypted(context, mk, "kv_${alias}_kyber_priv.key") ?: return false
            val x25519PubBytes = readEncrypted(context, mk, "kv_${alias}_x25519_pub.key") ?: return false
            val x25519PrivBytes = readEncrypted(context, mk, "kv_${alias}_x25519_priv.key") ?: return false

            val pqcProvider = org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider()
            val kf = KeyFactory.getInstance("Kyber", pqcProvider)
            val xf = KeyFactory.getInstance("X25519")
            store(alias, HybridKeyBundle(
                KeyPair(kf.generatePublic(X509EncodedKeySpec(kyberPubBytes)), kf.generatePrivate(PKCS8EncodedKeySpec(kyberPrivBytes))),
                KeyPair(xf.generatePublic(X509EncodedKeySpec(x25519PubBytes)), xf.generatePrivate(PKCS8EncodedKeySpec(x25519PrivBytes)))
            ))
            readEncrypted(context, mk, "kv_${alias}_session.key")?.let { storeSessionKey(alias, it); SecureWipe.wipe(it) }
            SecureWipe.wipeAll(kyberPubBytes, kyberPrivBytes, x25519PubBytes, x25519PrivBytes)
            lastError = null; true
        } catch (e: Exception) {
            lastError = "${e.javaClass.simpleName}: ${e.message}"; false
        }
    }

    private fun buildMasterKey(context: Context): MasterKey =
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setRequestStrongBoxBacked(true).build()

    private fun writeEncrypted(context: Context, mk: MasterKey, filename: String, data: ByteArray) {
        val file = File(context.filesDir, filename); file.delete()
        EncryptedFile.Builder(context, file, mk, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
            .build().openFileOutput().use { it.write(data) }
    }

    private fun readEncrypted(context: Context, mk: MasterKey, filename: String): ByteArray? {
        val file = File(context.filesDir, filename)
        if (!file.exists()) return null
        return EncryptedFile.Builder(context, file, mk, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
            .build().openFileInput().use { it.readBytes() }
    }

    private fun writeTempFile(context: Context, mk: MasterKey, name: String, data: ByteArray) {
        val file = File(context.filesDir, name); file.delete()
        EncryptedFile.Builder(context, file, mk, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
            .build().openFileOutput().use { it.write(data) }
    }

    private fun readTempFile(context: Context, mk: MasterKey, name: String): ByteArray? {
        val file = File(context.filesDir, name)
        if (!file.exists()) return null
        return EncryptedFile.Builder(context, file, mk, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
            .build().openFileInput().use { it.readBytes() }
    }

    private data class KeySlot(val bundle: HybridKeyBundle)
}
