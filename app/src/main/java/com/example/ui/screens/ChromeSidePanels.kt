package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BrowserProfileEntity
import com.example.domain.CountryIdentityCatalog
import com.example.viewmodel.GiyaniDestination
import com.example.viewmodel.GiyaniUiState
import com.example.viewmodel.GiyaniViewModel
import com.example.vpn.VpnTunnelStatus

/**
 * Left Panel (Show/Hide by Toggle):
 * Manages Multi-Profile Workflow:
 * - Buat Profile: Nama & Email (tanpa password) + Identitas Browser tersendiri (Negara, Kota, Device Preset)
 * - List Profile, Pilih Profile, Edit Profile, Hapus Profile, Use Profile
 * - Aktif / Buka banyak profile sekaligus asalkan dalam 1 Negara atau 1 VPN yang sama
 * - Quick Tab Group & Tab Switcher for the focused profile
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeftProfileAndTabsPanel(
    visible: Boolean,
    uiState: GiyaniUiState,
    viewModel: GiyaniViewModel,
    modifier: Modifier = Modifier
) {
    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<BrowserProfileEntity?>(null) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { -it }),
        exit = slideOutHorizontally(targetOffsetX = { -it }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(335.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
                )
                .testTag("left_toggle_panel"),
            shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Panel Top Header + Hide Toggle Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ManageAccounts,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Profiles & Tab Workspace",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uiState.activeProfiles.size} Profile Aktif (${uiState.selectedCountry.flagEmoji} ${uiState.selectedCountry.isoCode})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.setLeftPanelOpen(false) },
                        modifier = Modifier.testTag("close_left_panel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hide Left Panel"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Rule Banner: Multi-Profile Active Constraint (1 Country or VPN) + Keep Login Session Master Switch
                val allActiveKeepLogin = uiState.activeProfiles.all { it.keepLoginSession }
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Brankas Akun Terisolasi 100% (${uiState.selectedCountry.name}): Setiap profile memiliki Cookies, LocalStorage, & IndexedDB terpisah agar akun Quarterfull/Google tidak pernah tercampur.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Simpan Sesi Login Masing-Masing Profile Saat Pindah",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = allActiveKeepLogin,
                                onCheckedChange = { viewModel.setAllActiveProfilesKeepLoginSession(it) },
                                modifier = Modifier.testTag("master_keep_login_session_switch")
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.purgeAllMixedSessionsEverywhere() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .testTag("left_panel_purge_all_sessions_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Bersihkan Semua Sesi",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bersihkan & Isolasi Ulang Semua Sesi Akun",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Button: Buat Profile Baru (Nama + Email, Tanpa Password)
                Button(
                    onClick = { showCreateProfileDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("left_panel_create_profile_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Buat Profile",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buat Profile Baru (Nama & Email)")
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Section 1: List Profile (Pilih, Edit, Hapus, Use, Multi-Open Toggle)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DAFTAR PROFILE (${uiState.profiles.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            TextButton(
                                onClick = { viewModel.navigateTo(GiyaniDestination.PROFILES_PRIVACY) }
                            ) {
                                Text("Kelola Lengkap", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    items(uiState.profiles, key = { it.id }) { profile ->
                        val pCountry = CountryIdentityCatalog.getCountryByCode(profile.countryCode)
                        val pDevice = CountryIdentityCatalog.getDeviceById(profile.devicePresetId)
                        val isSameCountryAsCurrent = profile.countryCode.equals(
                            uiState.selectedCountry.isoCode,
                            ignoreCase = true
                        )

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
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.useProfile(profile, forceSwitchCountry = true) }
                                .testTag("left_panel_profile_card_${profile.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (profile.isFocused) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Top row: Flag, Name, Email, Status Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = pCountry.flagEmoji,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = profile.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = profile.email.ifBlank { "no-email@giyani.local" },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Status Badge: Single-line compact badge
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = when {
                                            profile.isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                            profile.isActive -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                                            else -> MaterialTheme.colorScheme.surface
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                profile.isFocused -> "AKTIF • UTAMA"
                                                profile.isActive -> "AKTIF BERSAMA"
                                                else -> "NONAKTIF"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            softWrap = false,
                                            color = when {
                                                profile.isFocused -> MaterialTheme.colorScheme.primary
                                                profile.isActive -> MaterialTheme.colorScheme.secondary
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Unique Browser Identity details per profile + Isolated Partition ID
                                Text(
                                    text = "Identitas: ${pCountry.name} (${profile.cityName}) • ${profile.timezoneId}\nDevice: ${pDevice.name} (${pDevice.browserName})\nBrankas Akun Terisolasi: #${profile.id.takeLast(6)} (Cookie + DOM + IDB Terpisah)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Per-Profile Keep Login Session Toggle Row
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = if (profile.keepLoginSession) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (profile.keepLoginSession) {
                                                    "Simpan Sesi Akun Profile Ini Saat Pindah"
                                                } else {
                                                    "Auto-Logout Profile Ini Saat Pindah"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (profile.keepLoginSession) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (profile.keepLoginSession) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                        Switch(
                                            checked = profile.keepLoginSession,
                                            onCheckedChange = {
                                                viewModel.toggleProfileKeepLoginSession(profile, it)
                                            },
                                            modifier = Modifier.testTag("keep_login_switch_${profile.id}")
                                        )
                                    }
                                }

                                // Action Buttons Row 1: Use Profile + Multi-Open Toggle (2 Equal Columns)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!profile.isFocused) {
                                        Button(
                                            onClick = {
                                                viewModel.useProfile(profile, forceSwitchCountry = true)
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(32.dp)
                                                .testTag("use_profile_button_${profile.id}")
                                        ) {
                                            Text(
                                                text = if (isSameCountryAsCurrent) "Use Profile" else "Pindah & Use",
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Toggle Multi-Active (Open/Close alongside current profile if same Country/VPN)
                                    OutlinedButton(
                                        onClick = { viewModel.toggleProfileMultiActive(profile) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                            .testTag("toggle_multi_active_profile_${profile.id}")
                                    ) {
                                        Text(
                                            text = if (profile.isActive) "Tutup Sesi" else "Buka Bersama",
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Action Buttons Row 2: Reset Sesi Login + Edit & Delete Icons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.resetProfileLoginSession(profile) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(30.dp)
                                            .testTag("reset_session_profile_${profile.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Reset Sesi",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Reset Sesi Login",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            maxLines = 1
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { editingProfile = profile },
                                            modifier = Modifier
                                                .size(30.dp)
                                                .testTag("edit_profile_button_${profile.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Profile",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        if (uiState.profiles.size > 1) {
                                            IconButton(
                                                onClick = { viewModel.deleteProfile(profile) },
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .testTag("delete_profile_left_button_${profile.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Hapus Profile",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Active Profile's Tab Groups & Open Tabs
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TAB & GRUP (${uiState.activeProfile.name})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = { viewModel.navigateTo(GiyaniDestination.TAB_GROUPS) }
                            ) {
                                Text("Tab Manager (${uiState.tabs.size})", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    items(uiState.tabs, key = { it.id }) { tab ->
                        val group = uiState.tabGroups.find { it.id == tab.groupId }
                        val groupColor = group?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.secondary

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (tab.isActive) 1.5.dp else 1.dp,
                                    color = if (tab.isActive) groupColor else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    viewModel.switchToTab(tab)
                                    viewModel.setLeftPanelOpen(false)
                                },
                            color = if (tab.isActive) {
                                groupColor.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (group != null) {
                                        Text(
                                            text = group.name.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = groupColor
                                        )
                                    }
                                    Text(
                                        text = tab.title.ifBlank { tab.url },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (tab.isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.closeTab(tab) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Tutup Tab",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Profile Dialog (Nama, Email — Tanpa Password, plus Identitas Browser)
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

    // Edit Profile Dialog
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

/**
 * Right Panel (Show/Hide by Toggle):
 * Quick Settings Panel for the Currently Focused Profile:
 * - Country Identity Selector (Negara, Kota, Timezone otomatis)
 * - Device Compatibility Profile Selector (Pixel, Galaxy, iPhone, Desktop, dll)
 * - Integrated Per-App WireGuard VPN Connect / Disconnect & Status
 * - Privacy & Fingerprint Protection Toggles (Trackers, 3P Cookies, Canvas, WebRTC)
 * - Quick links to Downloads, Identity Diagnostics, and Full Settings
 */
@Composable
fun RightSettingsAndIdentityPanel(
    visible: Boolean,
    uiState: GiyaniUiState,
    viewModel: GiyaniViewModel,
    onRequestVpnPermission: (Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeProfile = uiState.activeProfile
    val country = uiState.selectedCountry
    val city = uiState.selectedCity
    val device = uiState.selectedDevice
    val vpnState = uiState.vpnState
    val isVpnConnected = vpnState.status == VpnTunnelStatus.CONNECTED

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(340.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                )
                .testTag("right_toggle_panel"),
            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Top Header + Hide Toggle Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pengaturan Identitas & VPN",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Profile: ${activeProfile.name} (${activeProfile.email})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setRightPanelOpen(false) },
                        modifier = Modifier.testTag("close_right_panel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hide Right Panel"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    // 0. Quick Display, Theme (Dark/Light), Fullscreen & Desktop Viewport Card
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Tampilan, Tema & Mode Layar",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Dark / Light Theme Toggle Button
                                    OutlinedButton(
                                        onClick = { viewModel.toggleDarkTheme() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("right_panel_theme_toggle_button"),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "Toggle Dark/Light Theme",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (uiState.isDarkTheme) "Light Mode" else "Dark Mode",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }

                                    // Fullscreen Toggle Button
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.setRightPanelOpen(false)
                                            viewModel.toggleFullscreenMode()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("right_panel_fullscreen_toggle_button"),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fullscreen,
                                            contentDescription = "Toggle Fullscreen",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (uiState.isFullscreenMode) "Exit Fullscreen" else "Fullscreen",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                // Desktop Site Viewport Override Switch
                                CompactSwitchRow(
                                    label = "Tampilan Situs Desktop (1440x900)",
                                    checked = uiState.isDesktopViewportOverride,
                                    onCheckedChange = { viewModel.toggleDesktopViewportOverride() }
                                )
                            }
                        }
                    }

                    // 1. Active Profile Identity Summary Card
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${country.flagEmoji} ${country.name} • ${city.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Timezone: ${city.timezoneId} (${city.utcOffset})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Locale: ${country.defaultLanguage} • Currency: ${country.currencyCode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Device: ${device.name} (${device.viewportWidth}x${device.viewportHeight})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    // 2. Country Selector for Current Profile
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "1. Pilih Negara Profile Ini",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(CountryIdentityCatalog.countries, key = { it.isoCode }) { preset ->
                                    FilterChip(
                                        selected = preset.isoCode == country.isoCode,
                                        onClick = { viewModel.selectCountry(preset.isoCode) },
                                        label = { Text("${preset.flagEmoji} ${preset.isoCode}") },
                                        modifier = Modifier.testTag("right_panel_country_${preset.isoCode}")
                                    )
                                }
                            }
                        }
                    }

                    // 3. City & Timezone Selector
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "2. Pilih Kota & Timezone (${country.name})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(country.cities, key = { it.name }) { cityOption ->
                                    FilterChip(
                                        selected = cityOption.name == city.name,
                                        onClick = { viewModel.selectCity(cityOption) },
                                        label = { Text(cityOption.name) },
                                        modifier = Modifier.testTag("right_panel_city_${cityOption.name}")
                                    )
                                }
                            }
                        }
                    }

                    // 4. Device Preset Selector (Unique Browser Identity per Profile - 28+ Smartphone Brands)
                    item {
                        var selectedBrandCat by remember { mutableStateOf("Semua") }
                        val brandCats = remember {
                            listOf("Semua") + CountryIdentityCatalog.devicePresets.map { it.category }.distinct()
                        }
                        val filteredDevs = remember(selectedBrandCat) {
                            if (selectedBrandCat == "Semua") {
                                CountryIdentityCatalog.devicePresets
                            } else {
                                CountryIdentityCatalog.devicePresets.filter { it.category == selectedBrandCat }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "3. Identitas Merek HP & Perangkat (${CountryIdentityCatalog.devicePresets.size} Model)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(brandCats, key = { it }) { cat ->
                                    FilterChip(
                                        selected = selectedBrandCat == cat,
                                        onClick = { selectedBrandCat = cat },
                                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(filteredDevs, key = { it.id }) { devPreset ->
                                    FilterChip(
                                        selected = devPreset.id == device.id,
                                        onClick = { viewModel.selectDevicePreset(devPreset.id) },
                                        label = { Text(devPreset.name) },
                                        modifier = Modifier.testTag("right_panel_device_${devPreset.id}")
                                    )
                                }
                            }
                        }
                    }

                    // 5. Integrated WireGuard VPN Quick Control (10 Built-In Freza Servers)
                    item {
                        val countryEndpoints = uiState.vpnEndpoints.filter {
                            it.countryCode.equals(country.isoCode, ignoreCase = true)
                        }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (isVpnConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.VpnKey,
                                            contentDescription = null,
                                            tint = if (isVpnConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "WireGuard VPN: ${vpnState.status.name}",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                }

                                Text(
                                    text = "Public IP: ${vpnState.publicIpTelemetry.publicIp} (${vpnState.publicIpTelemetry.detectedCountryCode})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Quick Server Chips for Current Country (e.g. Freza Tampan US-FREE#1, US-FREE#2, US-FREE#4, US-FREE#27)
                                if (countryEndpoints.isNotEmpty()) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(countryEndpoints, key = { it.id }) { ep ->
                                            val isEpConnected = isVpnConnected && vpnState.activeEndpoint?.id == ep.id
                                            FilterChip(
                                                selected = isEpConnected,
                                                onClick = {
                                                    viewModel.prepareAndConnectVpn(
                                                        endpoint = ep,
                                                        onPermissionRequired = onRequestVpnPermission
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = ep.name,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isVpnConnected) {
                                        Button(
                                            onClick = { viewModel.disconnectVpn() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Disconnect", style = MaterialTheme.typography.labelSmall)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                val matching = countryEndpoints.find {
                                                    it.id == activeProfile.preferredVpnServerId
                                                } ?: countryEndpoints.firstOrNull()
                                                    ?: uiState.vpnEndpoints.firstOrNull()
                                                if (matching != null) {
                                                    viewModel.prepareAndConnectVpn(
                                                        endpoint = matching,
                                                        onPermissionRequired = onRequestVpnPermission
                                                    )
                                                } else {
                                                    viewModel.navigateTo(GiyaniDestination.VPN_MANAGER)
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = if (uiState.vpnEndpoints.isEmpty()) "Atur Server VPN" else "Connect ${country.isoCode} VPN",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.navigateTo(GiyaniDestination.VPN_MANAGER) }
                                    ) {
                                        Text("10 Conf (${uiState.vpnEndpoints.size})", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    // 6. Privacy & Anti-Fingerprint Toggles for Current Profile
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "4. Proteksi Privasi & Fingerprint",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            CompactSwitchRow(
                                label = "Blokir Tracker & Iklan (${uiState.trackersBlockedCount})",
                                checked = activeProfile.blockTrackers,
                                onCheckedChange = { viewModel.updatePrivacySettings(blockTrackers = it) }
                            )
                            CompactSwitchRow(
                                label = "Isolasi Third-Party Cookies",
                                checked = activeProfile.blockThirdPartyCookies,
                                onCheckedChange = { viewModel.updatePrivacySettings(blockThirdPartyCookies = it) }
                            )
                            CompactSwitchRow(
                                label = "Proteksi Canvas 2D Fingerprint",
                                checked = activeProfile.canvasProtection,
                                onCheckedChange = { viewModel.updatePrivacySettings(canvasProtection = it) }
                            )
                            CompactSwitchRow(
                                label = "Proteksi Kebocoran IP WebRTC",
                                checked = activeProfile.webRtcProtection,
                                onCheckedChange = { viewModel.updatePrivacySettings(webRtcProtection = it) }
                            )
                        }
                    }

                    // 7. Quick Tool Navigation Buttons (Downloads, Diagnostics, Overview Hub)
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.navigateTo(GiyaniDestination.DOWNLOADS) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download Manager (${uiState.downloads.size})")
                            }

                            OutlinedButton(
                                onClick = { viewModel.navigateTo(GiyaniDestination.DIAGNOSTICS) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cek Diagnostik Identitas (${uiState.diagnostics.size})")
                            }

                            Button(
                                onClick = { viewModel.navigateTo(GiyaniDestination.USER_GUIDE) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("right_panel_open_guide_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            ) {
                                Text("📘 Panduan Pemakaian & Setup VPN")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Reusable Dialog for Creating or Editing a Browser Profile with:
 * - Nama Profile
 * - Email Profile (Tanpa Password)
 * - Identitas Browser Tersendiri (18 Negara, 70+ Kota/Timezone, dan 28+ Merek Ponsel/Device Preset)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditorDialog(
    dialogTitle: String,
    initialName: String,
    initialEmail: String,
    initialCountryCode: String,
    initialCityName: String,
    initialDeviceId: String,
    confirmButtonText: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, email: String, countryCode: String, cityName: String, deviceId: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }
    var selectedCountryIso by remember { mutableStateOf(initialCountryCode) }
    val selectedCountry = remember(selectedCountryIso) {
        CountryIdentityCatalog.getCountryByCode(selectedCountryIso)
    }
    var selectedCityName by remember(selectedCountryIso) {
        val match = selectedCountry.cities.find { it.name.equals(initialCityName, ignoreCase = true) }
        mutableStateOf(match?.name ?: selectedCountry.cities.first().name)
    }
    var selectedDeviceId by remember { mutableStateOf(initialDeviceId) }
    var selectedBrandCategory by remember { mutableStateOf("Semua") }
    val brandCategories = remember {
        listOf("Semua") + CountryIdentityCatalog.devicePresets.map { it.category }.distinct()
    }
    val filteredDevices = remember(selectedBrandCategory) {
        if (selectedBrandCategory == "Semua") {
            CountryIdentityCatalog.devicePresets
        } else {
            CountryIdentityCatalog.devicePresets.filter { it.category == selectedBrandCategory }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Buat identitas browser terisolasi hanya dengan Nama & Email (tanpa perlu password).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Profile (Wajib)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_dialog_name_input")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Profile (Tanpa Password)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_dialog_email_input")
                )

                Text(
                    text = "Negara Identitas Browser (${CountryIdentityCatalog.countries.size} Negara):",
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CountryIdentityCatalog.countries, key = { it.isoCode }) { c ->
                        FilterChip(
                            selected = selectedCountryIso == c.isoCode,
                            onClick = { selectedCountryIso = c.isoCode },
                            label = { Text("${c.flagEmoji} ${c.name}") },
                            modifier = Modifier.testTag("profile_dialog_country_${c.isoCode}")
                        )
                    }
                }

                Text(
                    text = "Kota & Timezone (${selectedCountry.name} • ${selectedCountry.cities.size} Kota):",
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(selectedCountry.cities, key = { it.name }) { city ->
                        FilterChip(
                            selected = selectedCityName == city.name,
                            onClick = { selectedCityName = city.name },
                            label = { Text("${city.name} (${city.utcOffset})") }
                        )
                    }
                }

                Text(
                    text = "Merek Ponsel & Perangkat (${CountryIdentityCatalog.devicePresets.size} Model):",
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(brandCategories, key = { it }) { cat ->
                        FilterChip(
                            selected = selectedBrandCategory == cat,
                            onClick = { selectedBrandCategory = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filteredDevices, key = { it.id }) { dev ->
                        FilterChip(
                            selected = selectedDeviceId == dev.id,
                            onClick = { selectedDeviceId = dev.id },
                            label = { Text(dev.name) },
                            modifier = Modifier.testTag("profile_dialog_device_${dev.id}")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, email, selectedCountryIso, selectedCityName, selectedDeviceId)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("profile_dialog_confirm_button")
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
