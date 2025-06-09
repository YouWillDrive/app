package ru.gd_alt.youwilldrive.ui.screens.Notifications

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ru.gd_alt.youwilldrive.R
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.models.Notification
import ru.gd_alt.youwilldrive.ui.components.Notifications
import ru.gd_alt.youwilldrive.ui.screens.Profile.LoadingCard
import ru.gd_alt.youwilldrive.ui.components.NotificationDetailDialog

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun NotificationsScreen(
) {
    val context = LocalContext.current.applicationContext
    val dataStoreManager = remember { DataStoreManager(context) }
    val factory = remember(dataStoreManager) {
        NotificationsViewModelFactory(dataStoreManager)
    }

    val viewModel: NotificationsViewModel = viewModel(factory = factory)

    val tabs = listOf(stringResource(R.string.unread), stringResource(R.string.read_notifications))
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val selectedTabIndex by remember { derivedStateOf { pagerState.currentPage } }
    val coroutineScope = rememberCoroutineScope()

    var showNotificationDetailsDialog by remember { mutableStateOf(false) }
    var selectedNotificationForDialog by remember { mutableStateOf<Notification?>(null) }

    LaunchedEffect(selectedTabIndex) {
        viewModel.fetchNotifications()
    }

    Column {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    // Display unread notifications
                    if (viewModel.notificationsState.collectAsState().value == NotificationsState.Loading) {
                        LoadingCard()
                    } else {
                        Log.d("NotificationsScreen", "Unread notifications: ${viewModel.unreadNotificationsState.collectAsState().value}")
                        Notifications(notifications = viewModel.unreadNotificationsState.collectAsState().value, onNotificationClick = { notification ->
                            selectedNotificationForDialog = notification
                            showNotificationDetailsDialog = true
                        })
                    }
                }
                1 -> {
                    // Display read notifications
                    if (viewModel.notificationsState.collectAsState().value == NotificationsState.Loading) {
                        LoadingCard()
                    } else {
                        Notifications(
                            notifications = viewModel.readNotificationsState.collectAsState().value
                        )
                    }
                }
            }
        }
    }

    if (showNotificationDetailsDialog && selectedNotificationForDialog != null) {
        NotificationDetailDialog(
            notification = selectedNotificationForDialog!!,
            onDismissRequest = { showNotificationDetailsDialog = false },
            onMarkAsRead = { notificationToMark ->
                viewModel.markNotificationAsRead(notificationToMark)
                // Dialog will dismiss via onDismissRequest called after action
            }
        )
    }
}