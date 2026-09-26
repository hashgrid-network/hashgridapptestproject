package com.example.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "user_persistence")
data class UserPersistenceEntity(
    @PrimaryKey val userId: String,
    val walletBalanceUsdt: Double = 0.0,
    val gridCoinBalance: Double = 0.0,
    val isGridMiningActive: Boolean = false,
    val miningSessionEndTimestamp: Long = 0L,
    val sessionStartTimeMillis: Long = System.currentTimeMillis(),
    val activeContractsJson: String = "[]",
    val activityListJson: String = "[]",
    val payoutsListJson: String = "[]",
    val canSpinToday: Boolean = true,
    val wheelCooldownEnd: Long = 0L,
    val lastSavedTimestamp: Long = System.currentTimeMillis()
)

@Dao
interface UserPersistenceDao {
    @Query("SELECT * FROM user_persistence WHERE userId = :userId LIMIT 1")
    suspend fun getUserData(userId: String): UserPersistenceEntity?

    @Query("SELECT * FROM user_persistence WHERE userId = :userId LIMIT 1")
    fun getUserDataSync(userId: String): UserPersistenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserData(data: UserPersistenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveUserDataSync(data: UserPersistenceEntity)
}

@Database(entities = [UserPersistenceEntity::class], version = 1, exportSchema = false)
abstract class HashGridRoomDatabase : RoomDatabase() {
    abstract fun userPersistenceDao(): UserPersistenceDao

    companion object {
        @Volatile
        private var INSTANCE: HashGridRoomDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Non-destructive ALTER TABLE migration example if schema evolves
                db.execSQL("ALTER TABLE user_persistence ADD COLUMN lastSavedTimestamp INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): HashGridRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HashGridRoomDatabase::class.java,
                    "hashgrid_room_database"
                )
                // REMOVED fallbackToDestructiveMigration() AND fallbackToDestructiveMigrationOnDowngrade()
                // TO PREVENT USER DATA LOSS ON APP UPDATES.
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
