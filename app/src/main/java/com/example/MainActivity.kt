package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.MusicViewModel
import com.example.ui.MusicViewModelFactory
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels {
        MusicViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("home") {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToPlayer = { navController.navigate("player") },
                            onNavigateToPlaylists = { navController.navigate("playlists") }
                        )
                    }
                    composable("player") {
                        PlayerScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.navigateUp() }
                        )
                    }
                    composable("playlists") {
                        PlaylistsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.navigateUp() }
                        )
                    }
                }
            }
        }
    }
}
