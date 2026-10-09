package com.homebrew.tabletopcompanion.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homebrew.tabletopcompanion.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Result of a connection test. */
private enum class TestState { IDLE, TESTING, SUCCESS, FAILURE }

/**
 * Dialog shown immediately after the user activates Premium.
 * Lets the user configure:
 *  - A custom photo API endpoint (GET, returns JSON with an image URL)
 *  - An API key sent as a Bearer token / header value
 *  - How many minutes between automatic photo displays
 *
 * Includes a one-shot "Test Connection" button that is permanently
 * disabled once a successful response (HTTP 2xx) is received.
 *
 * All three values are persisted via [onSave].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPhotoApiDialog(
    initialApiUrl: String,
    initialApiKey: String,
    initialIntervalMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (apiUrl: String, apiKey: String, intervalMinutes: Int) -> Unit
) {
    var apiUrl       by remember { mutableStateOf(initialApiUrl) }
    var apiKey       by remember { mutableStateOf(initialApiKey) }
    var intervalText by remember { mutableStateOf(if (initialIntervalMinutes > 0) initialIntervalMinutes.toString() else "15") }
    var intervalError by remember { mutableStateOf<String?>(null) }
    var urlError      by remember { mutableStateOf<String?>(null) }

    // Test-connection state
    var testState   by remember { mutableStateOf(TestState.IDLE) }
    var testMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // The button is disabled while testing OR after a successful test
    val testButtonEnabled = testState != TestState.TESTING && testState != TestState.SUCCESS

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                "🖼 Premium Photo Feed",
                fontWeight = FontWeight.Bold,
                color = GoldAccent
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Configure a custom API endpoint that returns a random photo. " +
                            "The app will make a GET request and display the image at the set interval.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                // --- API Endpoint ---
                Text("API Endpoint URL", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = {
                        apiUrl = it
                        urlError = null
                        // Reset test result when the URL changes
                        if (testState == TestState.SUCCESS || testState == TestState.FAILURE) {
                            testState = TestState.IDLE
                            testMessage = null
                        }
                    },
                    label = { Text("https://your-api.com/photos/random") },
                    singleLine = true,
                    isError = urlError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent,
                        focusedLabelColor = GoldAccent,
                        cursorColor = GoldAccent
                    )
                )
                if (urlError != null) {
                    Text(urlError!!, color = CrimsonPrimary, fontSize = 12.sp)
                }

                // --- API Key ---
                Text("API Key", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("your-api-key (sent as Authorization header)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent,
                        focusedLabelColor = GoldAccent,
                        cursorColor = GoldAccent
                    )
                )
                Text(
                    "Sent as: Authorization: Bearer <key>",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                // --- Interval ---
                Text("Show Photo Every (minutes)", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = {
                        intervalText = it
                        intervalError = null
                    },
                    label = { Text("Minutes (e.g. 15)") },
                    singleLine = true,
                    isError = intervalError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent,
                        focusedLabelColor = GoldAccent,
                        cursorColor = GoldAccent
                    )
                )
                if (intervalError != null) {
                    Text(intervalError!!, color = CrimsonPrimary, fontSize = 12.sp)
                }

                // --- Test Connection ---
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val trimmedUrl = apiUrl.trim()
                            if (trimmedUrl.isBlank() ||
                                (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://"))
                            ) {
                                urlError = "Please enter a valid http(s) URL first"
                                return@Button
                            }
                            testState = TestState.TESTING
                            testMessage = null
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    try {
                                        val conn = URL(trimmedUrl).openConnection() as HttpURLConnection
                                        conn.requestMethod = "GET"
                                        conn.setRequestProperty("Accept", "application/json")
                                        if (apiKey.trim().isNotBlank()) {
                                            conn.setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
                                        }
                                        conn.connectTimeout = 8_000
                                        conn.readTimeout   = 8_000
                                        val code = conn.responseCode
                                        conn.disconnect()
                                        code
                                    } catch (e: Exception) {
                                        -1
                                    }
                                }
                                if (result in 200..299) {
                                    testState   = TestState.SUCCESS
                                    testMessage = "✅ Connected! (HTTP $result)"
                                } else {
                                    testState   = TestState.FAILURE
                                    testMessage = if (result == -1)
                                        "❌ Could not reach the server"
                                    else
                                        "❌ Server returned HTTP $result"
                                }
                            }
                        },
                        enabled = testButtonEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (testState) {
                                TestState.SUCCESS -> GreenHp
                                TestState.FAILURE -> CrimsonPrimary
                                else              -> DarkSurfaceVariant
                            },
                            contentColor = TextPrimary,
                            disabledContainerColor = when (testState) {
                                TestState.SUCCESS -> GreenHp.copy(alpha = 0.5f)
                                else              -> DarkSurfaceVariant.copy(alpha = 0.4f)
                            },
                            disabledContentColor = TextPrimary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        if (testState == TestState.TESTING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = GoldAccent
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Testing…", fontSize = 13.sp)
                        } else {
                            Text(
                                text = when (testState) {
                                    TestState.SUCCESS -> "Connection OK ✓"
                                    TestState.FAILURE -> "Retry Test"
                                    else              -> "Test Connection"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Inline result message next to the button
                    testMessage?.let { msg ->
                        Text(
                            text = msg,
                            fontSize = 12.sp,
                            color = if (testState == TestState.SUCCESS) GreenHp else CrimsonPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedUrl = apiUrl.trim()
                    val minutes = intervalText.trim().toIntOrNull()

                    var valid = true
                    if (trimmedUrl.isBlank() || (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://"))) {
                        urlError = "Please enter a valid http(s) URL"
                        valid = false
                    }
                    if (minutes == null || minutes < 1) {
                        intervalError = "Please enter a number ≥ 1"
                        valid = false
                    }
                    if (valid) {
                        onSave(trimmedUrl, apiKey.trim(), minutes!!)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = DarkBackground
                )
            ) {
                Text("SAVE & ENABLE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("SKIP FOR NOW", color = TextSecondary)
            }
        }
    )
}
