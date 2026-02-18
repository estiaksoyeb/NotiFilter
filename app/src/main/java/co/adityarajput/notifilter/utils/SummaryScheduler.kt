package co.adityarajput.notifilter.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import co.adityarajput.notifilter.data.Repository
import co.adityarajput.notifilter.services.SummaryReceiver
import java.util.Calendar

object SummaryScheduler {
    fun scheduleAll(context: Context, repository: Repository, schedules: List<co.adityarajput.notifilter.data.models.SummarySchedule>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Cancel existing (simplified for this approach)
        // In a more robust impl, we'd track specific IDs, but for now we refresh enabled ones
        
        schedules.forEach { schedule ->
            if (schedule.enabled) {
                val intent = Intent(context, SummaryReceiver::class.java).apply {
                    putExtra("schedule_id", schedule.id)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    schedule.id,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, schedule.time / 60)
                    set(Calendar.MINUTE, schedule.time % 60)
                    set(Calendar.SECOND, 0)
                    if (before(Calendar.getInstance())) {
                        add(Calendar.DATE, 1)
                    }
                }

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancel(context: Context, scheduleId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SummaryReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
