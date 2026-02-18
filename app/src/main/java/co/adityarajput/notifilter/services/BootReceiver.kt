package co.adityarajput.notifilter.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.adityarajput.notifilter.data.AppContainer
import co.adityarajput.notifilter.utils.SummaryScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val repository = AppContainer(context).repository
            CoroutineScope(Dispatchers.IO).launch {
                val schedules = repository.summarySchedules().first()
                SummaryScheduler.scheduleAll(context, repository, schedules)
            }
        }
    }
}
