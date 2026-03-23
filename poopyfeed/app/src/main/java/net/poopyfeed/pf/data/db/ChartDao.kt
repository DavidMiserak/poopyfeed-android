package net.poopyfeed.pf.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** DAO for chart trend/summary cached data. */
@Dao
interface ChartDao {

  @Query(
      "SELECT * FROM feeding_trend_days WHERE child_id = :childId AND period = :period ORDER BY date")
  fun getFeedingTrends(childId: Int, period: Int): Flow<List<FeedingTrendDayEntity>>

  @Query(
      "SELECT * FROM sleep_summary_days WHERE child_id = :childId AND period = :period ORDER BY date")
  fun getSleepSummary(childId: Int, period: Int): Flow<List<SleepSummaryDayEntity>>

  @Query("DELETE FROM feeding_trend_days WHERE child_id = :childId AND period = :period")
  suspend fun clearFeedingTrends(childId: Int, period: Int)

  @Query("DELETE FROM sleep_summary_days WHERE child_id = :childId AND period = :period")
  suspend fun clearSleepSummary(childId: Int, period: Int)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFeedingTrends(days: List<FeedingTrendDayEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSleepSummary(days: List<SleepSummaryDayEntity>)
}
