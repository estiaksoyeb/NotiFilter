package co.adityarajput.notifilter.utils

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Context.POWER_SERVICE
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

fun Context.hasNotificationListenerPermission() =
    Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        ?.contains(packageName) ?: false

fun Context.hasAccessibilityServicePermission() =
    Settings.Secure.getString(contentResolver, "enabled_accessibility_services")
        ?.contains(packageName) ?: false

fun Context.hasUnrestrictedBackgroundUsagePermission() =
    (getSystemService(POWER_SERVICE) as PowerManager)
        .isIgnoringBatteryOptimizations(packageName)

fun Context.hasPostNotificationsPermission() =
    (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        .areNotificationsEnabled()

fun Context.hasExactAlarmPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    } else {
        true
    }
}
