package com.f1tracker.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    data class Downloaded(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class ApkDownloader(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun download(url: String, fileName: String = "BOXBOXBOX-F1-update.apk"): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0))

        // Delete existing file if present to avoid duplicates
        try {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            // Ignore
        }

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("BOXBOXBOX F1 Update")
            .setDescription("Downloading update...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)

        val downloadId = downloadManager.enqueue(request)
        android.util.Log.d("ApkDownloader", "Download started with ID: $downloadId")

        var isDownloading = true
        while (isDownloading) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = cursor.getInt(statusIndex)

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        isDownloading = false
                        android.util.Log.d("ApkDownloader", "Download successful")
                        emit(DownloadState.Downloading(100)) // Ensure 100% is shown
                        
                        // Use the explicit file path we defined
                        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                        
                        if (file.exists()) {
                            android.util.Log.d("ApkDownloader", "File path: ${file.absolutePath}")
                            emit(DownloadState.Downloaded(file))
                        } else {
                            android.util.Log.e("ApkDownloader", "File not found at expected path: ${file.absolutePath}")
                            // Fallback to trying to get path from cursor if explicit path fails (unlikely if download successful)
                            val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val uriString = cursor.getString(uriIndex)
                            val fileUri = Uri.parse(uriString)
                            if (fileUri.scheme == "file") {
                                emit(DownloadState.Downloaded(File(fileUri.path!!)))
                            } else {
                                emit(DownloadState.Error("Could not determine file path"))
                            }
                        }
                    }
                    DownloadManager.STATUS_FAILED -> {
                        isDownloading = false
                        android.util.Log.e("ApkDownloader", "Download failed")
                        emit(DownloadState.Error("Download failed"))
                    }
                    DownloadManager.STATUS_RUNNING -> {
                        val downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        
                        val downloaded = cursor.getLong(downloadedIndex)
                        val total = cursor.getLong(totalIndex)
                        
                        if (total > 0) {
                            val progress = (downloaded * 100 / total).toInt()
                            android.util.Log.d("ApkDownloader", "Progress: $progress% ($downloaded / $total)")
                            emit(DownloadState.Downloading(progress))
                        } else {
                            // Indeterminate or starting
                            emit(DownloadState.Downloading(0))
                        }
                    }
                }
            } else {
                isDownloading = false
                android.util.Log.e("ApkDownloader", "Download cancelled or cursor empty")
                emit(DownloadState.Error("Download cancelled"))
            }
            cursor.close()
            
            if (isDownloading) {
                delay(100) // Poll every 100ms for smoother updates
            }
        }
    }

    fun installApk(file: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
