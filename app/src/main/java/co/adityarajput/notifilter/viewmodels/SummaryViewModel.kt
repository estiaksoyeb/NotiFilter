package co.adityarajput.notifilter.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.adityarajput.notifilter.data.Repository
import co.adityarajput.notifilter.data.models.SummarySchedule
import co.adityarajput.notifilter.utils.SummaryScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SummaryViewModel(private val repository: Repository, private val application: Application) : ViewModel() {
    val schedules: StateFlow<List<SummarySchedule>> = repository.summarySchedules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSchedule(timeInMinutes: Int) {
        viewModelScope.launch {
            val schedule = SummarySchedule(time = timeInMinutes)
            repository.upsert(schedule)
            updateAlarms()
        }
    }

    fun toggleSchedule(schedule: SummarySchedule) {
        viewModelScope.launch {
            val updated = schedule.copy(enabled = !schedule.enabled)
            repository.upsert(updated)
            updateAlarms()
        }
    }

    fun deleteSchedule(schedule: SummarySchedule) {
        viewModelScope.launch {
            repository.delete(schedule)
            SummaryScheduler.cancel(application, schedule.id)
            updateAlarms()
        }
    }

    private suspend fun updateAlarms() {
        val allSchedules = repository.summarySchedules().first()
        SummaryScheduler.scheduleAll(application, repository, allSchedules)
    }
}
