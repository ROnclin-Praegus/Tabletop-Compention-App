package com.homebrew.tabletopcompanion.server

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import java.util.UUID

object GameServerManager {
    var server: LocalGameServer? = null
    var isRunning = false
    var apiKey: String = ""
    var uploads = mutableListOf<UploadRecord>()
    var onSyncReceived: ((String) -> Unit)? = null
    
    private const val PREFS_NAME = "GameServerPrefs"
    private const val KEY_API_KEY = "apiKey"

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        apiKey = prefs.getString(KEY_API_KEY, null) ?: generateNewKey(context)
    }

    fun generateNewKey(context: Context): String {
        val newKey = UUID.randomUUID().toString().substring(0, 8).uppercase()
        apiKey = newKey
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_API_KEY, newKey).apply()
        return newKey
    }

    fun startServer(context: Context) {
        if (isRunning) return
        try {
            server = LocalGameServer(8080) { jsonData ->
                addUpload(jsonData)
            }
            server?.start()
            isRunning = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopServer() {
        if (!isRunning) return
        try {
            server?.stop()
            server = null
            isRunning = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addUpload(jsonData: String) {
        val record = UploadRecord(System.currentTimeMillis(), jsonData)
        uploads.add(0, record)
        if (uploads.size > 5) {
            uploads = uploads.take(5).toMutableList()
        }
        onSyncReceived?.invoke(jsonData)
    }

    fun getLocalIpAddress(context: Context): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
        } catch (e: Exception) {
            e.printStackTrace()
            "127.0.0.1"
        }
    }
}
