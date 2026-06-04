package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadyKeysDao {
    @Query("SELECT * FROM ready_keys ORDER BY syncedAt DESC")
    fun observeAll(): Flow<List<ReadyKeyEntity>>

    @Query("SELECT * FROM ready_keys WHERE provider LIKE '%' || :query || '%' OR models LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<ReadyKeyEntity>>

    @Query("SELECT * FROM ready_keys WHERE provider = :provider")
    fun filterByProvider(provider: String): Flow<List<ReadyKeyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ReadyKeyEntity>)

    @Query("DELETE FROM ready_keys")
    suspend fun clear()

    @Transaction
    suspend fun clearAndReplace(items: List<ReadyKeyEntity>) {
        clear()
        insertAll(items)
    }

    @Query("SELECT * FROM ready_keys WHERE id = :id")
    suspend fun getById(id: String): ReadyKeyEntity?

    @Query("UPDATE ready_keys SET healthStatus = :healthStatus WHERE id = :id")
    suspend fun updateHealthStatus(id: String, healthStatus: String)
}

@Dao
interface AggregatorsDao {
    @Query("SELECT * FROM aggregators ORDER BY name ASC")
    fun observeAll(): Flow<List<AggregatorEntity>>

    @Query("SELECT * FROM aggregators WHERE id = :id")
    suspend fun getById(id: String): AggregatorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AggregatorEntity>)

    @Query("SELECT * FROM aggregators WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<AggregatorEntity>>

    @Query("DELETE FROM aggregators")
    suspend fun clear()

    @Transaction
    suspend fun clearAndReplace(items: List<AggregatorEntity>) {
        clear()
        insertAll(items)
    }
}

@Dao
interface OwnAccountProvidersDao {
    @Query("SELECT * FROM own_account_providers ORDER BY providerName ASC")
    fun observeAll(): Flow<List<OwnAccountProviderEntity>>

    @Query("SELECT * FROM own_account_providers WHERE id = :id")
    suspend fun getById(id: String): OwnAccountProviderEntity?

    @Query("SELECT * FROM own_account_providers WHERE providerName LIKE '%' || :query || '%' OR modelsAvailable LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<OwnAccountProviderEntity>>

    @Query("SELECT * FROM own_account_providers WHERE category = :category AND (:cardRequired IS NULL OR cardRequired = :cardRequired)")
    fun filter(category: String, cardRequired: Boolean?): Flow<List<OwnAccountProviderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<OwnAccountProviderEntity>)

    @Query("DELETE FROM own_account_providers")
    suspend fun clear()

    @Transaction
    suspend fun clearAndReplace(items: List<OwnAccountProviderEntity>) {
        clear()
        insertAll(items)
    }
}

@Dao
interface MyApisDao {
    @Query("SELECT * FROM my_apis ORDER BY dateAdded DESC")
    fun observeAll(): Flow<List<MyApiEntity>>

    @Query("SELECT * FROM my_apis WHERE projectId = :projectId ORDER BY dateAdded DESC")
    fun observeByProject(projectId: String?): Flow<List<MyApiEntity>>

    @Query("SELECT * FROM my_apis WHERE providerName LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<MyApiEntity>>

    @Query("SELECT * FROM my_apis WHERE id = :id")
    suspend fun getById(id: String): MyApiEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MyApiEntity)

    @Update
    suspend fun update(item: MyApiEntity)

    @Query("DELETE FROM my_apis WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE my_apis SET healthStatus = :healthStatus, healthHttpCode = :httpCode, healthLatencyMs = :latencyMs, healthCheckedAt = :checkedAt WHERE id = :id")
    suspend fun updateHealthStatus(id: String, healthStatus: String, httpCode: Int?, latencyMs: Long?, checkedAt: Long)

    @Query("UPDATE my_apis SET dateLastUsed = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long)
}

@Dao
interface ProjectsDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity)

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun delete(projectId: String)
}

@Dao
interface SyncLogDao {
    @Query("SELECT * FROM sync_logs ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SyncLogEntity>>

    @Query("SELECT * FROM sync_logs WHERE section = :section ORDER BY startedAt DESC LIMIT 1")
    fun observeLatest(section: String): Flow<SyncLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SyncLogEntity)
}

@Dao
interface ParserWarningsDao {
    @Query("SELECT * FROM parser_warnings ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ParserWarningEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(warning: ParserWarningEntity)

    @Query("DELETE FROM parser_warnings WHERE sourceRepo = :repo")
    suspend fun clearByRepo(repo: String)

    @Query("DELETE FROM parser_warnings")
    suspend fun clearAll()
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs WHERE myApiId = :apiId ORDER BY createdAt DESC")
    fun observeForApi(apiId: String): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AuditLogEntity)
}
