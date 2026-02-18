package co.adityarajput.notifilter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import co.adityarajput.notifilter.data.models.SummarySchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    @Upsert
    suspend fun upsert(vararg schedule: SummarySchedule)

    @Query("SELECT * FROM summary_schedules ORDER BY time ASC")
    fun list(): Flow<List<SummarySchedule>>

    @Delete
    suspend fun delete(schedule: SummarySchedule)

    @Query("SELECT * FROM summary_schedules WHERE enabled = 1")
    suspend fun getEnabledSchedules(): List<SummarySchedule>
}
