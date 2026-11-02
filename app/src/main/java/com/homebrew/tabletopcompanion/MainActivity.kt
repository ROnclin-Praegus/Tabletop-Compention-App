package com.homebrew.tabletopcompanion

import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import android.widget.Toast
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.work.ExistingWorkPolicy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.homebrew.tabletopcompanion.data.CharacterRepository
import com.homebrew.tabletopcompanion.model.Character
import com.homebrew.tabletopcompanion.ui.screens.CharacterSelectScreen
import com.homebrew.tabletopcompanion.ui.screens.UpdateAvailableDialog
import com.homebrew.tabletopcompanion.ui.screens.UseCharacterScreen
import com.homebrew.tabletopcompanion.ui.screens.PremiumPhotoApiDialog
import com.homebrew.tabletopcompanion.ui.theme.DarkBackground
import com.homebrew.tabletopcompanion.ui.theme.GoldAccent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import com.homebrew.tabletopcompanion.ui.theme.HomebrewRPGTheme
import com.homebrew.tabletopcompanion.utils.GitHubUpdateChecker
import com.homebrew.tabletopcompanion.utils.UpdateInfo
import android.content.Intent
import com.homebrew.tabletopcompanion.worker.PhotoApiWorker
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Screen {
    object Select : Screen()
    data class Use(val character: Character) : Screen()
}

class MainActivity : ComponentActivity() {
    private lateinit var repository: CharacterRepository
    private var pendingPhotoPathState = mutableStateOf<String?>(null)
    private var openPendingPhotoState = mutableStateOf(false)

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    /**
     * True only if [path] really points inside our cache folder. MainActivity is exported, so the path
     * can come from another app; comparing real (canonical) paths stops tricks like "<cache>/../shared_prefs/x".
     */
    private fun isInsideCache(path: String): Boolean = try {
        File(path).canonicalPath.startsWith(cacheDir.canonicalPath + File.separator)
    } catch (e: Exception) {
        false
    }

    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let {
            if (it.getBooleanExtra(PhotoApiWorker.EXTRA_OPEN_PHOTO, false)) {
                val path = it.getStringExtra(PhotoApiWorker.EXTRA_PHOTO_PATH)?.takeIf { p -> isInsideCache(p) }
                if (!path.isNullOrBlank()) {
                    pendingPhotoPathState.value = path
                    openPendingPhotoState.value = true
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PhotoApiWorker.createNotificationChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { _: Boolean -> }
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        repository = CharacterRepository(this)

        handleNotificationIntent(intent)

        if (repository.isPremium() && repository.getPhotoApiUrl().isNotBlank()) {
            PhotoApiWorker.scheduleNext(this, repository.getPhotoIntervalMinutes(), ExistingWorkPolicy.KEEP)
        }

        setContent {
            HomebrewRPGTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()

                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Select) }
                    var characters by remember { mutableStateOf(repository.getCharacters()) }
                    var unreadableBackup by remember { mutableStateOf(repository.unreadableBackup()) }

                    // Tell the user once if some saved heroes could not be read (a copy of the data is kept).
                    LaunchedEffect(Unit) {
                        val unreadable = repository.takeUnreadableNotice()
                        if (unreadable != 0) {
                            val message = if (unreadable < 0) "Saved heroes could not be read. A copy of the data was kept."
                            else "$unreadable saved hero(es) could not be read and were left out. A copy of the data was kept."
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                    var updateInfoState by remember { mutableStateOf<UpdateInfo?>(null) }
                    var isPremium by remember { mutableStateOf(repository.isPremium()) }

                    // Premium photo API settings (persisted)
                    var photoApiUrl by remember { mutableStateOf(repository.getPhotoApiUrl()) }
                    var photoApiKey by remember { mutableStateOf(repository.getPhotoApiKey()) }
                    var photoIntervalMinutes by remember { mutableStateOf(repository.getPhotoIntervalMinutes()) }

                    // Controls whether the "set up photo API" dialog is shown
                    var showPhotoApiSetupDialog by remember { mutableStateOf(false) }

                    // Premium image display state
                    var showPremiumImage by remember { mutableStateOf(false) }
                    var currentPremiumImageUrl by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(openPendingPhotoState.value) {
                        if (openPendingPhotoState.value) {
                            val path = pendingPhotoPathState.value
                            if (!path.isNullOrBlank()) {
                                currentPremiumImageUrl = path
                                showPremiumImage = true
                            }
                            openPendingPhotoState.value = false
                            pendingPhotoPathState.value = null
                        }
                    }

                    // ── Premium photo timer (Foreground fallback & background WorkManager) ─────
                    val apiConfigured = isPremium && photoApiUrl.isNotBlank()
                    LaunchedEffect(apiConfigured, photoIntervalMinutes, showPremiumImage) {
                        if (apiConfigured && !showPremiumImage) {
                            android.util.Log.d("PhotoAPI", "Photo timer active: next fetch from '$photoApiUrl' in $photoIntervalMinutes min")
                            val delayMs = photoIntervalMinutes.toLong() * 60_000L
                            kotlinx.coroutines.delay(delayMs)

                            android.util.Log.d("PhotoAPI", "Timer elapsed! Initiating photo GET request to: $photoApiUrl")
                            val fetchedUrl = withContext(Dispatchers.IO) {
                                PhotoApiWorker.fetchPhoto(context, photoApiUrl, photoApiKey)
                            }

                            if (fetchedUrl != null) {
                                android.util.Log.d("PhotoAPI", "Displaying photo modal overlay with image: $fetchedUrl")
                                currentPremiumImageUrl = fetchedUrl
                                showPremiumImage = true
                            } else {
                                android.util.Log.w("PhotoAPI", "Photo fetch resulted in null image path; modal will not open.")
                            }
                        }
                    }

                    fun runUpdateCheck(manual: Boolean) {
                        val versionName = try {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
                        } catch (e: Exception) {
                            "Unknown"
                        }
                        scope.launch {
                            val info = GitHubUpdateChecker.checkForUpdates(versionName)
                            if (info != null && info.isNewer) {
                                updateInfoState = info
                            } else if (manual) {
                                Toast.makeText(context, "App is up to date! (v$versionName)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        runUpdateCheck(manual = false)
                    }

                    when (val screen = currentScreen) {
                        is Screen.Select -> {
                            CharacterSelectScreen(
                                characters = characters,
                                isPremium = isPremium,
                                onSetPremium = { value ->
                                    repository.setPremium(value)
                                    isPremium = value
                                    if (value) {
                                        showPhotoApiSetupDialog = true
                                    } else {
                                        PhotoApiWorker.cancelWork(this@MainActivity)
                                    }
                                },
                                onSelectCharacter = { char ->
                                    currentScreen = Screen.Use(char)
                                },
                                onCreateCharacter = { newChar ->
                                    repository.addCharacter(newChar)
                                    characters = repository.getCharacters()
                                },
                                onUpdateCharacter = { updatedChar ->
                                    repository.updateCharacter(updatedChar)
                                    characters = repository.getCharacters()
                                },
                                onDeleteCharacter = { charId ->
                                    repository.deleteCharacter(charId)
                                    characters = repository.getCharacters()
                                },
                                onCheckForUpdates = {
                                    runUpdateCheck(manual = true)
                                },
                                onOpenPhotoApiSettings = {
                                    showPhotoApiSetupDialog = true
                                },
                                photoApiUrl = photoApiUrl,
                                photoIntervalMinutes = photoIntervalMinutes,
                                unreadableBackup = unreadableBackup,
                                onBackupExported = {
                                    repository.clearUnreadableBackup()
                                    unreadableBackup = null
                                }
                            )
                        }
                        is Screen.Use -> {
                            UseCharacterScreen(
                                character = screen.character,
                                onBack = {
                                    currentScreen = Screen.Select
                                },
                                onCharacterUpdated = { updatedHero ->
                                    repository.updateCharacter(updatedHero)
                                    characters = repository.getCharacters()
                                }
                            )
                        }
                    }

                    // ── Photo API setup dialog ──────────────────────────────────────────────
                    if (showPhotoApiSetupDialog) {
                        PremiumPhotoApiDialog(
                            initialApiUrl = photoApiUrl,
                            initialApiKey = photoApiKey,
                            initialIntervalMinutes = photoIntervalMinutes,
                            onDismiss = { showPhotoApiSetupDialog = false },
                            onSave = { url, key, minutes ->
                                repository.setPhotoApiUrl(url)
                                repository.setPhotoApiKey(key)
                                repository.setPhotoIntervalMinutes(minutes)
                                photoApiUrl = url
                                photoApiKey = key
                                photoIntervalMinutes = minutes
                                showPhotoApiSetupDialog = false
                                PhotoApiWorker.scheduleNext(this@MainActivity, minutes)
                                Toast.makeText(context, "Photo feed configured! First photo in $minutes min.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // ── Premium photo full-screen dialog ────────────────────────────────────
                    if (showPremiumImage && currentPremiumImageUrl != null) {
                        val closePhoto = {
                            val pathToDelete = currentPremiumImageUrl
                            currentPremiumImageUrl = null
                            showPremiumImage = false
                            if (pathToDelete != null && isInsideCache(pathToDelete)) {
                                try {
                                    val f = java.io.File(pathToDelete)
                                    if (f.exists()) {
                                        val deleted = f.delete()
                                        android.util.Log.d("PhotoAPI", "Cleaned up temp photo file from cache ($deleted): $pathToDelete")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("PhotoAPI", "Error cleaning up temp file: ${e.message}")
                                }
                            }
                        }

                        androidx.compose.ui.window.Dialog(
                            onDismissRequest = closePhoto,
                            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(androidx.compose.ui.graphics.Color.Black)
                            ) {
                                coil.compose.AsyncImage(
                                    model = currentPremiumImageUrl,
                                    contentDescription = "Premium Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                                androidx.compose.material3.Button(
                                    onClick = closePhoto,
                                    modifier = Modifier
                                        .align(androidx.compose.ui.Alignment.BottomCenter)
                                        .padding(32.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = GoldAccent,
                                        contentColor = DarkBackground
                                    )
                                ) {
                                    androidx.compose.material3.Text(
                                        "Close",
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    updateInfoState?.let { info ->
                        val currentAppVersion = try {
                            packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"
                        } catch (e: Exception) {
                            "Unknown"
                        }

                        UpdateAvailableDialog(
                            updateInfo = info,
                            currentVersion = "v$currentAppVersion",
                            characters = characters,
                            onDismiss = { updateInfoState = null }
                        )
                    }
                }
            }
        }
    }
}

