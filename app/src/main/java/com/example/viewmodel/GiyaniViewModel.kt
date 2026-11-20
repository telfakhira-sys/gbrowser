package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.VpnService
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.browser.BrowserEngineManager
import com.example.data.BookmarkEntity
import com.example.data.BrowserProfileEntity
import com.example.data.BrowserTabEntity
import com.example.data.DownloadEntity
import com.example.data.GiyaniDatabase
import com.example.data.HistoryEntity
import com.example.data.TabGroupEntity
import com.example.data.VpnEndpointEntity
import com.example.diagnostics.DiagnosticMetricItem
import com.example.diagnostics.IdentityDiagnosticsEngine
import com.example.domain.CityPreset
import com.example.domain.CountryIdentityCatalog
import com.example.domain.CountryPreset
import com.example.domain.DevicePreset
import com.example.download.DownloadEngine
import com.example.security.KeystoreCryptoManager
import com.example.vpn.GiyaniVpnService
import com.example.vpn.VpnConnectionState
import com.example.vpn.VpnEngineController
import com.example.vpn.VpnTunnelStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

private val Context.giyaniDataStore by preferencesDataStore(name = "giyani_browser_settings")

enum class GiyaniDestination {
    BROWSER_WEBVIEW,
    HOME_HUB,
    TAB_GROUPS,
    DOWNLOADS,
    VPN_MANAGER,
    DIAGNOSTICS,
    PROFILES_PRIVACY,
    USER_GUIDE
}

data class CrossCountrySwitchWarning(
    val targetProfile: BrowserProfileEntity,
    val currentCountryCode: String,
    val currentCountryName: String,
    val targetCountryCode: String,
    val targetCountryName: String,
    val affectedActiveProfiles: List<BrowserProfileEntity>
)

data class GiyaniUiState(
    val currentDestination: GiyaniDestination = GiyaniDestination.BROWSER_WEBVIEW,
    val isLeftPanelOpen: Boolean = false,
    val isRightPanelOpen: Boolean = false,
    val isAdvancedMode: Boolean = false,
    val isDarkTheme: Boolean = true,
    val isFullscreenMode: Boolean = false,
    val isDesktopViewportOverride: Boolean = false,
    val pendingCrossCountrySwitch: CrossCountrySwitchWarning? = null,
    val profiles: List<BrowserProfileEntity> = emptyList(),
    val activeProfiles: List<BrowserProfileEntity> = emptyList(),
    val activeProfile: BrowserProfileEntity = defaultProfile(),
    val selectedCountry: CountryPreset = CountryIdentityCatalog.getCountryByCode("US"),
    val selectedCity: CityPreset = CountryIdentityCatalog.getCountryByCode("US").cities.first(),
    val selectedDevice: DevicePreset = CountryIdentityCatalog.getDeviceById("google_pixel"),
    val tabGroups: List<TabGroupEntity> = emptyList(),
    val tabs: List<BrowserTabEntity> = emptyList(),
    val activeTab: BrowserTabEntity? = null,
    val downloads: List<DownloadEntity> = emptyList(),
    val vpnEndpoints: List<VpnEndpointEntity> = emptyList(),
    val vpnState: VpnConnectionState = VpnConnectionState(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val history: List<HistoryEntity> = emptyList(),
    val diagnostics: List<DiagnosticMetricItem> = emptyList(),
    val trackersBlockedCount: Int = 0,
    val webPageProgress: Int = 0,
    val isPageLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val snackbarMessage: String? = null
)

fun defaultProfile(): BrowserProfileEntity {
    return BrowserProfileEntity(
        id = "profile_default_us",
        name = "Alex Mercer",
        email = "alex.mercer@giyani.us",
        countryCode = "US",
        cityName = "New York",
        timezoneId = "America/New_York",
        languageTag = "en-US",
        devicePresetId = "google_pixel",
        preferredVpnServerId = null,
        blockThirdPartyCookies = true,
        blockTrackers = true,
        canvasProtection = true,
        webRtcProtection = true,
        geolocationPolicy = "ASK",
        simulatedLat = 40.7128,
        simulatedLon = -74.0060,
        isActive = true,
        isFocused = true
    )
}

class GiyaniViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val dao = GiyaniDatabase.getInstance(appContext).giyaniDao()

    private val KEY_ADVANCED_MODE = booleanPreferencesKey("advanced_mode")
    private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")

    // Default to Chrome APK Browser view with toggleable Left and Right Panels
    private val _currentDestination = MutableStateFlow(GiyaniDestination.BROWSER_WEBVIEW)
    private val _isLeftPanelOpen = MutableStateFlow(false)
    private val _isRightPanelOpen = MutableStateFlow(false)
    private val _isAdvancedMode = MutableStateFlow(false)
    private val _isDarkTheme = MutableStateFlow(true)
    private val _isFullscreenMode = MutableStateFlow(false)
    private val _isDesktopViewportOverride = MutableStateFlow(false)
    private val _pendingCrossCountrySwitch = MutableStateFlow<CrossCountrySwitchWarning?>(null)
    private val _trackersBlockedCount = MutableStateFlow(0)
    private val _webPageProgress = MutableStateFlow(0)
    private val _isPageLoading = MutableStateFlow(false)
    private val _canGoBack = MutableStateFlow(false)
    private val _canGoForward = MutableStateFlow(false)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _diagnosticsRefreshTrigger = MutableStateFlow(0L)
    private var lastDiagnosticsCacheKey: String = ""
    private var lastDiagnosticsResult: List<DiagnosticMetricItem> = emptyList()

    // WebView imperative command callbacks
    var onWebViewNavigate: ((String) -> Unit)? = null
    var onWebViewBack: (() -> Unit)? = null
    var onWebViewForward: (() -> Unit)? = null
    var onWebViewReload: (() -> Unit)? = null

    val uiState: StateFlow<GiyaniUiState> = combine(
        _currentDestination,
        _isLeftPanelOpen,
        _isRightPanelOpen,
        _isAdvancedMode,
        dao.observeProfiles(),
        dao.observeAllTabGroups(),
        dao.observeAllTabs(),
        dao.observeDownloads(),
        dao.observeVpnEndpoints(),
        VpnEngineController.state,
        dao.observeAllBookmarks(),
        dao.observeAllHistory(),
        _trackersBlockedCount,
        _webPageProgress,
        _isPageLoading,
        _canGoBack,
        _canGoForward,
        _snackbarMessage,
        _diagnosticsRefreshTrigger,
        _isDarkTheme,
        _isFullscreenMode,
        _isDesktopViewportOverride,
        _pendingCrossCountrySwitch
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val destination = args[0] as GiyaniDestination
        val leftOpen = args[1] as Boolean
        val rightOpen = args[2] as Boolean
        val isAdvanced = args[3] as Boolean
        val profiles = args[4] as List<BrowserProfileEntity>
        val allGroups = args[5] as List<TabGroupEntity>
        val allTabs = args[6] as List<BrowserTabEntity>
        val downloads = args[7] as List<DownloadEntity>
        val vpnEndpoints = args[8] as List<VpnEndpointEntity>
        val vpnState = args[9] as VpnConnectionState
        val allBookmarks = args[10] as List<BookmarkEntity>
        val allHistory = args[11] as List<HistoryEntity>
        val trackersBlocked = args[12] as Int
        val progress = args[13] as Int
        val isLoading = args[14] as Boolean
        val canBack = args[15] as Boolean
        val canFwd = args[16] as Boolean
        val snackbar = args[17] as String?
        val diagTrigger = args[18] as Long
        val isDark = args[19] as Boolean
        val isFullscreen = args[20] as Boolean
        val isDesktopOverride = args[21] as Boolean
        val pendingWarning = args[22] as CrossCountrySwitchWarning?

        val activeProfiles = profiles.filter { it.isActive }
        val focusedProfile = profiles.find { it.isFocused && it.isActive }
            ?: activeProfiles.firstOrNull()
            ?: profiles.firstOrNull()
            ?: defaultProfile()

        val country = CountryIdentityCatalog.getCountryByCode(focusedProfile.countryCode)
        val city = country.cities.find { it.name.equals(focusedProfile.cityName, ignoreCase = true) }
            ?: country.cities.first()
        val device = CountryIdentityCatalog.getDeviceById(focusedProfile.devicePresetId)

        val profileGroups = allGroups.filter { it.profileId == focusedProfile.id }
        val profileTabs = allTabs.filter { it.profileId == focusedProfile.id }
        val activeTab = profileTabs.find { it.isActive } ?: profileTabs.firstOrNull()

        val profileBookmarks = allBookmarks.filter { it.profileId == focusedProfile.id }
        val profileHistory = allHistory.filter { it.profileId == focusedProfile.id }

        val diagKey = "${focusedProfile.id}_${focusedProfile.countryCode}_${focusedProfile.cityName}_${focusedProfile.devicePresetId}_${vpnState.status}_${vpnState.activeEndpoint?.id}_${vpnState.publicIpTelemetry.lastCheckedMs}_$diagTrigger"
        val diagnostics = if (diagKey == lastDiagnosticsCacheKey && lastDiagnosticsResult.isNotEmpty()) {
            lastDiagnosticsResult
        } else {
            val evaluated = IdentityDiagnosticsEngine.evaluateDiagnostics(
                context = appContext,
                profile = focusedProfile,
                vpnState = vpnState
            )
            lastDiagnosticsCacheKey = diagKey
            lastDiagnosticsResult = evaluated
            evaluated
        }

        GiyaniUiState(
            currentDestination = destination,
            isLeftPanelOpen = leftOpen,
            isRightPanelOpen = rightOpen,
            isAdvancedMode = isAdvanced,
            isDarkTheme = isDark,
            isFullscreenMode = isFullscreen,
            isDesktopViewportOverride = isDesktopOverride,
            pendingCrossCountrySwitch = pendingWarning,
            profiles = profiles,
            activeProfiles = if (activeProfiles.isEmpty()) listOf(focusedProfile) else activeProfiles,
            activeProfile = focusedProfile,
            selectedCountry = country,
            selectedCity = city,
            selectedDevice = device,
            tabGroups = profileGroups,
            tabs = profileTabs,
            activeTab = activeTab,
            downloads = downloads,
            vpnEndpoints = vpnEndpoints,
            vpnState = vpnState,
            bookmarks = profileBookmarks,
            history = profileHistory,
            diagnostics = diagnostics,
            trackersBlockedCount = trackersBlocked,
            webPageProgress = progress,
            isPageLoading = isLoading,
            canGoBack = canBack,
            canGoForward = canFwd,
            snackbarMessage = snackbar
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GiyaniUiState()
    )

    init {
        BrowserEngineManager.sanitizeColdStartVaultsSync(appContext, "profile_default_us")
        VpnEngineController.onAutoSyncCountryCallback = { targetCountryIso, targetCityName ->
            autoSyncProfileToDetectedCountry(targetCountryIso, targetCityName)
        }
        seedInitialDataIfNeeded()
        observeDataStoreSettings()
    }

    private fun autoSyncProfileToDetectedCountry(targetCountryIso: String, targetCityName: String?) {
        val matchPreset = CountryIdentityCatalog.countries.find {
            it.isoCode.equals(targetCountryIso, ignoreCase = true)
        } ?: return

        viewModelScope.launch {
            val current = uiState.value.activeProfile
            if (current.countryCode.equals(matchPreset.isoCode, ignoreCase = true)) return@launch

            val matchedCity = matchPreset.cities.find {
                targetCityName != null && it.name.equals(targetCityName, ignoreCase = true)
            } ?: matchPreset.cities.first()

            val updated = current.copy(
                countryCode = matchPreset.isoCode,
                cityName = matchedCity.name,
                timezoneId = matchedCity.timezoneId,
                languageTag = matchPreset.defaultLanguage,
                simulatedLat = matchedCity.defaultLat,
                simulatedLon = matchedCity.defaultLon
            )
            dao.updateProfile(updated)
            dao.deactivateProfilesOutsideCountry(matchPreset.isoCode)
            dao.setProfileActiveState(updated.id, true)
            dao.setFocusedProfile(updated.id)
        }
    }

    private fun observeDataStoreSettings() {
        viewModelScope.launch {
            appContext.giyaniDataStore.data.collect { prefs ->
                _isAdvancedMode.value = prefs[KEY_ADVANCED_MODE] ?: false
                _isDarkTheme.value = prefs[KEY_DARK_THEME] ?: true
            }
        }
    }

    private fun seedInitialDataIfNeeded() {
        viewModelScope.launch {
            // 1. Always ensure the 10 embedded Freza WireGuard configurations are synced into DB
            // and any legacy dummy endpoints are replaced.
            syncBuiltInWireGuardEndpointsInternal(forceOverwriteAll = false)

            val existingProfiles = dao.getAllProfilesSnapshot()
            if (existingProfiles.isEmpty()) {
                // Seed 3 US profiles active simultaneously (each with a distinct Device Identity, US Server & KeepLoginSession = true)
                // so the user can immediately test logging into 3 different Quarterfull/Google accounts in parallel!
                val usProfile1 = defaultProfile().copy(
                    name = "Alex Mercer (US #1 • Pixel 9 Pro XL)",
                    email = "alex.mercer@giyani.us",
                    devicePresetId = "google_pixel",
                    preferredVpnServerId = "wg_freza_us_1",
                    keepLoginSession = true,
                    isActive = true,
                    isFocused = true
                )
                val usProfile2 = BrowserProfileEntity(
                    id = "profile_us_secondary",
                    name = "Sarah Jenkins (US #2 • Galaxy S24 Ultra)",
                    email = "sarah.jenkins@us-cloud.dev",
                    countryCode = "US",
                    cityName = "Los Angeles",
                    timezoneId = "America/Los_Angeles",
                    languageTag = "en-US",
                    devicePresetId = "samsung_galaxy",
                    preferredVpnServerId = "wg_freza_us_2",
                    keepLoginSession = true,
                    isActive = true, // Simultaneously active with usProfile1 & usProfile3 in US!
                    isFocused = false
                )
                val usProfile3 = BrowserProfileEntity(
                    id = "profile_us_tertiary",
                    name = "Michael Vance (US #3 • Win 11 Desktop)",
                    email = "m.vance@ny-publishing.us",
                    countryCode = "US",
                    cityName = "Chicago",
                    timezoneId = "America/Chicago",
                    languageTag = "en-US",
                    devicePresetId = "windows_desktop",
                    preferredVpnServerId = "wg_freza_us_3",
                    keepLoginSession = true,
                    isActive = true, // Simultaneously active 3rd US profile!
                    isFocused = false
                )
                val nlProfile = BrowserProfileEntity(
                    id = "profile_amsterdam_nl",
                    name = "Lars van Dijk (NL • Amsterdam • OPPO Find X7)",
                    email = "lars.vandijk@amsterdam-net.nl",
                    countryCode = "NL",
                    cityName = "Amsterdam",
                    timezoneId = "Europe/Amsterdam",
                    languageTag = "nl-NL",
                    devicePresetId = "oppo_find_x7",
                    preferredVpnServerId = "wg_freza_nl_1",
                    keepLoginSession = true,
                    isActive = false,
                    isFocused = false
                )
                val jpProfile = BrowserProfileEntity(
                    id = "profile_tokyo_jp",
                    name = "Kenji Takahashi (JP • Tokyo • Sony Xperia 1 VI)",
                    email = "kenji.t@tokyo-research.jp",
                    countryCode = "JP",
                    cityName = "Tokyo",
                    timezoneId = "Asia/Tokyo",
                    languageTag = "ja-JP",
                    devicePresetId = "sony_xperia_1vi",
                    preferredVpnServerId = "wg_freza_jp_1",
                    keepLoginSession = true,
                    isActive = false,
                    isFocused = false
                )
                val caProfile = BrowserProfileEntity(
                    id = "profile_toronto_ca",
                    name = "Chloe Tremblay (CA • Toronto • iPhone 16 Pro Max)",
                    email = "chloe.t@maple-tech.ca",
                    countryCode = "CA",
                    cityName = "Toronto",
                    timezoneId = "America/Toronto",
                    languageTag = "en-CA",
                    devicePresetId = "iphone_compat",
                    preferredVpnServerId = "wg_freza_ca_1",
                    keepLoginSession = true,
                    isActive = false,
                    isFocused = false
                )
                val chProfile = BrowserProfileEntity(
                    id = "profile_zurich_ch",
                    name = "Lukas Keller (CH • Zurich • ROG Phone 8 Pro)",
                    email = "lukas.keller@alpine-privacy.ch",
                    countryCode = "CH",
                    cityName = "Zurich",
                    timezoneId = "Europe/Zurich",
                    languageTag = "de-CH",
                    devicePresetId = "asus_rog_phone8",
                    preferredVpnServerId = "wg_freza_ch_1",
                    keepLoginSession = true,
                    isActive = false,
                    isFocused = false
                )
                val idProfile = BrowserProfileEntity(
                    id = "profile_jakarta_id",
                    name = "Budi Santoso (ID • Jakarta • Xiaomi 14 Ultra)",
                    email = "budi.santoso@nusantara.id",
                    countryCode = "ID",
                    cityName = "Jakarta",
                    timezoneId = "Asia/Jakarta",
                    languageTag = "id-ID",
                    devicePresetId = "xiaomi_14",
                    preferredVpnServerId = null,
                    keepLoginSession = true,
                    isActive = false,
                    isFocused = false
                )
                dao.insertProfile(usProfile1)
                dao.insertProfile(usProfile2)
                dao.insertProfile(usProfile3)
                dao.insertProfile(nlProfile)
                dao.insertProfile(jpProfile)
                dao.insertProfile(caProfile)
                dao.insertProfile(chProfile)
                dao.insertProfile(idProfile)

                // Seed initial organized Tab Groups & Tabs for US Profile 1
                val groupPrivacy = TabGroupEntity(
                    id = "group_privacy_audit",
                    profileId = usProfile1.id,
                    name = "US #1 Main Workspace",
                    colorHex = 0xFF10B981,
                    isExpanded = true,
                    sortOrder = 0
                )
                val groupRegional = TabGroupEntity(
                    id = "group_regional_news",
                    profileId = usProfile1.id,
                    name = "Web Novels & Portals",
                    colorHex = 0xFF06B6D4,
                    isExpanded = true,
                    sortOrder = 1
                )
                dao.insertTabGroup(groupPrivacy)
                dao.insertTabGroup(groupRegional)

                dao.insertTab(
                    BrowserTabEntity(
                        id = "tab_initial_1",
                        profileId = usProfile1.id,
                        groupId = groupRegional.id,
                        title = "QuarterFull — Read & Create",
                        url = "https://quarterfull.io",
                        isPrivate = false,
                        isActive = true,
                        lastAccessed = System.currentTimeMillis()
                    )
                )
                dao.insertTab(
                    BrowserTabEntity(
                        id = "tab_initial_2",
                        profileId = usProfile1.id,
                        groupId = groupPrivacy.id,
                        title = "Google",
                        url = "https://www.google.com",
                        isPrivate = false,
                        isActive = false,
                        lastAccessed = System.currentTimeMillis() - 10_000
                    )
                )

                // Seed initial tabs for US Profile 2 (Sarah Jenkins)
                val groupUs2 = TabGroupEntity(
                    id = "group_us2_workspace",
                    profileId = usProfile2.id,
                    name = "US #2 Workspace (Galaxy S24)",
                    colorHex = 0xFF8B5CF6,
                    isExpanded = true,
                    sortOrder = 0
                )
                dao.insertTabGroup(groupUs2)
                dao.insertTab(
                    BrowserTabEntity(
                        id = "tab_us2_1",
                        profileId = usProfile2.id,
                        groupId = groupUs2.id,
                        title = "QuarterFull (Account #2)",
                        url = "https://quarterfull.io",
                        isActive = true
                    )
                )
                dao.insertTab(
                    BrowserTabEntity(
                        id = "tab_us2_2",
                        profileId = usProfile2.id,
                        groupId = groupUs2.id,
                        title = "Google (US #2)",
                        url = "https://www.google.com",
                        isActive = false
                    )
                )

                // Seed initial tabs for US Profile 3 (Michael Vance)
                val groupUs3 = TabGroupEntity(
                    id = "group_us3_workspace",
                    profileId = usProfile3.id,
                    name = "US #3 Workspace (Win 11)",
                    colorHex = 0xFF3B82F6,
                    isExpanded = true,
                    sortOrder = 0
                )
                dao.insertTabGroup(groupUs3)
                dao.insertTab(
                    BrowserTabEntity(
                        id = "tab_us3_1",
                        profileId = usProfile3.id,
                        groupId = groupUs3.id,
                        title = "QuarterFull (Account #3)",
                        url = "https://quarterfull.io",
                        isActive = true
                    )
                )
                dao.insertTab(
                    BrowserTabEntity(
                        id = "tab_us3_2",
                        profileId = usProfile3.id,
                        groupId = groupUs3.id,
                        title = "Google (US #3)",
                        url = "https://www.google.com",
                        isActive = false
                    )
                )

                // Seed initial tabs for NL, JP, CA, CH, ID Profiles
                listOf(nlProfile, jpProfile, caProfile, chProfile, idProfile).forEach { prof ->
                    val grp = TabGroupEntity(
                        id = "group_${prof.id}_workspace",
                        profileId = prof.id,
                        name = "${prof.countryCode} Workspace (${prof.cityName})",
                        colorHex = 0xFFF59E0B,
                        isExpanded = true,
                        sortOrder = 0
                    )
                    dao.insertTabGroup(grp)
                    dao.insertTab(
                        BrowserTabEntity(
                            id = "tab_${prof.id}_1",
                            profileId = prof.id,
                            groupId = grp.id,
                            title = "QuarterFull (${prof.countryCode})",
                            url = "https://quarterfull.io",
                            isActive = true
                        )
                    )
                    dao.insertTab(
                        BrowserTabEntity(
                            id = "tab_${prof.id}_2",
                            profileId = prof.id,
                            groupId = grp.id,
                            title = "Google (${prof.countryCode})",
                            url = "https://www.google.com",
                            isActive = false
                        )
                    )
                }

                // Seed default Bookmarks
                dao.insertBookmark(
                    BookmarkEntity(
                        id = "bm_1",
                        profileId = usProfile1.id,
                        title = "QuarterFull — Read & Create",
                        url = "https://quarterfull.io"
                    )
                )
                dao.insertBookmark(
                    BookmarkEntity(
                        id = "bm_2",
                        profileId = usProfile1.id,
                        title = "Google Search",
                        url = "https://www.google.com"
                    )
                )
                dao.insertBookmark(
                    BookmarkEntity(
                        id = "bm_3",
                        profileId = usProfile1.id,
                        title = "DuckDuckGo Privacy Search",
                        url = "https://duckduckgo.com"
                    )
                )

                // Auto-activate VPN USA (Freza Tampan • US-FREE#1) on app launch
                val allEndpoints = dao.getAllVpnEndpointsSnapshot()
                val usEndpoint = allEndpoints.find { it.id == "wg_freza_us_1" }
                    ?: allEndpoints.find { it.countryCode.equals("US", ignoreCase = true) }
                if (usEndpoint != null) {
                    VpnEngineController.activateEndpointImmediately(appContext, usEndpoint)
                } else {
                    VpnEngineController.refreshPublicIpTelemetry("US")
                }
            } else {
                // On every app startup:
                // 1. Reset all login sessions from all profiles so every profile starts clean without login
                // 2. Activate USA Profile & USA Location (New York / US)
                // 3. Open https://quarterfull.io without login in the active tab
                // 4. Activate VPN USA (Freza Tampan • US-FREE#1)
                val usCountry = CountryIdentityCatalog.getCountryByCode("US")
                val defaultUsCity = usCountry.cities.first()
                val targetUsProfile = existingProfiles.find { it.id == "profile_default_us" }
                    ?: existingProfiles.find { it.countryCode.equals("US", ignoreCase = true) }
                    ?: existingProfiles.first()

                val matchedUsCity = usCountry.cities.find {
                    it.name.equals(targetUsProfile.cityName, ignoreCase = true)
                } ?: defaultUsCity

                val updatedUsProfile = targetUsProfile.copy(
                    countryCode = "US",
                    cityName = matchedUsCity.name,
                    timezoneId = matchedUsCity.timezoneId,
                    languageTag = "en-US",
                    simulatedLat = matchedUsCity.defaultLat,
                    simulatedLon = matchedUsCity.defaultLon,
                    preferredVpnServerId = targetUsProfile.preferredVpnServerId ?: "wg_freza_us_1",
                    isActive = true,
                    isFocused = true
                )
                dao.updateProfile(updatedUsProfile)
                dao.deactivateProfilesOutsideCountry("US")
                dao.setProfileActiveState(updatedUsProfile.id, true)
                dao.setFocusedProfile(updatedUsProfile.id)

                // Ensure active tab on startup is https://quarterfull.io (clean, without login)
                val profileTabs = dao.getTabsForProfileSnapshot(updatedUsProfile.id)
                val existingQfTab = profileTabs.find { it.url.contains("quarterfull.io", ignoreCase = true) }
                if (existingQfTab != null) {
                    dao.updateTab(
                        existingQfTab.copy(
                            url = "https://quarterfull.io",
                            title = "QuarterFull — Read & Create",
                            lastAccessed = System.currentTimeMillis()
                        )
                    )
                    dao.setActiveTab(updatedUsProfile.id, existingQfTab.id)
                } else if (profileTabs.isNotEmpty()) {
                    val firstTab = profileTabs.first()
                    dao.updateTab(
                        firstTab.copy(
                            url = "https://quarterfull.io",
                            title = "QuarterFull — Read & Create",
                            isActive = true,
                            lastAccessed = System.currentTimeMillis()
                        )
                    )
                    dao.setActiveTab(updatedUsProfile.id, firstTab.id)
                } else {
                    val newQfTab = BrowserTabEntity(
                        id = "tab_initial_qf_${updatedUsProfile.id}",
                        profileId = updatedUsProfile.id,
                        groupId = null,
                        title = "QuarterFull — Read & Create",
                        url = "https://quarterfull.io",
                        isPrivate = false,
                        isActive = true,
                        lastAccessed = System.currentTimeMillis()
                    )
                    dao.insertTab(newQfTab)
                    dao.setActiveTab(updatedUsProfile.id, newQfTab.id)
                }

                // Auto-activate VPN USA (Freza Tampan • US-FREE#1)
                val allEndpoints = dao.getAllVpnEndpointsSnapshot()
                val usEndpoint = allEndpoints.find { it.id == "wg_freza_us_1" }
                    ?: allEndpoints.find { it.countryCode.equals("US", ignoreCase = true) }
                if (usEndpoint != null) {
                    VpnEngineController.activateEndpointImmediately(appContext, usEndpoint)
                } else {
                    VpnEngineController.refreshPublicIpTelemetry("US")
                }
            }
        }
    }

    override fun onCleared() {
        BrowserEngineManager.purgeAllProfilesSessionsOnAppExit(appContext)
        super.onCleared()
    }

    /**
     * Synchronizes the 10 embedded Freza ProtonVPN WireGuard configurations into the Room database,
     * removing any legacy dummy endpoints and encrypting all private keys with Android Keystore AES-256-GCM.
     */
    private suspend fun syncBuiltInWireGuardEndpointsInternal(forceOverwriteAll: Boolean) {
        val currentEndpoints = dao.getAllVpnEndpointsSnapshot()
        val builtInCatalog = CountryIdentityCatalog.builtInWireGuardConfigs
        val builtInIds = builtInCatalog.map { it.id }.toSet()

        if (forceOverwriteAll) {
            dao.deleteAllVpnEndpoints()
        } else {
            // Remove any legacy dummy endpoints that are not part of the 10 real Freza configs
            // if the database hasn't been migrated to the Freza pack yet
            val hasAnyFrezaConfig = currentEndpoints.any { it.id in builtInIds }
            if (!hasAnyFrezaConfig && currentEndpoints.isNotEmpty()) {
                currentEndpoints.forEach { legacy ->
                    dao.deleteVpnEndpoint(legacy.id)
                }
            }
        }

        val updatedSnapshot = dao.getAllVpnEndpointsSnapshot().associateBy { it.id }
        builtInCatalog.forEach { preset ->
            if (forceOverwriteAll || !updatedSnapshot.containsKey(preset.id)) {
                val encryptedPrivateKey = KeystoreCryptoManager.encryptSecret(preset.privateKeyPlain)
                val entity = VpnEndpointEntity(
                    id = preset.id,
                    name = preset.name,
                    countryCode = preset.countryCode,
                    cityName = preset.cityName,
                    endpointHost = preset.endpointHost,
                    endpointPort = preset.endpointPort,
                    tunnelAddressIpv4 = preset.tunnelAddressIpv4,
                    tunnelAddressIpv6 = preset.tunnelAddressIpv6,
                    dnsServers = preset.dnsServers,
                    publicKey = preset.publicKey,
                    encryptedPrivateKey = encryptedPrivateKey,
                    allowedIps = preset.allowedIps,
                    persistentKeepalive = preset.persistentKeepalive,
                    browserOnlyRouting = true,
                    failClosedKillSwitch = true
                )
                dao.insertVpnEndpoint(entity)
            }
        }
    }

    fun restoreBuiltInWireGuardConfigs() {
        viewModelScope.launch {
            syncBuiltInWireGuardEndpointsInternal(forceOverwriteAll = true)
            showMessage("10 Konfigurasi WireGuard Asli (Freza US #1-4, NL, JP, CA, MX, CH, NO) berhasil ditanam ulang")
        }
    }

    // =========================================================================
    // LEFT PANEL & RIGHT PANEL TOGGLES (CHROME APK WORKSPACE)
    // =========================================================================

    fun toggleLeftPanel() {
        val next = !_isLeftPanelOpen.value
        _isLeftPanelOpen.value = next
        if (next) {
            _isRightPanelOpen.value = false
        }
    }

    fun setLeftPanelOpen(open: Boolean) {
        _isLeftPanelOpen.value = open
        if (open) _isRightPanelOpen.value = false
    }

    fun toggleRightPanel() {
        val next = !_isRightPanelOpen.value
        _isRightPanelOpen.value = next
        if (next) {
            _isLeftPanelOpen.value = false
        }
    }

    fun setRightPanelOpen(open: Boolean) {
        _isRightPanelOpen.value = open
        if (open) _isLeftPanelOpen.value = false
    }

    fun closeBothPanels() {
        _isLeftPanelOpen.value = false
        _isRightPanelOpen.value = false
    }

    fun navigateTo(destination: GiyaniDestination) {
        _currentDestination.value = destination
        closeBothPanels()
        if (destination == GiyaniDestination.DIAGNOSTICS) {
            refreshDiagnostics()
        }
    }

    fun setAdvancedMode(enabled: Boolean) {
        _isAdvancedMode.value = enabled
        viewModelScope.launch {
            appContext.giyaniDataStore.edit { prefs ->
                prefs[KEY_ADVANCED_MODE] = enabled
            }
        }
    }

    fun toggleDarkTheme() {
        setDarkTheme(!_isDarkTheme.value)
    }

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        viewModelScope.launch {
            appContext.giyaniDataStore.edit { prefs ->
                prefs[KEY_DARK_THEME] = isDark
            }
        }
        showMessage(if (isDark) "Tema: Stealth Cyber Dark Aktif" else "Tema: Crisp Daylight Light Aktif")
    }

    fun toggleFullscreenMode() {
        val next = !_isFullscreenMode.value
        _isFullscreenMode.value = next
        if (next) {
            closeBothPanels()
        }
    }

    fun setFullscreenMode(fullscreen: Boolean) {
        _isFullscreenMode.value = fullscreen
        if (fullscreen) {
            closeBothPanels()
        }
    }

    fun toggleDesktopViewportOverride() {
        val next = !_isDesktopViewportOverride.value
        _isDesktopViewportOverride.value = next
        showMessage(
            if (next) "Mode Tampilan: Desktop Site (1440x900) Aktif"
            else "Mode Tampilan: Sesuai Preset Perangkat (${uiState.value.selectedDevice.name})"
        )
    }

    fun dismissCrossCountrySwitchWarning() {
        _pendingCrossCountrySwitch.value = null
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    // =========================================================================
    // 1. MULTI-PROFILE MANAGEMENT (NAME, EMAIL, NO PASSWORD, MULTI-ACTIVE IN SAME COUNTRY/VPN)
    // =========================================================================

    /**
     * Toggles whether a profile preserves its login session (Cookies, localStorage, sessionStorage,
     * and live WebView instance) when switching between profiles.
     */
    fun toggleProfileKeepLoginSession(profile: BrowserProfileEntity, enabled: Boolean? = null) {
        val targetState = enabled ?: !profile.keepLoginSession
        viewModelScope.launch {
            dao.setProfileKeepLoginSession(profile.id, targetState)
            if (!targetState) {
                BrowserEngineManager.clearProfileVaultData(appContext, profile.id)
            } else {
                val activeUrls = uiState.value.tabs.map { it.url }
                BrowserEngineManager.saveProfileCookieSnapshot(appContext, profile.copy(keepLoginSession = true), activeUrls)
            }
            showMessage(
                if (targetState) "Sesi Login \"${profile.name}\" akan dipertahankan saat pindah profile"
                else "Sesi Login \"${profile.name}\" akan di-reset otomatis saat keluar dari profile"
            )
        }
    }

    fun resetProfileLoginSession(profile: BrowserProfileEntity) {
        viewModelScope.launch {
            val isFocused = profile.id == uiState.value.activeProfile.id
            BrowserEngineManager.resetSingleProfileSession(
                context = appContext,
                profile = profile,
                isCurrentlyFocused = isFocused
            )
            if (isFocused) {
                val targetUrl = uiState.value.activeTab?.url ?: "https://www.google.com"
                onWebViewNavigate?.invoke(targetUrl)
            }
            showMessage("Sesi login untuk profile \"${profile.name}\" telah di-reset & dibersihkan 100%")
        }
    }

    fun purgeAllMixedSessionsEverywhere() {
        viewModelScope.launch {
            val focusedId = uiState.value.activeProfile.id
            BrowserEngineManager.purgeAllProfilesSessionsEverywhere(appContext, focusedId)
            val targetUrl = uiState.value.activeTab?.url ?: "https://www.google.com"
            onWebViewNavigate?.invoke(targetUrl)
            showMessage("Seluruh sesi login di semua profile telah di-reset bersih & diisolasi ulang 100%")
        }
    }

    fun setAllActiveProfilesKeepLoginSession(keepLogin: Boolean) {
        viewModelScope.launch {
            uiState.value.activeProfiles.forEach { prof ->
                dao.setProfileKeepLoginSession(prof.id, keepLogin)
                if (!keepLogin) {
                    BrowserEngineManager.clearProfileVaultData(appContext, prof.id)
                }
            }
            showMessage(
                if (keepLogin) "Semua profile aktif (${uiState.value.selectedCountry.isoCode}) mempertahankan sesi login masing-masing secara terisolasi"
                else "Semua profile aktif akan mereset sesi login saat berpindah profile"
            )
        }
    }

    /**
     * Creates a new Browser Profile with Name, Email (no password required), and its own
     * dedicated Browser Identity (Country, City, Timezone, Language, Device Preset).
     */
    fun createBrowserProfile(
        name: String,
        email: String,
        countryCode: String,
        cityName: String,
        devicePresetId: String,
        keepLoginSession: Boolean = true
    ) {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        if (cleanName.isBlank()) {
            showMessage("Nama profile wajib diisi")
            return
        }
        viewModelScope.launch {
            val country = CountryIdentityCatalog.getCountryByCode(countryCode)
            val city = country.cities.find { it.name.equals(cityName, ignoreCase = true) }
                ?: country.cities.first()

            val currentCountry = uiState.value.activeProfile.countryCode
            val currentVpnId = uiState.value.activeProfile.preferredVpnServerId
            val sameCountryOrVpn = country.isoCode.equals(currentCountry, ignoreCase = true)

            val profile = BrowserProfileEntity(
                id = "profile_${UUID.randomUUID()}",
                name = cleanName,
                email = cleanEmail.ifBlank { "${cleanName.lowercase().replace(" ", ".")}@giyani.local" },
                countryCode = country.isoCode,
                cityName = city.name,
                timezoneId = city.timezoneId,
                languageTag = country.defaultLanguage,
                devicePresetId = devicePresetId,
                preferredVpnServerId = if (sameCountryOrVpn) currentVpnId else null,
                keepLoginSession = keepLoginSession,
                isActive = sameCountryOrVpn,
                isFocused = sameCountryOrVpn,
                simulatedLat = city.defaultLat,
                simulatedLon = city.defaultLon
            )
            dao.insertProfile(profile)

            // Seed default tab group & tabs (Google & QuarterFull) for the new profile
            val defaultGroup = TabGroupEntity(
                id = "group_${UUID.randomUUID()}",
                profileId = profile.id,
                name = "${country.name} Workspace",
                colorHex = 0xFF10B981,
                isExpanded = true
            )
            dao.insertTabGroup(defaultGroup)
            dao.insertTab(
                BrowserTabEntity(
                    id = "tab_${UUID.randomUUID()}",
                    profileId = profile.id,
                    groupId = defaultGroup.id,
                    title = "Google (${country.isoCode})",
                    url = "https://www.google.com",
                    isActive = true
                )
            )
            dao.insertTab(
                BrowserTabEntity(
                    id = "tab_${UUID.randomUUID()}",
                    profileId = profile.id,
                    groupId = defaultGroup.id,
                    title = "QuarterFull (${cleanName})",
                    url = "https://quarterfull.io",
                    isActive = false
                )
            )

            // Ensure new profile starts with a 100% clean isolated vault
            BrowserEngineManager.clearProfileVaultData(appContext, profile.id)

            // If same country, activate and focus immediately; if different country, show Cross-Country Warning Modal
            if (sameCountryOrVpn) {
                useProfile(profile, forceSwitchCountry = false)
            } else {
                requestCrossCountryProfileActivation(profile)
            }
        }
    }

    /**
     * Backward-compatible overload for createBrowserProfile without email parameter.
     */
    fun createBrowserProfile(
        name: String,
        countryCode: String,
        cityName: String,
        devicePresetId: String
    ) {
        createBrowserProfile(
            name = name,
            email = "${name.lowercase().replace(" ", ".")}@giyani.browser",
            countryCode = countryCode,
            cityName = cityName,
            devicePresetId = devicePresetId,
            keepLoginSession = true
        )
    }

    /**
     * Edits an existing Browser Profile's Name, Email, Country Identity, City, Device Preset, and Keep Login toggle.
     */
    fun editBrowserProfile(
        profile: BrowserProfileEntity,
        newName: String,
        newEmail: String,
        newCountryCode: String,
        newCityName: String,
        newDevicePresetId: String,
        keepLoginSession: Boolean = profile.keepLoginSession
    ) {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            val country = CountryIdentityCatalog.getCountryByCode(newCountryCode)
            val city = country.cities.find { it.name.equals(newCityName, ignoreCase = true) }
                ?: country.cities.first()

            val updated = profile.copy(
                name = cleanName,
                email = newEmail.trim(),
                countryCode = country.isoCode,
                cityName = city.name,
                timezoneId = city.timezoneId,
                languageTag = country.defaultLanguage,
                devicePresetId = newDevicePresetId,
                keepLoginSession = keepLoginSession,
                simulatedLat = city.defaultLat,
                simulatedLon = city.defaultLon
            )
            dao.updateProfile(updated)

            if (updated.isActive && !updated.countryCode.equals(uiState.value.activeProfile.countryCode, ignoreCase = true)) {
                requestCrossCountryProfileActivation(updated)
            } else {
                showMessage("Profile \"${updated.name}\" berhasil diperbarui")
            }
        }
    }

    /**
     * Triggers the Cross-Country Deactivation Warning Modal whenever the user attempts to activate
     * or switch to a profile from a different Country than the currently active Country profiles.
     */
    fun requestCrossCountryProfileActivation(targetProfile: BrowserProfileEntity) {
        val currentActiveProfiles = uiState.value.activeProfiles
        val currentCountryIso = uiState.value.activeProfile.countryCode
        val currentCountry = CountryIdentityCatalog.getCountryByCode(currentCountryIso)
        val targetCountry = CountryIdentityCatalog.getCountryByCode(targetProfile.countryCode)

        _pendingCrossCountrySwitch.value = CrossCountrySwitchWarning(
            targetProfile = targetProfile,
            currentCountryCode = currentCountry.isoCode,
            currentCountryName = currentCountry.name,
            targetCountryCode = targetCountry.isoCode,
            targetCountryName = targetCountry.name,
            affectedActiveProfiles = currentActiveProfiles.filter {
                !it.countryCode.equals(targetCountry.isoCode, ignoreCase = true)
            }
        )
    }

    /**
     * Executed when the user confirms in the Warning Modal that they want to deactivate all
     * profiles in the current Country (e.g., all 3 US profiles) and switch to the new Country profile.
     */
    fun confirmCrossCountryProfileSwitch(keepOldCountryLoginSessionsInVault: Boolean = true) {
        val warning = _pendingCrossCountrySwitch.value ?: return
        _pendingCrossCountrySwitch.value = null
        val targetProfile = warning.targetProfile

        viewModelScope.launch {
            val previousProfile = uiState.value.activeProfile
            val activeDomains = uiState.value.tabs.map { it.url }

            // 1. First, isolate & switch session BEFORE destroying old country WebViews so
            //    previousProfile's DOMStorage/Cookies are saved to its own vault and wiped from shared WebView storage!
            BrowserEngineManager.isolateAndSwitchProfileSession(
                context = appContext,
                previousProfile = previousProfile.copy(
                    keepLoginSession = previousProfile.keepLoginSession && keepOldCountryLoginSessionsInVault
                ),
                newProfile = targetProfile,
                activeDomains = activeDomains
            )

            // 2. Release live WebView instances for all profiles from the old country
            BrowserEngineManager.releaseWebViewsOutsideCountry(
                context = appContext,
                profilesToDeactivate = warning.affectedActiveProfiles,
                keepOldSessionsInVault = keepOldCountryLoginSessionsInVault
            )

            // 3. Deactivate all profiles outside the new country in Room DB & focus targetProfile
            dao.deactivateProfilesOutsideCountry(targetProfile.countryCode)
            dao.setProfileActiveState(targetProfile.id, true)
            dao.setFocusedProfile(targetProfile.id)
            VpnEngineController.refreshPublicIpTelemetry(targetProfile.countryCode)

            showMessage(
                "Seluruh ${warning.affectedActiveProfiles.size} profile ${warning.currentCountryName} (${warning.currentCountryCode}) dinonaktifkan. Kini aktif: \"${targetProfile.name}\" (${warning.targetCountryName} • Brankas Sesi Terpisah)"
            )
        }
    }

    /**
     * Activates/Opens or Deactivates a profile for multi-profile simultaneous browsing.
     * - If the target profile is in the SAME Country or VPN, it immediately joins the active multi-profile pool.
     * - If the target profile is in a DIFFERENT Country, it opens the Cross-Country Warning Modal!
     */
    fun toggleProfileMultiActive(targetProfile: BrowserProfileEntity) {
        viewModelScope.launch {
            val currentlyActive = uiState.value.activeProfiles
            val referenceCountry = uiState.value.activeProfile.countryCode
            val referenceVpn = uiState.value.activeProfile.preferredVpnServerId

            if (targetProfile.isActive) {
                if (currentlyActive.size <= 1) {
                    showMessage("Minimal 1 profile harus tetap aktif di browser")
                    return@launch
                }
                val activeUrls = uiState.value.tabs.map { it.url }
                if (targetProfile.keepLoginSession) {
                    BrowserEngineManager.saveProfileCookieSnapshot(appContext, targetProfile, activeUrls)
                }
                BrowserEngineManager.releaseProfileWebView(appContext, targetProfile)
                dao.setProfileActiveState(targetProfile.id, false)

                if (targetProfile.isFocused) {
                    val nextFocus = currentlyActive.firstOrNull { it.id != targetProfile.id }
                    if (nextFocus != null) {
                        useProfile(nextFocus, forceSwitchCountry = false)
                    }
                } else {
                    showMessage(
                        if (targetProfile.keepLoginSession) "Sesi \"${targetProfile.name}\" dinonaktifkan (Login tetap tersimpan di brankas)"
                        else "Sesi \"${targetProfile.name}\" ditutup & login di-reset"
                    )
                }
            } else {
                val sameCountry = targetProfile.countryCode.equals(referenceCountry, ignoreCase = true)
                val sameVpn = !referenceVpn.isNullOrBlank() && referenceVpn == targetProfile.preferredVpnServerId

                if (sameCountry || sameVpn) {
                    dao.setProfileActiveState(targetProfile.id, true)
                    showMessage(
                        "Multi-Profile ${targetProfile.countryCode} Aktif: \"${targetProfile.name}\" dibuka paralel dengan identitas ${CountryIdentityCatalog.getDeviceById(targetProfile.devicePresetId).name}"
                    )
                } else {
                    // Show modal warning that all active profiles in referenceCountry will be deactivated
                    requestCrossCountryProfileActivation(targetProfile)
                }
            }
        }
    }

    /**
     * Focuses and uses a profile in the main Chrome browser viewport (`useProfile` / `switchProfile`).
     * - If the target profile is in the SAME Country or VPN as currently active profiles:
     *   Switches focus instantly while keeping all other same-country profiles active AND preserving
     *   each profile's login session (when `keepLoginSession == true`).
     * - If the target profile is in a DIFFERENT Country:
     *   Displays the Cross-Country Deactivation Warning Modal so the user is explicitly warned that
     *   all active profiles in the current country (e.g., all US profiles) will be deactivated.
     */
    fun useProfile(
        targetProfile: BrowserProfileEntity,
        forceSwitchCountry: Boolean = false
    ) {
        viewModelScope.launch {
            val previousProfile = uiState.value.activeProfile
            val sameCountry = targetProfile.countryCode.equals(previousProfile.countryCode, ignoreCase = true)
            val sameVpn = !previousProfile.preferredVpnServerId.isNullOrBlank() &&
                previousProfile.preferredVpnServerId == targetProfile.preferredVpnServerId

            if (!sameCountry && !sameVpn) {
                // Always show the explicit Cross-Country Deactivation Warning Modal when other profiles are active
                val activeInOldCountry = uiState.value.activeProfiles.filter {
                    !it.countryCode.equals(targetProfile.countryCode, ignoreCase = true)
                }
                if (activeInOldCountry.isNotEmpty() && !forceSwitchCountry) {
                    requestCrossCountryProfileActivation(targetProfile)
                    return@launch
                } else if (activeInOldCountry.isNotEmpty() && forceSwitchCountry) {
                    requestCrossCountryProfileActivation(targetProfile)
                    return@launch
                }
            }

            val activeDomains = uiState.value.tabs.map { it.url }
            BrowserEngineManager.isolateAndSwitchProfileSession(
                context = appContext,
                previousProfile = previousProfile,
                newProfile = targetProfile,
                activeDomains = activeDomains
            )
            dao.setProfileActiveState(targetProfile.id, true)
            dao.setFocusedProfile(targetProfile.id)
            VpnEngineController.refreshPublicIpTelemetry(targetProfile.countryCode)

            val deviceName = CountryIdentityCatalog.getDeviceById(targetProfile.devicePresetId).name
            showMessage(
                "Beralih ke \"${targetProfile.name}\" ($deviceName) • ${if (targetProfile.keepLoginSession) "Sesi Login Terjaga" else "Sesi Bersih"}"
            )
        }
    }

    fun switchProfile(targetProfile: BrowserProfileEntity) {
        useProfile(targetProfile, forceSwitchCountry = false)
    }

    fun deleteProfile(profile: BrowserProfileEntity) {
        if (uiState.value.profiles.size <= 1) {
            showMessage("Minimal 1 profile harus tetap tersimpan")
            return
        }
        viewModelScope.launch {
            BrowserEngineManager.clearProfileVaultData(appContext, profile.id)
            BrowserEngineManager.releaseProfileWebView(appContext, profile)
            dao.deleteProfile(profile.id)
            val remaining = uiState.value.profiles.filter { it.id != profile.id }
            if (profile.isFocused && remaining.isNotEmpty()) {
                val nextSameCountry = remaining.find {
                    it.isActive && it.countryCode.equals(profile.countryCode, ignoreCase = true)
                } ?: remaining.find { it.isActive } ?: remaining.first()
                dao.setProfileActiveState(nextSameCountry.id, true)
                dao.setFocusedProfile(nextSameCountry.id)
            } else {
                showMessage("Profile \"${profile.name}\" dihapus")
            }
        }
    }

    // =========================================================================
    // 2. COUNTRY IDENTITY, CITY/TIMEZONE & DEVICE PROFILE ENGINE
    // =========================================================================

    fun selectCountry(countryIso: String) {
        viewModelScope.launch {
            val preset = CountryIdentityCatalog.getCountryByCode(countryIso)
            val defaultCity = preset.cities.first()
            val current = uiState.value.activeProfile
            val updated = current.copy(
                countryCode = preset.isoCode,
                cityName = defaultCity.name,
                timezoneId = defaultCity.timezoneId,
                languageTag = preset.defaultLanguage,
                simulatedLat = defaultCity.defaultLat,
                simulatedLon = defaultCity.defaultLon
            )
            dao.updateProfile(updated)
            // Suspend active profiles that don't match the newly selected country
            dao.deactivateProfilesOutsideCountry(preset.isoCode)
            dao.setProfileActiveState(updated.id, true)
            dao.setFocusedProfile(updated.id)
            VpnEngineController.refreshPublicIpTelemetry(preset.isoCode)
            showMessage("Identitas Negara: ${preset.name} (${defaultCity.name} • ${defaultCity.timezoneId})")
        }
    }

    fun selectCity(city: CityPreset) {
        viewModelScope.launch {
            val updated = uiState.value.activeProfile.copy(
                cityName = city.name,
                timezoneId = city.timezoneId,
                simulatedLat = city.defaultLat,
                simulatedLon = city.defaultLon
            )
            dao.updateProfile(updated)
            showMessage("Kota & Timezone: ${city.name} (${city.timezoneId})")
        }
    }

    fun selectDevicePreset(deviceId: String) {
        viewModelScope.launch {
            val preset = CountryIdentityCatalog.getDeviceById(deviceId)
            val updated = uiState.value.activeProfile.copy(
                devicePresetId = preset.id
            )
            dao.updateProfile(updated)
            showMessage("Identitas Perangkat (${uiState.value.activeProfile.name}): ${preset.name}")
        }
    }

    fun updatePrivacySettings(
        blockThirdPartyCookies: Boolean? = null,
        blockTrackers: Boolean? = null,
        canvasProtection: Boolean? = null,
        webRtcProtection: Boolean? = null,
        geolocationPolicy: String? = null,
        simulatedLat: Double? = null,
        simulatedLon: Double? = null
    ) {
        viewModelScope.launch {
            val current = uiState.value.activeProfile
            val updated = current.copy(
                blockThirdPartyCookies = blockThirdPartyCookies ?: current.blockThirdPartyCookies,
                blockTrackers = blockTrackers ?: current.blockTrackers,
                canvasProtection = canvasProtection ?: current.canvasProtection,
                webRtcProtection = webRtcProtection ?: current.webRtcProtection,
                geolocationPolicy = geolocationPolicy ?: current.geolocationPolicy,
                simulatedLat = simulatedLat ?: current.simulatedLat,
                simulatedLon = simulatedLon ?: current.simulatedLon
            )
            dao.updateProfile(updated)
            _diagnosticsRefreshTrigger.value = System.currentTimeMillis()
        }
    }

    // =========================================================================
    // 3. TAB GROUPING & TAB MANAGER ENGINE
    // =========================================================================

    fun createTabGroup(name: String, colorHex: Long) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            val profileId = uiState.value.activeProfile.id
            val nextOrder = uiState.value.tabGroups.size
            val group = TabGroupEntity(
                id = "group_${UUID.randomUUID()}",
                profileId = profileId,
                name = cleanName,
                colorHex = colorHex,
                isExpanded = true,
                sortOrder = nextOrder
            )
            dao.insertTabGroup(group)
            showMessage("Grup tab \"$cleanName\" dibuat")
        }
    }

    fun renameOrRecolorTabGroup(group: TabGroupEntity, newName: String, newColorHex: Long) {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            dao.updateTabGroup(
                group.copy(
                    name = cleanName,
                    colorHex = newColorHex
                )
            )
            showMessage("Grup tab \"$cleanName\" diperbarui")
        }
    }

    fun toggleTabGroupExpanded(group: TabGroupEntity) {
        viewModelScope.launch {
            dao.updateTabGroup(group.copy(isExpanded = !group.isExpanded))
        }
    }

    fun expandOrCollapseAllGroups(expand: Boolean) {
        viewModelScope.launch {
            uiState.value.tabGroups.forEach { group ->
                if (group.isExpanded != expand) {
                    dao.updateTabGroup(group.copy(isExpanded = expand))
                }
            }
        }
    }

    fun deleteTabGroup(groupId: String, closeTabsInside: Boolean) {
        viewModelScope.launch {
            if (closeTabsInside) {
                dao.deleteTabsInGroup(groupId)
            } else {
                dao.ungroupTabsInGroup(groupId)
            }
            dao.deleteTabGroup(groupId)
            showMessage(if (closeTabsInside) "Grup & tab di dalamnya ditutup" else "Grup dihapus (tab dipindahkan ke Ungrouped)")
        }
    }

    fun createNewTab(
        url: String = "https://www.google.com",
        title: String = "Google Search",
        groupId: String? = null,
        isPrivate: Boolean = false,
        openImmediately: Boolean = true
    ) {
        viewModelScope.launch {
            val profileId = uiState.value.activeProfile.id
            val newTab = BrowserTabEntity(
                id = "tab_${UUID.randomUUID()}",
                profileId = profileId,
                groupId = groupId,
                title = title,
                url = normalizeUrlOrSearch(url),
                isPrivate = isPrivate,
                isActive = openImmediately,
                lastAccessed = System.currentTimeMillis()
            )
            dao.insertTab(newTab)
            if (openImmediately) {
                dao.setActiveTab(profileId, newTab.id)
                _currentDestination.value = GiyaniDestination.BROWSER_WEBVIEW
            } else {
                showMessage("Tab baru ditambahkan")
            }
        }
    }

    fun moveTabToGroup(tabId: String, targetGroupId: String?) {
        viewModelScope.launch {
            dao.moveTabToGroup(tabId, targetGroupId)
            val targetName = if (targetGroupId == null) {
                "Ungrouped Tabs"
            } else {
                uiState.value.tabGroups.find { it.id == targetGroupId }?.name ?: "Group"
            }
            showMessage("Tab dipindahkan ke $targetName")
        }
    }

    fun switchToTab(tab: BrowserTabEntity) {
        viewModelScope.launch {
            dao.setActiveTab(tab.profileId, tab.id)
            dao.updateTab(tab.copy(isActive = true, lastAccessed = System.currentTimeMillis()))
            _currentDestination.value = GiyaniDestination.BROWSER_WEBVIEW
        }
    }

    fun closeTab(tab: BrowserTabEntity) {
        viewModelScope.launch {
            dao.deleteTab(tab.id)
            val remaining = uiState.value.tabs.filter { it.id != tab.id }
            if (tab.isActive && remaining.isNotEmpty()) {
                val nextActive = remaining.first()
                dao.setActiveTab(tab.profileId, nextActive.id)
            }
        }
    }

    // =========================================================================
    // 4. BROWSER WEBVIEW NAVIGATION, BOOKMARKS & HISTORY
    // =========================================================================

    fun loadUrlInActiveTab(rawInput: String) {
        val targetUrl = normalizeUrlOrSearch(rawInput)
        viewModelScope.launch {
            val active = uiState.value.activeTab
            if (active != null) {
                dao.updateTab(
                    active.copy(
                        url = targetUrl,
                        lastAccessed = System.currentTimeMillis()
                    )
                )
                _currentDestination.value = GiyaniDestination.BROWSER_WEBVIEW
                onWebViewNavigate?.invoke(targetUrl)
            } else {
                createNewTab(url = targetUrl, title = targetUrl, openImmediately = true)
            }
        }
    }

    fun onWebViewPageStarted(url: String) {
        _isPageLoading.value = true
        _webPageProgress.value = 15
    }

    fun onWebViewProgressChanged(progress: Int) {
        _webPageProgress.value = progress
        _isPageLoading.value = progress < 100
    }

    fun onWebViewPageFinished(
        url: String,
        title: String?,
        canBack: Boolean,
        canFwd: Boolean
    ) {
        _isPageLoading.value = false
        _webPageProgress.value = 100
        _canGoBack.value = canBack
        _canGoForward.value = canFwd

        val cleanTitle = title?.takeIf { it.isNotBlank() } ?: url
        viewModelScope.launch {
            val active = uiState.value.activeTab ?: return@launch
            dao.updateTab(
                active.copy(
                    url = url,
                    title = cleanTitle,
                    lastAccessed = System.currentTimeMillis()
                )
            )
            if (!active.isPrivate && url.startsWith("http")) {
                dao.insertHistory(
                    HistoryEntity(
                        id = "hist_${UUID.randomUUID()}",
                        profileId = active.profileId,
                        title = cleanTitle,
                        url = url,
                        visitedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun incrementTrackersBlocked() {
        _trackersBlockedCount.value += 1
    }

    fun addCurrentPageToBookmarks() {
        val active = uiState.value.activeTab ?: return
        viewModelScope.launch {
            dao.insertBookmark(
                BookmarkEntity(
                    id = "bm_${UUID.randomUUID()}",
                    profileId = active.profileId,
                    title = active.title.ifBlank { active.url },
                    url = active.url
                )
            )
            showMessage("Disimpan ke Bookmark (${uiState.value.activeProfile.name})")
        }
    }

    fun deleteBookmark(id: String) {
        viewModelScope.launch {
            dao.deleteBookmark(id)
        }
    }

    fun clearBrowsingHistory() {
        viewModelScope.launch {
            dao.clearHistoryForProfile(uiState.value.activeProfile.id)
            showMessage("Riwayat browsing ${uiState.value.activeProfile.name} dibersihkan")
        }
    }

    // =========================================================================
    // 5. DOWNLOAD MANAGER (PAUSE, RESUME, BACKGROUND, OPEN, SHARE LOCATION)
    // =========================================================================

    fun enqueueDownload(url: String, suggestedFileName: String? = null, mimeType: String? = null) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return
        DownloadEngine.enqueueNewDownload(
            context = appContext,
            url = cleanUrl,
            suggestedFileName = suggestedFileName,
            mimeType = mimeType
        )
        showMessage("Download dimulai di latar belakang")
    }

    fun pauseDownload(downloadId: String) {
        DownloadEngine.pauseDownload(appContext, downloadId)
    }

    fun resumeDownload(downloadId: String) {
        DownloadEngine.startOrResumeDownload(appContext, downloadId)
    }

    fun deleteDownload(downloadId: String, deleteFile: Boolean = true) {
        DownloadEngine.cancelAndDeleteDownload(appContext, downloadId, deleteFile)
        showMessage("Download dihapus")
    }

    fun openDownloadedFile(item: DownloadEntity) {
        val result = DownloadEngine.openDownloadedFile(appContext, item)
        result.onFailure { err ->
            showMessage("Gagal membuka file: ${err.localizedMessage}")
        }
    }

    fun shareDownloadedFileLocation(item: DownloadEntity) {
        val result = DownloadEngine.shareDownloadedFileLocation(appContext, item)
        result.onFailure { err ->
            showMessage("Gagal membagikan file: ${err.localizedMessage}")
        }
    }

    fun getDownloadDirectoryPath(): String {
        return DownloadEngine.getDownloadDirectory(appContext).absolutePath
    }

    // =========================================================================
    // 6. INTEGRATED WIREGUARD VPN ENGINE
    // =========================================================================

    fun importWireGuardConfig(
        profileName: String,
        countryCode: String,
        cityName: String,
        rawConfText: String,
        browserOnly: Boolean,
        failClosed: Boolean
    ) {
        val parsed = VpnEngineController.parseWireGuardConfig(
            profileName = profileName,
            countryCode = countryCode,
            cityName = cityName,
            rawConfText = rawConfText,
            browserOnly = browserOnly,
            failClosed = failClosed
        )
        parsed.onSuccess { endpoint ->
            viewModelScope.launch {
                dao.insertVpnEndpoint(endpoint)
                showMessage("Endpoint WireGuard \"${endpoint.name}\" dienkripsi & disimpan ke Keystore")
            }
        }.onFailure { err ->
            showMessage(err.localizedMessage ?: "Format konfigurasi WireGuard tidak valid")
        }
    }

    fun addManualWireGuardEndpoint(
        name: String,
        countryCode: String,
        cityName: String,
        host: String,
        port: Int,
        tunnelIpv4: String,
        tunnelIpv6: String?,
        dns: String,
        publicKey: String,
        privateKey: String,
        browserOnly: Boolean,
        failClosed: Boolean
    ) {
        if (host.isBlank() || publicKey.isBlank() || privateKey.isBlank()) {
            showMessage("Host, Public Key, dan Private Key wajib diisi")
            return
        }
        viewModelScope.launch {
            val encryptedPrivate = KeystoreCryptoManager.encryptSecret(privateKey.trim())
            val entity = VpnEndpointEntity(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { "$countryCode - $cityName WireGuard" },
                countryCode = countryCode.uppercase(),
                cityName = cityName,
                endpointHost = host.trim(),
                endpointPort = port,
                tunnelAddressIpv4 = tunnelIpv4.ifBlank { "10.66.66.2/32" },
                tunnelAddressIpv6 = tunnelIpv6?.takeIf { it.isNotBlank() },
                dnsServers = dns.ifBlank { "1.1.1.1, 1.0.0.1" },
                publicKey = publicKey.trim(),
                encryptedPrivateKey = encryptedPrivate,
                browserOnlyRouting = browserOnly,
                failClosedKillSwitch = failClosed
            )
            dao.insertVpnEndpoint(entity)
            showMessage("Endpoint WireGuard \"${entity.name}\" disimpan ke Android Keystore")
        }
    }

    fun editWireGuardEndpointFromConf(
        existingEndpoint: VpnEndpointEntity,
        profileName: String,
        countryCode: String,
        cityName: String,
        rawConfText: String,
        browserOnly: Boolean,
        failClosed: Boolean
    ) {
        val parsed = VpnEngineController.parseWireGuardConfig(
            profileName = profileName,
            countryCode = countryCode,
            cityName = cityName,
            rawConfText = rawConfText,
            browserOnly = browserOnly,
            failClosed = failClosed
        )
        parsed.onSuccess { newParsed ->
            viewModelScope.launch {
                val updated = newParsed.copy(id = existingEndpoint.id)
                dao.updateVpnEndpoint(updated)
                if (uiState.value.vpnState.activeEndpoint?.id == existingEndpoint.id &&
                    uiState.value.vpnState.status == VpnTunnelStatus.CONNECTED
                ) {
                    startVpnServiceWithEndpoint(updated)
                }
                showMessage("Konfigurasi WireGuard \"${updated.name}\" berhasil diperbarui")
            }
        }.onFailure { err ->
            showMessage(err.localizedMessage ?: "Format konfigurasi WireGuard tidak valid")
        }
    }

    fun editWireGuardEndpointManual(
        existingEndpoint: VpnEndpointEntity,
        name: String,
        countryCode: String,
        cityName: String,
        host: String,
        port: Int,
        tunnelIpv4: String,
        tunnelIpv6: String?,
        dns: String,
        publicKey: String,
        newPrivateKeyPlain: String,
        browserOnly: Boolean,
        failClosed: Boolean
    ) {
        if (host.isBlank() || publicKey.isBlank()) {
            showMessage("Host dan Public Key wajib diisi")
            return
        }
        viewModelScope.launch {
            val encryptedPriv = if (newPrivateKeyPlain.isNotBlank() &&
                !newPrivateKeyPlain.contains("••••") &&
                !newPrivateKeyPlain.contains("REDACTED")
            ) {
                KeystoreCryptoManager.encryptSecret(newPrivateKeyPlain.trim())
            } else {
                existingEndpoint.encryptedPrivateKey
            }

            val updated = existingEndpoint.copy(
                name = name.ifBlank { "$countryCode - $cityName WireGuard" },
                countryCode = countryCode.uppercase(),
                cityName = cityName,
                endpointHost = host.trim(),
                endpointPort = port,
                tunnelAddressIpv4 = tunnelIpv4.ifBlank { "10.66.66.2/32" },
                tunnelAddressIpv6 = tunnelIpv6?.takeIf { it.isNotBlank() },
                dnsServers = dns.ifBlank { "1.1.1.1, 1.0.0.1" },
                publicKey = publicKey.trim(),
                encryptedPrivateKey = encryptedPriv,
                browserOnlyRouting = browserOnly,
                failClosedKillSwitch = failClosed
            )
            dao.updateVpnEndpoint(updated)
            if (uiState.value.vpnState.activeEndpoint?.id == existingEndpoint.id &&
                uiState.value.vpnState.status == VpnTunnelStatus.CONNECTED
            ) {
                startVpnServiceWithEndpoint(updated)
            }
            showMessage("Konfigurasi WireGuard \"${updated.name}\" berhasil diperbarui")
        }
    }

    fun deleteVpnEndpoint(id: String) {
        viewModelScope.launch {
            if (uiState.value.vpnState.activeEndpoint?.id == id) {
                disconnectVpn()
            }
            dao.deleteVpnEndpoint(id)
            showMessage("Konfigurasi WireGuard dihapus")
        }
    }

    fun deleteAllVpnEndpoints() {
        viewModelScope.launch {
            disconnectVpn()
            dao.deleteAllVpnEndpoints()
            showMessage("Semua konfigurasi WireGuard telah dihapus")
        }
    }

    fun prepareAndConnectVpn(
        endpoint: VpnEndpointEntity?,
        onPermissionRequired: (android.content.Intent) -> Unit
    ) {
        if (endpoint == null) {
            VpnEngineController.updateState {
                it.copy(
                    status = VpnTunnelStatus.NO_ENDPOINT_CONFIGURED,
                    statusMessage = "Belum ada server WireGuard untuk ${uiState.value.selectedCountry.name}. Tambahkan atau impor endpoint terlebih dahulu."
                )
            }
            showMessage("Belum ada endpoint WireGuard. Buka pengaturan VPN untuk menambah endpoint.")
            return
        }

        val prepareIntent = VpnService.prepare(appContext)
        if (prepareIntent != null) {
            VpnEngineController.updateState {
                it.copy(
                    status = VpnTunnelStatus.PERMISSION_REQUIRED,
                    activeEndpoint = endpoint,
                    statusMessage = "Izin VPN Android dibutuhkan untuk membuka tunnel Per-App."
                )
            }
            onPermissionRequired(prepareIntent)
        } else {
            startVpnServiceWithEndpoint(endpoint)
        }
    }

    fun startVpnServiceWithEndpoint(endpoint: VpnEndpointEntity) {
        VpnEngineController.activateEndpointImmediately(appContext, endpoint)
        if (VpnEngineController.state.value.autoSyncCountryWithVpn) {
            autoSyncProfileToDetectedCountry(endpoint.countryCode, endpoint.cityName)
        }
        val connectIntent = GiyaniVpnService.buildConnectIntent(
            context = appContext,
            endpoint = endpoint,
            selectedCountry = endpoint.countryCode.ifBlank { uiState.value.selectedCountry.isoCode }
        )
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                appContext.startForegroundService(connectIntent)
            } else {
                appContext.startService(connectIntent)
            }
        } catch (_: Exception) {
        }
    }

    fun setIpv6LeakProtection(enabled: Boolean) {
        VpnEngineController.setIpv6LeakProtection(
            enabled = enabled,
            selectedCountryIso = uiState.value.selectedCountry.isoCode
        )
        showMessage(
            if (enabled) "Proteksi Kebocoran IPv6 ISP Aktif (IPv6 Sinkhole ::/0)"
            else "Proteksi Kebocoran IPv6 ISP Dinonaktifkan"
        )
    }

    fun setAutoSyncCountryWithVpn(enabled: Boolean) {
        VpnEngineController.setAutoSyncCountryWithVpn(
            enabled = enabled,
            selectedCountryIso = uiState.value.selectedCountry.isoCode
        )
        showMessage(
            if (enabled) "Auto-Sync Negara Profile dengan Lokasi VPN/IP Diaktifkan"
            else "Auto-Sync Negara Profile Dinonaktifkan"
        )
    }

    fun syncProfileCountryWithDetectedIp() {
        val vpnState = uiState.value.vpnState
        val targetIso = if (vpnState.status == VpnTunnelStatus.CONNECTED && vpnState.activeEndpoint != null) {
            vpnState.activeEndpoint.countryCode
        } else {
            vpnState.publicIpTelemetry.detectedCountryCode
        }
        val matchPreset = CountryIdentityCatalog.countries.find {
            it.isoCode.equals(targetIso, ignoreCase = true)
        }
        if (matchPreset != null) {
            selectCountry(matchPreset.isoCode)
        } else {
            showMessage("Negara IP ($targetIso) belum terverifikasi")
        }
    }

    fun disconnectVpn() {
        val disconnectIntent = GiyaniVpnService.buildDisconnectIntent(appContext)
        appContext.startService(disconnectIntent)
    }

    fun refreshDiagnostics() {
        VpnEngineController.refreshPublicIpTelemetry(uiState.value.selectedCountry.isoCode)
        _diagnosticsRefreshTrigger.value = System.currentTimeMillis()
        showMessage("Diagnostik identitas & jaringan diperbarui")
    }

    private fun normalizeUrlOrSearch(input: String): String {
        var trimmed = input.trim()
        if (trimmed.isBlank()) return "https://www.google.com"

        // Smart Omnibox Protocol Auto-Healer: Fix common protocol typos (e.g. htps://quarterfull.io, htp://, https;//, https:/)
        val lower = trimmed.lowercase()
        when {
            lower.startsWith("htps://") -> trimmed = "https://" + trimmed.substring(7)
            lower.startsWith("htp://") -> trimmed = "https://" + trimmed.substring(6)
            lower.startsWith("https;//") -> trimmed = "https://" + trimmed.substring(8)
            lower.startsWith("http;//") -> trimmed = "http://" + trimmed.substring(7)
            lower.startsWith("https:/") && !lower.startsWith("https://") -> trimmed = "https://" + trimmed.substring(7).trimStart('/')
            lower.startsWith("http:/") && !lower.startsWith("http://") -> trimmed = "http://" + trimmed.substring(6).trimStart('/')
            lower.startsWith("https//") -> trimmed = "https://" + trimmed.substring(7)
            lower.startsWith("http//") -> trimmed = "http://" + trimmed.substring(6)
        }

        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }

        // Strip any other malformed scheme prefix like "htps:" before a domain
        if (trimmed.contains("://")) {
            val afterScheme = trimmed.substringAfter("://").trimStart('/')
            if (afterScheme.isNotBlank()) {
                return "https://$afterScheme"
            }
        }

        val looksLikeDomain = !trimmed.contains(" ") && trimmed.contains(".")
        return if (looksLikeDomain) {
            "https://$trimmed"
        } else {
            "https://www.google.com/search?q=${android.net.Uri.encode(trimmed)}"
        }
    }
}
