package ru.gd_alt.youwilldrive.ui.screens.CadetInfo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
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
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultUser
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultUser1
import ru.gd_alt.youwilldrive.models.Plan
import ru.gd_alt.youwilldrive.models.User
import ru.gd_alt.youwilldrive.ui.navigation.Route
import ru.gd_alt.youwilldrive.ui.screens.CadetsList.CadetListsViewModel
import ru.gd_alt.youwilldrive.ui.screens.CadetsList.CadetsListViewModelFactory
import ru.gd_alt.youwilldrive.ui.screens.Profile.LoadingCard


@Composable
fun CadetInfo(
    cadet: Cadet = DefaultCadet,
    navController: NavController = rememberNavController()
) {
    val context = LocalContext.current.applicationContext
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()
    var plan: Plan? by remember { mutableStateOf(null) }
    var instructorUser: User? by remember { mutableStateOf(null) }

    val factory = remember { CadetInfoViewModelFactory(dataStoreManager) }
    val viewModel: CadetInfoViewModel = viewModel(factory = factory)
    val instructorAvatarBitmap by viewModel.instructorAvatarBitmap.collectAsState()
    val unreadMessagesCount by viewModel.unreadMessagesCount.collectAsState()

    LaunchedEffect(scope) {
        viewModel.fetchInstructorUser(cadet) { data, _ ->
            instructorUser = data
        }
        viewModel.fetchPlan(cadet) { data, _ ->
            plan = data
        }
    }

    LaunchedEffect(instructorUser) {
        if (instructorUser != null) {
            viewModel.fetchUnreadMessagesCount(cadet, instructorUser!!)
        }
    }

    if (viewModel.planState.collectAsState().value == PlanState.Loading) {
        Box(Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)) {
            LoadingCard()
        }
    }

    CadetInfoRows(
        planName = plan?.name ?: stringResource(R.string.plan),
        practiceHours = cadet.hoursAlready,
        totalPractice = plan?.practiceHours ?: 50
    )

    Spacer(Modifier.height(20.dp))

    InstructorCard(instructorUser ?: DefaultUser1, instructorAvatar = instructorAvatarBitmap, unreadMessagesCount) { navController.navigate("${Route.Chat}/${instructorUser!!.id}") }
}

@Composable
fun CadetInfoRows(
    planName: String = "Тариф",
    practiceHours: Int = 25,
    totalPractice: Int = 50
) {
    Card(
        Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.plan),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    planName,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(R.string.practice_hours),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$practiceHours", style = MaterialTheme.typography.bodyMedium)
                Text("$totalPractice", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { practiceHours.toFloat() / totalPractice.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}


@Preview
@Composable
fun InstructorCard(
    user: User = DefaultUser,
    instructorAvatar: ImageBitmap? = null,
    unreadMessagesCount: Int = 0,
    onClick: () -> Unit = {}
) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth(),
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
                        if (instructorAvatar != null) {
                            Image(
                                painter = BitmapPainter(instructorAvatar),
                                contentDescription = stringResource(R.string.profile),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
                Text(
                    /* "${cadet.me().surname} ${cadet.me().name.first()}. ${cadet.me().patronymic.first()}.", */
                    text = "${user.surname} ${user.name.first()}. ${user.patronymic.first()}.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    { onClick() }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Message,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
