package com.homebrew.tabletopcompanion.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

        // While the app is open, its own timer shows the photo. Skip here so one interval gives one photo.
        val appIsOpen = withContext(Dispatchers.Main) {
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        if (appIsOpen) {
            Log.d("PhotoAPI", "PhotoApiWorker: app is open, the foreground timer handles this round")
            scheduleNext(context, intervalMinutes, ExistingWorkPolicy.APPEND_OR_REPLACE)
            return Result.success()
        }

        val photoPath: String? = fetchPhoto(context, photoApiUrl, photoApiKey)

        // Post appropriate notification based on fetch result
        if (!photoPath.isNullOrBlank()) {
            Log.d("PhotoAPI", "PhotoApiWorker fetch SUCCESS -> posting notification 'a suprise is ready for you'")
            showNotification(
                context = context,
                title = "🖼 Premium Photo Feed",
                message = "a suprise is ready for you",
                photoPath = photoPath
            )
            repository.setPhotoLastFailed(false)
        } else {
            // Tell the user once per outage, not once per interval.
            if (!repository.getPhotoLastFailed()) {
                Log.w("PhotoAPI", "PhotoApiWorker fetch FAILED -> posting notification 'no photo was found'")
                showNotification(
                    context = context,
                    title = "🖼 Premium Photo Feed",
                    message = "no photo was found",
                    photoPath = null
                )
            }
            repository.setPhotoLastFailed(true)
        }

        // Schedule next execution according to configured interval
        scheduleNext(context, intervalMinutes, ExistingWorkPolicy.APPEND_OR_REPLACE)

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


    companion object {
        const val CHANNEL_ID = "photo_feed_channel"
        const val NOTIFICATION_ID = 8099
        const val WORK_NAME = "PhotoApiWorkerWork"
        const val EXTRA_OPEN_PHOTO = "open_premium_photo"
        const val EXTRA_PHOTO_PATH = "premium_photo_path"

        private const val MAX_KEPT_PHOTOS = 3

        /**
         * Downloads one photo into the cache and returns its path, or null on failure.
         * Blocking: call it off the main thread. Shared by the background worker and the foreground timer.
         */
        fun fetchPhoto(context: Context, photoApiUrl: String, photoApiKey: String): String? {
            return try {
                val conn = openConnection(photoApiUrl, photoApiKey)
                val code = conn.responseCode
                val contentType = conn.contentType ?: ""
                Log.d("PhotoAPI", "Photo response: HTTP $code ($contentType)")
                when {
                    code !in 200..299 -> null
                    contentType.contains("json", ignoreCase = true) -> {
                        val json = conn.inputStream.bufferedReader().use { it.readText() }
                        val imageUrl = extractImageUrl(json)
                        if (imageUrl.isNullOrBlank()) null else savePhoto(context, openConnection(imageUrl, null))
                    }
                    // An HTML/text answer is an error page, not a photo.
                    contentType.startsWith("text/", ignoreCase = true) -> null
                    else -> savePhoto(context, conn)
                }
            } catch (e: Exception) {
                Log.e("PhotoAPI", "Photo fetch failed: ${e.message}", e)
                null
            }
        }

        private fun openConnection(url: String, key: String?): HttpURLConnection {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "image/*, application/json")
            if (!key.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $key")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.instanceFollowRedirects = true
            return conn
        }

        private fun savePhoto(context: Context, conn: HttpURLConnection): String {
            val file = File(context.cacheDir, "premium_photo_${System.currentTimeMillis()}.jpg")
            try {
                conn.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
            } catch (e: Exception) {
                file.delete() // never leave a half-written photo behind
                throw e
            }
            // Photos from notifications nobody opened would pile up: keep only the newest few.
            context.cacheDir.listFiles { f -> f.name.startsWith("premium_photo_") }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(MAX_KEPT_PHOTOS)
                ?.forEach { it.delete() }
            return file.absolutePath
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
                val urls = obj.optJSONObject("urls")
                val u = urls?.optString("regular")?.takeIf { it.isNotEmpty() } ?: urls?.optString("full") ?: ""
                if (u.startsWith("http")) return u
                val src = obj.optJSONObject("src")
                val s = src?.optString("original")?.takeIf { it.isNotEmpty() } ?: src?.optString("large") ?: ""
                if (s.startsWith("http")) return s
                null
            } catch (e: Exception) {
                null
            }
        }

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

        /**
         * [policy]: REPLACE when the user changes the interval, KEEP at app start (so opening the app does not
         * push the next run back), APPEND_OR_REPLACE when the worker schedules its own successor.
         */
        fun scheduleNext(context: Context, intervalMinutes: Int, policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE) {
            val delay = if (intervalMinutes < 1) 15L else intervalMinutes.toLong()
            Log.d("PhotoAPI", "Scheduling next PhotoApiWorker in $delay minutes")
            val workRequest = OneTimeWorkRequestBuilder<PhotoApiWorker>()
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                policy,
                workRequest
            )
        }

        fun cancelWork(context: Context) {
            Log.d("PhotoAPI", "Canceling PhotoApiWorker background work")
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
