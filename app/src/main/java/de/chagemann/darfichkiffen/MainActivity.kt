package de.chagemann.darfichkiffen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import de.chagemann.darfichkiffen.map.MapScreen
import de.chagemann.darfichkiffen.ui.theme.DarfIchKiffenTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DarfIchKiffenTheme {
                ScreenNavHost()
            }
        }
    }
}

enum class Screen {
    Map,
    Settings,
    About,
}

@Composable
fun ScreenNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Map.name
    ) {
        composable(route = Screen.Map.name) {
            MapScreen()
        }
    }
}

@Preview
@Composable
private fun ScreenNavHostPreview() {
    DarfIchKiffenTheme {
        ScreenNavHost()
    }
}