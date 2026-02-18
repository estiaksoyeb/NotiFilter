package co.adityarajput.notifilter.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import co.adityarajput.notifilter.Constants
import co.adityarajput.notifilter.MainActivity
import co.adityarajput.notifilter.R
import co.adityarajput.notifilter.data.AppContainer
import co.adityarajput.notifilter.utils.Logger
import co.adityarajput.notifilter.utils.SummaryScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SummaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Logger.d("SummaryReceiver", "Alarm triggered")
        val repository = AppContainer(context).repository
        val sharedPrefs = context.getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE)
        val lastSummaryTime = sharedPrefs.getLong("last_summary_timestamp", 0L)
        val since = if (lastSummaryTime == 0L) (System.currentTimeMillis() - 24 * 60 * 60 * 1000) else lastSummaryTime

        CoroutineScope(Dispatchers.IO).launch {
            val count = repository.countSince(since)
            Logger.d("SummaryReceiver", "Found $count notifications since $since")
            if (count > 0) {
                showNotification(context, count)
                sharedPrefs.edit().putLong("last_summary_timestamp", System.currentTimeMillis()).apply()
            }
            
            // Reschedule for next day
            val schedules = repository.getEnabledSummarySchedules()
            SummaryScheduler.scheduleAll(context, repository, schedules)
        }
    }

    private fun showNotification(context: Context, count: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (notificationManager.getNotificationChannel(Constants.SUMMARY_NOTIFICATION_CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    Constants.SUMMARY_NOTIFICATION_CHANNEL_ID,
                    "NotiFilter Summaries",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "history")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.SUMMARY_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.summary_notification_title))
            .setContentText(context.getString(R.string.summary_notification_content, count))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Constants.SUMMARY_NOTIFICATION_ID, notification)
        Logger.d("SummaryReceiver", "Posted summary notification for $count items")
    }
}
