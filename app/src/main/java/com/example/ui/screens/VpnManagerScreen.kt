package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.VpnEndpointEntity
import com.example.security.KeystoreCryptoManager
import com.example.viewmodel.GiyaniDestination
import com.example.viewmodel.GiyaniUiState
import com.example.viewmodel.GiyaniViewModel
import com.example.vpn.VpnEngineController
import com.example.vpn.VpnTunnelStatus

@Composable
fun VpnManagerScreen(
    uiState: GiyaniUiState,
    viewModel: GiyaniViewModel,
    onRequestVpnPermission: (Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(GiyaniDestination.HOME_HUB)
    }

    val clipboardManager = LocalClipboardManager.current
    var showImportConfDialog by remember { mutableStateOf(false) }
    var showManualBuilderDialog by remember { mutableStateOf(false) }
    var exportedConfigPreview by remember { mutableStateOf<String?>(null) }
    var editingEndpoint by remember { mutableStateOf<VpnEndpointEntity?>(null) }
    var deletingEndpoint by remember { mutableStateOf<VpnEndpointEntity?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val vpnState = uiState.vpnState
    val isConnected = vpnState.status == VpnTunnelStatus.CONNECTED

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("vpn_manager_screen")
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WireGuard Per-App VPN",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Android VpnService • Android Keystore AES-GCM Key Vault",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { viewModel.refreshDiagnostics() },
                modifier = Modifier.testTag("refresh_vpn_ip_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Verify Public IP",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Live VPN Tunnel & Public IP Verification Card
            item {
                val borderColor = when (vpnState.status) {
                    VpnTunnelStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                    VpnTunnelStatus.FAIL_CLOSED_BLOCKING -> MaterialTheme.colorScheme.error
                    VpnTunnelStatus.CONNECTING -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.outline
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
                        .testTag("vpn_status_card"),
                    shape = RoundedCornerShape(18.dp),
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
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(borderColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Tunnel Status: ${vpnState.status.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = vpnState.statusMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isConnected || vpnState.status == VpnTunnelStatus.FAIL_CLOSED_BLOCKING) {
                                Button(
                                    onClick = { viewModel.disconnectVpn() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    modifier = Modifier.testTag("vpn_disconnect_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PowerSettingsNew,
                                        contentDescription = "Disconnect",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stop")
                                }
                            }
                        }

                        // Tunnel Parameters & Public IP Telemetry Grid
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Detected Public IP: ${vpnState.publicIpTelemetry.publicIp}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "IP Geolocation: ${vpnState.publicIpTelemetry.detectedCountryName} (${vpnState.publicIpTelemetry.detectedCountryCode}) • ${vpnState.publicIpTelemetry.detectedCity}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Selected Country Match: " +
                                        if (vpnState.publicIpTelemetry.matchesSelectedCountry) {
                                            "MATCHED (${uiState.selectedCountry.isoCode})"
                                        } else {
                                            "MISMATCH (Preset: ${uiState.selectedCountry.isoCode} vs IP: ${vpnState.publicIpTelemetry.detectedCountryCode})"
                                        },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (vpnState.publicIpTelemetry.matchesSelectedCountry) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.tertiary
                                    }
                                )
                                Text(
                                    text = "Assigned Tunnel IPv4/IPv6: ${vpnState.assignedTunnelIpv4} | ${vpnState.assignedTunnelIpv6}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Tunnel DNS: ${vpnState.configuredDns} • Routing: ${if (vpnState.browserOnlyPerApp) "Browser-Only (Per-App)" else "System-Wide"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (!vpnState.publicIpTelemetry.matchesSelectedCountry &&
                                    vpnState.publicIpTelemetry.detectedCountryCode != "--"
                                ) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = { viewModel.syncProfileCountryWithDetectedIp() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("sync_profile_country_with_ip_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary,
                                            contentColor = MaterialTheme.colorScheme.onTertiary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Samakan Negara Profile (${uiState.selectedCountry.isoCode}) → Lokasi IP/VPN (${vpnState.publicIpTelemetry.detectedCountryCode})",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }

                        // Auto-Sync Country Identity & IPv6 Leak Blocker Controls
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Auto-Sync Negara Profile dengan VPN / IP",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Otomatis samakan Negara Profile (Preset) dengan Negara Server VPN agar tidak terjadi MISMATCH",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = vpnState.autoSyncCountryWithVpn,
                                        onCheckedChange = { viewModel.setAutoSyncCountryWithVpn(it) },
                                        modifier = Modifier.testTag("switch_auto_sync_country_vpn")
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Blokir Kebocoran IPv6 ISP (Strict IPv4 DNS & ::/0 Sinkhole)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Mencegah OS Android membocorkan alamat IPv6 kartu SIM/Wi-Fi lokal (misal 2404:8000:...) saat VPN aktif",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = vpnState.ipv6LeakProtection,
                                        onCheckedChange = { viewModel.setIpv6LeakProtection(it) },
                                        modifier = Modifier.testTag("switch_ipv6_leak_protection")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Import / Add / Delete All WireGuard Endpoint Action Row
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showImportConfDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_wireguard_conf_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Import .conf",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import .conf", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = { showManualBuilderDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_manual_wireguard_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Endpoint",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Endpoint", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    if (uiState.vpnEndpoints.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daftar Konfigurasi WireGuard (${uiState.vpnEndpoints.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { viewModel.restoreBuiltInWireGuardConfigs() },
                                    modifier = Modifier.testTag("restore_builtin_wireguard_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Pulihkan 10 Conf Asli",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Pulihkan 10 Conf Asli",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                TextButton(
                                    onClick = { showDeleteAllDialog = true },
                                    modifier = Modifier.testTag("delete_all_wireguard_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus Semua Konfigurasi",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Hapus Semua",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Configured WireGuard Endpoints List (or Restore Freza Pack State)
            if (uiState.vpnEndpoints.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("no_vpn_endpoints_honest_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Daftar Konfigurasi WireGuard Kosong",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Anda dapat memuat kembali 10 Konfigurasi WireGuard Asli (Freza US #1–4, Belanda, Jepang, Kanada, Meksiko, Swiss, Norwegia) atau mengimpor file .conf baru di bawah ini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Seluruh Private Key dienkripsi otomatis dengan Android Keystore (AES-256-GCM).",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.restoreBuiltInWireGuardConfigs() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("vpn_restore_freza_pack_button")
                                ) {
                                    Text("⚡ Pulihkan 10 Conf Asli", style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.navigateTo(GiyaniDestination.USER_GUIDE) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("vpn_open_user_guide_button")
                                ) {
                                    Text("📘 Panduan Setup VPN", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            } else {
                items(uiState.vpnEndpoints, key = { it.id }) { endpoint ->
                    val countryFlag = com.example.domain.CountryIdentityCatalog
                        .getCountryByCode(endpoint.countryCode)
                        .flagEmoji
                    val isCurrentlyActiveEndpoint = isConnected && vpnState.activeEndpoint?.id == endpoint.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isCurrentlyActiveEndpoint) 2.dp else 1.dp,
                                color = if (isCurrentlyActiveEndpoint) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .testTag("vpn_endpoint_card_${endpoint.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$countryFlag ${endpoint.name} (${endpoint.countryCode} • ${endpoint.cityName})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Endpoint: ${endpoint.endpointHost}:${endpoint.endpointPort} • Tunnel: ${endpoint.tunnelAddressIpv4}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Button(
                                    onClick = {
                                        viewModel.prepareAndConnectVpn(
                                            endpoint = endpoint,
                                            onPermissionRequired = onRequestVpnPermission
                                        )
                                    },
                                    modifier = Modifier.testTag("connect_endpoint_button_${endpoint.id}")
                                ) {
                                    Text("Connect")
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Peer PubKey: ${KeystoreCryptoManager.maskKeyForDisplay(endpoint.publicKey)} • PrivKey: Android Keystore Protected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = "DNS: ${endpoint.dnsServers} • ${if (endpoint.browserOnlyRouting) "Per-App Browser Only" else "All Apps"} • ${if (endpoint.failClosedKillSwitch) "Fail-Closed ON" else "Fail-Open"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { editingEndpoint = endpoint },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("edit_endpoint_button_${endpoint.id}"),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Konfigurasi WireGuard / .conf",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit .conf", style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = {
                                        exportedConfigPreview = VpnEngineController.exportWireGuardConfig(
                                            endpoint = endpoint,
                                            includePrivateKey = false
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_endpoint_button_${endpoint.id}"),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Export .conf", style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = { deletingEndpoint = endpoint },
                                    modifier = Modifier
                                        .testTag("delete_endpoint_button_${endpoint.id}"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus Konfigurasi WireGuard",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hapus", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. Import WireGuard .conf Dialog
    if (showImportConfDialog) {
        var profileName by remember { mutableStateOf("${uiState.selectedCountry.name} WireGuard") }
        var confText by remember {
            mutableStateOf(
                """
                [Interface]
                Address = 10.66.66.2/32, fd42:42:42::2/128
                DNS = 1.1.1.1, 1.0.0.1
                PrivateKey = 

                [Peer]
                PublicKey = 
                Endpoint = 
                AllowedIPs = 0.0.0.0/0, ::/0
                PersistentKeepalive = 25
                """.trimIndent()
            )
        }
        var browserOnly by remember { mutableStateOf(true) }
        var failClosed by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showImportConfDialog = false },
            title = { Text("Import WireGuard (.conf)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Profile Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confText,
                        onValueChange = { confText = it },
                        label = { Text("Paste WireGuard Configuration") },
                        minLines = 6,
                        maxLines = 9,
                        textStyle = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wireguard_conf_text_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Per-App (Giyani Browser Only)", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = browserOnly, onCheckedChange = { browserOnly = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fail-Closed Kill Switch", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = failClosed, onCheckedChange = { failClosed = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importWireGuardConfig(
                            profileName = profileName,
                            countryCode = uiState.selectedCountry.isoCode,
                            cityName = uiState.selectedCity.name,
                            rawConfText = confText,
                            browserOnly = browserOnly,
                            failClosed = failClosed
                        )
                        showImportConfDialog = false
                    },
                    modifier = Modifier.testTag("confirm_import_wireguard_button")
                ) {
                    Text("Encrypt & Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Manual WireGuard Endpoint Builder Dialog
    if (showManualBuilderDialog) {
        var name by remember { mutableStateOf("${uiState.selectedCity.name} Node") }
        var host by remember { mutableStateOf("") }
        var port by remember { mutableStateOf("51820") }
        var tunnelIpv4 by remember { mutableStateOf("10.66.66.2/32") }
        var dns by remember { mutableStateOf("1.1.1.1, 1.0.0.1") }
        var pubKey by remember { mutableStateOf("") }
        var privKey by remember { mutableStateOf("") }
        var browserOnly by remember { mutableStateOf(true) }
        var failClosed by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showManualBuilderDialog = false },
            title = { Text("Configure WireGuard Endpoint") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Server Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("Host / IP") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(2f)
                                .testTag("wg_manual_host_input")
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("Port") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = pubKey,
                        onValueChange = { pubKey = it },
                        label = { Text("Server Public Key (Base64)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wg_manual_pubkey_input")
                    )
                    OutlinedTextField(
                        value = privKey,
                        onValueChange = { privKey = it },
                        label = { Text("Client Private Key (Encrypted in Keystore)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wg_manual_privkey_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tunnelIpv4,
                            onValueChange = { tunnelIpv4 = it },
                            label = { Text("Tunnel CIDR") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dns,
                            onValueChange = { dns = it },
                            label = { Text("DNS") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addManualWireGuardEndpoint(
                            name = name,
                            countryCode = uiState.selectedCountry.isoCode,
                            cityName = uiState.selectedCity.name,
                            host = host,
                            port = port.toIntOrNull() ?: 51820,
                            tunnelIpv4 = tunnelIpv4,
                            tunnelIpv6 = "fd42:42:42::2/128",
                            dns = dns,
                            publicKey = pubKey,
                            privateKey = privKey,
                            browserOnly = browserOnly,
                            failClosed = failClosed
                        )
                        showManualBuilderDialog = false
                    },
                    modifier = Modifier.testTag("confirm_save_manual_wg_button")
                ) {
                    Text("Save to Keystore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualBuilderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Export Config Preview Dialog
    exportedConfigPreview?.let { configStr ->
        AlertDialog(
            onDismissRequest = { exportedConfigPreview = null },
            title = { Text("Exported WireGuard Configuration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Private keys remain redacted by default for security.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = configStr,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(configStr))
                        viewModel.showMessage("Copied exported WireGuard config to clipboard")
                        exportedConfigPreview = null
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Config")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportedConfigPreview = null }) {
                    Text("Close")
                }
            }
        )
    }

    // 4. Edit WireGuard / .conf Configuration Dialog (Dual Mode: Raw .conf Text or Parameter Form)
    editingEndpoint?.let { targetEndpoint ->
        var editAsRawConf by remember(targetEndpoint.id) { mutableStateOf(true) }
        var profileName by remember(targetEndpoint.id) { mutableStateOf(targetEndpoint.name) }
        var countryCode by remember(targetEndpoint.id) { mutableStateOf(targetEndpoint.countryCode) }
        var cityName by remember(targetEndpoint.id) { mutableStateOf(targetEndpoint.cityName) }
        var rawConfText by remember(targetEndpoint.id) {
            mutableStateOf(
                VpnEngineController.exportWireGuardConfig(
                    endpoint = targetEndpoint,
                    includePrivateKey = true
                )
            )
        }
        var host by remember(targetEndpoint.id) { mutableStateOf(targetEndpoint.endpointHost) }
        var port by remember(targetEndpoint.id) { mutableStateOf(targetEndpoint.endpointPort.toString()) }
        var tunnelIpv4 by remember(targetEndpoint.id) { mutableStateOf(targetEndpoint.tunnelAddressIpv4) }
        var tunnelIpv6 by remember(targetEndpoint.id) { mutableStateOf(targetEndpoint.tunnelAddressIpv6) }
        var dns by remember(targetEndpoint.id) { mutableStateOf(targetEndpoint.dnsServers) }
        var pubKey by remember(targetEndpoint.id) { mutableStateOf(targetEndpoint.publicKey) }
        var privKey by remember(targetEndpoint.id) {
            mutableStateOf(
                KeystoreCryptoManager.decryptSecret(targetEndpoint.encryptedPrivateKey).orEmpty()
            )
        }
        var browserOnly by remember(targetEndpoint.id) { mutableStateOf(targetEndpoint.browserOnlyRouting) }
        var failClosed by remember(targetEndpoint.id) { mutableStateOf(targetEndpoint.failClosedKillSwitch) }

        AlertDialog(
            onDismissRequest = { editingEndpoint = null },
            title = { Text("Edit Konfigurasi WireGuard / .conf") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Mode Switcher: Edit Teks .conf vs Edit Form Parameter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { editAsRawConf = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_wg_mode_conf_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (editAsRawConf) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (editAsRawConf) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("Edit Teks .conf", style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = { editAsRawConf = false },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_wg_mode_form_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!editAsRawConf) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (!editAsRawConf) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("Edit Parameter", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Nama Konfigurasi") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_wg_name_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = countryCode,
                            onValueChange = { countryCode = it.uppercase().take(2) },
                            label = { Text("Kode Negara (US/ID/JP)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_wg_country_input")
                        )
                        OutlinedTextField(
                            value = cityName,
                            onValueChange = { cityName = it },
                            label = { Text("Kota Server") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("edit_wg_city_input")
                        )
                    }

                    if (editAsRawConf) {
                        OutlinedTextField(
                            value = rawConfText,
                            onValueChange = { rawConfText = it },
                            label = { Text("Isi File WireGuard (.conf)") },
                            minLines = 6,
                            maxLines = 9,
                            textStyle = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_wireguard_conf_text_input")
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = host,
                                onValueChange = { host = it },
                                label = { Text("Endpoint Host / IP") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(2f)
                                    .testTag("edit_wg_host_input")
                            )
                            OutlinedTextField(
                                value = port,
                                onValueChange = { port = it },
                                label = { Text("Port") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = pubKey,
                            onValueChange = { pubKey = it },
                            label = { Text("Peer Public Key") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = privKey,
                            onValueChange = { privKey = it },
                            label = { Text("Interface Private Key") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = tunnelIpv4,
                                onValueChange = { tunnelIpv4 = it },
                                label = { Text("Tunnel IPv4 CIDR") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = dns,
                                onValueChange = { dns = it },
                                label = { Text("DNS") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Per-App (Giyani Browser Only)", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = browserOnly, onCheckedChange = { browserOnly = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fail-Closed Kill Switch", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = failClosed, onCheckedChange = { failClosed = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editAsRawConf) {
                            viewModel.editWireGuardEndpointFromConf(
                                existingEndpoint = targetEndpoint,
                                profileName = profileName,
                                countryCode = countryCode.ifBlank { uiState.selectedCountry.isoCode },
                                cityName = cityName.ifBlank { uiState.selectedCity.name },
                                rawConfText = rawConfText,
                                browserOnly = browserOnly,
                                failClosed = failClosed
                            )
                        } else {
                            viewModel.editWireGuardEndpointManual(
                                existingEndpoint = targetEndpoint,
                                name = profileName,
                                countryCode = countryCode.ifBlank { uiState.selectedCountry.isoCode },
                                cityName = cityName.ifBlank { uiState.selectedCity.name },
                                host = host,
                                port = port.toIntOrNull() ?: 51820,
                                tunnelIpv4 = tunnelIpv4,
                                tunnelIpv6 = tunnelIpv6,
                                dns = dns,
                                publicKey = pubKey,
                                newPrivateKeyPlain = privKey,
                                browserOnly = browserOnly,
                                failClosed = failClosed
                            )
                        }
                        editingEndpoint = null
                    },
                    modifier = Modifier.testTag("confirm_edit_wireguard_button")
                ) {
                    Text("Simpan Perubahan")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEndpoint = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // 5. Confirm Delete Single WireGuard Configuration Dialog
    deletingEndpoint?.let { targetEndpoint ->
        AlertDialog(
            onDismissRequest = { deletingEndpoint = null },
            title = { Text("Hapus Konfigurasi WireGuard?") },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus konfigurasi \"${targetEndpoint.name}\" (${targetEndpoint.endpointHost}:${targetEndpoint.endpointPort})? Jika VPN ini sedang tersambung, koneksi akan diputus secara otomatis."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVpnEndpoint(targetEndpoint.id)
                        deletingEndpoint = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_single_wireguard_button")
                ) {
                    Text("Hapus Konfigurasi")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingEndpoint = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // 6. Confirm Delete All WireGuard Configurations Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Hapus Semua Konfigurasi WireGuard?") },
            text = {
                Text(
                    "Semua konfigurasi WireGuard (.conf) yang tersimpan (${uiState.vpnEndpoints.size} server) akan dihapus dan koneksi VPN aktif akan dihentikan."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllVpnEndpoints()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_all_wireguard_button")
                ) {
                    Text("Hapus Semua")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
