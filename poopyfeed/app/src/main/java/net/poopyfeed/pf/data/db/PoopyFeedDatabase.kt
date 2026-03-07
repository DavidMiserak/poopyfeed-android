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
 * Schema version 1: children, feedings, diapers, naps. Schema version 2: children can_edit,
 * feeding_reminder_interval columns.
 */
@Database(
    entities = [ChildEntity::class, FeedingEntity::class, DiaperEntity::class, NapEntity::class],
    version = 2,
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

    @Volatile private var instance: PoopyFeedDatabase? = null

    fun getInstance(context: Context): PoopyFeedDatabase {
      return instance
          ?: synchronized(this) {
            instance
                ?: Room.databaseBuilder(
                        context.applicationContext, PoopyFeedDatabase::class.java, DATABASE_NAME)
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
          }
    }
  }
}
