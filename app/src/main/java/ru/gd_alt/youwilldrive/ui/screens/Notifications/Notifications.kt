package ru.gd_alt.youwilldrive.ui.screens.Notifications

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ru.gd_alt.youwilldrive.R
import ru.gd_alt.youwilldrive.ui.components.Notifications

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun NotificationsScreen(
    navController: NavController? = null,
    viewModel: NotificationsViewModel = viewModel()
) {
    Box(Modifier.fillMaxSize()) {
        Notifications()
    }
}