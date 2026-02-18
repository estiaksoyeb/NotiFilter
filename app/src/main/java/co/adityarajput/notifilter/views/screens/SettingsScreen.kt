package co.adityarajput.notifilter.views.screens

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import co.adityarajput.notifilter.Constants.RUN_IN_FOREGROUND
import co.adityarajput.notifilter.Constants.SETTINGS
import co.adityarajput.notifilter.R
import co.adityarajput.notifilter.data.AppContainer
import co.adityarajput.notifilter.services.NotificationListener
import co.adityarajput.notifilter.utils.Logger
import co.adityarajput.notifilter.utils.hasExactAlarmPermission
import co.adityarajput.notifilter.utils.hasPostNotificationsPermission
import co.adityarajput.notifilter.utils.hasUnrestrictedBackgroundUsagePermission
import co.adityarajput.notifilter.viewmodels.Provider
import co.adityarajput.notifilter.viewmodels.SummaryViewModel
import co.adityarajput.notifilter.views.Theme
import co.adityarajput.notifilter.views.components.AppBar
import co.adityarajput.notifilter.views.components.ErrorText
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("BatteryLife")
@Composable
fun SettingsScreen(
    goToAboutScreen: () -> Unit = {},
    goBack: () -> Unit = {},
    summaryViewModel: SummaryViewModel = viewModel(factory = Provider.Factory)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val appContainer = remember { AppContainer(context) }
    val handler = remember { Handler(Looper.getMainLooper()) }
    val sharedPreferences = remember { context.getSharedPreferences(SETTINGS, MODE_PRIVATE) }

    val schedules by summaryViewModel.schedules.collectAsState()

    var hasNotificationPermission by remember { mutableStateOf(context.hasPostNotificationsPermission()) }
    var hasAlarmPermission by remember { mutableStateOf(context.hasExactAlarmPermission()) }
    var isInvincible by remember { mutableStateOf(context.hasUnrestrictedBackgroundUsagePermission()) }
    var isRunningInForeground by remember {
        mutableStateOf(sharedPreferences.getBoolean(RUN_IN_FOREGROUND, false))
    }

    val watcher = object : Runnable {
        override fun run() {
            hasNotificationPermission = context.hasPostNotificationsPermission()
            hasAlarmPermission = context.hasExactAlarmPermission()
            isInvincible = context.hasUnrestrictedBackgroundUsagePermission()
            handler.postDelayed(this, 1000)
        }
    }
    DisposableEffect(Unit) {
        handler.post(watcher)
        onDispose { handler.removeCallbacksAndMessages(null) }
    }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            summaryViewModel.addSchedule(hourOfDay * 60 + minute)
        },
        12, 0, false
    )

    Scaffold(
        topBar = { AppBar(stringResource(R.string.settings), true, goBack) },
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(dimensionResource(R.dimen.padding_small)),
                Arrangement.Top,
                Alignment.CenterHorizontally,
            ) {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.padding_small)),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                dimensionResource(R.dimen.padding_large),
                                dimensionResource(R.dimen.padding_medium),
                            ),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.scheduled_summaries),
                            fontWeight = FontWeight.Medium,
                        )
                        IconButton({ timePickerDialog.show() }) {
                            Icon(Icons.Default.Add, stringResource(R.string.add_summary_schedule))
                        }
                    }

                    if (!hasNotificationPermission || !hasAlarmPermission) {
                        Column(
                            Modifier.padding(horizontal = dimensionResource(R.dimen.padding_large)),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!hasNotificationPermission) {
                                ErrorText(R.string.summary_notification_permission_warning)
                                Button(
                                    onClick = {
                                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        })
                                    },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text(stringResource(R.string.grant_permission))
                                }
                            }
                            if (!hasAlarmPermission) {
                                ErrorText(R.string.summary_alarm_permission_warning)
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = "package:${context.packageName}".toUri()
                                            })
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text(stringResource(R.string.grant_permission))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    schedules.forEach { schedule ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensionResource(R.dimen.padding_large), vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(schedule.enabled, { summaryViewModel.toggleSchedule(schedule) })
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    String.format(Locale.getDefault(), "%02d:%02d", schedule.time / 60, schedule.time % 60),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            IconButton({ summaryViewModel.deleteSchedule(schedule) }) {
                                Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Spacer(Modifier.height(dimensionResource(R.dimen.padding_medium)))
                }
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.padding_small)),
                ) {
                    Text(
                        stringResource(R.string.settings_section_1),
                        Modifier.padding(
                            dimensionResource(R.dimen.padding_large),
                            dimensionResource(R.dimen.padding_medium),
                        ),
                        fontWeight = FontWeight.Medium,
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(R.dimen.padding_large)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.disable_battery_optimization),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                stringResource(R.string.explain_disabling_battery_optimization),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            isInvincible,
                            {
                                if (it) {
                                    val intent = Intent(
                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        "package:${context.packageName}".toUri(),
                                    )
                                    context.startActivity(intent)
                                } else {
                                    val intent =
                                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            },
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                dimensionResource(R.dimen.padding_large),
                                dimensionResource(R.dimen.padding_medium),
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.run_in_foreground),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                stringResource(R.string.explain_running_in_foreground),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            isRunningInForeground,
                            {
                                isRunningInForeground = it
                                sharedPreferences.edit { putBoolean(RUN_IN_FOREGROUND, it) }
                                NotificationListener.updateForegroundStatus(it)
                            },
                        )
                    }
                }
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.padding_small)),
                ) {
                    val importSuccess = stringResource(R.string.import_success)
                    val exportSuccess = stringResource(R.string.export_success)
                    val appNameAndVersion =
                        "${stringResource(R.string.app_name)}_${stringResource(R.string.app_version)}"

                    val importLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.OpenDocument(),
                    ) { uri ->
                        scope.launch {
                            uri
                                ?.let { context.contentResolver.openInputStream(it) }
                                ?.use {
                                    appContainer.import(it.bufferedReader().readText())
                                    goBack()
                                    Toast
                                        .makeText(context, importSuccess, Toast.LENGTH_SHORT)
                                        .show()
                                }
                        }
                    }
                    val exportLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.CreateDocument("application/json"),
                    ) { uri ->
                        scope.launch {
                            uri
                                ?.let { context.contentResolver.openOutputStream(it) }
                                ?.use {
                                    it.write(appContainer.export().toByteArray())
                                    Toast
                                        .makeText(context, exportSuccess, Toast.LENGTH_SHORT)
                                        .show()
                                }
                        }
                    }

                    Text(
                        stringResource(R.string.settings_section_2),
                        Modifier.padding(
                            dimensionResource(R.dimen.padding_large),
                            dimensionResource(R.dimen.padding_medium),
                        ),
                        fontWeight = FontWeight.Medium,
                    )
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(R.dimen.padding_large))
                            .clickable { importLauncher.launch(arrayOf("application/json")) },
                    ) {
                        Text(
                            stringResource(R.string.import_filters),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            stringResource(R.string.import_warning),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                dimensionResource(R.dimen.padding_large),
                                dimensionResource(R.dimen.padding_medium),
                            )
                            .clickable {
                                exportLauncher.launch(
                                    appNameAndVersion + "_${
                                        Instant.now().atZone(ZoneId.systemDefault())
                                            .toLocalDateTime().format(
                                                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"),
                                            )
                                    }.json",
                                )
                            },
                    ) {
                        Text(
                            stringResource(R.string.export_filters),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            stringResource(R.string.export_explanation),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.padding_small)),
                ) {
                    val copySuccess = stringResource(R.string.copy_success)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                dimensionResource(R.dimen.padding_large),
                                dimensionResource(R.dimen.padding_medium),
                                dimensionResource(R.dimen.padding_large),
                                dimensionResource(R.dimen.padding_small),
                            )
                            .clickable {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        ClipData.newPlainText(
                                            "logs",
                                            Logger.logs.joinToString("\n"),
                                        ).toClipEntry(),
                                    )
                                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                                        Toast
                                            .makeText(context, copySuccess, Toast.LENGTH_SHORT)
                                            .show()
                                }
                            },
                        Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                    ) {
                        Icon(
                            painterResource(R.drawable.list_alt),
                            stringResource(R.string.alttext_logs),
                        )
                        Text(
                            stringResource(R.string.copy_logs),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                dimensionResource(R.dimen.padding_large),
                                dimensionResource(R.dimen.padding_small),
                                dimensionResource(R.dimen.padding_large),
                                dimensionResource(R.dimen.padding_medium),
                            )
                            .clickable { goToAboutScreen() },
                        Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                    ) {
                        Icon(
                            painterResource(R.drawable.info),
                            stringResource(R.string.alttext_info),
                        )
                        Text(
                            stringResource(R.string.about_app),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() = Theme { SettingsScreen() }
