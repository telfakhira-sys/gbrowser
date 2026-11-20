package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.browser.BrowserEngineManager
import com.example.domain.CountryIdentityCatalog
import com.example.viewmodel.GiyaniDestination
import com.example.viewmodel.GiyaniUiState
import com.example.viewmodel.GiyaniViewModel
import com.example.vpn.VpnTunnelStatus

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebViewScreen(
    uiState: GiyaniUiState,
    viewModel: GiyaniViewModel,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val urlFocusRequester = remember { FocusRequester() }

    val activeTab = uiState.activeTab
    var addressInput by remember(activeTab?.id, activeTab?.url) {
        mutableStateOf(activeTab?.url ?: "https://quarterfull.io")
    }
    var isUrlBarFocused by remember { mutableStateOf(false) }
    var hasLoadedFirstPage by remember(uiState.activeProfile.id) { mutableStateOf(false) }
    var rendererRecoveryTick by remember { mutableStateOf(0) }
    var lastRequestedWebViewUrl by remember(uiState.activeProfile.id, rendererRecoveryTick) {
        mutableStateOf<String?>(null)
    }

    var webViewRef by remember(uiState.activeProfile.id, rendererRecoveryTick) {
        mutableStateOf<WebView?>(BrowserEngineManager.getActiveProfileWebView(uiState.activeProfile.id))
    }

    // HTML5 CustomView Fullscreen state (for full-bleed video / canvas embeds)
    var html5CustomView by remember { mutableStateOf<View?>(null) }
    var html5CustomViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val activeTabGroup = remember(activeTab?.groupId, uiState.tabGroups) {
        uiState.tabGroups.find { it.id == activeTab?.groupId }
    }

    val groupTabs = remember(activeTab?.groupId, uiState.tabs) {
        if (activeTab?.groupId != null) {
            uiState.tabs.filter { it.groupId == activeTab.groupId }
        } else {
            uiState.tabs
        }
    }

    // Filter live URL suggestions from Bookmarks + Browsing History + Quick Portals
    val urlSuggestions = remember(addressInput, uiState.bookmarks, uiState.history) {
        val query = addressInput.trim().lowercase()
        val quickSites = listOf(
            "Google Search" to "https://www.google.com",
            "QuarterFull — Read & Create" to "https://quarterfull.io",
            "DuckDuckGo Privacy Search" to "https://duckduckgo.com",
            "Wikipedia" to "https://www.wikipedia.org"
        )
        val combined = buildList {
            addAll(quickSites)
            uiState.bookmarks.forEach { add(it.title to it.url) }
            uiState.history.forEach { add(it.title to it.url) }
        }.distinctBy { it.second }

        if (query.isBlank() || query == "https://www.google.com") {
            combined.take(5)
        } else {
            combined.filter { (title, url) ->
                title.lowercase().contains(query) || url.lowercase().contains(query)
            }.take(5)
        }
    }

    BackHandler {
        when {
            html5CustomView != null -> {
                html5CustomViewCallback?.onCustomViewHidden()
                html5CustomView = null
            }
            isUrlBarFocused -> {
                focusManager.clearFocus()
                isUrlBarFocused = false
            }
            uiState.isFullscreenMode -> {
                viewModel.setFullscreenMode(false)
            }
            uiState.isLeftPanelOpen || uiState.isRightPanelOpen -> {
                viewModel.closeBothPanels()
            }
            webViewRef?.canGoBack() == true -> {
                webViewRef?.goBack()
            }
            else -> {
                viewModel.navigateTo(GiyaniDestination.HOME_HUB)
            }
        }
    }

    DisposableEffect(viewModel, uiState.activeProfile.id) {
        viewModel.onWebViewNavigate = { targetUrl ->
            val regionalUrl = BrowserEngineManager.normalizeRegionalUrl(targetUrl, uiState.activeProfile)
            addressInput = regionalUrl
            isUrlBarFocused = false
            focusManager.clearFocus()
            val headers = BrowserEngineManager.buildRequestHeaders(
                country = uiState.selectedCountry,
                device = uiState.selectedDevice,
                profile = uiState.activeProfile,
                desktopViewportOverride = uiState.isDesktopViewportOverride
            )
            webViewRef?.loadUrl(regionalUrl, headers)
        }
        viewModel.onWebViewBack = { webViewRef?.goBack() }
        viewModel.onWebViewForward = { webViewRef?.goForward() }
        viewModel.onWebViewReload = { webViewRef?.reload() }

        onDispose {
            // Snapshot cookies when leaving or switching so login session is preserved
            if (uiState.activeProfile.keepLoginSession) {
                BrowserEngineManager.saveProfileCookieSnapshot(
                    context = webViewRef?.context ?: return@onDispose,
                    profile = uiState.activeProfile,
                    candidateUrls = uiState.tabs.map { it.url } + listOfNotNull(webViewRef?.url)
                )
            }
            viewModel.onWebViewNavigate = null
            viewModel.onWebViewBack = null
            viewModel.onWebViewForward = null
            viewModel.onWebViewReload = null
        }
    }

    // Re-apply isolated profile identity settings whenever focused profile, active tab, theme, or desktop viewport changes
    LaunchedEffect(
        uiState.activeProfile.id,
        uiState.activeProfile.devicePresetId,
        uiState.activeProfile.countryCode,
        uiState.activeProfile.cityName,
        uiState.activeProfile.keepLoginSession,
        activeTab?.id,
        uiState.isDarkTheme,
        uiState.isDesktopViewportOverride
    ) {
        val wv = webViewRef ?: return@LaunchedEffect
        BrowserEngineManager.applyProfileToWebView(
            webView = wv,
            profile = uiState.activeProfile,
            isPrivateTab = activeTab?.isPrivate == true,
            desktopViewportOverride = uiState.isDesktopViewportOverride
        )
        val targetUrl = BrowserEngineManager.normalizeRegionalUrl(
            activeTab?.url ?: "https://quarterfull.io",
            uiState.activeProfile
        )
        val currentWvUrl = wv.url
        if (currentWvUrl.isNullOrBlank()) {
            if (lastRequestedWebViewUrl != targetUrl) {
                lastRequestedWebViewUrl = targetUrl
                val headers = BrowserEngineManager.buildRequestHeaders(
                    country = uiState.selectedCountry,
                    device = uiState.selectedDevice,
                    profile = uiState.activeProfile,
                    desktopViewportOverride = uiState.isDesktopViewportOverride
                )
                wv.loadUrl(targetUrl, headers)
            }
        } else {
            // Re-inject coherent JS identity & per-profile DOMStorage login vault without resetting active login page!
            val script = BrowserEngineManager.buildCoherentIdentityScript(
                country = uiState.selectedCountry,
                device = uiState.selectedDevice,
                profile = uiState.activeProfile,
                isDarkTheme = uiState.isDarkTheme,
                desktopViewportOverride = uiState.isDesktopViewportOverride
            )
            wv.evaluateJavascript(script, null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("browser_webview_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // =====================================================================
            // ALWAYS-PRESENT CHROME APK TOP HEADER & ADAPTIVE DYNAMIC OMNIBOX
            // Visible at all times (even before initial google.com/duckduckgo loads
            // or during Fail-Closed Kill Switch) unless Fullscreen Mode is active.
            // =====================================================================
            AnimatedVisibility(
                visible = !uiState.isFullscreenMode && html5CustomView == null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        // 1. Multi-Profile Active Switcher Strip + Theme Toggle + VPN Badge
                        // (Collapsed automatically when URL Input is focused so the user gets maximum typing space)
                        AnimatedVisibility(visible = !isUrlBarFocused) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.65f))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                LazyRow(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("active_multi_profiles_strip"),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(uiState.activeProfiles, key = { it.id }) { prof ->
                                        val isFocused = prof.id == uiState.activeProfile.id
                                        val pCountry = CountryIdentityCatalog.getCountryByCode(prof.countryCode)
                                        val pDevice = CountryIdentityCatalog.getDeviceById(prof.devicePresetId)

                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isFocused) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            modifier = Modifier
                                                .border(
                                                    width = if (isFocused) 1.5.dp else 1.dp,
                                                    color = if (isFocused) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.outline
                                                    },
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                                .clickable {
                                                    viewModel.useProfile(prof, forceSwitchCountry = false)
                                                }
                                                .testTag("active_profile_chip_${prof.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isFocused) {
                                                                MaterialTheme.colorScheme.primary
                                                            } else {
                                                                MaterialTheme.colorScheme.secondary
                                                            }
                                                        )
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${pCountry.flagEmoji} ${prof.name.substringBefore(" (")}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isFocused) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "• ${pDevice.name.substringBefore(" ")}${if (prof.keepLoginSession) " 🔒" else ""}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    // Quick button to open Left Profile Manager Panel
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                            modifier = Modifier
                                                .clickable { viewModel.toggleLeftPanel() }
                                                .testTag("add_or_manage_profiles_chip")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PersonAdd,
                                                    contentDescription = "Manage Profiles",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "+ Profile",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Quick Dark / Light Theme Toggle Chip
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .clickable { viewModel.toggleDarkTheme() }
                                        .testTag("chrome_top_theme_toggle_badge")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "Toggle Dark/Light Theme",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // VPN & Country Badge on the right of the profile strip
                                Surface(
                                    color = if (uiState.vpnState.status == VpnTunnelStatus.CONNECTED) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .clickable { viewModel.toggleRightPanel() }
                                        .testTag("chrome_top_vpn_badge")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VpnKey,
                                            contentDescription = null,
                                            tint = if (uiState.vpnState.status == VpnTunnelStatus.CONNECTED) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.tertiary
                                            },
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${uiState.selectedCountry.isoCode} ${if (uiState.vpnState.status == VpnTunnelStatus.CONNECTED) "VPN ON" else "DIRECT"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (uiState.vpnState.status == VpnTunnelStatus.CONNECTED) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Chrome APK Style Horizontal Tab Strip (for current focused profile)
                        AnimatedVisibility(visible = !isUrlBarFocused) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val accent = activeTabGroup?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary
                                LazyRow(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("in_group_tab_strip"),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(groupTabs, key = { it.id }) { tabItem ->
                                        val isCurrent = tabItem.id == activeTab?.id
                                        Surface(
                                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                                            color = if (isCurrent) {
                                                accent.copy(alpha = 0.22f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                            },
                                            modifier = Modifier
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isCurrent) accent else Color.Transparent,
                                                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                                )
                                                .clickable {
                                                    viewModel.switchToTab(tabItem)
                                                    val targetHeaders = BrowserEngineManager.buildRequestHeaders(
                                                        country = uiState.selectedCountry,
                                                        device = uiState.selectedDevice,
                                                        profile = uiState.activeProfile,
                                                        desktopViewportOverride = uiState.isDesktopViewportOverride
                                                    )
                                                    webViewRef?.loadUrl(tabItem.url, targetHeaders)
                                                }
                                                .testTag("strip_tab_chip_${tabItem.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = tabItem.title.ifBlank { tabItem.url }.take(20),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                                if (groupTabs.size > 1) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Tutup tab",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier
                                                            .size(13.dp)
                                                            .clickable { viewModel.closeTab(tabItem) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Quick + New Tab button on Chrome tab bar
                                IconButton(
                                    onClick = {
                                        viewModel.createNewTab(
                                            url = "https://www.google.com",
                                            title = "Google (${uiState.activeProfile.name.substringBefore(" ")})",
                                            groupId = activeTab?.groupId,
                                            isPrivate = false,
                                            openImmediately = true
                                        )
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("chrome_quick_new_tab_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "New Tab",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // 3. ADAPTIVE DYNAMIC OMNIBOX TOOLBAR:
                        // When `isUrlBarFocused == true`, side buttons automatically collapse so the
                        // URL Input Field expands to 100% width with Clear, Paste & Go, and Cancel controls!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!isUrlBarFocused) {
                                // LEFT PANEL TOGGLE BUTTON (Show/Hide Profiles & Tab Workspace Drawer)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (uiState.isLeftPanelOpen) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    },
                                    modifier = Modifier
                                        .clickable { viewModel.toggleLeftPanel() }
                                        .testTag("toggle_left_panel_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ManageAccounts,
                                            contentDescription = "Toggle Left Profile Panel",
                                            tint = if (uiState.isLeftPanelOpen) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${uiState.activeProfiles.size}P",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (uiState.isLeftPanelOpen) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    }
                                }

                                // Back Button
                                IconButton(
                                    onClick = { webViewRef?.goBack() },
                                    enabled = uiState.canGoBack || webViewRef?.canGoBack() == true,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("browser_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Navigate Back",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // ADAPTIVE EXPANDING OMNIBOX URL INPUT FIELD
                            OutlinedTextField(
                                value = addressInput,
                                onValueChange = { addressInput = it },
                                singleLine = true,
                                placeholder = {
                                    Text(
                                        text = "Ketik URL (misal quarterfull.io) atau cari di Google",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isUrlBarFocused) Icons.Default.Search else Icons.Default.Lock,
                                        contentDescription = "HTTPS / Search",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isUrlBarFocused) {
                                            if (addressInput.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { addressInput = "" },
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .testTag("omnibox_clear_url_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Clear URL",
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    val clip = clipboardManager.getText()?.text.orEmpty().trim()
                                                    if (clip.isNotBlank()) {
                                                        addressInput = clip
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .testTag("omnibox_paste_url_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentPaste,
                                                    contentDescription = "Paste URL",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        } else {
                                            IconButton(
                                                onClick = { webViewRef?.reload() },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .testTag("browser_refresh_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Reload Page",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        isUrlBarFocused = false
                                        focusManager.clearFocus()
                                        viewModel.loadUrlInActiveTab(addressInput)
                                    }
                                ),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .focusRequester(urlFocusRequester)
                                    .onFocusChanged { focusState ->
                                        isUrlBarFocused = focusState.isFocused
                                    }
                                    .testTag("browser_address_bar")
                            )

                            if (isUrlBarFocused) {
                                // "Go" and "Batal" quick buttons when URL bar is expanded 100%
                                Button(
                                    onClick = {
                                        isUrlBarFocused = false
                                        focusManager.clearFocus()
                                        viewModel.loadUrlInActiveTab(addressInput)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .testTag("omnibox_go_button")
                                ) {
                                    Text("Buka", style = MaterialTheme.typography.labelMedium)
                                }
                                TextButton(
                                    onClick = {
                                        addressInput = activeTab?.url ?: "https://www.google.com"
                                        isUrlBarFocused = false
                                        focusManager.clearFocus()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("omnibox_cancel_focus_button")
                                ) {
                                    Text("Batal", style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                // FULLSCREEN TOGGLE BUTTON
                                IconButton(
                                    onClick = { viewModel.toggleFullscreenMode() },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("browser_fullscreen_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Fullscreen Mode",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Chrome Square Tab Count Button
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .border(
                                            width = 1.5.dp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.navigateTo(GiyaniDestination.TAB_GROUPS) }
                                        .testTag("browser_tab_switcher_button")
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${uiState.tabs.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // RIGHT PANEL TOGGLE BUTTON (Show/Hide Identity, Country, Device & VPN Settings Drawer)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (uiState.isRightPanelOpen) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                                    },
                                    modifier = Modifier
                                        .clickable { viewModel.toggleRightPanel() }
                                        .testTag("toggle_right_panel_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "Toggle Right Settings Panel",
                                            tint = if (uiState.isRightPanelOpen) {
                                                MaterialTheme.colorScheme.onSecondary
                                            } else {
                                                MaterialTheme.colorScheme.secondary
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 4. EXPANDED URL SUGGESTIONS, DOMAIN CHIPS & PASTE-AND-GO BAR (Visible when URL Input is focused)
                        AnimatedVisibility(visible = isUrlBarFocused) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Domain snippet & Paste-and-Go chips
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            modifier = Modifier
                                                .clickable {
                                                    val clip = clipboardManager.getText()?.text.orEmpty().trim()
                                                    if (clip.isNotBlank()) {
                                                        addressInput = clip
                                                        isUrlBarFocused = false
                                                        focusManager.clearFocus()
                                                        viewModel.loadUrlInActiveTab(clip)
                                                    }
                                                }
                                                .testTag("omnibox_paste_and_go_chip")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentPaste,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Tempel & Buka",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    val domainTokens = listOf("https://", "www.", ".com", ".io", ".id", "quarterfull.io", "google.com")
                                    items(domainTokens) { token ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    addressInput = when {
                                                        token == "quarterfull.io" -> "https://quarterfull.io"
                                                        token == "google.com" -> "https://www.google.com"
                                                        token.startsWith("https://") -> "https://" + addressInput.removePrefix("https://").removePrefix("http://")
                                                        else -> addressInput + token
                                                    }
                                                }
                                        ) {
                                            Text(
                                                text = token,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }

                                    // Desktop Viewport Quick Chip
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (uiState.isDesktopViewportOverride) {
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            },
                                            modifier = Modifier
                                                .border(
                                                    width = 1.dp,
                                                    color = if (uiState.isDesktopViewportOverride) {
                                                        MaterialTheme.colorScheme.secondary
                                                    } else {
                                                        MaterialTheme.colorScheme.outline
                                                    },
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable { viewModel.toggleDesktopViewportOverride() }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DesktopWindows,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (uiState.isDesktopViewportOverride) "Desktop: ON" else "Desktop Mode",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                }

                                // Instant Bookmark & History Suggestions List
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 170.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(urlSuggestions, key = { it.second }) { (title, url) ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    addressInput = url
                                                    isUrlBarFocused = false
                                                    focusManager.clearFocus()
                                                    viewModel.loadUrlInActiveTab(url)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Public,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = title,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = url,
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

                        if (uiState.isPageLoading) {
                            LinearProgressIndicator(
                                progress = { (uiState.webPageProgress / 100f).coerceIn(0.05f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 5. NON-BLOCKING QUICK LAUNCH BAR (Shown while initial page is still loading or before first render)
            // Ensures the user always has instant 1-tap navigation & URL bar access even if google.com/duckduckgo hasn't rendered yet!
            AnimatedVisibility(visible = (!hasLoadedFirstPage || uiState.isPageLoading) && !uiState.isFullscreenMode) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Akses Cepat (${uiState.activeProfile.name.substringBefore(" ")}):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "Google" to "https://www.google.com",
                                "QuarterFull.io" to "https://quarterfull.io",
                                "DuckDuckGo" to "https://duckduckgo.com"
                            ).forEach { (label, targetUrl) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            addressInput = targetUrl
                                            viewModel.loadUrlInActiveTab(targetUrl)
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. MAIN WEB CONTENT AREA (Fail-Closed Kill-Switch Curtain OR HTML5 Video Fullscreen OR Per-Profile Isolated WebView)
            // Note: The Top Omnibox URL Bar above remains ALWAYS VISIBLE even if the Kill Switch is active!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.vpnState.status == VpnTunnelStatus.FAIL_CLOSED_BLOCKING) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(24.dp)
                            .testTag("fail_closed_killswitch_banner"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Fail-Closed Kill Switch Active",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "Fail-Closed Kill Switch Active",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = uiState.vpnState.statusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { viewModel.navigateTo(GiyaniDestination.VPN_MANAGER) }
                                ) {
                                    Text("Configure VPN Endpoint")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.disconnectVpn() }
                                ) {
                                    Text("Disable Kill Switch")
                                }
                            }
                        }
                    }
                } else if (html5CustomView != null) {
                    // Render HTML5 Fullscreen Video / Canvas CustomView
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { html5CustomView!! }
                    )
                } else {
                    // Key by focused Profile ID so each active profile in the same country swaps its own
                    // live isolated WebView from BrowserEngineManager's pool without losing login state!
                    key(uiState.activeProfile.id, rendererRecoveryTick) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("giyani_android_webview"),
                            factory = { context ->
                                BrowserEngineManager.getOrCreateProfileWebView(
                                    context = context,
                                    profile = uiState.activeProfile
                                ) { newWebView ->
                                    newWebView.apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        isFocusable = true
                                        isFocusableInTouchMode = true
                                        isVerticalScrollBarEnabled = true
                                        isHorizontalScrollBarEnabled = false

                                        BrowserEngineManager.applyProfileToWebView(
                                            webView = this,
                                            profile = uiState.activeProfile,
                                            isPrivateTab = activeTab?.isPrivate == true,
                                            desktopViewportOverride = uiState.isDesktopViewportOverride
                                        )

                                        setDownloadListener { url, _, contentDisposition, mimeType, _ ->
                                            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                                            viewModel.enqueueDownload(
                                                url = url,
                                                suggestedFileName = fileName,
                                                mimeType = mimeType
                                            )
                                            viewModel.navigateTo(GiyaniDestination.DOWNLOADS)
                                        }

                                        val initialUrl = BrowserEngineManager.normalizeRegionalUrl(
                                            activeTab?.url ?: "https://quarterfull.io",
                                            uiState.activeProfile
                                        )
                                        val headers = BrowserEngineManager.buildRequestHeaders(
                                            country = uiState.selectedCountry,
                                            device = uiState.selectedDevice,
                                            profile = uiState.activeProfile,
                                            desktopViewportOverride = uiState.isDesktopViewportOverride
                                        )
                                        lastRequestedWebViewUrl = initialUrl
                                        loadUrl(initialUrl, headers)
                                    }
                                }.apply {
                                    // Always bind latest callbacks to the active profile's WebView
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val target = request?.url?.toString().orEmpty()
                                            if (target.isBlank()) return false
                                            if (target.startsWith("htps://", ignoreCase = true) ||
                                                target.startsWith("htp://", ignoreCase = true)
                                            ) {
                                                viewModel.loadUrlInActiveTab(target)
                                                return true
                                            }
                                            if (!target.startsWith("http://", ignoreCase = true) &&
                                                !target.startsWith("https://", ignoreCase = true) &&
                                                !target.startsWith("about:", ignoreCase = true) &&
                                                !target.startsWith("data:", ignoreCase = true) &&
                                                !target.startsWith("blob:", ignoreCase = true)
                                            ) {
                                                return true
                                            }
                                            // Prevent Google.com from redirecting to Google Indonesia (.co.id)
                                            val normalizedGoogleUrl = BrowserEngineManager.normalizeRegionalUrl(
                                                target,
                                                uiState.activeProfile
                                            )
                                            if (normalizedGoogleUrl != target && request?.isForMainFrame == true) {
                                                val headers = BrowserEngineManager.buildRequestHeaders(
                                                    country = uiState.selectedCountry,
                                                    device = uiState.selectedDevice,
                                                    profile = uiState.activeProfile,
                                                    desktopViewportOverride = uiState.isDesktopViewportOverride
                                                )
                                                view?.loadUrl(normalizedGoogleUrl, headers)
                                                return true
                                            }
                                            return false
                                        }

                                        override fun onRenderProcessGone(
                                            view: WebView?,
                                            detail: android.webkit.RenderProcessGoneDetail?
                                        ): Boolean {
                                            webViewRef = null
                                            val handled = BrowserEngineManager.handleRenderProcessGone(view)
                                            rendererRecoveryTick += 1
                                            return handled
                                        }

                                        override fun shouldInterceptRequest(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): WebResourceResponse? {
                                            val req = request ?: return null
                                            val reqUrl = req.url?.toString().orEmpty()
                                            if (uiState.activeProfile.blockTrackers && BrowserEngineManager.isTrackerUrl(reqUrl)) {
                                                viewModel.incrementTrackersBlocked()
                                                return BrowserEngineManager.createBlockedResponse()
                                            }
                                            val rewritten = BrowserEngineManager.interceptAndRewriteNetworkRequest(
                                                request = req,
                                                country = uiState.selectedCountry,
                                                device = uiState.selectedDevice,
                                                profile = uiState.activeProfile,
                                                isDarkTheme = uiState.isDarkTheme,
                                                desktopViewportOverride = uiState.isDesktopViewportOverride
                                            )
                                            if (rewritten != null) {
                                                return rewritten
                                            }
                                            return super.shouldInterceptRequest(view, request)
                                        }

                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            if (url != null) {
                                                if (!isUrlBarFocused) {
                                                    addressInput = url
                                                }
                                                viewModel.onWebViewPageStarted(url)
                                            }
                                            val script = BrowserEngineManager.buildCoherentIdentityScript(
                                                country = uiState.selectedCountry,
                                                device = uiState.selectedDevice,
                                                profile = uiState.activeProfile,
                                                isDarkTheme = uiState.isDarkTheme,
                                                desktopViewportOverride = uiState.isDesktopViewportOverride
                                            )
                                            view?.evaluateJavascript(script, null)
                                        }

                                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                                            super.onPageCommitVisible(view, url)
                                            val script = BrowserEngineManager.buildCoherentIdentityScript(
                                                country = uiState.selectedCountry,
                                                device = uiState.selectedDevice,
                                                profile = uiState.activeProfile,
                                                isDarkTheme = uiState.isDarkTheme,
                                                desktopViewportOverride = uiState.isDesktopViewportOverride
                                            )
                                            view?.evaluateJavascript(script, null)
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            hasLoadedFirstPage = true
                                            val script = BrowserEngineManager.buildCoherentIdentityScript(
                                                country = uiState.selectedCountry,
                                                device = uiState.selectedDevice,
                                                profile = uiState.activeProfile,
                                                isDarkTheme = uiState.isDarkTheme,
                                                desktopViewportOverride = uiState.isDesktopViewportOverride
                                            )
                                            view?.evaluateJavascript(script, null)

                                            if (url != null) {
                                                if (!isUrlBarFocused) {
                                                    addressInput = url
                                                }
                                                viewModel.onWebViewPageFinished(
                                                    url = url,
                                                    title = view?.title,
                                                    canBack = view?.canGoBack() == true,
                                                    canFwd = view?.canGoForward() == true
                                                )
                                                if (uiState.activeProfile.keepLoginSession && view?.context != null) {
                                                    BrowserEngineManager.saveProfileCookieSnapshot(
                                                        context = view.context,
                                                        profile = uiState.activeProfile,
                                                        candidateUrls = uiState.tabs.map { it.url } + url
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            super.onProgressChanged(view, newProgress)
                                            viewModel.onWebViewProgressChanged(newProgress)
                                            if (newProgress in 10..40) {
                                                val script = BrowserEngineManager.buildCoherentIdentityScript(
                                                    country = uiState.selectedCountry,
                                                    device = uiState.selectedDevice,
                                                    profile = uiState.activeProfile,
                                                    isDarkTheme = uiState.isDarkTheme,
                                                    desktopViewportOverride = uiState.isDesktopViewportOverride
                                                )
                                                view?.evaluateJavascript(script, null)
                                            }
                                            if (newProgress >= 80) {
                                                hasLoadedFirstPage = true
                                            }
                                        }

                                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                            if (html5CustomView != null) {
                                                callback?.onCustomViewHidden()
                                                return
                                            }
                                            html5CustomView = view
                                            html5CustomViewCallback = callback
                                            viewModel.setFullscreenMode(true)
                                        }

                                        override fun onHideCustomView() {
                                            html5CustomView = null
                                            html5CustomViewCallback?.onCustomViewHidden()
                                            html5CustomViewCallback = null
                                        }

                                        override fun onGeolocationPermissionsShowPrompt(
                                            origin: String?,
                                            callback: GeolocationPermissions.Callback?
                                        ) {
                                            when (uiState.activeProfile.geolocationPolicy) {
                                                "BLOCKED" -> callback?.invoke(origin, false, false)
                                                else -> callback?.invoke(origin, false, false)
                                            }
                                        }
                                    }

                                    webViewRef = this
                                }
                            },
                            update = { wv ->
                                webViewRef = wv
                            }
                        )
                    }
                }
            }
        }

        // =====================================================================
        // FLOATING STEALTH CONTROL PILL (Shown when Fullscreen Mode is active)
        // Lets the user open the URL Bar or exit Fullscreen anytime with 1 tap!
        // =====================================================================
        AnimatedVisibility(
            visible = uiState.isFullscreenMode || html5CustomView != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .testTag("fullscreen_floating_control_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Active Profile & Country indicator inside Floating Pill
                    Text(
                        text = "${uiState.selectedCountry.flagEmoji} ${uiState.activeProfile.name.substringBefore(" ")}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Tap to exit Fullscreen and immediately focus the Expanding URL Bar
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        modifier = Modifier
                            .clickable {
                                html5CustomViewCallback?.onCustomViewHidden()
                                html5CustomView = null
                                viewModel.setFullscreenMode(false)
                                isUrlBarFocused = true
                            }
                            .testTag("floating_pill_open_url_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Ketik URL",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ketik URL",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Exit Fullscreen Button
                    IconButton(
                        onClick = {
                            html5CustomViewCallback?.onCustomViewHidden()
                            html5CustomView = null
                            viewModel.setFullscreenMode(false)
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("floating_pill_exit_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Keluar Fullscreen",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
