package com.example

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.browser.BrowserEngineManager
import com.example.domain.CountryIdentityCatalog
import com.example.download.DownloadEngine
import com.example.ui.screens.BrowserWebViewScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.DownloadManagerScreen
import com.example.ui.screens.HomeHubScreen
import com.example.ui.screens.LeftProfileAndTabsPanel
import com.example.ui.screens.ProfilesAndPrivacyScreen
import com.example.ui.screens.RightSettingsAndIdentityPanel
import com.example.ui.screens.TabManagerScreen
import com.example.ui.screens.UserGuideScreen
import com.example.ui.screens.VpnManagerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GiyaniDestination
import com.example.viewmodel.GiyaniViewModel
import com.example.viewmodel.defaultProfile

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Lock JVM process Locale (en-US) & TimeZone (America/New_York) BEFORE WebView/Chromium initializes
        BrowserEngineManager.enforceJvmLocaleAndTimezone(applicationContext, defaultProfile())
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val giyaniViewModel: GiyaniViewModel = viewModel()
            val uiState by giyaniViewModel.uiState.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = uiState.isDarkTheme) {
                GiyaniBrowserApp(giyaniViewModel = giyaniViewModel)
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            // Reset all login sessions across all profiles whenever the application is closed
            BrowserEngineManager.purgeAllProfilesSessionsOnAppExit(applicationContext)
        }
        super.onDestroy()
    }
}

private data class NavDestinationItem(
    val destination: GiyaniDestination,
    val label: String,
    val icon: ImageVector,
    val testTag: String,
    val badgeCount: Int = 0
)

@Composable
fun GiyaniBrowserApp(
    giyaniViewModel: GiyaniViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by giyaniViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var autoVpnBootTriggered by rememberSaveable { mutableStateOf(false) }

    // Android 13+ notification permission launcher so the VPN foreground notification icon is visible
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Android VpnService.prepare() consent launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val activeEndpoint = uiState.vpnState.activeEndpoint
                ?: uiState.vpnEndpoints.find { it.id == "wg_freza_us_1" }
                ?: uiState.vpnEndpoints.find { it.countryCode.equals("US", ignoreCase = true) }
                ?: uiState.vpnEndpoints.firstOrNull()
            if (activeEndpoint != null) {
                giyaniViewModel.startVpnServiceWithEndpoint(activeEndpoint)
            }
        } else {
            giyaniViewModel.showMessage("Izin VPN Android ditolak oleh pengguna.")
        }
    }

    // Automatically turn on VPN USA (Freza Tampan • US-FREE#1) and display the VPN icon in the phone's
    // status/notification bar on first app launch.
    LaunchedEffect(uiState.vpnEndpoints) {
        if (!autoVpnBootTriggered && uiState.vpnEndpoints.isNotEmpty()) {
            autoVpnBootTriggered = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotifPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasNotifPerm) {
                    try {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } catch (_: Exception) {
                    }
                }
            }
            val usEndpoint = uiState.vpnEndpoints.find { it.id == "wg_freza_us_1" }
                ?: uiState.vpnEndpoints.find { it.countryCode.equals("US", ignoreCase = true) }
                ?: uiState.vpnEndpoints.first()
            val prepareIntent = try {
                VpnService.prepare(context)
            } catch (_: Exception) {
                null
            }
            if (prepareIntent != null) {
                // First time launch: automatically request Android VpnService permission so the system VPN key icon turns on
                giyaniViewModel.prepareAndConnectVpn(
                    endpoint = usEndpoint,
                    onPermissionRequired = { intent -> vpnPermissionLauncher.launch(intent) }
                )
            } else {
                // Permission already granted: start the foreground VpnService immediately
                giyaniViewModel.startVpnServiceWithEndpoint(usEndpoint)
            }
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            giyaniViewModel.clearSnackbar()
        }
    }

    val activeDownloadCount = remember(uiState.downloads) {
        uiState.downloads.count {
            it.status == DownloadEngine.STATUS_DOWNLOADING ||
                it.status == DownloadEngine.STATUS_PAUSED ||
                it.status == DownloadEngine.STATUS_QUEUED
        }
    }

    val navItems = remember(uiState.tabs.size, activeDownloadCount, uiState.activeProfiles.size) {
        listOf(
            NavDestinationItem(
                destination = GiyaniDestination.BROWSER_WEBVIEW,
                label = "Chrome",
                icon = Icons.Default.Public,
                testTag = "nav_browser"
            ),
            NavDestinationItem(
                destination = GiyaniDestination.PROFILES_PRIVACY,
                label = "Profiles",
                icon = Icons.Default.ManageAccounts,
                testTag = "nav_profiles",
                badgeCount = uiState.activeProfiles.size
            ),
            NavDestinationItem(
                destination = GiyaniDestination.TAB_GROUPS,
                label = "Tabs",
                icon = Icons.Default.Folder,
                testTag = "nav_tab_groups",
                badgeCount = uiState.tabs.size
            ),
            NavDestinationItem(
                destination = GiyaniDestination.DOWNLOADS,
                label = "Unduhan",
                icon = Icons.Default.CloudDownload,
                testTag = "nav_downloads",
                badgeCount = activeDownloadCount
            ),
            NavDestinationItem(
                destination = GiyaniDestination.VPN_MANAGER,
                label = "VPN",
                icon = Icons.Default.VpnKey,
                testTag = "nav_vpn"
            ),
            NavDestinationItem(
                destination = GiyaniDestination.USER_GUIDE,
                label = "Panduan",
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                testTag = "nav_user_guide"
            )
        )
    }

    // Hide Bottom Navigation Bar & Navigation Rail when Fullscreen mode is active in the browser
    val isBrowserFullscreen = uiState.currentDestination == GiyaniDestination.BROWSER_WEBVIEW && uiState.isFullscreenMode

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isExpandedScreen && !isBrowserFullscreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("giyani_bottom_navigation")
                    ) {
                        navItems.forEach { item ->
                            val selected = uiState.currentDestination == item.destination
                            NavigationBarItem(
                                selected = selected,
                                onClick = { giyaniViewModel.navigateTo(item.destination) },
                                icon = {
                                    if (item.badgeCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text(text = item.badgeCount.toString())
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main Content Layer
                Row(modifier = Modifier.fillMaxSize()) {
                    if (isExpandedScreen && !isBrowserFullscreen) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.testTag("giyani_navigation_rail")
                        ) {
                            navItems.forEach { item ->
                                val selected = uiState.currentDestination == item.destination
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { giyaniViewModel.navigateTo(item.destination) },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }

                    when (uiState.currentDestination) {
                        GiyaniDestination.BROWSER_WEBVIEW -> BrowserWebViewScreen(
                            uiState = uiState,
                            viewModel = giyaniViewModel,
                            modifier = Modifier.weight(1f)
                        )

                        GiyaniDestination.HOME_HUB -> HomeHubScreen(
                            uiState = uiState,
                            viewModel = giyaniViewModel,
                            onRequestVpnPermission = { intent -> vpnPermissionLauncher.launch(intent) },
                            modifier = Modifier.weight(1f)
                        )

                        GiyaniDestination.TAB_GROUPS -> TabManagerScreen(
                            uiState = uiState,
                            viewModel = giyaniViewModel,
                            modifier = Modifier.weight(1f)
                        )

                        GiyaniDestination.DOWNLOADS -> DownloadManagerScreen(
                            uiState = uiState,
                            viewModel = giyaniViewModel,
                            modifier = Modifier.weight(1f)
                        )

                        GiyaniDestination.VPN_MANAGER -> VpnManagerScreen(
                            uiState = uiState,
                            viewModel = giyaniViewModel,
                            onRequestVpnPermission = { intent -> vpnPermissionLauncher.launch(intent) },
                            modifier = Modifier.weight(1f)
                        )

                        GiyaniDestination.DIAGNOSTICS -> DiagnosticsScreen(
                            uiState = uiState,
                            viewModel = giyaniViewModel,
                            modifier = Modifier.weight(1f)
                        )

                        GiyaniDestination.PROFILES_PRIVACY -> ProfilesAndPrivacyScreen(
                            uiState = uiState,
                            viewModel = giyaniViewModel,
                            modifier = Modifier.weight(1f)
                        )

                        GiyaniDestination.USER_GUIDE -> UserGuideScreen(
                            uiState = uiState,
                            viewModel = giyaniViewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Scrim Overlay when Left-Panel or Right-Panel is open
                if (uiState.isLeftPanelOpen || uiState.isRightPanelOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                giyaniViewModel.closeBothPanels()
                            }
                            .testTag("side_panel_scrim_overlay")
                    )
                }

                // LEFT PANEL (Show / Hide by Toggle): Profiles (Create Name+Email, Edit, Delete, Use, Multi-Open 1 Country/VPN) & Tabs
                LeftProfileAndTabsPanel(
                    visible = uiState.isLeftPanelOpen,
                    uiState = uiState,
                    viewModel = giyaniViewModel,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                // RIGHT PANEL (Show / Hide by Toggle): Country Identity, City/Timezone, Device Profile, WireGuard VPN & Privacy
                RightSettingsAndIdentityPanel(
                    visible = uiState.isRightPanelOpen,
                    uiState = uiState,
                    viewModel = giyaniViewModel,
                    onRequestVpnPermission = { intent -> vpnPermissionLauncher.launch(intent) },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )

                // GLOBAL CROSS-COUNTRY DEACTIVATION WARNING MODAL
                // Triggered whenever a user activates a profile from another country (e.g. Japan/Indonesia)
                // while multiple profiles in the current country (e.g. 3 US profiles) are active.
                uiState.pendingCrossCountrySwitch?.let { warning ->
                    var keepOldSessionsInVault by remember(warning.targetProfile.id) { mutableStateOf(true) }
                    val currentPreset = CountryIdentityCatalog.getCountryByCode(warning.currentCountryCode)
                    val targetPreset = CountryIdentityCatalog.getCountryByCode(warning.targetCountryCode)

                    AlertDialog(
                        onDismissRequest = { giyaniViewModel.dismissCrossCountrySwitchWarning() },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Peringatan Pindah Negara",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(36.dp)
                            )
                        },
                        title = {
                            Text(
                                text = "Nonaktifkan Semua Profile ${warning.currentCountryName}?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Anda memilih untuk mengaktifkan profile \"${warning.targetProfile.name}\" dari negara ${targetPreset.flagEmoji} ${warning.targetCountryName} (${warning.targetCountryCode}).",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Peringatan Aturan 1-Negara / 1-VPN:",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "Seluruh ${warning.affectedActiveProfiles.size} profile aktif dari negara ${currentPreset.flagEmoji} ${warning.currentCountryName} (${warning.currentCountryCode}) berikut akan dinonaktifkan:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        warning.affectedActiveProfiles.forEach { prof ->
                                            val dev = CountryIdentityCatalog.getDeviceById(prof.devicePresetId)
                                            Text(
                                                text = "• ${prof.name} (${dev.name.substringBefore(" ")})",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Simpan Sesi Login ${warning.currentCountryCode} di Brankas",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Jika ON, saat Anda kembali ke profile ${warning.currentCountryCode} nanti, login Quarterfull/Google tetap tersimpan.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = keepOldSessionsInVault,
                                            onCheckedChange = { keepOldSessionsInVault = it },
                                            modifier = Modifier.testTag("cross_country_keep_sessions_switch")
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    giyaniViewModel.confirmCrossCountryProfileSwitch(
                                        keepOldCountryLoginSessionsInVault = keepOldSessionsInVault
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier.testTag("confirm_cross_country_switch_button")
                            ) {
                                Text("Nonaktifkan ${warning.currentCountryCode} & Pindah ke ${warning.targetCountryCode}")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { giyaniViewModel.dismissCrossCountrySwitchWarning() },
                                modifier = Modifier.testTag("cancel_cross_country_switch_button")
                            ) {
                                Text("Batal (Tetap di ${warning.currentCountryCode})")
                            }
                        }
                    )
                }
            }
        }
    }
}
