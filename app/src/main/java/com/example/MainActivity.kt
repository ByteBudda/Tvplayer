package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppViewModel
import com.example.ui.components.AdaptiveBottomBar
import com.example.ui.components.AdaptiveNavigationRail
import com.example.ui.screens.ParentalScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.RecordingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: AppViewModel = viewModel()
                val currentScreen by viewModel.currentScreen.collectAsState()

                // Check responsiveness based on screen configurations
                val configuration = LocalConfiguration.current
                val isExpanded = configuration.screenWidthDp > 600

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (!isExpanded) {
                            AdaptiveBottomBar(
                                currentScreen = currentScreen,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isExpanded) {
                            AdaptiveNavigationRail(
                                currentScreen = currentScreen,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            when (currentScreen) {
                                AppViewModel.Screen.PLAYER -> PlayerScreen(viewModel = viewModel)
                                AppViewModel.Screen.PLAYLISTS -> PlaylistsScreen(viewModel = viewModel)
                                AppViewModel.Screen.RECORDINGS -> RecordingsScreen(viewModel = viewModel)
                                AppViewModel.Screen.PARENTAL -> ParentalScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
