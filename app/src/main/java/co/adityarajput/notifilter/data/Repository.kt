package co.adityarajput.notifilter.data

import co.adityarajput.notifilter.data.models.Filter
import co.adityarajput.notifilter.data.models.Notification
import co.adityarajput.notifilter.data.models.SummarySchedule
import co.adityarajput.notifilter.utils.Logger

class Repository(
    private val filterDao: FilterDao,
    private val notificationDao: NotificationDao,
    private val summaryDao: SummaryDao,
) {
    suspend fun upsert(vararg filters: Filter) = filterDao.upsert(*filters)

    suspend fun upsert(vararg notifications: Notification) = notificationDao.upsert(*notifications)

    suspend fun upsert(vararg schedules: SummarySchedule) = summaryDao.upsert(*schedules)

    fun filters() = filterDao.list()

    fun notifications() = notificationDao.list()

    suspend fun countSince(since: Long) = notificationDao.countSince(since)

    fun summarySchedules() = summaryDao.list()

    suspend fun getEnabledSummarySchedules() = summaryDao.getEnabledSchedules()

    suspend fun registerHit(filter: Filter, notification: Notification) {
        filterDao.registerHit(filter.id)
        notificationDao.upsert(notification)

        val count = notificationDao.count()
        if (count > 50) {
            Logger.d("Repository.registerHit", "Deleting oldest ${count - 50} notification(s)")
            notificationDao.trim(count - 50)
        }
    }

    suspend fun toggleHistory(filter: Filter) = filterDao.toggleHistory(filter.id)

    suspend fun toggleEnabled(filter: Filter) = filterDao.toggleEnabled(filter.id)

    suspend fun delete(filter: Filter) = filterDao.delete(filter)

    suspend fun delete(notification: Notification) = notificationDao.delete(notification)

    suspend fun delete(schedule: SummarySchedule) = summaryDao.delete(schedule)

    suspend fun deleteFilters() = filterDao.deleteAll()

    suspend fun deleteNotifications() = notificationDao.deleteAll()
}
