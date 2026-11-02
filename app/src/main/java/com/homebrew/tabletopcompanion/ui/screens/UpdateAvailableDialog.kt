package com.homebrew.tabletopcompanion.ui.screens

import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.homebrew.tabletopcompanion.model.Character
import com.homebrew.tabletopcompanion.ui.theme.*
import com.homebrew.tabletopcompanion.utils.GitHubUpdateChecker
import com.homebrew.tabletopcompanion.utils.UpdateInfo
import kotlinx.coroutines.launch
import com.google.gson.Gson

@Composable
fun UpdateAvailableDialog(
    updateInfo: UpdateInfo,
    currentVersion: String,
    characters: List<Character>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showBackupPrompt by remember { mutableStateOf(false) }
    var backupFailedCount by remember { mutableStateOf<Int?>(null) }

    fun startUpdate() {
        showBackupPrompt = false
        isDownloading = true
        statusMessage = "Downloading update..."
        scope.launch {
            val downloadedFile = GitHubUpdateChecker.downloadApk(
                context = context,
                downloadUrl = updateInfo.apkDownloadUrl,
                onProgress = { progress ->
                    downloadProgress = progress
                }
            )

            if (downloadedFile != null && downloadedFile.exists()) {
                statusMessage = "Opening package installer..."
                GitHubUpdateChecker.promptInstallApk(context, downloadedFile)
                isDownloading = false
                onDismiss()
            } else {
                isDownloading = false
                statusMessage = "Download failed. Please check your internet connection."
            }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) {
            // User cancelled the folder picker
            Toast.makeText(context, "Backup cancelled. Starting update...", Toast.LENGTH_SHORT).show()
            startUpdate()
            return@rememberLauncherForActivityResult
        }
        val gson = Gson()
        var saved = 0
        var failed = 0
        val folder = try {
            DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        } catch (e: Exception) {
            null
        }
        characters.forEach { char ->
            // One broken hero must not stop the others from being saved.
            try {
                val file = DocumentsContract.createDocument(
                    context.contentResolver, folder!!, "application/json", safeFileName(char.name) + ".json"
                )
                context.contentResolver.openOutputStream(file!!)?.use { out ->
                    out.write(gson.toJson(char).toByteArray())
                }
                saved++
            } catch (e: Exception) {
                failed++
            }
        }
        if (failed == 0) {
            Toast.makeText(context, "Backed up $saved characters successfully!", Toast.LENGTH_LONG).show()
            startUpdate()
        } else {
            // Do not install an update over heroes that are not saved without asking first.
            backupFailedCount = failed
        }
    }

    backupFailedCount?.let { failed ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Backup incomplete", fontWeight = FontWeight.Bold, color = GoldAccent) },
            text = { Text("$failed character(s) could not be saved. Update anyway?", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = { backupFailedCount = null; startUpdate() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkBackground)
                ) { Text("UPDATE ANYWAY", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { backupFailedCount = null }) { Text("CANCEL", color = TextSecondary) }
            },
            containerColor = DarkSurface
        )
    }

    if (showBackupPrompt) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Backup Characters?", fontWeight = FontWeight.Bold, color = GoldAccent) },
            text = { Text("Would you like to backup all your characters before installing the update?", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = { backupLauncher.launch(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkBackground)
                ) {
                    Text("YES, BACKUP FIRST", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { startUpdate() }) {
                    Text("NO, JUST UPDATE", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    Dialog(
        onDismissRequest = { if (!isDownloading && !showBackupPrompt && backupFailedCount == null) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🚀", fontSize = 26.sp) // rocket
                    Column {
                        Text(
                            text = "UPDATE AVAILABLE!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GoldAccent
                        )
                        Text(
                            text = "A new version of Homebrew RPG Companion is ready",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkSurfaceVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // VERSION BADGES
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CURRENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text(currentVersion, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text("→", fontSize = 16.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("LATEST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text(updateInfo.versionTag, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GreenHp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // CHANGELOG BOX
                Text(
                    text = "CHANGELOG",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (updateInfo.changelog.isNotBlank()) updateInfo.changelog else "Bug fixes and performance improvements.",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // DOWNLOAD PROGRESS / STATUS
                if (isDownloading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = GoldAccent,
                            trackColor = DarkSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = statusMessage ?: "Downloading update... %",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                } else {
                    statusMessage?.let { msg ->
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = CrimsonPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("LATER", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (characters.isNotEmpty()) {
                                    showBackupPrompt = true
                                } else {
                                    startUpdate()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAccent,
                                contentColor = DarkBackground
                            )
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DOWNLOAD & INSTALL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/** Turns a hero name into a safe file name: characters a file name cannot contain become "_". */
internal fun safeFileName(name: String): String =
    name.replace(Regex("""[\/:*?"<>|]"""), "_").trim().ifEmpty { "hero" }
