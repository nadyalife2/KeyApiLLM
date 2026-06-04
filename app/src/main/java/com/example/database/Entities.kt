package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@Entity(tableName = "ready_keys")
data class ReadyKeyEntity(
    @PrimaryKey val id: String,
    val provider: String,
    val endpoint: String?,
    val apiKeyMasked: String,
    val apiKeyEncryptedTemp: String?,
    val budget: String?,
    val rpm: String?,
    val ttl: String?,
    val lastUpdatedText: String?,
    val models: List<String>,
    val freshnessStatus: String, // "STALE", "OUTDATED", "UNKNOWN", "FRESH"
    val healthStatus: String,    // "UNKNOWN", "HEALTHY", "UNHEALTHY"
    val trustLevel: String,      // "LOW", "MEDIUM", "HIGH"
    val riskLevel: String,       // "LOW", "MEDIUM", "HIGH"
    val sourceRepo: String,
    val rawSectionRef: String?,
    val syncedAt: Long
)

@Entity(tableName = "aggregators")
data class AggregatorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val providersList: List<String>,
    val totalTokens: String?,
    val features: List<String>,
    val setupType: String?,
    val githubUrl: String,
    val lastCommitAt: Long?,
    val endpointBaseUrl: String?,
    val requiresOwnKeys: Boolean,
    val selfHosted: Boolean,
    val openAiCompatible: Boolean,
    val dockerRequired: Boolean,
    val healthStatus: String,
    val syncedAt: Long
)

@Entity(tableName = "own_account_providers")
data class OwnAccountProviderEntity(
    @PrimaryKey val id: String,
    val providerName: String,
    val freeTier: String?,
    val duration: String?,
    val cardRequired: Boolean,
    val registrationUrl: String?,
    val guideUrl: String?,
    val modelsAvailable: List<String>,
    val limits: String?,
    val category: String, // "CHAT", "IMAGE", "TTS", "EMBEDDING", "MULTI", "OTHER"
    val sourceCount: Int,
    val confidenceLevel: String, // "LOW", "MEDIUM", "HIGH"
    val sourceRepos: List<String>,
    val syncedAt: Long
)

@Entity(tableName = "my_apis")
data class MyApiEntity(
    @PrimaryKey val id: String,
    val providerName: String,
    val apiKeyEncrypted: String,
    val apiKeyMask: String,
    val baseUrl: String?,
    val keyType: String, // "CHAT", "EMBEDDING", "IMAGE", "TTS", "MULTI", "OTHER"
    val isActive: Boolean,
    val instructionsMarkdown: String?,
    val source: String, // "MANUAL", "IMPORTED_READY", "IMPORTED_AGGREGATOR"
    val originRepo: String?,
    val dateAdded: Long,
    val dateLastUsed: Long?,
    val tags: List<String>,
    val colorLabel: Int?,
    val projectId: String?,
    val keyFormatValid: Boolean?,
    val healthStatus: String, // "UNKNOWN", "HEALTHY", "UNHEALTHY"
    val healthHttpCode: Int?,
    val healthLatencyMs: Long?,
    val healthCheckedAt: Long?
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorLabel: Int?,
    val createdAt: Long
)

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey val id: String,
    val section: String, // "READY_KEYS", "AGGREGATORS", "OWN_ACCOUNT"
    val startedAt: Long,
    val finishedAt: Long?,
    val status: String, // "SUCCESS", "FAILED"
    val newItems: Int,
    val changedItems: Int,
    val removedItems: Int,
    val errorMessage: String?
)

@Entity(tableName = "parser_warnings")
data class ParserWarningEntity(
    @PrimaryKey val id: String,
    val sourceRepo: String,
    val section: String,
    val warningType: String,
    val message: String,
    val rawFragment: String?,
    val createdAt: Long
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val myApiId: String,
    val action: String, // "CREATED", "DECRYPTED", "UPDATED", "DELETED"
    val createdAt: Long
)

class AppConverters {
    private val moshi = Moshi.Builder().build()
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return stringListAdapter.toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            stringListAdapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
