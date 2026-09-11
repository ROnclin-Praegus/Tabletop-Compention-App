package com.homebrew.tabletopcompanion

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
import com.homebrew.tabletopcompanion.ui.theme.DarkBackground
import com.homebrew.tabletopcompanion.ui.theme.GoldAccent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import com.homebrew.tabletopcompanion.ui.theme.HomebrewRPGTheme
import com.homebrew.tabletopcompanion.utils.GitHubUpdateChecker
import com.homebrew.tabletopcompanion.utils.UpdateInfo
import com.homebrew.tabletopcompanion.R
import kotlinx.coroutines.launch

sealed class Screen {
    object Select : Screen()
    data class Use(val character: Character) : Screen()
}

class MainActivity : ComponentActivity() {
    private lateinit var repository: CharacterRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = CharacterRepository(this)

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
                    var showPremiumImage by remember { mutableStateOf(false) }
                    var currentPremiumImageResId by remember { mutableStateOf(R.drawable.images1) }

                    LaunchedEffect(isPremium, showPremiumImage) {
                        if (isPremium && !showPremiumImage) {
                            // Delay 15 seconds for debug
                            kotlinx.coroutines.delay(900000L) // 15 minutes
                            val images = listOf(
                                R.drawable.images1,
                                R.drawable.images2,
                                R.drawable.images3,
                                R.drawable.images4,
                                R.drawable.images5
                            )
                            currentPremiumImageResId = images.random()
                            showPremiumImage = true
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
                                onSetPremium = { 
                                    repository.setPremium(it)
                                    isPremium = it 
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

                    if (showPremiumImage) {
                        androidx.compose.ui.window.Dialog(
                            onDismissRequest = { showPremiumImage = false },
                            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = currentPremiumImageResId),
                                    contentDescription = "Premium Image",
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
                                    androidx.compose.material3.Text("Close", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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
