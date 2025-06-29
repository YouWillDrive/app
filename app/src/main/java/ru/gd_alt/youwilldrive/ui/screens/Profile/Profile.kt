package ru.gd_alt.youwilldrive.ui.screens.Profile

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import ru.gd_alt.youwilldrive.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.models.Cadet
import ru.gd_alt.youwilldrive.models.Instructor
import ru.gd_alt.youwilldrive.models.Notification
import ru.gd_alt.youwilldrive.models.Participant
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultUser
import ru.gd_alt.youwilldrive.models.User
import ru.gd_alt.youwilldrive.ui.navigation.Route
import ru.gd_alt.youwilldrive.ui.screens.CadetInfo.CadetInfo
import ru.gd_alt.youwilldrive.ui.screens.Calendar.CalendarViewModelFactory
import ru.gd_alt.youwilldrive.ui.screens.Calendar.ProfileViewModelFactory
import ru.gd_alt.youwilldrive.ui.screens.InstructorInfo.InstructorInfo

@Composable
fun ProfileScreen(
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    var userData: Participant? by remember { mutableStateOf(null) }
    var user: User? by remember { mutableStateOf(null) }

    val context = LocalContext.current.applicationContext
    val dataStoreManager = remember { DataStoreManager(context) }
    val factory = remember(dataStoreManager) {
        ProfileViewModelFactory(dataStoreManager)
    }

    val viewModel: ProfileViewModel = viewModel(factory = factory)

    LaunchedEffect(scope) {
        viewModel.fetchData()
    }
    userData = viewModel.userDataState.collectAsState().value
    user = viewModel.userState.collectAsState().value

    val userAvatarBitmap by viewModel.userAvatarBitmap.collectAsState()
    val profileState by viewModel.profileState.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            Log.d("ProfileScreen", "Selected image URI: $it")
            viewModel.uploadProfilePhoto(context, it)
        }
    }


    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (viewModel.profileState.collectAsState().value == ProfileState.Loading)
            LoadingProfileHeader()
        else
            ProfileHeader(user ?: DefaultUser, userAvatarBitmap) {
                // Trigger image picker when avatar area is clicked
                imagePickerLauncher.launch("image/*") // Launch picker for image types
            }

        Spacer(modifier = Modifier.height(24.dp))

        if (viewModel.profileState.collectAsState().value == ProfileState.Loading || profileState == ProfileState.UploadingImage)
            Box(Modifier.fillMaxWidth().fillMaxHeight(0.5f)) {
                LoadingCard()
            }
        else {
            Log.d("ProfileScreen", "Loaded userData.")

            when (userData) {
                is Cadet -> {
                    CadetInfo(userData as Cadet, navController = navController)
                }
                is Instructor -> {
                    InstructorInfo(userData as Instructor, navController = navController)
                }
            }

            Log.d("ProfileScreen", "UserData: $userData")
            Log.d("ProfileScreen", "Page loaded successfully.")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                scope.launch {
                    viewModel.logout()
                    // Navigate to Login and clear the entire back stack
                    navController.navigate(Route.Login) {
                        popUpTo(0) { // Pops everything up to the start destination of the graph
                            inclusive = true
                        }
                        launchSingleTop = true // Avoids creating multiple instances of the login screen
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text(text = stringResource(R.string.logout_btn))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingProfileHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.size(16.dp))

        Column column@{
            val surnameHeight = with(LocalDensity.current) {
                MaterialTheme.typography.titleLarge.lineHeight.toDp()
            }
            val nameHeight = with(LocalDensity.current) {
                MaterialTheme.typography.titleMedium.lineHeight.toDp()
            }

            LinearProgressIndicator(
                Modifier
                    .fillMaxWidth(0.3f)
                    .height(surnameHeight)
                    .padding(vertical = surnameHeight / 4),
                color = MaterialTheme.colorScheme.primary.copy(0.5f),
                trackColor = MaterialTheme.colorScheme.primary.copy(0.2f),
                strokeCap = StrokeCap.Butt,
                gapSize = 0.dp
            )


            LinearProgressIndicator(
                Modifier
                    .fillMaxWidth(0.5f)
                    .height(nameHeight)
                    .padding(vertical = nameHeight / 4),
                color = Color.Black.copy(0.5f),
                trackColor = Color.Black.copy(0.2f),
                strokeCap = StrokeCap.Butt,
                gapSize = 0.dp
            )
        }
    }
}

@Composable
fun ProfileHeader(user: User) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.size(16.dp))

        Column {
            Text(
                text = user.surname,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${user.name} ${user.patronymic}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Composable for displaying the user's profile header with an avatar and name.
 * The avatar is clickable to allow image selection.
 *
 * @param user The User object containing profile information.
 * @param avatarBitmap The ImageBitmap to display as the avatar, or null for default icon.
 * @param onAvatarClick Lambda to be invoked when the avatar is clicked.
 */
@Composable
fun ProfileHeader(user: User, avatarBitmap: ImageBitmap?, onAvatarClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape) // Clip to a circular shape
                .background(MaterialTheme.colorScheme.primary) // Background color for the circle
                .clickable(onClick = onAvatarClick), // Make the entire Box clickable
            contentAlignment = Alignment.Center
        ) {
            if (avatarBitmap != null) {
                // Display the loaded avatar bitmap
                Image(
                    painter = BitmapPainter(avatarBitmap),
                    contentDescription = stringResource(R.string.profile),
                    modifier = Modifier.fillMaxSize(), // Fill the entire Box
                    contentScale = ContentScale.Crop // Crop the image to fill the circular shape
                )
            } else {
                // Display a default person icon if no avatar is loaded
                Icon(
                    Icons.Default.Person,
                    contentDescription = stringResource(R.string.profile),
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.size(16.dp))

        Column {
            Text(
                text = user.surname,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${user.name} ${user.patronymic}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingCard(
    colors: CardColors = CardDefaults.cardColors()
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = colors
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                Modifier
                    .padding(50.dp)
                    .size(100.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeCap = StrokeCap.Round,
                strokeWidth = 10.dp
            )
        }
    }
}