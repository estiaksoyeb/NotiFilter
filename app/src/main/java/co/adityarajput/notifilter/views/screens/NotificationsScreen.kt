package co.adityarajput.notifilter.views.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import co.adityarajput.notifilter.R
import co.adityarajput.notifilter.utils.getFirst
import co.adityarajput.notifilter.utils.toShortHumanReadableTime
import co.adityarajput.notifilter.viewmodels.NotificationDialogState
import co.adityarajput.notifilter.viewmodels.NotificationsViewModel
import co.adityarajput.notifilter.viewmodels.Provider
import co.adityarajput.notifilter.views.components.AppBar
import co.adityarajput.notifilter.views.components.ManageHistoryDialog
import co.adityarajput.notifilter.views.components.Tile
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    goBack: () -> Unit,
    viewModel: NotificationsViewModel = viewModel(factory = Provider.Factory),
) {
    val state = viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            AppBar(stringResource(R.string.history), true, goBack) {
                IconButton({ viewModel.dialogState = NotificationDialogState.CLEAR_HISTORY }) {
                    Icon(
                        painterResource(R.drawable.clear_all),
                        stringResource(R.string.clear_history),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
    ) { paddingValues ->
        if (state.value.notifications == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else if (state.value.notifications!!.isEmpty()) {
            Box(
                Modifier.fillMaxSize(),
                Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.no_notifications),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                Modifier
                    .padding(paddingValues)
                    .padding(dimensionResource(R.dimen.padding_small))
                    .fillMaxSize(),
            ) {
                items(state.value.notifications!!, { it.id }) {
                    var isDismissed by remember { mutableStateOf(false) }

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) {
                                isDismissed = true
                                true
                            } else {
                                false
                            }
                        }
                    )

                    LaunchedEffect(isDismissed) {
                        if (isDismissed) {
                            delay(300)
                            viewModel.delete(it)
                        }
                    }

                    AnimatedVisibility(
                        visible = !isDismissed,
                        exit = shrinkVertically(tween(300)) + fadeOut()
                    ) {
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = true,
                            backgroundContent = {
                            val color = if (isDismissed) Color.Transparent else when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer
                                else -> Color.Transparent
                            }
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(dimensionResource(R.dimen.padding_small))
                                        .background(color, MaterialTheme.shapes.medium),
                                    contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_large)),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        ) {
                            Tile(
                                it.title,
                                it.content,
                                it.origin.getFirst(30),
                                it.timestamp.toShortHumanReadableTime(),
                                null,
                                onClick = { viewModel.openNotification(context, it) },
                                onLongClick = {
                                    if (viewModel.selectedNotification == it) viewModel.selectedNotification =
                                        null
                                    else viewModel.selectedNotification = it
                                },
                                buttons = {
                                    TextButton(
                                        { viewModel.openNotification(context, it) },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary,
                                        ),
                                    ) {
                                        Text(stringResource(R.string.open))
                                    }
                                    IconButton(
                                        { viewModel.delete(it) },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            contentColor = MaterialTheme.colorScheme.tertiary,
                                        ),
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.delete),
                                            stringResource(R.string.delete),
                                        )
                                    }
                                },
                                expanded = viewModel.selectedNotification == it,
                            )
                        }
                    }
                }
            }
        }
        if (viewModel.dialogState != null)
            ManageHistoryDialog(viewModel)
    }
}
