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
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        apiKey = prefs.getString(KEY_API_KEY, null) ?: generateNewKey(context)
        
        val uploadsJson = prefs.getString("uploads_list", null)
        if (uploadsJson != null) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<UploadRecord>>() {}.type
                val list: List<UploadRecord> = com.google.gson.Gson().fromJson(uploadsJson, type)
                uploads = list.toMutableList()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun saveUploads() {
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.putString("uploads_list", com.google.gson.Gson().toJson(uploads))?.apply()
    }
    
    fun removeUpload(upload: UploadRecord) {
        uploads.remove(upload)
        saveUploads()
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
        val gson = com.google.gson.Gson()
        val characterId = try {
            gson.fromJson(jsonData, com.homebrew.tabletopcompanion.model.Character::class.java)?.id
        } catch(e: Exception) { null }
        
        uploads.add(0, record)
        
        if (characterId != null) {
            val charUploads = uploads.filter { 
                try { gson.fromJson(it.jsonData, com.homebrew.tabletopcompanion.model.Character::class.java)?.id == characterId } catch(e:Exception) { false }
            }
            if (charUploads.size > 5) {
                val toRemove = charUploads.drop(5)
                uploads.removeAll(toRemove)
            }
        } else {
             if (uploads.size > 50) uploads = uploads.take(50).toMutableList()
        }
        
        saveUploads()
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
