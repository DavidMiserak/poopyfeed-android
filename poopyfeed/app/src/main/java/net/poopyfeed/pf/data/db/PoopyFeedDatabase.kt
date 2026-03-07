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
 * feeding_reminder_interval. Version 3: feedings duration_minutes, side.
 */
@Database(
    entities = [ChildEntity::class, FeedingEntity::class, DiaperEntity::class, NapEntity::class],
    version = 3,
    exportSchema = true)
abstract class PoopyFeedDatabase : RoomDatabase() {

  abstract fun childDao(): ChildDao

  abstract fun feedingDao(): FeedingDao

  abstract fun diaperDao(): DiaperDao

  abstract fun napDao(): NapDao

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

    @Volatile private var instance: PoopyFeedDatabase? = null

    fun getInstance(context: Context): PoopyFeedDatabase {
      return instance
          ?: synchronized(this) {
            instance
                ?: Room.databaseBuilder(
                        context.applicationContext, PoopyFeedDatabase::class.java, DATABASE_NAME)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
          }
    }
  }
}
