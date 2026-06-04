package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupManager
import com.example.database.AppConverters
import com.example.database.AuditLogEntity
import com.example.database.FreeLlmHubDatabase
import com.example.database.MyApiEntity
import com.example.database.ParserWarningEntity
import com.example.database.ProjectEntity
import com.example.database.ReadyKeyEntity
import com.example.database.SyncLogEntity
import com.example.database.AggregatorEntity
import com.example.database.OwnAccountProviderEntity
import com.example.network.GitHubSyncService
import com.example.parser.MarkdownParser
import com.example.security.SecurityManager
import com.example.settings.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

sealed interface Screens {
    object ReadyKeysList : Screens
    data class ReadyKeyDetail(val id: String) : Screens
    object AggregatorsList : Screens
    data class AggregatorDetail(val id: String) : Screens
    object OwnAccountList : Screens
    data class OwnAccountDetail(val id: String) : Screens
    object MyApisList : Screens
    data class MyApiDetail(val id: String) : Screens
    data class CreateEditMyApi(val editingId: String? = null, val prefillKey: String? = null, val prefillProviderName: String? = null, val prefillEndpoint: String? = null) : Screens
    object ProjectsList : Screens
    object Settings : Screens
    object SyncHistory : Screens
    object ParserWarnings : Screens
    object BackupCenter : Screens
    object SearchScreen : Screens
}

class FreeLlmHubViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FreeLlmHubDatabase.getDatabase(application)
    
    // DAOs
    val readyKeysDao = db.readyKeysDao()
    val aggregatorsDao = db.aggregatorsDao()
    val ownAccountProvidersDao = db.ownAccountProvidersDao()
    val myApisDao = db.myApisDao()
    val projectsDao = db.projectsDao()
    val syncLogDao = db.syncLogDao()
    val parserWarningsDao = db.parserWarningsDao()
    val auditLogDao = db.auditLogDao()

    // Screen State / Backstack
    var activeScreen by mutableStateOf<Screens>(Screens.ReadyKeysList)
    private val backstack = mutableListOf<Screens>()

    // Local Search & Filter State
    var globalSearchQuery by mutableStateOf("")
    var filterProvider by mutableStateOf("")
    var filterCategory by mutableStateOf("ALL") // "ALL", "CHAT", "IMAGE", "TTS", "EMBEDDING"
    var filterCardRequired by mutableStateOf<Boolean?>(null) // null = any, true, false

    // Network Sync State
    var isSyncing by mutableStateOf(false)
    var syncStatusText by mutableStateOf("")

    // Active Testing UI State
    var activeCheckId by mutableStateOf<String?>(null)
    var activeCheckMessage by mutableStateOf("")

    // Expose DB queries as flows
    val readyKeys: StateFlow<List<ReadyKeyEntity>> = readyKeysDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aggregators: StateFlow<List<AggregatorEntity>> = aggregatorsDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ownAccountProviders: StateFlow<List<OwnAccountProviderEntity>> = ownAccountProvidersDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myApis: StateFlow<List<MyApiEntity>> = myApisDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<ProjectEntity>> = projectsDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLogEntity>> = syncLogDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val parserWarnings: StateFlow<List<ParserWarningEntity>> = parserWarningsDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Pre-populate with realistic mock fallbacks on initial DB load to guarantee functional rich lists
        viewModelScope.launch(Dispatchers.IO) {
            if (readyKeysDao.getById("ready_1") == null) {
                readyKeysDao.insertAll(MarkdownParser.getFallbackReadyKeys("internal-fallback"))
            }
            if (ownAccountProvidersDao.observeAll().first().isEmpty()) {
                ownAccountProvidersDao.insertAll(MarkdownParser.getFallbackOwnAccountProviders())
            }
            if (projectsDao.observeAll().first().isEmpty()) {
                projectsDao.insert(ProjectEntity("proj_1", "Рабочие API", 0xFF6200EE.toInt(), System.currentTimeMillis()))
                projectsDao.insert(ProjectEntity("proj_2", "Тестовые API", 0xFF03DAC6.toInt(), System.currentTimeMillis()))
            }
        }
    }

    // Custom Robust Screen Navigation
    fun navigateTo(screen: Screens) {
        backstack.add(activeScreen)
        activeScreen = screen
    }

    fun navigateBack(): Boolean {
        if (backstack.isNotEmpty()) {
            activeScreen = backstack.removeAt(backstack.size - 1)
            return true
        }
        return false
    }

    fun navigateToHome() {
        backstack.clear()
        activeScreen = Screens.ReadyKeysList
    }

    // Epic 1: Synchronization for Ready Keys
    fun startSyncReadyKeys(context: Context) {
        if (isSyncing) return
        viewModelScope.launch(Dispatchers.IO) {
            isSyncing = true
            syncStatusText = "Загрузка таблицы публичных ключей..."
            val syncId = UUID.randomUUID().toString()
            val startTime = System.currentTimeMillis()
            val warningsList = mutableListOf<ParserWarningEntity>()
            val pat = SettingsManager.getGitHubPat(context)

            try {
                // Fetch public LLM api keys from github raw content
                val url = "https://raw.githubusercontent.com/free-llm/free-llm-api-keys/main/README.md"
                val markdown = GitHubSyncService.fetchMarkdown(url, pat)
                
                syncStatusText = "Анализ матрицы ключей..."
                val keys = MarkdownParser.parseReadyKeys(markdown, "free-llm-api-keys", warningsList)
                
                // Save locally
                readyKeysDao.clearAndReplace(keys)

                // Save warnings
                if (warningsList.isNotEmpty()) {
                    parserWarningsDao.clearByRepo("free-llm-api-keys")
                    warningsList.forEach { parserWarningsDao.insert(it) }
                }

                syncLogDao.insert(
                    SyncLogEntity(
                        id = syncId,
                        section = "READY_KEYS",
                        startedAt = startTime,
                        finishedAt = System.currentTimeMillis(),
                        status = "SUCCESS",
                        newItems = keys.size,
                        changedItems = 0,
                        removedItems = 0,
                        errorMessage = null
                    )
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Публичные ключи успешно синхронизированы!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Graceful parsing fallout using offline samples
                val fallbackKeys = MarkdownParser.getFallbackReadyKeys("local-stored")
                readyKeysDao.clearAndReplace(fallbackKeys)

                syncLogDao.insert(
                    SyncLogEntity(
                        id = syncId,
                        section = "READY_KEYS",
                        startedAt = startTime,
                        finishedAt = System.currentTimeMillis(),
                        status = "FAILED",
                        newItems = fallbackKeys.size,
                        changedItems = 0,
                        removedItems = 0,
                        errorMessage = e.localizedMessage ?: "Unknown offline network condition"
                    )
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Офлайн: загружена локальная копия базы ключей.", Toast.LENGTH_LONG).show()
                }
            } finally {
                isSyncing = false
                syncStatusText = ""
            }
        }
    }

    // Epic 2: Synchronization for Aggregators
    fun startSyncAggregators(context: Context) {
        if (isSyncing) return
        viewModelScope.launch(Dispatchers.IO) {
            isSyncing = true
            syncStatusText = "Загрузка списка провайдеров и прокси..."
            val syncId = UUID.randomUUID().toString()
            val startTime = System.currentTimeMillis()
            val warningsList = mutableListOf<ParserWarningEntity>()
            val pat = SettingsManager.getGitHubPat(context)

            try {
                val url = "https://raw.githubusercontent.com/free-llm/freellmapi/main/README.md"
                val markdown = GitHubSyncService.fetchMarkdown(url, pat)
                
                syncStatusText = "Анализ списка прокси-серверов..."
                val aggregatorsList = MarkdownParser.parseAggregators(markdown, "freellmapi", warningsList)
                
                aggregatorsDao.clearAndReplace(aggregatorsList)

                if (warningsList.isNotEmpty()) {
                    parserWarningsDao.clearByRepo("freellmapi")
                    warningsList.forEach { parserWarningsDao.insert(it) }
                }

                syncLogDao.insert(
                    SyncLogEntity(
                        id = syncId,
                        section = "AGGREGATORS",
                        startedAt = startTime,
                        finishedAt = System.currentTimeMillis(),
                        status = "SUCCESS",
                        newItems = aggregatorsList.size,
                        changedItems = 0,
                        removedItems = 0,
                        errorMessage = null
                    )
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Список прокси успешно синхронизирован!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val fallbacks = listOf(
                    AggregatorEntity("agg_1", "Aozora Free Gateway", "Public openai proxy with Google Gemini pooling.", listOf("Google Gemini"), "10M tokens", listOf("Zero Auth"), "Direct API", "https://github.com", null, "https://free.aozora.ai/v1", false, false, true, false, "UNKNOWN", System.currentTimeMillis()),
                    AggregatorEntity("agg_2", "Local Multiplexer", "Aggregate hundreds of free trial accounts", listOf("All"), "Rolling Load", listOf("Self-Host"), "Docker Compose", "https://github.com", null, "http://localhost:3000/v1", true, true, true, true, "UNKNOWN", System.currentTimeMillis())
                )
                aggregatorsDao.clearAndReplace(fallbacks)

                syncLogDao.insert(
                    SyncLogEntity(
                        id = syncId,
                        section = "AGGREGATORS",
                        startedAt = startTime,
                        finishedAt = System.currentTimeMillis(),
                        status = "FAILED",
                        newItems = fallbacks.size,
                        changedItems = 0,
                        removedItems = 0,
                        errorMessage = e.localizedMessage ?: "Fallback load"
                    )
                )
            } finally {
                isSyncing = false
                syncStatusText = ""
            }
        }
    }

    // Epic 3: Synchronization for Own-Account Providers (3 Sources combined)
    fun startSyncOwnAccount(context: Context) {
        if (isSyncing) return
        viewModelScope.launch(Dispatchers.IO) {
            isSyncing = true
            syncStatusText = "Объединение и очистка дубликатов бесплатных тарифов..."
            val syncId = UUID.randomUUID().toString()
            val startTime = System.currentTimeMillis()
            val warningsList = mutableListOf<ParserWarningEntity>()
            val pat = SettingsManager.getGitHubPat(context)

            try {
                // Core list 1
                val url1 = "https://raw.githubusercontent.com/free-llm/awesome-free-llm-apis/main/README.md"
                val md1 = try { GitHubSyncService.fetchMarkdown(url1, pat) } catch (e: Exception) { "" }

                // Resources list 2
                val url2 = "https://raw.githubusercontent.com/free-llm/free-llm-api-resources/main/README.md"
                val md2 = try { GitHubSyncService.fetchMarkdown(url2, pat) } catch (e: Exception) { "" }

                // Tools list 3
                val url3 = "https://raw.githubusercontent.com/free-llm/free-ai-tools/main/README.md"
                val md3 = try { GitHubSyncService.fetchMarkdown(url3, pat) } catch (e: Exception) { "" }

                val merged = MarkdownParser.parseOwnAccountProviders(
                    md1, md2, md3, 
                    listOf("awesome-free-llm", "freellm-resources", "free-tools-list"),
                    warningsList
                )

                ownAccountProvidersDao.clearAndReplace(merged)

                syncLogDao.insert(
                    SyncLogEntity(
                        id = syncId,
                        section = "OWN_ACCOUNT",
                        startedAt = startTime,
                        finishedAt = System.currentTimeMillis(),
                        status = "SUCCESS",
                        newItems = merged.size,
                        changedItems = 0,
                        removedItems = 0,
                        errorMessage = null
                    )
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Бесплатные тарифы синхронизированы и объединены!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val mockOwn = MarkdownParser.getFallbackOwnAccountProviders()
                ownAccountProvidersDao.clearAndReplace(mockOwn)

                syncLogDao.insert(
                    SyncLogEntity(
                        id = syncId,
                        section = "OWN_ACCOUNT",
                        startedAt = startTime,
                        finishedAt = System.currentTimeMillis(),
                        status = "FAILED",
                        newItems = mockOwn.size,
                        changedItems = 0,
                        removedItems = 0,
                        errorMessage = e.localizedMessage
                    )
                )
            } finally {
                isSyncing = false
                syncStatusText = ""
            }
        }
    }

    // Epic 4: Vault Functions
    fun addMyApi(
        providerName: String,
        apiKeyRaw: String,
        baseUrl: String?,
        keyType: String,
        instructions: String?,
        tags: List<String>,
        projectId: String?,
        context: Context
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            val encrypted = SecurityManager.encrypt(apiKeyRaw)
            val masked = MarkdownParser.maskKey(apiKeyRaw)
            val isValidFormat = apiKeyRaw.startsWith("sk-") || apiKeyRaw.startsWith("gsk-") || apiKeyRaw.startsWith("co-") || apiKeyRaw.startsWith("AIza")

            val entity = MyApiEntity(
                id = id,
                providerName = providerName,
                apiKeyEncrypted = encrypted,
                apiKeyMask = masked,
                baseUrl = baseUrl,
                keyType = keyType,
                isActive = true,
                instructionsMarkdown = instructions,
                source = "MANUAL",
                originRepo = null,
                dateAdded = System.currentTimeMillis(),
                dateLastUsed = null,
                tags = tags,
                colorLabel = 0,
                projectId = projectId,
                keyFormatValid = isValidFormat,
                healthStatus = "UNKNOWN",
                healthHttpCode = null,
                healthLatencyMs = null,
                healthCheckedAt = null
            )

            myApisDao.insert(entity)
            auditLogDao.insert(AuditLogEntity(UUID.randomUUID().toString(), id, "CREATED", System.currentTimeMillis()))

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Ключ API успешно добавлен в Сейф!", Toast.LENGTH_SHORT).show()
                navigateBack()
            }
        }
    }

    fun updateMyApi(
        id: String,
        providerName: String,
        apiKeyRaw: String, // could be masked originally, check if edited
        baseUrl: String?,
        keyType: String,
        instructions: String?,
        tags: List<String>,
        projectId: String?,
        isActive: Boolean,
        context: Context
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = myApisDao.getById(id) ?: return@launch
            
            val encryptedKey: String
            val mask: String
            if (apiKeyRaw.contains("...") || apiKeyRaw.contains("*") || apiKeyRaw == existing.apiKeyMask) {
                // Key was not modified
                encryptedKey = existing.apiKeyEncrypted
                mask = existing.apiKeyMask
            } else {
                // Key underwent changes
                encryptedKey = SecurityManager.encrypt(apiKeyRaw)
                mask = MarkdownParser.maskKey(apiKeyRaw)
            }

            val isValidFormat = apiKeyRaw.startsWith("sk-") || apiKeyRaw.startsWith("gsk-") || apiKeyRaw.startsWith("co-") || apiKeyRaw.startsWith("AIza")

            val updated = existing.copy(
                providerName = providerName,
                apiKeyEncrypted = encryptedKey,
                apiKeyMask = mask,
                baseUrl = baseUrl,
                keyType = keyType,
                instructionsMarkdown = instructions,
                isActive = isActive,
                tags = tags,
                projectId = projectId,
                keyFormatValid = isValidFormat
            )

            myApisDao.update(updated)
            auditLogDao.insert(AuditLogEntity(UUID.randomUUID().toString(), id, "UPDATED", System.currentTimeMillis()))

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Параметры API успешно сохранены.", Toast.LENGTH_SHORT).show()
                navigateBack()
            }
        }
    }

    fun deleteMyApi(id: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            myApisDao.delete(id)
            auditLogDao.insert(AuditLogEntity(UUID.randomUUID().toString(), id, "DELETED", System.currentTimeMillis()))
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Запись удалена из Сейфа.", Toast.LENGTH_SHORT).show()
                navigateBack()
            }
        }
    }

    // Epic 1: Quick Import Public Key into Personal Vault
    fun importReadyKeyToVault(keyEntity: ReadyKeyEntity, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val encrypted = SecurityManager.encrypt(keyEntity.apiKeyEncryptedTemp ?: "")
            val id = UUID.randomUUID().toString()

            val imported = MyApiEntity(
                id = id,
                providerName = keyEntity.provider,
                apiKeyEncrypted = encrypted,
                apiKeyMask = keyEntity.apiKeyMasked,
                baseUrl = keyEntity.endpoint,
                keyType = "CHAT",
                isActive = true,
                instructionsMarkdown = "Импортировано из готового каталога (${keyEntity.sourceRepo})",
                source = "IMPORTED_READY",
                originRepo = keyEntity.sourceRepo,
                dateAdded = System.currentTimeMillis(),
                dateLastUsed = null,
                tags = listOf("imported", "public"),
                colorLabel = 1,
                projectId = null,
                keyFormatValid = true,
                healthStatus = keyEntity.healthStatus,
                healthHttpCode = null,
                healthLatencyMs = null,
                healthCheckedAt = null
            )

            myApisDao.insert(imported)
            auditLogDao.insert(AuditLogEntity(UUID.randomUUID().toString(), id, "CREATED", System.currentTimeMillis()))

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Успешно импортировано ${keyEntity.provider} в Сейф!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Epic 5: Quick Single-Endpoint Verification (Health Check)
    fun testMyApiHealth(id: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val api = myApisDao.getById(id) ?: return@launch
            activeCheckId = id
            activeCheckMessage = "Соединение с ${api.providerName}..."

            val rawKey = SecurityManager.decrypt(api.apiKeyEncrypted)
            val testEndpoint = api.baseUrl ?: "https://api.openai.com/v1"

            val result = GitHubSyncService.verifyEndpointHealth(testEndpoint, rawKey)

            myApisDao.updateHealthStatus(
                id = id,
                healthStatus = result.status,
                httpCode = result.httpCode,
                latencyMs = result.latencyMs,
                checkedAt = System.currentTimeMillis()
            )

            // Log decrypted request action
            auditLogDao.insert(AuditLogEntity(UUID.randomUUID().toString(), id, "DECRYPTED", System.currentTimeMillis()))

            activeCheckId = null
            activeCheckMessage = ""

            withContext(Dispatchers.Main) {
                val latencyText = "${result.latencyMs} мс"
                val statusTextStr = if (result.status == "HEALTHY") "АКТИВЕН" else "НЕАКТИВЕН"
                Toast.makeText(context, "Результат: $statusTextStr (${result.httpCode ?: "Нет сети"}) - $latencyText", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun testReadyKeyHealth(id: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val keyObj = readyKeysDao.getById(id) ?: return@launch
            activeCheckId = id
            activeCheckMessage = "Соединение с ${keyObj.provider}..."

            val rawKey = keyObj.apiKeyEncryptedTemp ?: ""
            val testEndpoint = keyObj.endpoint ?: "https://api.openai.com/v1"

            val result = GitHubSyncService.verifyEndpointHealth(testEndpoint, rawKey)

            readyKeysDao.updateHealthStatus(id, result.status)

            activeCheckId = null
            activeCheckMessage = ""

            withContext(Dispatchers.Main) {
                val latencyText = "${result.latencyMs} мс"
                val statusTextStr = if (result.status == "HEALTHY") "АКТИВЕН" else "НЕАКТИВЕН"
                Toast.makeText(context, "Результат: $statusTextStr (${result.httpCode ?: "Нет сети"}) - $latencyText", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Export local My API entries
    fun exportMyApisBackup(passphrase: String, context: Context): String? {
        if (passphrase.isEmpty()) {
            Toast.makeText(context, "Симметричный пароль обязателен для шифрования!", Toast.LENGTH_SHORT).show()
            return null
        }
        return try {
            val listApis = myApis.value
            val listProjects = projects.value
            BackupManager.exportBackup(listApis, listProjects, passphrase)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // Import secure JSON backups
    fun importBackupPayload(encryptedJson: String, passphrase: String, context: Context) {
        if (encryptedJson.isEmpty() || passphrase.isEmpty()) {
            Toast.makeText(context, "Требуется пароль и зашифрованный текст резервной копии!", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val payload = BackupManager.importBackup(encryptedJson, passphrase)
            if (payload == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не удалось расшифровать копию. Неверный пароль или поврежденные данные.", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            // Write items back into database
            payload.projects.forEach {
                projectsDao.insert(ProjectEntity(it.id, it.name, it.colorLabel, System.currentTimeMillis()))
            }

            payload.apis.forEach {
                myApisDao.insert(
                    MyApiEntity(
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
                        dateAdded = System.currentTimeMillis(),
                        dateLastUsed = null,
                        tags = it.tags,
                        colorLabel = it.colorLabel,
                        projectId = it.projectId,
                        keyFormatValid = true,
                        healthStatus = "UNKNOWN",
                        healthHttpCode = null,
                        healthLatencyMs = null,
                        healthCheckedAt = null
                    )
                )
                auditLogDao.insert(AuditLogEntity(UUID.randomUUID().toString(), it.id, "CREATED", System.currentTimeMillis()))
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Копия восстановлена: импортировано ${payload.apis.size} API!", Toast.LENGTH_SHORT).show()
                navigateTo(Screens.MyApisList)
            }
        }
    }

    // Projects Management
    fun createProject(name: String, colorLabel: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val project = ProjectEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                colorLabel = colorLabel,
                createdAt = System.currentTimeMillis()
            )
            projectsDao.insert(project)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            projectsDao.delete(projectId)
        }
    }
}
