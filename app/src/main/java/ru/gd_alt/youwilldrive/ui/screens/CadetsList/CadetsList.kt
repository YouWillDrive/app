package ru.gd_alt.youwilldrive.ui.screens.CadetsList

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import ru.gd_alt.youwilldrive.R
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.models.Cadet
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultCadet
import ru.gd_alt.youwilldrive.models.User
import ru.gd_alt.youwilldrive.ui.navigation.Route

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun CadetListScreenPreview() {
    Scaffold(
        Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.cadets), fontWeight = FontWeight.Bold)
                },
                modifier = Modifier.clip(RoundedCornerShape(0.dp, 0.dp, 20.dp, 20.dp)),
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) {
        Box(
            Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            CadetsListScreen()
        }
    }
}

@Composable
fun CadetsListScreen(
    navController: NavController = rememberNavController()
) {
    val context = LocalContext.current.applicationContext
    val dataStoreManager = remember { DataStoreManager(context) }

    val factory = remember { CadetsListViewModelFactory(dataStoreManager) }
    val viewModel: CadetListsViewModel = viewModel(factory = factory)

    val cadets by viewModel.cadets.collectAsState()
    val listState by viewModel.cadetsListState.collectAsState()
    val cadetAvatars by viewModel.cadetAvatars.collectAsState()
    Log.d("CadetsListScreen", "$cadets")
    Log.d("CadetsListScreen", "$cadetAvatars")
    val unreadMessageCounts by viewModel.unreadMessageCounts.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCadets()
    }

    if (listState == CadetsListState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp,
                strokeCap = StrokeCap.Round
            )
        }
    } else {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(cadets) { cadet ->
                // Pass viewModel and navController to the card
                CadetCard(
                    cadet = cadet,
                    viewModel = viewModel,
                    navController = navController,
                    cadetAvatar = cadetAvatars[cadet.id],
                    unreadMessagesCount = unreadMessageCounts[cadet.id] ?: 0
                )
            }
        }
    }
}

@Composable
fun CadetCard(cadet: Cadet, viewModel: CadetListsViewModel, navController: NavController, cadetAvatar: ImageBitmap? = null, unreadMessagesCount: Int = 0) {
    var cadetUser by remember { mutableStateOf<User?>(null) }
    var planPracticeHours by remember { mutableIntStateOf(50) } // Default value

    Log.i("CadetCard", "Messages count for cadet ${cadet.id}: $unreadMessagesCount")

    // Fetch the specific user details for this cadet when the card is composed
    LaunchedEffect(cadet.id) {
        viewModel.fetchCadetUser(cadet) { user, _ ->
            cadetUser = user
        }
        val plan = cadet.actualPlanPoint()?.relatedPlan()
        planPracticeHours = plan?.practiceHours ?: 50
    }

    // Function to handle navigation to chat
    val onChatClick = {
        cadetUser?.id?.let { userId ->
            Log.d("onChatClick", userId)
            navController.navigate("${Route.Chat}/$userId")
        }
    }

    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onChatClick as () -> Unit),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BadgedBox(badge = { Badge { Text(unreadMessagesCount.toString()) } }) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cadetAvatar != null) {
                            Image(
                                painter = BitmapPainter(cadetAvatar),
                                contentDescription = stringResource(R.string.profile),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }
                }
                // Display cadet's name, or "Loading..." if not yet fetched
                Text(
                    text = cadetUser?.let { "${it.surname} ${it.name.first()}. ${it.patronymic.first()}." } ?: "Загрузка...",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                )
                IconButton(onClick = onChatClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.Message,
                        contentDescription = stringResource(R.string.chat),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.practice_hours),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${cadet.hoursAlready} / $planPracticeHours ч.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { cadet.hoursAlready.toFloat() / planPracticeHours.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surface,
                color = MaterialTheme.colorScheme.primary,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
