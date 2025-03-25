package ru.gd_alt.youwilldrive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import ru.gd_alt.youwilldrive.models.SupabaseClient
import ru.gd_alt.youwilldrive.ui.navigation.NavigationGraph
import ru.gd_alt.youwilldrive.ui.screens.Login.LoginScreenPreview
import ru.gd_alt.youwilldrive.ui.theme.YouWillDriveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val client = SupabaseClient.client
        enableEdgeToEdge()
        setContent {
            YouWillDriveTheme {
                NavigationGraph()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    YouWillDriveTheme {
        Greeting("Android")
    }
}