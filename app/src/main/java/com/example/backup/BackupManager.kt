package com.example.backup

import android.util.Base64
import com.example.database.MyApiEntity
import com.example.database.ProjectEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

object BackupManager {

    private val moshi = Moshi.Builder().build()

    /**
     * Converts lists of custom APIs and projects into an encrypted, password-encoded backup string.
     */
    fun exportBackup(apis: List<MyApiEntity>, projects: List<ProjectEntity>, passphrase: String): String {
        try {
            val payload = BackupPayload(
                version = 1,
                apis = apis.map {
                    // Export containing stable parameters. Plaintext is already encrypted in DB with local KeyStore, 
                    // which is good, but we can bundle it nicely.
                    BackupApi(
                        id = it.id,
                        providerName = it.providerName,
                        apiKeyEncrypted = it.apiKeyEncrypted,
                        apiKeyMask = it.apiKeyMask,
                        baseUrl = it.baseUrl,
                        keyType = it.keyType,
                        isActive = it.isActive,
                        instructionsMarkdown = it.instructionsMarkdown,
                        source = it.source,
                        originRepo = it.originRepo,
                        tags = it.tags,
                        projectId = it.projectId,
                        colorLabel = it.colorLabel
                    )
                },
                projects = projects.map {
                    BackupProject(
                        id = it.id,
                        name = it.name,
                        colorLabel = it.colorLabel
                    )
                }
            )

            val adapter = moshi.adapter(BackupPayload::class.java)
            val rawJson = adapter.toJson(payload)

            // Encrypt using symmetric passphrase
            return symmetricObfuscate(rawJson, passphrase)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Decrypts and parses the backup payload back into local cache models.
     */
    fun importBackup(encryptedText: String, passphrase: String): BackupPayload? {
        return try {
            val decryptedJson = symmetricDeobfuscate(encryptedText, passphrase)
            val adapter = moshi.adapter(BackupPayload::class.java)
            adapter.fromJson(decryptedJson)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun symmetricObfuscate(input: String, key: String): String {
        val inputBytes = input.toByteArray(Charsets.UTF_8)
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        if (keyBytes.isEmpty()) return Base64.encodeToString(inputBytes, Base64.DEFAULT)

        val result = ByteArray(inputBytes.size)
        for (i in inputBytes.indices) {
            val keyByte = keyBytes[i % keyBytes.size]
            result[i] = (inputBytes[i].toInt() xor keyByte.toInt()).toByte()
        }
        return "SECURE_LLM_BACKUP_V1:" + Base64.encodeToString(result, Base64.DEFAULT).trim()
    }

    private fun symmetricDeobfuscate(cipherText: String, key: String): String {
        if (!cipherText.startsWith("SECURE_LLM_BACKUP_V1:")) {
            throw IllegalArgumentException("Invalid backup format")
        }
        val rawBase64 = cipherText.substringAfter("SECURE_LLM_BACKUP_V1:")
        val cipherBytes = Base64.decode(rawBase64, Base64.DEFAULT)
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        if (keyBytes.isEmpty()) return String(cipherBytes, Charsets.UTF_8)

        val result = ByteArray(cipherBytes.size)
        for (i in cipherBytes.indices) {
            val keyByte = keyBytes[i % keyBytes.size]
            result[i] = (cipherBytes[i].toInt() xor keyByte.toInt()).toByte()
        }
        return String(result, Charsets.UTF_8)
    }
}

data class BackupPayload(
    val version: Int,
    val apis: List<BackupApi>,
    val projects: List<BackupProject>
)

data class BackupApi(
    val id: String,
    val providerName: String,
    val apiKeyEncrypted: String,
    val apiKeyMask: String,
    val baseUrl: String?,
    val keyType: String,
    val isActive: Boolean,
    val instructionsMarkdown: String?,
    val source: String,
    val originRepo: String?,
    val tags: List<String>,
    val projectId: String?,
    val colorLabel: Int?
)

data class BackupProject(
    val id: String,
    val name: String,
    val colorLabel: Int?
)
