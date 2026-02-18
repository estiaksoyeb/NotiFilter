package co.adityarajput.notifilter.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "summary_schedules")
data class SummarySchedule(
    val time: Int, // Minutes from start of day (0 - 1439)
    val enabled: Boolean = true,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)
