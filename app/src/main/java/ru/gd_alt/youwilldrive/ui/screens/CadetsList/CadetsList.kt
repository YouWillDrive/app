package ru.gd_alt.youwilldrive.ui.screens.CadetsList

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import ru.gd_alt.youwilldrive.R
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.models.Cadet
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultCadet
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultInstructor
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultUser
import ru.gd_alt.youwilldrive.models.User
import ru.gd_alt.youwilldrive.ui.navigation.Route

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
    viewModel: CadetListsViewModel = viewModel(),
    navController: NavController = rememberNavController()
) {
    val context = LocalContext.current.applicationContext
    val dataStoreManager = remember { DataStoreManager(context) }
    var cadets by remember { mutableStateOf(listOf(DefaultCadet, DefaultCadet, DefaultCadet, DefaultCadet)) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(scope) {
        val user = User.fromId(dataStoreManager.getUserId().first { !it.isNullOrEmpty() } ?: "")

        viewModel.fetchCadets(user?.isInstructor() ?: DefaultInstructor) { data, _ ->
//            cadets = data ?: emptyList()
        }
    }

    Column(
        Modifier
            .padding(16.dp)
    ) {
        for (cadet in cadets) {
            CadetCard(cadet) {
                navController.navigate(Route.Chat)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun CadetCard(cadet: Cadet, onClick: () -> Unit = {}) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier.fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(
                    /* "${cadet.me().surname} ${cadet.me().name.first()}. ${cadet.me().patronymic.first()}.", */
                    text = "${DefaultUser.surname} ${DefaultUser.name.first()}. ${DefaultUser.patronymic.first()}.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    { /* TODO: Open chat */ }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Message,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${cadet.hoursAlready}", style = MaterialTheme.typography.bodyMedium)
                Text("${50}", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { cadet.hoursAlready.toFloat() / 50.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
