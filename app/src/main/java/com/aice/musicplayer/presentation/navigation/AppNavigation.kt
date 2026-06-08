package com.aice.musicplayer.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aice.musicplayer.data.repository.MusicRepository
import com.aice.musicplayer.presentation.components.MiniPlayer
import com.aice.musicplayer.presentation.folder.FolderViewModel
import com.aice.musicplayer.presentation.player.PlayerViewModel
import com.aice.musicplayer.presentation.screen.FolderBrowserScreen
import com.aice.musicplayer.presentation.screen.LibraryScreen
import com.aice.musicplayer.presentation.screen.NowPlayingScreen
import com.aice.musicplayer.presentation.theme.BlackPure
import com.aice.musicplayer.presentation.theme.BlackSurface
import com.aice.musicplayer.presentation.theme.GoldPrimary
import com.aice.musicplayer.presentation.theme.WhiteSecondary

sealed class Screen(val route: String, val label: String) {
    data object Library : Screen("library", "曲库")
    data object Folders : Screen("folders", "文件夹")
    data object NowPlaying : Screen("now_playing", "正在播放")
}

@Composable
fun AppNavigation(
    hasStoragePermission: Boolean,
    musicRepository: MusicRepository,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    folderViewModel: FolderViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val playerState by playerViewModel.playerState.collectAsState()
    val currentSong = playerState.currentSong

    val screens = listOf(Screen.Library, Screen.Folders)

    Scaffold(
        modifier = Modifier.background(BlackPure),
        containerColor = BlackPure,
        bottomBar = {
            // Only show bottom bar when not on NowPlaying screen
            if (currentRoute != Screen.NowPlaying.route) {
                NavigationBar(
                    containerColor = BlackSurface,
                    contentColor = GoldPrimary
                ) {
                    screens.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Library.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        Screen.Library -> Icons.Default.LibraryMusic
                                        Screen.Folders -> Icons.Default.Folder
                                        else -> Icons.Default.Album
                                    },
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = GoldPrimary,
                                selectedTextColor = GoldPrimary,
                                unselectedIconColor = WhiteSecondary,
                                unselectedTextColor = WhiteSecondary,
                                indicatorColor = GoldPrimary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Library.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Library.route) {
                    LibraryScreen(
                        musicRepository = musicRepository,
                        playerViewModel = playerViewModel
                    )
                }
                composable(Screen.Folders.route) {
                    FolderBrowserScreen(
                        folderViewModel = folderViewModel,
                        playerViewModel = playerViewModel,
                        onNowPlaying = {
                            navController.navigate(Screen.NowPlaying.route)
                        }
                    )
                }
                composable(Screen.NowPlaying.route) {
                    NowPlayingScreen(
                        viewModel = playerViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Mini Player — overlays bottom, hidden on NowPlaying screen
            if (currentRoute != Screen.NowPlaying.route) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .zIndex(1f)
                ) {
                    MiniPlayer(
                        currentSong = currentSong,
                        isPlaying = playerState.isPlaying,
                        onPlayPauseClick = { playerViewModel.togglePlayPause() },
                        onSkipNext = { playerViewModel.skipNext() },
                        onSkipPrevious = { playerViewModel.skipPrevious() },
                        onMiniPlayerClick = {
                            navController.navigate(Screen.NowPlaying.route)
                        }
                    )
                }
            }
        }
    }
}
