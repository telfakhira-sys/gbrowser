package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BrowserProfileEntity
import com.example.diagnostics.DiagnosticMetricItem
import com.example.diagnostics.DiagnosticStatus
import com.example.domain.CountryIdentityCatalog
import com.example.viewmodel.GiyaniDestination
import com.example.viewmodel.GiyaniUiState
import com.example.viewmodel.GiyaniViewModel

@Composable
fun DiagnosticsScreen(
    uiState: GiyaniUiState,
    viewModel: GiyaniViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(GiyaniDestination.BROWSER_WEBVIEW)
    }

    val protectedCount = uiState.diagnostics.count { it.status == DiagnosticStatus.PROTECTED }
    val mismatchCount = uiState.diagnostics.count { it.status == DiagnosticStatus.MISMATCH }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("diagnostics_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Identity Diagnostics",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Profile: ${uiState.activeProfile.name} (${uiState.activeProfile.email}) • $protectedCount Protected • $mismatchCount Mismatches",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { viewModel.refreshDiagnostics() },
                modifier = Modifier.testTag("refresh_diagnostics_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Diagnostics",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Refresh", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("diagnostics_lazy_column"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(uiState.diagnostics, key = { it.id }) { item ->
                DiagnosticItemCard(item = item)
            }
        }
    }
}

@Composable
private fun DiagnosticItemCard(item: DiagnosticMetricItem) {
    val badgeColor = when (item.status) {
        DiagnosticStatus.PROTECTED -> MaterialTheme.colorScheme.primary
        DiagnosticStatus.AVAILABLE -> MaterialTheme.colorScheme.secondary
        DiagnosticStatus.RESTRICTED -> MaterialTheme.colorScheme.tertiary
        DiagnosticStatus.MISMATCH -> MaterialTheme.colorScheme.error
        DiagnosticStatus.UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = badgeColor.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("diagnostic_metric_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.measuredValue,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Text(
                text = item.detailNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProfilesAndPrivacyScreen(
    uiState: GiyaniUiState,
    viewModel: GiyaniViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(GiyaniDestination.BROWSER_WEBVIEW)
    }

    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<BrowserProfileEntity?>(null) }
    val activeProfile = uiState.activeProfile

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("profiles_privacy_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        // 1. Multi-Profile Manager Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Manajemen Profile & Identitas",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Buat profile dengan Nama & Email (tanpa password). Buka banyak profile bersamaan selama dalam 1 Negara atau 1 VPN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showCreateProfileDialog = true },
                    modifier = Modifier.testTag("create_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Buat Profile",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buat Profile")
                }
            }
        }

        // 2. Multi-Profile Same-Country / VPN Status Notice + Global Session Isolation Reset
        item {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Brankas Multi-Profile Terisolasi 100%: ${uiState.activeProfiles.size} Profile Aktif (${uiState.selectedCountry.flagEmoji} ${uiState.selectedCountry.name})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Setiap profile memiliki brankas Cookies, LocalStorage, SessionStorage, dan IndexedDB tersendiri. Akun Quarterfull/Google di satu profile tidak akan pernah bocor ke profile lain.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.purgeAllMixedSessionsEverywhere() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("full_purge_all_sessions_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Reset Semua Sesi",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bersihkan & Isolasi Ulang Semua Sesi Akun (Reset Total)",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 3. Profile Cards List (Pilih, Edit, Hapus, Use, Buka Banyak Profile 1 Negara/VPN + Keep Login Session Toggle)
        items(uiState.profiles, key = { it.id }) { profile ->
            val presetCountry = CountryIdentityCatalog.getCountryByCode(profile.countryCode)
            val presetDevice = CountryIdentityCatalog.getDeviceById(profile.devicePresetId)
            val isSameCountry = profile.countryCode.equals(uiState.selectedCountry.isoCode, ignoreCase = true)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (profile.isFocused) 2.dp else 1.dp,
                        color = when {
                            profile.isFocused -> MaterialTheme.colorScheme.primary
                            profile.isActive -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.outline
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { viewModel.useProfile(profile, forceSwitchCountry = false) }
                    .testTag("profile_card_${profile.id}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Row: Flag + Name (weight 1f) + Single-line Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = presetCountry.flagEmoji,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Email: ${profile.email.ifBlank { "no-email@giyani.local" }}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            color = when {
                                profile.isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                profile.isActive -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = when {
                                    profile.isFocused -> "AKTIF • UTAMA"
                                    profile.isActive -> "AKTIF (${profile.countryCode})"
                                    else -> "STANDBY (${profile.countryCode})"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                color = when {
                                    profile.isFocused -> MaterialTheme.colorScheme.primary
                                    profile.isActive -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Identity & Sandbox details block
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Identitas: ${presetCountry.name} (${profile.cityName}) • ${profile.timezoneId} • ${presetDevice.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Partisi Brankas Akun: #${profile.id.takeLast(6)} (4-Layer Zero-Leak Sandbox)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Keep Login Session Alive Toggle Row per Profile
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (profile.keepLoginSession) {
                                        "Simpan Sesi Login Akun Profile Ini Saat Pindah: AKTIF"
                                    } else {
                                        "Auto-Reset Sesi Profile Ini Saat Pindah: AKTIF"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (profile.keepLoginSession) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.tertiary
                                    }
                                )
                                Text(
                                    text = if (profile.keepLoginSession) {
                                        "Akun Quarterfull/Google di profile ini disimpan di brankas #${profile.id.takeLast(6)} & direset saat aplikasi ditutup"
                                    } else {
                                        "Cookies, LocalStorage & IndexedDB profile ini otomatis dihapus setiap kali pindah profile"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = profile.keepLoginSession,
                                onCheckedChange = { keep ->
                                    viewModel.toggleProfileKeepLoginSession(profile, keep)
                                },
                                modifier = Modifier.testTag("full_keep_login_switch_${profile.id}")
                            )
                        }
                    }

                    // Profile Action Buttons — Row 1: Primary Navigation & Session Active State (2 Equal Columns)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.useProfile(profile, forceSwitchCountry = false)
                                if (isSameCountry) {
                                    viewModel.navigateTo(GiyaniDestination.BROWSER_WEBVIEW)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("use_profile_full_button_${profile.id}")
                        ) {
                            Text(
                                text = if (profile.isFocused) "Buka di Browser" else if (isSameCountry) "Use Profile" else "Aktifkan ${profile.countryCode}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.toggleProfileMultiActive(profile) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("multi_open_profile_button_${profile.id}")
                        ) {
                            Text(
                                text = if (profile.isActive) "Nonaktifkan Sesi" else "Aktifkan Bersama",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Profile Action Buttons — Row 2: Reset Login Session + Edit & Delete Icons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.resetProfileLoginSession(profile) },
                            modifier = Modifier.testTag("full_reset_session_profile_button_${profile.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Reset Sesi Login",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reset Sesi Login",
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { editingProfile = profile },
                                modifier = Modifier.testTag("edit_profile_full_button_${profile.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (uiState.profiles.size > 1) {
                                IconButton(
                                    onClick = { viewModel.deleteProfile(profile) },
                                    modifier = Modifier.testTag("delete_profile_button_${profile.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Profile",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Privacy & Fingerprinting Protection Controls for Active Profile
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("privacy_engine_controls_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Privacy & Fingerprint Protection (${activeProfile.name})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    PrivacyToggleRow(
                        title = "Third-Party Tracker & Ad-Domain Blocking",
                        subtitle = "Intercepts known tracking scripts and sends DNT=1 & Sec-GPC=1 headers",
                        checked = activeProfile.blockTrackers,
                        onCheckedChange = { viewModel.updatePrivacySettings(blockTrackers = it) },
                        testTag = "switch_block_trackers"
                    )

                    PrivacyToggleRow(
                        title = "Block Third-Party Cross-Site Cookies",
                        subtitle = "Enforces strict first-party cookie isolation inside WebView CookieManager",
                        checked = activeProfile.blockThirdPartyCookies,
                        onCheckedChange = { viewModel.updatePrivacySettings(blockThirdPartyCookies = it) },
                        testTag = "switch_block_3p_cookies"
                    )

                    PrivacyToggleRow(
                        title = "Deterministic Canvas Readback Protection",
                        subtitle = "Applies session-consistent bit mask on HTMLCanvasElement.toDataURL()",
                        checked = activeProfile.canvasProtection,
                        onCheckedChange = { viewModel.updatePrivacySettings(canvasProtection = it) },
                        testTag = "switch_canvas_protection"
                    )

                    PrivacyToggleRow(
                        title = "WebRTC Local LAN IP Leak Protection",
                        subtitle = "Forces RTCPeerConnection iceTransportPolicy to 'relay'",
                        checked = activeProfile.webRtcProtection,
                        onCheckedChange = { viewModel.updatePrivacySettings(webRtcProtection = it) },
                        testTag = "switch_webrtc_protection"
                    )

                    HorizontalDivider()

                    Text(
                        text = "Browser Geolocation Policy & Testing Mode",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Clearly distinguishes physical device GPS, W3C browser permission, simulated development testing coordinates, and VPN IP location.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ASK", "BLOCKED", "SIMULATED_TESTING").forEach { policy ->
                            FilterChip(
                                selected = activeProfile.geolocationPolicy == policy,
                                onClick = { viewModel.updatePrivacySettings(geolocationPolicy = policy) },
                                label = { Text(policy) },
                                modifier = Modifier.testTag("geo_policy_chip_$policy")
                            )
                        }
                    }
                }
            }
        }

        // 5. Browsing History & Clear History for Active Profile
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Riwayat Browsing (${activeProfile.name}: ${uiState.history.size})",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (uiState.history.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { viewModel.clearBrowsingHistory() },
                                modifier = Modifier.testTag("clear_history_button")
                            ) {
                                Text("Clear History")
                            }
                        }
                    }

                    if (uiState.history.isEmpty()) {
                        Text(
                            text = "Belum ada riwayat browsing untuk profile ${activeProfile.name}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.history.take(8).forEach { hist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.loadUrlInActiveTab(hist.url) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = hist.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = hist.url,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create New Profile Dialog (Nama + Email, Tanpa Password)
    if (showCreateProfileDialog) {
        ProfileEditorDialog(
            dialogTitle = "Buat Profile Browser Baru",
            initialName = "",
            initialEmail = "",
            initialCountryCode = uiState.selectedCountry.isoCode,
            initialCityName = uiState.selectedCity.name,
            initialDeviceId = uiState.selectedDevice.id,
            confirmButtonText = "Buat & Gunakan Profile",
            onDismiss = { showCreateProfileDialog = false },
            onConfirm = { name, email, countryIso, cityName, deviceId ->
                viewModel.createBrowserProfile(
                    name = name,
                    email = email,
                    countryCode = countryIso,
                    cityName = cityName,
                    devicePresetId = deviceId
                )
                showCreateProfileDialog = false
            }
        )
    }

    // Edit Existing Profile Dialog
    editingProfile?.let { profileToEdit ->
        ProfileEditorDialog(
            dialogTitle = "Edit Profile & Identitas Browser",
            initialName = profileToEdit.name,
            initialEmail = profileToEdit.email,
            initialCountryCode = profileToEdit.countryCode,
            initialCityName = profileToEdit.cityName,
            initialDeviceId = profileToEdit.devicePresetId,
            confirmButtonText = "Simpan Perubahan",
            onDismiss = { editingProfile = null },
            onConfirm = { name, email, countryIso, cityName, deviceId ->
                viewModel.editBrowserProfile(
                    profile = profileToEdit,
                    newName = name,
                    newEmail = email,
                    newCountryCode = countryIso,
                    newCityName = cityName,
                    newDevicePresetId = deviceId
                )
                editingProfile = null
            }
        )
    }
}

@Composable
private fun PrivacyToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
