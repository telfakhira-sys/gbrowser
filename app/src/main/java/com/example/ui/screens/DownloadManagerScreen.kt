package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DownloadEntity
import com.example.download.DownloadEngine
import com.example.viewmodel.GiyaniDestination
import com.example.viewmodel.GiyaniUiState
import com.example.viewmodel.GiyaniViewModel
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadManagerScreen(
    uiState: GiyaniUiState,
    viewModel: GiyaniViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(GiyaniDestination.HOME_HUB)
    }

    val clipboardManager = LocalClipboardManager.current
    var showNewDownloadDialog by remember { mutableStateOf(false) }
    var showStorageLocationDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val downloadDirPath = remember { viewModel.getDownloadDirectoryPath() }
    val filteredDownloads = remember(uiState.downloads, selectedFilter) {
        when (selectedFilter) {
            "ACTIVE" -> uiState.downloads.filter {
                it.status == DownloadEngine.STATUS_DOWNLOADING ||
                    it.status == DownloadEngine.STATUS_PAUSED ||
                    it.status == DownloadEngine.STATUS_QUEUED
            }
            "COMPLETED" -> uiState.downloads.filter { it.status == DownloadEngine.STATUS_COMPLETED }
            else -> uiState.downloads
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("download_manager_screen")
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Download Manager",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "HTTP Range Pause/Resume • Background Foreground Service",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showNewDownloadDialog = true },
                modifier = Modifier.testTag("new_download_url_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Download",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add URL", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Download Location Banner Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { showStorageLocationDialog = true }
                .testTag("download_location_banner"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Download Storage Location",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Download Storage Location",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = downloadDirPath,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(downloadDirPath))
                        viewModel.showMessage("Copied download directory path to clipboard")
                    },
                    modifier = Modifier.testTag("copy_download_location_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Download Path",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips + Quick Verified Test Files
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("All (${uiState.downloads.size})") },
                modifier = Modifier.testTag("filter_downloads_all")
            )
            FilterChip(
                selected = selectedFilter == "ACTIVE",
                onClick = { selectedFilter = "ACTIVE" },
                label = { Text("Active / Paused") },
                modifier = Modifier.testTag("filter_downloads_active")
            )
            FilterChip(
                selected = selectedFilter == "COMPLETED",
                onClick = { selectedFilter = "COMPLETED" },
                label = { Text("Completed") },
                modifier = Modifier.testTag("filter_downloads_completed")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredDownloads.isEmpty()) {
            // Empty state with one-tap verified public specification downloads
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("downloads_empty_state_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "No Downloads in Queue",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Files triggered inside Giyani Browser or added via URL appear here with real-time progress, file size, speed, ETA, and HTTP Range pause/resume.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Quick Download Public Standards & Docs:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.enqueueDownload(
                                    url = "https://www.rfc-editor.org/rfc/rfc9110.txt",
                                    suggestedFileName = "RFC9110_HTTP_Semantics.txt",
                                    mimeType = "text/plain"
                                )
                            },
                            modifier = Modifier.testTag("quick_download_rfc9110_button")
                        ) {
                            Text("RFC 9110 HTTP Spec (.txt)")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.enqueueDownload(
                                    url = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                                    suggestedFileName = "W3C_Verification_Document.pdf",
                                    mimeType = "application/pdf"
                                )
                            },
                            modifier = Modifier.testTag("quick_download_w3c_pdf_button")
                        ) {
                            Text("W3C Test Document (.pdf)")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("downloads_lazy_column"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredDownloads, key = { it.id }) { item ->
                    DownloadItemCard(
                        item = item,
                        onPause = { viewModel.pauseDownload(item.id) },
                        onResume = { viewModel.resumeDownload(item.id) },
                        onOpenFile = { viewModel.openDownloadedFile(item) },
                        onShareLocation = { viewModel.shareDownloadedFileLocation(item) },
                        onDelete = { viewModel.deleteDownload(item.id, deleteFile = true) }
                    )
                }
            }
        }
    }

    // Add New Download by URL Dialog
    if (showNewDownloadDialog) {
        var urlInput by remember { mutableStateOf("https://www.rfc-editor.org/rfc/rfc9110.txt") }
        var fileNameInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewDownloadDialog = false },
            title = { Text("Start Background Download") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Direct File URL (HTTPS)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("download_url_input")
                    )
                    OutlinedTextField(
                        value = fileNameInput,
                        onValueChange = { fileNameInput = it },
                        label = { Text("Optional Filename (e.g., report.pdf)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("download_filename_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.enqueueDownload(
                            url = urlInput,
                            suggestedFileName = fileNameInput.takeIf { it.isNotBlank() }
                        )
                        showNewDownloadDialog = false
                    },
                    enabled = urlInput.isNotBlank(),
                    modifier = Modifier.testTag("confirm_start_download_button")
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewDownloadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Download Storage Directory Inspector Dialog
    if (showStorageLocationDialog) {
        AlertDialog(
            onDismissRequest = { showStorageLocationDialog = false },
            title = { Text("Download Storage Directory") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "All downloaded files are stored inside Giyani Browser's isolated, zero-permission storage directory and shared securely via Android FileProvider:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = downloadDirPath,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Text(
                        text = "Total Files in Storage: ${uiState.downloads.count { it.status == DownloadEngine.STATUS_COMPLETED }}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(downloadDirPath))
                        viewModel.showMessage("Copied path to clipboard")
                        showStorageLocationDialog = false
                    }
                ) {
                    Text("Copy Path")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStorageLocationDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DownloadItemCard(
    item: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onOpenFile: () -> Unit,
    onShareLocation: () -> Unit,
    onDelete: () -> Unit
) {
    val progressFraction = remember(item.downloadedBytes, item.totalBytes) {
        if (item.totalBytes > 0L) {
            (item.downloadedBytes.toFloat() / item.totalBytes.toFloat()).coerceIn(0f, 1f)
        } else if (item.status == DownloadEngine.STATUS_COMPLETED) {
            1f
        } else {
            0f
        }
    }
    val progressPercent = (progressFraction * 100).toInt()

    val statusColor = when (item.status) {
        DownloadEngine.STATUS_COMPLETED -> MaterialTheme.colorScheme.primary
        DownloadEngine.STATUS_DOWNLOADING, DownloadEngine.STATUS_QUEUED -> MaterialTheme.colorScheme.secondary
        DownloadEngine.STATUS_PAUSED -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = statusColor.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("download_item_card_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: File Name + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = statusColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (item.status) {
                            DownloadEngine.STATUS_COMPLETED -> Icons.Default.CheckCircle
                            DownloadEngine.STATUS_FAILED -> Icons.Default.ErrorOutline
                            DownloadEngine.STATUS_PAUSED -> Icons.Default.Pause
                            else -> Icons.Default.CloudDownload
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = item.status,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }
            }

            // Progress Bar
            if (item.totalBytes > 0L || item.status == DownloadEngine.STATUS_COMPLETED || item.status == DownloadEngine.STATUS_PAUSED) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else if (item.status == DownloadEngine.STATUS_DOWNLOADING || item.status == DownloadEngine.STATUS_QUEUED) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Telemetry Row: File Size, Progress %, Speed, and Estimated Time Remaining (ETA)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sizeLabel = if (item.totalBytes > 0L) {
                    "${formatBytes(item.downloadedBytes)} / ${formatBytes(item.totalBytes)} ($progressPercent%)"
                } else {
                    "${formatBytes(item.downloadedBytes)} downloaded"
                }

                Text(
                    text = sizeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val etaAndSpeedLabel = when (item.status) {
                    DownloadEngine.STATUS_DOWNLOADING -> {
                        val speedStr = if (item.speedBytesPerSec > 0) "${formatBytes(item.speedBytesPerSec)}/s" else "Measuring..."
                        val etaStr = formatEtaSeconds(item.etaSeconds)
                        "$speedStr • ETA: $etaStr"
                    }
                    DownloadEngine.STATUS_PAUSED -> "Paused • Range Resume Ready"
                    DownloadEngine.STATUS_COMPLETED -> "Completed • Ready to Open"
                    DownloadEngine.STATUS_FAILED -> item.errorMessage ?: "Download Failed"
                    else -> "Queued..."
                }

                Text(
                    text = etaAndSpeedLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }

            // File Path display
            Text(
                text = "Location: ${item.localPath}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Action Controls Row: Pause, Resume, Open File, Share/Location, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (item.status) {
                        DownloadEngine.STATUS_DOWNLOADING, DownloadEngine.STATUS_QUEUED -> {
                            OutlinedButton(
                                onClick = onPause,
                                modifier = Modifier.testTag("pause_download_button_${item.id}"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause Download",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pause", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        DownloadEngine.STATUS_PAUSED, DownloadEngine.STATUS_FAILED -> {
                            Button(
                                onClick = onResume,
                                modifier = Modifier.testTag("resume_download_button_${item.id}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.status == DownloadEngine.STATUS_PAUSED) Icons.Default.PlayArrow else Icons.Default.Refresh,
                                    contentDescription = "Resume Download",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (item.status == DownloadEngine.STATUS_PAUSED) "Resume" else "Retry",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        DownloadEngine.STATUS_COMPLETED -> {
                            Button(
                                onClick = onOpenFile,
                                modifier = Modifier.testTag("open_downloaded_file_button_${item.id}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileOpen,
                                    contentDescription = "Open File",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open File", style = MaterialTheme.typography.labelLarge)
                            }

                            OutlinedButton(
                                onClick = onShareLocation,
                                modifier = Modifier.testTag("share_download_location_button_${item.id}"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Access Location / Share",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export / Location", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_download_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Download",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.2f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}

fun formatEtaSeconds(seconds: Long): String {
    if (seconds < 0L) return "--"
    if (seconds == 0L) return "0s"
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}
