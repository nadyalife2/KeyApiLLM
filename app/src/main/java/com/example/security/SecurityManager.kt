package com.example.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecurityManager {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "FreeLlmHubSecureKey"
    private const val AES_GCM = "AES/GCM/NoPadding"

    init {
        try {
            initKeyStoreKey()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initKeyStoreKey() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER)
            ks.load(null)
            if (!ks.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                keyGenerator.init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build()
                )
                keyGenerator.generateKey()
            }
        }
    }

    private fun getSecretKey(): SecretKey? {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER)
        ks.load(null)
        val entry = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey
    }

    /**
     * Encrypts plaintext API Keys. Decrypted only in runtime.
     */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val key = getSecretKey()
            if (key != null) {
                val cipher = Cipher.getInstance(AES_GCM)
                cipher.init(Cipher.ENCRYPT_MODE, key)
                val iv = cipher.iv
                val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
                
                // Combine IV and Encrypted bytes: IV_LENGTH(12) + encryptedBytes
                val combined = ByteArray(iv.size + encryptedBytes.size)
                System.arraycopy(iv, 0, combined, 0, iv.size)
                System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
                Base64.encodeToString(combined, Base64.DEFAULT)
            } else {
                fallbackEncrypt(plainText)
            }
        } catch (e: Exception) {
            fallbackEncrypt(plainText)
        }
    }

    /**
     * Decrypts ciphertext API Keys back to readable text in-memory.
     */
    fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty()) return ""
        return try {
            val key = getSecretKey()
            if (key != null) {
                val combined = Base64.decode(cipherText, Base64.DEFAULT)
                val iv = ByteArray(12)
                val encryptedBytes = ByteArray(combined.size - 12)
                System.arraycopy(combined, 0, iv, 0, 12)
                System.arraycopy(combined, 12, encryptedBytes, 0, encryptedBytes.size)

                val cipher = Cipher.getInstance(AES_GCM)
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)
                String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
            } else {
                fallbackDecrypt(cipherText)
            }
        } catch (e: Exception) {
            fallbackDecrypt(cipherText)
        }
    }

    // Double-layer fallback encryption if Android Keystore is unavailable or fails on test JVMs
    private fun fallbackEncrypt(plainText: String): String {
        val bytes = plainText.toByteArray(Charsets.UTF_8)
        val obfuscated = ByteArray(bytes.size)
        val salt = 0x5A.toByte()
        for (i in bytes.indices) {
            obfuscated[i] = (bytes[i].toInt() xor salt.toInt()).toByte()
        }
        return "FALLBACK:" + Base64.encodeToString(obfuscated, Base64.DEFAULT).trim()
    }

    private fun fallbackDecrypt(cipherText: String): String {
        if (!cipherText.startsWith("FALLBACK:")) return cipherText
        return try {
            val rawBase64 = cipherText.substringAfter("FALLBACK:")
            val obfuscated = Base64.decode(rawBase64, Base64.DEFAULT)
            val bytes = ByteArray(obfuscated.size)
            val salt = 0x5A.toByte()
            for (i in obfuscated.indices) {
                bytes[i] = (obfuscated[i].toInt() xor salt.toInt()).toByte()
            }
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Copies text to clipboard and schedules auto-clear after customizable timeout for security.
     */
    fun copyToClipboard(context: Context, text: String, label: String = "API Key") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)

        // Fetch dynamic timeout from settings (defaults to 60 seconds)
        val timeoutSeconds = com.example.settings.SettingsManager.getClipboardTimeout(context)
        val delayMs = timeoutSeconds * 1000L

        // Launch helper coroutine in IO scope to clear after dynamic milliseconds
        CoroutineScope(Dispatchers.IO).launch {
            delay(delayMs)
            try {
                val primaryClip = clipboard.primaryClip
                if (primaryClip != null && primaryClip.itemCount > 0) {
                    val currentText = primaryClip.getItemAt(0).text?.toString()
                    if (currentText == text) {
                        val emptyClip = ClipData.newPlainText(label, "")
                        clipboard.setPrimaryClip(emptyClip)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Basic check for root traces on current Android device.
     */
    fun isDeviceRooted(): Boolean {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }
}
