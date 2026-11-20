package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.CountryIdentityCatalog
import com.example.viewmodel.GiyaniDestination
import com.example.viewmodel.GiyaniUiState
import com.example.viewmodel.GiyaniViewModel
import com.example.vpn.VpnTunnelStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeHubScreen(
    uiState: GiyaniUiState,
    viewModel: GiyaniViewModel,
    onRequestVpnPermission: (Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    val country = uiState.selectedCountry
    val city = uiState.selectedCity
    val device = uiState.selectedDevice
    val vpnState = uiState.vpnState
    val matchingEndpoints = uiState.vpnEndpoints.filter {
        it.countryCode.equals(country.isoCode, ignoreCase = true)
    }
    var homeQuickUrlInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("home_hub_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        // 1. Top Identity, Dark/Light Theme & Mode Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Giyani Shield",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GIYANI BROWSER",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "Profile: ${uiState.activeProfile.name} (${uiState.activeProfiles.size} Aktif di ${country.isoCode}) • ${if (uiState.isDarkTheme) "Dark Theme" else "Light Theme"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Dark / Light Theme Toggle Button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { viewModel.toggleDarkTheme() }
                            .testTag("home_theme_toggle_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Dark/Light Theme",
                                tint = if (uiState.isDarkTheme) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Simple Mode / Advanced Mode Toggle Pill
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("mode_toggle_surface")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.isAdvancedMode) "Adv" else "Simple",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (uiState.isAdvancedMode) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = uiState.isAdvancedMode,
                                onCheckedChange = { viewModel.setAdvancedMode(it) },
                                modifier = Modifier.testTag("simple_advanced_mode_switch")
                            )
                        }
                    }
                }
            }
        }

        // 1B. Always-Present Quick URL / Search Bar on Home Hub
        item {
            OutlinedTextField(
                value = homeQuickUrlInput,
                onValueChange = { homeQuickUrlInput = it },
                placeholder = {
                    Text("Ketik alamat URL (quarterfull.io, google.com) atau cari...")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search or Enter URL",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (homeQuickUrlInput.isNotBlank()) {
                            IconButton(
                                onClick = { homeQuickUrlInput = "" },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                val target = homeQuickUrlInput.ifBlank { "https://www.google.com" }
                                viewModel.loadUrlInActiveTab(target)
                            },
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .testTag("home_quick_url_go_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Buka", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        val target = homeQuickUrlInput.ifBlank { "https://www.google.com" }
                        viewModel.loadUrlInActiveTab(target)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_quick_url_input")
            )
        }

        // 2. Hero Country Identity Shield & VPN Status Banner
        item {
            val isVpnConnected = vpnState.status == VpnTunnelStatus.CONNECTED
            val ipMatches = vpnState.publicIpTelemetry.matchesSelectedCountry

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = if (isVpnConnected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                    .testTag("country_identity_hero_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = country.flagEmoji,
                                    style = MaterialTheme.typography.displayLarge
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = country.name,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${city.name} (${city.region}) • ${city.timezoneId}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            // Start Browsing CTA
                            Button(
                                onClick = { viewModel.navigateTo(GiyaniDestination.BROWSER_WEBVIEW) },
                                modifier = Modifier.testTag("start_browsing_hero_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Launch Browser",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Browse", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        // Coherent Environment Badges
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EnvironmentBadge(
                                label = "Locale: ${country.defaultLanguage}",
                                color = MaterialTheme.colorScheme.primary
                            )
                            EnvironmentBadge(
                                label = "Currency: ${country.currencyCode} (${country.currencySymbol})",
                                color = MaterialTheme.colorScheme.secondary
                            )
                            EnvironmentBadge(
                                label = "Date: ${country.dateFormat} • ${country.measurementSystem}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            EnvironmentBadge(
                                label = "Device: ${device.name}",
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        // Transparent VPN & Network Location Status Bar
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
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
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (vpnState.status) {
                                                        VpnTunnelStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                                                        VpnTunnelStatus.FAIL_CLOSED_BLOCKING -> MaterialTheme.colorScheme.error
                                                        VpnTunnelStatus.CONNECTING -> MaterialTheme.colorScheme.tertiary
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "WireGuard Per-App VPN: ${vpnState.status.name}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (isVpnConnected) {
                                        OutlinedButton(
                                            onClick = { viewModel.disconnectVpn() },
                                            modifier = Modifier.testTag("home_vpn_disconnect_button"),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text("Disconnect", style = MaterialTheme.typography.labelLarge)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                val targetEndpoint = matchingEndpoints.find {
                                                    it.id == uiState.activeProfile.preferredVpnServerId
                                                } ?: matchingEndpoints.firstOrNull()
                                                    ?: uiState.vpnEndpoints.firstOrNull()
                                                if (targetEndpoint == null) {
                                                    viewModel.navigateTo(GiyaniDestination.VPN_MANAGER)
                                                    viewModel.showMessage("Import or configure a WireGuard server endpoint for ${country.name}")
                                                } else {
                                                    viewModel.prepareAndConnectVpn(
                                                        endpoint = targetEndpoint,
                                                        onPermissionRequired = onRequestVpnPermission
                                                    )
                                                }
                                            },
                                            modifier = Modifier.testTag("home_vpn_connect_button"),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VpnKey,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (matchingEndpoints.isNotEmpty()) {
                                                    "Connect ${country.isoCode} VPN (${matchingEndpoints.size})"
                                                } else {
                                                    "Configure VPN"
                                                },
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }

                                // Quick Server Chips for Current Country (e.g. US-FREE#1, US-FREE#2, US-FREE#4, US-FREE#27)
                                if (matchingEndpoints.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.testTag("home_country_vpn_servers_row")
                                    ) {
                                        items(matchingEndpoints, key = { it.id }) { ep ->
                                            val isEpActive = isVpnConnected && vpnState.activeEndpoint?.id == ep.id
                                            FilterChip(
                                                selected = isEpActive,
                                                onClick = {
                                                    viewModel.prepareAndConnectVpn(
                                                        endpoint = ep,
                                                        onPermissionRequired = onRequestVpnPermission
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = "${ep.name} (${ep.endpointHost})",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.VpnKey,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                },
                                                modifier = Modifier.testTag("home_vpn_chip_${ep.id}")
                                            )
                                        }
                                    }
                                }

                                // Honest Location & Public IP Transparency Notice
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (ipMatches) Icons.Default.VerifiedUser else Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = if (ipMatches) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Public IP: ${vpnState.publicIpTelemetry.publicIp} (${vpnState.publicIpTelemetry.detectedCountryName}) • " +
                                            if (ipMatches) "Matches ${country.isoCode} Identity" else "Browser locale is ${country.isoCode}; connect ${country.isoCode} VPN to align IP location",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Country Selector Carousel (18 Global Countries)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Select Country Identity Preset (${CountryIdentityCatalog.countries.size} Negara)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("country_selector_lazy_row")
                ) {
                    items(CountryIdentityCatalog.countries, key = { it.isoCode }) { preset ->
                        val isSelected = preset.isoCode == country.isoCode
                        Card(
                            modifier = Modifier
                                .width(156.dp)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.selectCountry(preset.isoCode) }
                                .testTag("country_card_${preset.isoCode}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = preset.flagEmoji,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = preset.isoCode,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${preset.cities.size} Kota • ${preset.defaultLanguage} • ${preset.currencyCode}",
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

        // 4. City & Automatic Timezone Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. Select City & Regional Timezone (${country.name} • ${country.cities.size} Kota)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = city.utcOffset,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("city_selector_lazy_row")
                ) {
                    items(country.cities, key = { it.name }) { cityOption ->
                        val isCitySelected = cityOption.name == city.name
                        FilterChip(
                            selected = isCitySelected,
                            onClick = { viewModel.selectCity(cityOption) },
                            label = {
                                Text("${cityOption.name} (${cityOption.timezoneId.substringAfterLast("/")})")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationCity,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("city_chip_${cityOption.name}")
                        )
                    }
                }
            }
        }

        // 5. Device Profile Preset Selector (25+ Smartphone Brands & Form Factors)
        item {
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "3. Device Compatibility Profile (${CountryIdentityCatalog.devicePresets.size} Merek & Model HP/PC)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Brand Category Filter Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("device_brand_category_row")
                ) {
                    items(brandCategories, key = { it }) { cat ->
                        FilterChip(
                            selected = selectedBrandCategory == cat,
                            onClick = { selectedBrandCategory = cat },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("brand_category_chip_$cat")
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("device_preset_lazy_row")
                ) {
                    items(filteredDevices, key = { it.id }) { preset ->
                        val isDeviceSelected = preset.id == device.id
                        Card(
                            modifier = Modifier
                                .width(205.dp)
                                .border(
                                    width = if (isDeviceSelected) 1.5.dp else 1.dp,
                                    color = if (isDeviceSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.selectDevicePreset(preset.id) }
                                .testTag("device_preset_card_${preset.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDeviceSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = if (isDeviceSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = preset.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${preset.viewportWidth}x${preset.viewportHeight} • ${preset.deviceMemoryGb}GB RAM • ${preset.browserName}",
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

        // 6. Quick Workspace Hub Cards: Tab Groups, Download Manager, Diagnostics, Multi-Profile
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Workspace & Modules",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tab Groups Quick Card
                    QuickModuleCard(
                        title = "Tab Groups",
                        subtitle = "${uiState.tabGroups.size} Groups • ${uiState.tabs.size} Tabs",
                        icon = Icons.Default.Folder,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.navigateTo(GiyaniDestination.TAB_GROUPS) },
                        testTag = "quick_card_tab_groups",
                        modifier = Modifier.weight(1f)
                    )

                    // Download Manager Quick Card
                    QuickModuleCard(
                        title = "Downloads",
                        subtitle = "${uiState.downloads.size} Files • Range Resume",
                        icon = Icons.Default.CloudDownload,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = { viewModel.navigateTo(GiyaniDestination.DOWNLOADS) },
                        testTag = "quick_card_downloads",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Identity Diagnostics Quick Card
                    QuickModuleCard(
                        title = "Diagnostics",
                        subtitle = "${uiState.diagnostics.size} Runtime Metrics",
                        icon = Icons.Default.Security,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        onClick = { viewModel.navigateTo(GiyaniDestination.DIAGNOSTICS) },
                        testTag = "quick_card_diagnostics",
                        modifier = Modifier.weight(1f)
                    )

                    // Multi-Profiles & Privacy Quick Card
                    QuickModuleCard(
                        title = "Profiles & Privacy",
                        subtitle = "${uiState.profiles.size} Isolated Profiles",
                        icon = Icons.Default.ManageAccounts,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.navigateTo(GiyaniDestination.PROFILES_PRIVACY) },
                        testTag = "quick_card_profiles",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 7. Advanced Mode Inspector Section (Only visible when Advanced Mode is toggled ON)
        if (uiState.isAdvancedMode) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("advanced_mode_telemetry_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Advanced Identity & Header Configuration",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "User-Agent: ${device.userAgent}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Accept-Language: ${country.acceptLanguageHeader}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Sec-CH-UA: ${device.secChUa.ifBlank { "Omitted for Compatibility" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Viewport & Hardware: ${device.viewportWidth}x${device.viewportHeight} @ ${device.devicePixelRatio}x • ${device.hardwareConcurrency} cores • ${device.deviceMemoryGb}GB RAM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 8. Bookmarks Quick Launch
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Profile Bookmarks (${uiState.bookmarks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                uiState.bookmarks.forEach { bm ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.loadUrlInActiveTab(bm.url) }
                            .testTag("bookmark_item_${bm.id}"),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = bm.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = bm.url,
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
}

@Composable
private fun EnvironmentBadge(
    label: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(
            width = 1.dp,
            color = color.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp)
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun QuickModuleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
