package com.example.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.DownloadEntity
import com.example.data.GiyaniDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Resumable Background Download Manager Engine & Foreground Service for Giyani Browser.
 * Supports:
 * - Pausing and resuming downloads using HTTP `Range: bytes=<offset>-` and RandomAccessFile.
 * - Real-time calculation of download progress, total file size, transfer speed (bytes/sec), and ETA.
 * - Background downloading via Android Foreground Service (`dataSync`).
 * - Opening completed files via FileProvider or inspecting the download storage directory.
 */
object DownloadEngine {
    const val STATUS_QUEUED = "QUEUED"
    const val STATUS_DOWNLOADING = "DOWNLOADING"
    const val STATUS_PAUSED = "PAUSED"
    const val STATUS_COMPLETED = "COMPLETED"
    const val STATUS_FAILED = "FAILED"
    const val STATUS_CANCELED = "CANCELED"

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun getDownloadDirectory(context: Context): File {
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val baseDir = externalDir ?: File(context.filesDir, "Downloads")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        return baseDir
    }

    fun enqueueNewDownload(
        context: Context,
        url: String,
        suggestedFileName: String? = null,
        mimeType: String? = null
    ) {
        engineScope.launch {
            val dao = GiyaniDatabase.getInstance(context).giyaniDao()
            val cleanFileName = sanitizeFileName(
                suggestedFileName ?: guessFileNameFromUrl(url)
            )
            val dir = getDownloadDirectory(context)
            val targetFile = resolveUniqueFile(dir, cleanFileName)
            val resolvedMime = mimeType?.takeIf { it.isNotBlank() }
                ?: guessMimeType(targetFile.name)

            val entity = DownloadEntity(
                id = UUID.randomUUID().toString(),
                fileName = targetFile.name,
                url = url.trim(),
                mimeType = resolvedMime,
                localPath = targetFile.absolutePath,
                totalBytes = -1L,
                downloadedBytes = 0L,
                status = STATUS_QUEUED,
                speedBytesPerSec = 0L,
                etaSeconds = -1L
            )
            dao.insertDownload(entity)
            startOrResumeDownload(context, entity.id)
        }
    }

    fun startOrResumeDownload(context: Context, downloadId: String) {
        if (activeJobs[downloadId]?.isActive == true) return

        // Start Foreground Service to keep download alive in background
        try {
            val serviceIntent = Intent(context, GiyaniDownloadService::class.java).apply {
                action = GiyaniDownloadService.ACTION_SYNC_FOREGROUND
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (_: Exception) {
            // Safely continue coroutine execution if service start is restricted in background/test
        }

        val job = engineScope.launch {
            val dao = GiyaniDatabase.getInstance(context).giyaniDao()
            val current = dao.getDownloadById(downloadId) ?: return@launch
            val destFile = File(current.localPath)
            destFile.parentFile?.mkdirs()

            val existingBytes = if (destFile.exists()) destFile.length() else 0L
            dao.updateDownload(
                current.copy(
                    downloadedBytes = existingBytes,
                    status = STATUS_DOWNLOADING,
                    errorMessage = null
                )
            )

            try {
                val requestBuilder = Request.Builder()
                    .url(current.url)
                    .header("User-Agent", "GiyaniBrowser/1.0 (Android; ResumableDownloadEngine)")

                if (existingBytes > 0L) {
                    requestBuilder.header("Range", "bytes=$existingBytes-")
                }

                okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                    val code = response.code
                    if (!response.isSuccessful && code != 206) {
                        // If 416 Range Not Satisfiable and we already have full bytes, mark completed
                        if (code == 416 && current.totalBytes > 0 && existingBytes >= current.totalBytes) {
                            dao.updateDownload(
                                current.copy(
                                    downloadedBytes = existingBytes,
                                    status = STATUS_COMPLETED,
                                    speedBytesPerSec = 0L,
                                    etaSeconds = 0L
                                )
                            )
                            return@use
                        }
                        dao.updateDownload(
                            current.copy(
                                status = STATUS_FAILED,
                                speedBytesPerSec = 0L,
                                etaSeconds = -1L,
                                errorMessage = "HTTP $code: ${response.message}"
                            )
                        )
                        return@use
                    }

                    val isPartialContent = (code == 206)
                    val acceptRangesHeader = response.header("Accept-Ranges").orEmpty()
                    val supportsRange = isPartialContent || acceptRangesHeader.contains("bytes", ignoreCase = true)
                    val contentLength = response.body?.contentLength() ?: -1L

                    val startOffset = if (isPartialContent) existingBytes else 0L
                    val totalBytes = when {
                        isPartialContent && contentLength > 0 -> existingBytes + contentLength
                        contentLength > 0 -> contentLength
                        current.totalBytes > 0 -> current.totalBytes
                        else -> -1L
                    }

                    val bodyStream = response.body?.byteStream()
                    if (bodyStream == null) {
                        dao.updateDownload(
                            current.copy(
                                status = STATUS_FAILED,
                                errorMessage = "Empty response stream from server"
                            )
                        )
                        return@use
                    }

                    RandomAccessFile(destFile, "rw").use { raf ->
                        if (!isPartialContent) {
                            raf.setLength(0)
                        }
                        raf.seek(startOffset)

                        val buffer = ByteArray(16 * 1024)
                        var totalReadSoFar = startOffset
                        var windowStartMs = System.currentTimeMillis()
                        var windowBytes = 0L

                        while (true) {
                            val read = bodyStream.read(buffer)
                            if (read == -1) break

                            raf.write(buffer, 0, read)
                            totalReadSoFar += read
                            windowBytes += read

                            val now = System.currentTimeMillis()
                            val elapsedMs = now - windowStartMs
                            if (elapsedMs >= 350L) {
                                val speed = ((windowBytes * 1000L) / elapsedMs.coerceAtLeast(1L))
                                val remainingBytes = if (totalBytes > totalReadSoFar) {
                                    totalBytes - totalReadSoFar
                                } else {
                                    -1L
                                }
                                val eta = if (remainingBytes >= 0L && speed > 0L) {
                                    remainingBytes / speed
                                } else {
                                    -1L
                                }

                                dao.updateDownload(
                                    current.copy(
                                        downloadedBytes = totalReadSoFar,
                                        totalBytes = totalBytes,
                                        status = STATUS_DOWNLOADING,
                                        speedBytesPerSec = speed,
                                        etaSeconds = eta,
                                        supportsRange = supportsRange,
                                        errorMessage = null
                                    )
                                )
                                windowStartMs = now
                                windowBytes = 0L
                            }
                        }

                        val finalTotal = if (totalBytes > 0) totalBytes else totalReadSoFar
                        dao.updateDownload(
                            current.copy(
                                downloadedBytes = totalReadSoFar,
                                totalBytes = finalTotal,
                                status = STATUS_COMPLETED,
                                speedBytesPerSec = 0L,
                                etaSeconds = 0L,
                                supportsRange = supportsRange,
                                errorMessage = null
                            )
                        )
                    }
                }
            } catch (ce: CancellationException) {
                // Job was paused or canceled intentionally
                throw ce
            } catch (e: Exception) {
                val latest = dao.getDownloadById(downloadId)
                if (latest != null && latest.status != STATUS_PAUSED && latest.status != STATUS_CANCELED) {
                    dao.updateDownload(
                        latest.copy(
                            status = STATUS_FAILED,
                            speedBytesPerSec = 0L,
                            etaSeconds = -1L,
                            errorMessage = e.localizedMessage ?: "Network interrupted"
                        )
                    )
                }
            } finally {
                activeJobs.remove(downloadId)
            }
        }

        activeJobs[downloadId] = job
    }

    fun pauseDownload(context: Context, downloadId: String) {
        activeJobs.remove(downloadId)?.cancel()
        engineScope.launch {
            val dao = GiyaniDatabase.getInstance(context).giyaniDao()
            val item = dao.getDownloadById(downloadId) ?: return@launch
            val fileLen = File(item.localPath).takeIf { it.exists() }?.length() ?: item.downloadedBytes
            dao.updateDownload(
                item.copy(
                    downloadedBytes = fileLen,
                    status = STATUS_PAUSED,
                    speedBytesPerSec = 0L,
                    etaSeconds = -1L
                )
            )
        }
    }

    fun cancelAndDeleteDownload(context: Context, downloadId: String, deleteFileFromDisk: Boolean = true) {
        activeJobs.remove(downloadId)?.cancel()
        engineScope.launch {
            val dao = GiyaniDatabase.getInstance(context).giyaniDao()
            val item = dao.getDownloadById(downloadId)
            if (item != null && deleteFileFromDisk) {
                try {
                    File(item.localPath).delete()
                } catch (_: Exception) {
                }
            }
            dao.deleteDownload(downloadId)
        }
    }

    fun hasActiveDownloads(): Boolean = activeJobs.values.any { it.isActive }

    /**
     * Opens a downloaded file using Android FileProvider and an appropriate ACTION_VIEW intent.
     */
    fun openDownloadedFile(context: Context, item: DownloadEntity): Result<Unit> {
        return try {
            val file = File(item.localPath)
            if (!file.exists()) {
                return Result.failure(IllegalStateException("File no longer exists at ${item.localPath}"))
            }
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(viewIntent, "Open ${item.fileName}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shares or exports a downloaded file to the system Files app or external handler.
     */
    fun shareDownloadedFileLocation(context: Context, item: DownloadEntity): Result<Unit> {
        return try {
            val file = File(item.localPath)
            if (!file.exists()) {
                return Result.failure(IllegalStateException("File not found on disk: ${item.localPath}"))
            }
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Save or Share ${item.fileName}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sanitizeFileName(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim('_')
        return cleaned.ifBlank { "giyani_download_${System.currentTimeMillis()}.bin" }
    }

    private fun guessFileNameFromUrl(url: String): String {
        val path = url.substringBefore("?").substringBefore("#")
        val lastSegment = path.substringAfterLast("/")
        return if (lastSegment.isNotBlank() && lastSegment.contains(".")) {
            lastSegment
        } else {
            "download_${System.currentTimeMillis()}.bin"
        }
    }

    private fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast(".", "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    private fun resolveUniqueFile(dir: File, desiredName: String): File {
        var candidate = File(dir, desiredName)
        if (!candidate.exists()) return candidate
        val base = desiredName.substringBeforeLast(".", desiredName)
        val ext = desiredName.substringAfterLast(".", "")
        var counter = 1
        while (candidate.exists()) {
            val nextName = if (ext.isNotBlank()) "${base}_$counter.$ext" else "${base}_$counter"
            candidate = File(dir, nextName)
            counter++
        }
        return candidate
    }
}

class GiyaniDownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.download_channel_name))
            .setContentText("Background download manager active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
        }

        // Observe active downloads and stop service once all downloads settle
        serviceScope.launch {
            GiyaniDatabase.getInstance(applicationContext).giyaniDao().observeDownloads().collect { list ->
                val runningCount = list.count { it.status == DownloadEngine.STATUS_DOWNLOADING || it.status == DownloadEngine.STATUS_QUEUED }
                if (runningCount == 0 && !DownloadEngine.hasActiveDownloads()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.download_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.download_channel_desc)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "giyani_downloads_channel"
        const val NOTIFICATION_ID = 4201
        const val ACTION_SYNC_FOREGROUND = "com.example.download.ACTION_SYNC_FOREGROUND"
    }
}
