package com.homebrew.tabletopcompanion.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.homebrew.tabletopcompanion.MainActivity
import com.homebrew.tabletopcompanion.R
import com.homebrew.tabletopcompanion.data.CharacterRepository
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class PhotoApiWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("PhotoAPI", "PhotoApiWorker doWork() triggered in background")
        val repository = CharacterRepository(context)

        val isPremium = repository.isPremium()
        val photoApiUrl = repository.getPhotoApiUrl()
        val photoApiKey = repository.getPhotoApiKey()
        val intervalMinutes = repository.getPhotoIntervalMinutes()

        if (!isPremium || photoApiUrl.isBlank()) {
            Log.d("PhotoAPI", "PhotoApiWorker: Premium disabled or URL blank; skipping fetch")
            return Result.success()
        }

        var photoPath: String? = null
        try {
            Log.d("PhotoAPI", "PhotoApiWorker fetching from '$photoApiUrl'")
            val conn = URL(photoApiUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "image/*, application/json")
            if (photoApiKey.isNotBlank()) {
                conn.setRequestProperty("Authorization", "Bearer $photoApiKey")
            }
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.instanceFollowRedirects = true

            val code = conn.responseCode
            val contentType = conn.contentType ?: "unknown"
            Log.d("PhotoAPI", "PhotoApiWorker response: HTTP $code ($contentType)")

            if (code in 200..299) {
                if (contentType.contains("json", ignoreCase = true)) {
                    val json = conn.inputStream.bufferedReader().readText()
                    val imageUrl = extractImageUrl(json)
                    Log.d("PhotoAPI", "PhotoApiWorker JSON image URL extracted: $imageUrl")

                    if (!imageUrl.isNullOrBlank()) {
                        val imgConn = URL(imageUrl).openConnection() as HttpURLConnection
                        imgConn.connectTimeout = 10_000
                        imgConn.readTimeout = 10_000
                        val tempFile = File(context.cacheDir, "premium_photo_${System.currentTimeMillis()}.jpg")
                        imgConn.inputStream.use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        photoPath = tempFile.absolutePath
                    }
                } else {
                    val tempFile = File(context.cacheDir, "premium_photo_${System.currentTimeMillis()}.jpg")
                    conn.inputStream.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    photoPath = tempFile.absolutePath
                }
            } else {
                Log.e("PhotoAPI", "PhotoApiWorker fetch HTTP error: $code")
            }
        } catch (e: Exception) {
            Log.e("PhotoAPI", "PhotoApiWorker fetch EXCEPTION: ${e.message}", e)
        }

        // Post appropriate notification based on fetch result
        if (!photoPath.isNullOrBlank()) {
            Log.d("PhotoAPI", "PhotoApiWorker fetch SUCCESS -> posting notification 'a suprise is ready for you'")
            showNotification(
                context = context,
                title = "🖼 Premium Photo Feed",
                message = "a suprise is ready for you",
                photoPath = photoPath
            )
        } else {
            Log.w("PhotoAPI", "PhotoApiWorker fetch FAILED -> posting notification 'no photo was found'")
            showNotification(
                context = context,
                title = "🖼 Premium Photo Feed",
                message = "no photo was found",
                photoPath = null
            )
        }

        // Schedule next execution according to configured interval
        scheduleNext(context, intervalMinutes)

        return Result.success()
    }

    private fun showNotification(context: Context, title: String, message: String, photoPath: String?) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_PHOTO, true)
            if (photoPath != null) {
                putExtra(EXTRA_PHOTO_PATH, photoPath)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun extractImageUrl(json: String): String? {
        return try {
            val obj = JSONObject(json)
            for (key in listOf("url", "download_url", "link", "image_url", "photo_url", "file_url")) {
                if (obj.has(key) && !obj.isNull(key)) {
                    val v = obj.optString(key)
                    if (v.startsWith("http")) return v
                }
            }
            if (obj.has("urls")) {
                val urls = obj.optJSONObject("urls")
                val v = urls?.optString("regular") ?: urls?.optString("full") ?: ""
                if (v.startsWith("http")) return v
            }
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

    companion object {
        const val CHANNEL_ID = "photo_feed_channel"
        const val NOTIFICATION_ID = 8099
        const val WORK_NAME = "PhotoApiWorkerWork"
        const val EXTRA_OPEN_PHOTO = "open_premium_photo"
        const val EXTRA_PHOTO_PATH = "premium_photo_path"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Premium Photo Feed Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for background photo feed updates"
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }

        fun scheduleNext(context: Context, intervalMinutes: Int) {
            val delay = if (intervalMinutes < 1) 15L else intervalMinutes.toLong()
            Log.d("PhotoAPI", "Scheduling next PhotoApiWorker in $delay minutes")
            val workRequest = OneTimeWorkRequestBuilder<PhotoApiWorker>()
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun cancelWork(context: Context) {
            Log.d("PhotoAPI", "Canceling PhotoApiWorker background work")
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
