package net.poopyfeed.pf.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * PoopyFeed Room database.
 *
 * Schema version 1:
 * - children table
 * - feedings table (foreign key to children, cascade delete)
 * - diapers table (foreign key to children, cascade delete)
 * - naps table (foreign key to children, cascade delete)
 */
@Database(
    entities = [ChildEntity::class, FeedingEntity::class, DiaperEntity::class, NapEntity::class],
    version = 1,
    exportSchema = true)
abstract class PoopyFeedDatabase : RoomDatabase() {

  abstract fun childDao(): ChildDao

  abstract fun feedingDao(): FeedingDao

  abstract fun diaperDao(): DiaperDao

  abstract fun napDao(): NapDao

  companion object {
    private const val DATABASE_NAME = "poopyfeed_db"

    @Volatile private var instance: PoopyFeedDatabase? = null

    fun getInstance(context: Context): PoopyFeedDatabase {
      return instance
          ?: synchronized(this) {
            instance
                ?: Room.databaseBuilder(
                        context.applicationContext, PoopyFeedDatabase::class.java, DATABASE_NAME)
                    .build()
                    .also { instance = it }
          }
    }
  }
}
