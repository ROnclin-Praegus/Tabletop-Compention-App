package com.homebrew.tabletopcompanion

import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed class Screen {
    object Select : Screen()
    data class Use(val character: Character) : Screen()
}

class MainActivity : ComponentActivity() {
    private lateinit var repository: CharacterRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { _: Boolean -> }
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        repository = CharacterRepository(this)
        com.homebrew.tabletopcompanion.server.GameServerManager.init(this)

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

                    LaunchedEffect(Unit) {
                        com.homebrew.tabletopcompanion.server.GameServerManager.onSyncReceived = { jsonData ->
                            try {
                                val gson = com.google.gson.Gson()
                                val syncedChar = gson.fromJson(jsonData, Character::class.java)
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, "Upload received: ${syncedChar.name}!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    // ── Premium photo timer ─────────────────────────────────────────────────
                    // Restarts whenever isPremium, photoApiUrl, photoApiKey, or the interval
                    // changes.  Only runs when the API is fully configured.
                    val apiConfigured = isPremium && photoApiUrl.isNotBlank()
                    LaunchedEffect(apiConfigured, photoIntervalMinutes, showPremiumImage) {
                        if (apiConfigured && !showPremiumImage) {
                            val delayMs = photoIntervalMinutes.toLong() * 60_000L
                            kotlinx.coroutines.delay(delayMs)

                            // Fetch a random photo URL from the user-configured endpoint
                            val fetchedUrl = withContext(Dispatchers.IO) {
                                try {
                                    val conn = URL(photoApiUrl).openConnection() as HttpURLConnection
                                    conn.requestMethod = "GET"
                                    conn.setRequestProperty("Accept", "application/json")
                                    if (photoApiKey.isNotBlank()) {
                                        conn.setRequestProperty("Authorization", "Bearer $photoApiKey")
                                    }
                                    conn.connectTimeout = 10_000
                                    conn.readTimeout = 10_000
                                    if (conn.responseCode == 200) {
                                        val json = conn.inputStream.bufferedReader().readText()
                                        // Try common fields: "url", "src", "download_url", "urls.regular"
                                        extractImageUrl(json)
                                    } else null
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    null
                                }
                            }

                            if (fetchedUrl != null) {
                                currentPremiumImageUrl = fetchedUrl
                                showPremiumImage = true
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
                                    // When activating premium, ask the user to configure the photo API
                                    if (value) {
                                        showPhotoApiSetupDialog = true
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
                                photoIntervalMinutes = photoIntervalMinutes
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
                                Toast.makeText(context, "Photo feed configured! First photo in $minutes min.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // ── Premium photo full-screen dialog ────────────────────────────────────
                    if (showPremiumImage && currentPremiumImageUrl != null) {
                        androidx.compose.ui.window.Dialog(
                            onDismissRequest = { showPremiumImage = false },
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
                                    onClick = { showPremiumImage = false },
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

/**
 * Heuristically extracts an image URL from a JSON response string.
 * Supports common photo-API patterns:
 *  - `{ "url": "..." }`
 *  - `{ "download_url": "..." }` (picsum.photos)
 *  - `{ "src": { "original": "..." } }` (Pexels)
 *  - `{ "urls": { "regular": "..." } }` (Unsplash)
 *  - `{ "link": "..." }`
 *  - `{ "image_url": "..." }`
 */
private fun extractImageUrl(json: String): String? {
    return try {
        val obj = JSONObject(json)
        // Direct string fields
        for (key in listOf("url", "download_url", "link", "image_url", "photo_url", "file_url")) {
            if (obj.has(key) && !obj.isNull(key)) {
                val v = obj.optString(key)
                if (v.startsWith("http")) return v
            }
        }
        // Nested: urls.regular / urls.full (Unsplash)
        if (obj.has("urls")) {
            val urls = obj.optJSONObject("urls")
            val v = urls?.optString("regular") ?: urls?.optString("full") ?: ""
            if (v.startsWith("http")) return v
        }
        // Nested: src.original / src.large (Pexels)
        if (obj.has("src")) {
            val src = obj.optJSONObject("src")
            val v = src?.optString("original") ?: src?.optString("large") ?: ""
            if (v.startsWith("http")) return v
        }
        null
    } catch (e: Exception) {
        null
    }
}
