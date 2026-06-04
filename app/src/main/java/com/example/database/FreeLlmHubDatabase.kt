package com.example.database

import android.content.Context
import androidx.room.*

@Database(
    entities = [
        ReadyKeyEntity::class,
        AggregatorEntity::class,
        OwnAccountProviderEntity::class,
        MyApiEntity::class,
        ProjectEntity::class,
        SyncLogEntity::class,
        ParserWarningEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(AppConverters::class)
abstract class FreeLlmHubDatabase : RoomDatabase() {
    abstract fun readyKeysDao(): ReadyKeysDao
    abstract fun aggregatorsDao(): AggregatorsDao
    abstract fun ownAccountProvidersDao(): OwnAccountProvidersDao
    abstract fun myApisDao(): MyApisDao
    abstract fun projectsDao(): ProjectsDao
    abstract fun syncLogDao(): SyncLogDao
    abstract fun parserWarningsDao(): ParserWarningsDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: FreeLlmHubDatabase? = null

        fun getDatabase(context: Context): FreeLlmHubDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FreeLlmHubDatabase::class.java,
                    "free_llm_hub_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
