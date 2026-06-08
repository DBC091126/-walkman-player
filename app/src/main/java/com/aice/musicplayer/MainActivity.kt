package com.aice.musicplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.aice.musicplayer.data.repository.MusicRepository
import com.aice.musicplayer.presentation.navigation.AppNavigation
import com.aice.musicplayer.presentation.theme.WalkmanPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var musicRepository: MusicRepository

    private var hasStoragePermission by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasStoragePermission = checkPermissions()
    }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasStoragePermission = checkPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        checkAndRequestPermissions()

        setContent {
            WalkmanPlayerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        hasStoragePermission = hasStoragePermission,
                        musicRepository = musicRepository
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasStoragePermission = checkPermissions()
    }

    private fun checkPermissions(): Boolean {
        // Android 11+: need MANAGE_EXTERNAL_STORAGE for SD card access
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                return false
            }
        }

        // Android 13+: need READ_MEDIA_AUDIO
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        // Older: need READ_EXTERNAL_STORAGE
        else {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        return true
    }

    private fun checkAndRequestPermissions() {
        hasStoragePermission = checkPermissions()
        if (hasStoragePermission) return

        // Step 1: On Android 11+, request MANAGE_EXTERNAL_STORAGE first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                manageStorageLauncher.launch(intent)
                return
            }
        }

        // Step 2: Request media/read permissions
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }
}
