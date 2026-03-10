package net.poopyfeed.pf.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * PoopyFeed Room database.
 *
 * Schema version 1: children, feedings, diapers, naps. Version 2: children can_edit,
 * feeding_reminder_interval. Version 3: feedings duration_minutes, side. Version 4: children
 * custom_bottle_low_oz, custom_bottle_mid_oz, custom_bottle_high_oz. Version 5: pending_sync table
 * for offline-first background sync. Version 6: remote_keys table for Paging 3 pagination state.
 */
@Database(
    entities =
        [
            ChildEntity::class,
            FeedingEntity::class,
            DiaperEntity::class,
            NapEntity::class,
            PendingSyncEntity::class,
            RemoteKeyEntity::class],
    version = 6,
    exportSchema = true)
abstract class PoopyFeedDatabase : RoomDatabase() {

  abstract fun childDao(): ChildDao

  abstract fun feedingDao(): FeedingDao

  abstract fun diaperDao(): DiaperDao

  abstract fun napDao(): NapDao

  abstract fun pendingSyncDao(): PendingSyncDao

  abstract fun remoteKeyDao(): RemoteKeyDao

  companion object {
    private const val DATABASE_NAME = "poopyfeed_db"

    private val MIGRATION_1_2 =
        object : Migration(1, 2) {
          override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE children ADD COLUMN can_edit INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE children ADD COLUMN feeding_reminder_interval INTEGER NULL")
          }
        }

    private val MIGRATION_2_3 =
        object : Migration(2, 3) {
          override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE feedings ADD COLUMN duration_minutes INTEGER NULL")
            db.execSQL("ALTER TABLE feedings ADD COLUMN side TEXT NULL")
          }
        }

    private val MIGRATION_3_4 =
        object : Migration(3, 4) {
          override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE children ADD COLUMN custom_bottle_low_oz TEXT NULL")
            db.execSQL("ALTER TABLE children ADD COLUMN custom_bottle_mid_oz TEXT NULL")
            db.execSQL("ALTER TABLE children ADD COLUMN custom_bottle_high_oz TEXT NULL")
          }
        }

    private val MIGRATION_4_5 =
        object : Migration(4, 5) {
          override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS pending_sync (
                  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                  entity_type TEXT NOT NULL,
                  child_id INTEGER NOT NULL,
                  request_json TEXT NOT NULL,
                  temp_local_id INTEGER NOT NULL,
                  sync_status TEXT NOT NULL DEFAULT 'pending',
                  error_message TEXT,
                  created_at INTEGER NOT NULL,
                  retry_count INTEGER NOT NULL DEFAULT 0
                )""")
          }
        }

    private val MIGRATION_5_6 =
        object : Migration(5, 6) {
          override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS remote_keys (
                  child_id INTEGER NOT NULL,
                  entity_type TEXT NOT NULL,
                  next_page INTEGER,
                  PRIMARY KEY(child_id, entity_type)
                )""")
          }
        }

    @Volatile private var instance: PoopyFeedDatabase? = null

    fun getInstance(context: Context): PoopyFeedDatabase {
      return instance
          ?: synchronized(this) {
            instance
                ?: Room.databaseBuilder(
                        context.applicationContext, PoopyFeedDatabase::class.java, DATABASE_NAME)
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                    .also { instance = it }
          }
    }
  }
}
