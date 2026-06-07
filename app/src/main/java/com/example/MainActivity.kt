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

import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.content.pm.ActivityInfo
import android.content.res.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: AppViewModel = viewModel()
                val currentScreen by viewModel.currentScreen.collectAsState()
                val isFullscreen by viewModel.isFullscreen.collectAsState()

                val context = LocalContext.current
                LaunchedEffect(isFullscreen) {
                    val activity = context as? android.app.Activity ?: return@LaunchedEffect
                    val window = activity.window
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    if (isFullscreen) {
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        
                        // Mobile full-screen: FORCE HORIZONTAL
                        val configuration = activity.resources.configuration
                        if (configuration.screenWidthDp < 600) {
                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        }
                    } else {
                        controller.show(WindowInsetsCompat.Type.systemBars())
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }

                // Check responsiveness based on screen configurations
                val configuration = LocalConfiguration.current
                val isExpanded = configuration.screenWidthDp > 600

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (!isExpanded && !isFullscreen) {
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
                            .padding(if (isFullscreen) PaddingValues(0.dp) else innerPadding)
                    ) {
                        if (isExpanded && !isFullscreen) {
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
