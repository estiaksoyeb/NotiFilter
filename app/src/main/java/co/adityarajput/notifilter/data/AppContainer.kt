package co.adityarajput.notifilter.data

import android.content.Context
import co.adityarajput.notifilter.data.models.*
import co.adityarajput.notifilter.utils.SummaryScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

@Serializable
data class Backup(
    val filters: List<Filter>,
    val schedules: List<SummarySchedule> = emptyList()
)

class AppContainer(private val context: Context) {
    val repository: Repository by lazy {
        Repository(
            NotiFilterDatabase.getDatabase(context).filterDao(),
            NotiFilterDatabase.getDatabase(context).notificationDao(),
            NotiFilterDatabase.getDatabase(context).summaryDao(),
        )
    }

    suspend fun export(): String {
        val filters = repository.filters().first()
        val schedules = repository.summarySchedules().first()
        return Json.encodeToString(Backup(filters, schedules))
    }

    suspend fun import(json: String) {
        val element = Json.parseToJsonElement(json)
        val backup = if (element is JsonArray) {
            // Legacy format: Array of Filters
            Backup(filters = Json.decodeFromString<List<Filter>>(json))
        } else {
            // New format: Backup object
            Json.decodeFromString<Backup>(json)
        }

        repository.deleteFilters()
        repository.upsert(*backup.filters.toTypedArray())
        
        // Handle schedules
        // Clear old ones first (optional, but consistent with deleteFilters)
        val oldSchedules = repository.summarySchedules().first()
        oldSchedules.forEach { 
            repository.delete(it) 
            SummaryScheduler.cancel(context, it.id)
        }
        
        repository.upsert(*backup.schedules.map { it.copy(id = 0) }.toTypedArray())
        
        // Reschedule alarms
        val newSchedules = repository.summarySchedules().first()
        SummaryScheduler.scheduleAll(context, repository, newSchedules)
    }

    fun seedDemoData() {
        runBlocking {
            if (
                repository.filters().first().isEmpty() &&
                repository.notifications().first().isEmpty()
            ) {
                repository.upsert(
                    Filter(
                        App("Clock", "com.google.android.deskclock"),
                        "Upcoming alarm",
                        Action.DISMISS,
                        RegexTarget.TITLE,
                        hits = 87,
                    ),
                    Filter(
                        App("Gmail", "com.google.android.gm"),
                        "Verify your identity",
                        Action.TAP_NOTIFICATION,
                        RegexTarget.CONTENT,
                        enabled = false,
                    ),
                    Filter(
                        App("Software update", "com.wssyncmldm"),
                        "software update",
                        Action.TAP_BUTTON("Remind me"),
                        RegexTarget.CONTENT,
                        schedule = Schedule(days = setOf(2, 3, 4, 5, 6)),
                        hits = 23,
                    ),
                    Filter(
                        App("Gmail", "com.google.android.gm"),
                        "[Nn]ewsletter",
                        Action.BATCH(3),
                        RegexTarget.OR,
                        historyEnabled = false,
                    ),
                    Filter(
                        App("WhatsApp", "com.whatsapp"),
                        "Book Club",
                        Action.DELAY,
                        RegexTarget.AND,
                        "^Bob",
                        schedule = Schedule(start = 9 * 60, end = 17 * 60),
                        hits = 15,
                    ),
                    Filter(
                        App("WhatsApp", "com.whatsapp"),
                        "Photo",
                        Action.DEBOUNCE(1),
                        RegexTarget.CONTENT,
                        historyEnabled = false,
                    ),
                    Filter(
                        App("Instagram", "com.instagram.android"),
                        "reels",
                        Action.MUTE,
                        RegexTarget.TITLE,
                        historyEnabled = false,
                    ),
                    Filter(
                        App("F-Droid", "org.fdroid"),
                        "Update available",
                        Action.ALERT,
                        RegexTarget.AND,
                        "NotiFilter",
                        historyEnabled = false,
                    ),
                )
                repository.upsert(
                    Notification(
                        "Download paused",
                        "A software update is available.",
                        "Software update",
                        "com.wssyncmldm",
                        System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000,
                    ),
                    Notification(
                        "Upcoming alarm",
                        "Wed 8:30 AM - Wake up",
                        "Clock",
                        "com.google.android.deskclock",
                        System.currentTimeMillis() - 28 * 60 * 60 * 1000,
                    ),
                    Notification(
                        "Upcoming alarm",
                        "Wed 11:30 AM - Exercise",
                        "Clock",
                        "com.google.android.deskclock",
                        System.currentTimeMillis() - 25 * 60 * 60 * 1000,
                    ),
                    Notification(
                        "tom@newsletter.tomscott.com",
                        "The week: a microphone, a ropeway, and something very sour.\nHello!\nOver the last few days...",
                        "Gmail",
                        "com.google.android.gm",
                        System.currentTimeMillis() - 3 * 60 * 60 * 1000,
                    ),
                    Notification(
                        "Book Club",
                        "Bob: Please go for something lighter this time. I'm tired of tomes!",
                        "WhatsApp",
                        "com.whatsapp",
                        System.currentTimeMillis() - 37 * 60 * 1000,
                    ),
                )
            }
        }
    }
}
