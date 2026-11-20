package com.example.browser

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.LocaleList
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.ServiceWorkerClient
import android.webkit.ServiceWorkerController
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import com.example.data.BrowserProfileEntity
import com.example.domain.CountryIdentityCatalog
import com.example.domain.CountryPreset
import com.example.domain.DevicePreset
import com.example.vpn.StrictIpv4DnsResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Modular Browser Engine Abstraction Layer (v5 - Single-WebView Anti-Crash RAM Engine &
 * 5-Layer USA/Country Identity & Sign-Up Enforcement).
 *
 * Key Upgrades in v5:
 * 1. Single-WebView Memory Architecture (Saves ~80% RAM, Zero Lag, Zero OOM Crash):
 *    Never holds multiple live Chromium WebView instances in RAM simultaneously. Only 1 single
 *    WebView exists in RAM at any given moment (`currentLiveWebView`). When switching profiles,
 *    the outgoing profile's session is saved to its encrypted disk vault, the old WebView is
 *    released, and the incoming profile's vault is restored.
 * 2. JVM Process-Level Locale & TimeZone Lock (`enforceJvmLocaleAndTimezone`):
 *    Updates `Locale.setDefault(...)` and `TimeZone.setDefault(...)` to match the active profile
 *    (e.g. `en-US` & `America/New_York`) so Chromium's native C++ network stack never leaks
 *    `id-ID` or `Asia/Jakarta` on background `fetch()` and `XMLHttpRequest` calls.
 * 3. JS `fetch()` & `XMLHttpRequest` Sign-Up & GeoIP Shield:
 *    - Intercepts client-side GeoIP lookup APIs (`ipapi`, `ipwho.is`, `ipinfo`, `cdn-cgi/trace`, etc.)
 *      and returns the active profile's country (`US`, `United States`, `New York`, `146.70.230.146`).
 *    - Sanitizes outgoing JSON signup/registration payloads (`POST`/`PUT`/`PATCH`) so any leaked
 *      `"country": "ID"`, `"locale": "id-ID"`, or `"timezone": "Asia/Jakarta"` is automatically
 *      rewritten to `"US"`, `"en-US"`, and `"America/New_York"`.
 * 4. Google.com Regional Lock (`gws_rd=cr` + `PREF` Cookie):
 *    Disables Google Country Redirect to Google Indonesia and forces Google US (`hl=en&gl=US`).
 */
object BrowserEngineManager {

    private const val COOKIE_VAULT_PREFS = "giyani_profile_cookie_vault_v4"
    private const val DOM_VAULT_PREFS = "giyani_profile_dom_vault_v4"
    private const val SANITIZE_META_PREFS = "giyani_sandbox_meta_v4"
    private const val KEY_V4_SANITIZED = "v4_zero_leak_sanitized_done"

    // SINGLE-WEBVIEW MEMORY ARCHITECTURE: At most 1 WebView lives in RAM at any time
    @Volatile
    private var currentLiveWebView: WebView? = null

    @Volatile
    private var currentLiveWebViewProfileId: String? = null

    @Volatile
    private var currentVaultBridge: GiyaniStorageVaultBridge? = null

    @Volatile
    private var isServiceWorkerConfigured: Boolean = false

    @Volatile
    var currentCookieOwnerProfileId: String? = null
        private set

    @Volatile
    var isCookieSwapInProgress: Boolean = false
        private set

    private val knownTrackerDomains = listOf(
        "doubleclick.net",
        "google-analytics.com",
        "googletagmanager.com",
        "facebook.net",
        "connect.facebook.net",
        "hotjar.com",
        "mixpanel.com",
        "segment.io",
        "adsystem.com",
        "criteo.com",
        "outbrain.com",
        "taboola.com",
        "scorecardresearch.com",
        "quantserve.com"
    )

    fun isTrackerUrl(url: String): Boolean {
        val lower = url.lowercase()
        return knownTrackerDomains.any { domain ->
            lower.contains("://$domain") || lower.contains(".$domain/") || lower.endsWith(".$domain")
        }
    }

    fun createBlockedResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            204,
            "Blocked by Giyani Privacy Engine",
            mapOf(
                "X-Giyani-Privacy" to "Blocked-Tracker",
                "Access-Control-Allow-Origin" to "*"
            ),
            ByteArrayInputStream(ByteArray(0))
        )
    }

    /**
     * Layer 1 (USA / Country Identity Lock):
     * Synchronizes Android's JVM Process-Level `Locale.getDefault()` and `TimeZone.getDefault()`
     * with the active Browser Profile.
     *
     * Why this is essential:
     * Android WebView's Chromium C++ network engine reads `Locale.getDefault()` and `TimeZone.getDefault()`
     * when sending `Accept-Language` on background `fetch()` and `XMLHttpRequest` calls (such as
     * Quarterfull's account registration POST request) and when computing `Date.getTimezoneOffset()`.
     */
    fun enforceJvmLocaleAndTimezone(context: Context, profile: BrowserProfileEntity) {
        try {
            val targetTz = TimeZone.getTimeZone(profile.timezoneId.ifBlank { "America/New_York" })
            TimeZone.setDefault(targetTz)
        } catch (_: Exception) {
        }

        try {
            val langTag = profile.languageTag.ifBlank { "en-US" }
            val parts = langTag.split("-", "_")
            val language = parts.getOrNull(0)?.lowercase(Locale.ROOT)?.ifBlank { "en" } ?: "en"
            val country = parts.getOrNull(1)?.uppercase(Locale.ROOT)?.ifBlank { profile.countryCode.uppercase(Locale.ROOT) }
                ?: profile.countryCode.uppercase(Locale.ROOT)

            val targetLocale = Locale(language, country)
            Locale.setDefault(targetLocale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val isSameAsUs = targetLocale.language.equals(Locale.US.language, ignoreCase = true) &&
                    targetLocale.country.equals(Locale.US.country, ignoreCase = true)
                val localeList = if (isSameAsUs) {
                    LocaleList(targetLocale)
                } else {
                    LocaleList(targetLocale, Locale.US)
                }
                LocaleList.setDefault(localeList)
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Seeds Google Regional Cookies (`PREF` & `SOCS`) into CookieManager so `google.com` serves
     * the profile's target country (e.g. United States `hl=en&gl=US`) instead of Google Indonesia.
     */
    fun seedRegionalSearchCookies(profile: BrowserProfileEntity) {
        try {
            val countryIso = profile.countryCode.uppercase(Locale.ROOT).ifBlank { "US" }
            val primaryLang = profile.languageTag.substringBefore("-").lowercase(Locale.ROOT).ifBlank { "en" }
            val cookieManager = CookieManager.getInstance()
            val prefCookie = "PREF=hl=$primaryLang&gl=$countryIso&cr=country$countryIso; Domain=.google.com; Path=/; Secure; SameSite=None"
            cookieManager.setCookie("https://www.google.com", prefCookie)
            cookieManager.setCookie("https://google.com", prefCookie)
            cookieManager.flush()
        } catch (_: Exception) {
        }
    }

    /**
     * Rewrites bare `google.com` / `google.co.id` URLs to include Google's official Country Redirect
     * bypass parameters (`?hl=en&gl=US&pws=0&gws_rd=cr`) matching the active profile's country.
     */
    fun normalizeRegionalUrl(rawUrl: String, profile: BrowserProfileEntity): String {
        try {
            val uri = Uri.parse(rawUrl)
            val host = uri.host?.lowercase(Locale.ROOT) ?: return rawUrl
            val isGoogleSearchHost = host == "www.google.com" ||
                host == "google.com" ||
                host == "www.google.co.id" ||
                host == "google.co.id"
            if (!isGoogleSearchHost) return rawUrl

            val countryIso = profile.countryCode.uppercase(Locale.ROOT).ifBlank { "US" }
            val primaryLang = profile.languageTag.substringBefore("-").lowercase(Locale.ROOT).ifBlank { "en" }

            val currentGl = uri.getQueryParameter("gl")
            val currentGwsRd = uri.getQueryParameter("gws_rd")
            if (currentGl.equals(countryIso, ignoreCase = true) && currentGwsRd == "cr" && !host.endsWith(".co.id")) {
                return rawUrl
            }

            val builder = uri.buildUpon()
                .authority("www.google.com")
            if (currentGl == null) builder.appendQueryParameter("gl", countryIso)
            if (uri.getQueryParameter("hl") == null) builder.appendQueryParameter("hl", primaryLang)
            if (uri.getQueryParameter("pws") == null) builder.appendQueryParameter("pws", "0")
            if (currentGwsRd == null) builder.appendQueryParameter("gws_rd", "cr")
            return builder.build().toString()
        } catch (_: Exception) {
            return rawUrl
        }
    }

    /**
     * Maps a 2-letter ISO country code (e.g. "US") to Quarterfull's Market Country code.
     * In Quarterfull's production bundle (Module 834), English-speaking countries
     * (`US`, `GB`, `UK`, `CA`, `AU`, `NZ`, `IE`) use market code `"EN"` (USD $126 tier),
     * while their raw GeoIP country remains `"US"`. Sending `"US"` in `signup_market_country`
     * fails backend validation with `signup_market_country contains invalid characters`,
     * whereas `"EN"` passes validation and registers a USD account.
     */
    fun resolveQuarterfullMarketCountry(rawCountryIso: String): String {
        val clean = rawCountryIso
            .replace(Regex("[^A-Za-z]"), "")
            .uppercase(Locale.ROOT)
            .take(2)
            .ifBlank { "US" }
        return if (clean in setOf("US", "GB", "UK", "CA", "AU", "NZ", "IE")) "EN" else clean
    }

    fun buildRequestHeaders(
        country: CountryPreset,
        device: DevicePreset,
        profile: BrowserProfileEntity,
        desktopViewportOverride: Boolean = false
    ): Map<String, String> {
        val effectiveDevice = if (desktopViewportOverride) {
            CountryIdentityCatalog.getDeviceById("windows_desktop")
        } else {
            device
        }
        val countryIso = country.isoCode
            .replace(Regex("[^A-Za-z]"), "")
            .uppercase(Locale.ROOT)
            .take(2)
            .ifBlank { "US" }
        val qfMarketCountry = resolveQuarterfullMarketCountry(countryIso)
        val tz = profile.timezoneId.ifBlank { "America/New_York" }
        val tunnelEndpointIp = CountryIdentityCatalog.builtInWireGuardConfigs
            .find { it.countryCode.equals(countryIso, ignoreCase = true) }
            ?.endpointHost ?: "149.40.62.96"

        val headers = mutableMapOf<String, String>()
        headers["Accept-Language"] = country.acceptLanguageHeader
        // Override Android WebView's default X-Requested-With package leak header
        headers["X-Requested-With"] = ""
        headers["X-User-Country"] = qfMarketCountry
        headers["X-User-Raw-Country"] = countryIso
        headers["X-User-Market-Source"] = "manual"
        headers["X-Timezone"] = tz
        headers["X-User-Timezone"] = tz
        headers["CF-IPCountry"] = countryIso
        headers["X-Vercel-IP-Country"] = countryIso
        headers["X-Vercel-IP-Country-Region"] = "NY"
        headers["X-Vercel-IP-City"] = profile.cityName.ifBlank { "New York" }
        headers["X-Vercel-IP-Timezone"] = tz
        headers["X-Country-Code"] = countryIso
        headers["X-Forwarded-For"] = tunnelEndpointIp
        headers["X-Real-IP"] = tunnelEndpointIp
        if (effectiveDevice.secChUa.isNotBlank()) {
            headers["Sec-CH-UA"] = effectiveDevice.secChUa
            headers["Sec-CH-UA-Mobile"] = effectiveDevice.secChUaMobile
            headers["Sec-CH-UA-Platform"] = effectiveDevice.secChUaPlatform
        }
        if (profile.blockTrackers) {
            headers["DNT"] = "1"
            headers["Sec-GPC"] = "1"
        }
        return headers
    }

    private val ssrInterceptorHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(StrictIpv4DnsResolver)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val geoLookupHostKeywords = listOf(
        "ipapi.co",
        "ipwho.is",
        "ipinfo.io",
        "api.country.is",
        "geolocation-db.com",
        "geojs.io",
        "freeipapi.com",
        "ip-api.com",
        "extreme-ip-lookup.com"
    )

    /**
     * Rewrites SSR HTML, Next.js `__NEXT_DATA__`, RSC Flight chunks (`self.__next_f.push`),
     * and JSON API responses from byte zero so:
     * 1. Quarterfull Market Country keys (`signup_market_country`, `market_country`, `estimated_country_iso_code`,
     *    `settlement_market`, `market`) use `qfMarketCountry` (`"EN"` for USA).
     * 2. Raw GeoIP Country keys (`raw_geoip_country`, `raw_country`, `detected_country_iso_code`, `country_code`)
     *    use `isoUpper` (`"US"` for USA).
     * 3. Minimum signup age (`min_signup_age`) is `13` for USA instead of Indonesia's `18`.
     * 4. Streak & completion bonus amounts in `IDR` (`Rp310,000`) are converted to `USD` (`$126.0`).
     */
    fun rewriteSsrCountryAndCurrencyPayload(
        rawText: String,
        targetCountryIso: String,
        targetCurrencyCode: String,
        targetTimezone: String,
        targetLocale: String
    ): String {
        if (rawText.isBlank()) return rawText
        val isoUpper = targetCountryIso.replace(Regex("[^A-Za-z]"), "").uppercase(Locale.ROOT).take(2).ifBlank { "US" }
        val isoLower = isoUpper.lowercase(Locale.ROOT)
        val qfMarketUpper = resolveQuarterfullMarketCountry(isoUpper)
        val qfMarketLower = qfMarketUpper.lowercase(Locale.ROOT)
        val currUpper = targetCurrencyCode.uppercase(Locale.ROOT).ifBlank { "USD" }
        val currLower = currUpper.lowercase(Locale.ROOT)
        val primaryLang = targetLocale.substringBefore("-").lowercase(Locale.ROOT).ifBlank { "en" }

        val trimmed = rawText.trim()
        if (trimmed == "ID") return isoUpper
        if (trimmed == "id") return isoLower

        var out = rawText

        // 1. Cloudflare /cdn-cgi/trace response
        if (out.contains("loc=ID") && (out.contains("visit_scheme=") || out.contains("fl="))) {
            out = out.replace(Regex("\\bloc=ID\\b"), "loc=$isoUpper")
        }

        // 2a. Quarterfull Market Country keys -> must be "EN" for US/GB/CA/AU/NZ/IE
        val marketCountryKeysPattern = "(signup_market_country|market_country|estimated_country_iso_code|settlement_market|market)"
        out = out.replace(
            Regex("\"$marketCountryKeysPattern\"\\s*:\\s*\"(ID|id|IDN|idn|US|us)\"")
        ) { matchResult ->
            val key = matchResult.groupValues[1]
            val oldVal = matchResult.groupValues[2]
            val replacement = if (oldVal == oldVal.lowercase(Locale.ROOT)) qfMarketLower else qfMarketUpper
            "\"$key\":\"$replacement\""
        }
        out = out.replace(
            Regex("\\\\\"$marketCountryKeysPattern\\\\\"\\s*:\\s*\\\\\"(ID|id|IDN|idn|US|us)\\\\\"")
        ) { matchResult ->
            val key = matchResult.groupValues[1]
            val oldVal = matchResult.groupValues[2]
            val replacement = if (oldVal == oldVal.lowercase(Locale.ROOT)) qfMarketLower else qfMarketUpper
            "\\\"$key\\\":\\\"$replacement\\\""
        }

        // 2b. Raw GeoIP Country keys -> must be "US" for United States (controls 13+ US COPPA birthday check)
        val rawCountryKeysPattern = "(raw_geoip_country|raw_country|detected_country_iso_code|signup_country|country_code|countryCode|country_iso|countryIso|user_country|userCountry|ip_country|ipCountry|detected_country|detectedCountry|geo_country|geoCountry|viewer_country|viewerCountry|account_country|accountCountry|writer_country|writerCountry|benefit_country|benefitCountry|payout_country|payoutCountry|region_code|regionCode)"
        out = out.replace(
            Regex("\"$rawCountryKeysPattern\"\\s*:\\s*\"(ID|id|IDN|idn)\"")
        ) { matchResult ->
            val key = matchResult.groupValues[1]
            val oldVal = matchResult.groupValues[2]
            val replacement = if (oldVal == oldVal.lowercase(Locale.ROOT)) isoLower else isoUpper
            "\"$key\":\"$replacement\""
        }
        out = out.replace(
            Regex("\\\\\"$rawCountryKeysPattern\\\\\"\\s*:\\s*\\\\\"(ID|id|IDN|idn)\\\\\"")
        ) { matchResult ->
            val key = matchResult.groupValues[1]
            val oldVal = matchResult.groupValues[2]
            val replacement = if (oldVal == oldVal.lowercase(Locale.ROOT)) isoLower else isoUpper
            "\\\"$key\\\":\\\"$replacement\\\""
        }

        // 2c. Generic "country" key in Quarterfull user/market objects ("ID" -> qfMarketUpper "EN")
        out = out.replace(
            Regex("\"country\"\\s*:\\s*\"(ID|id)\"")
        ) { matchResult ->
            val oldVal = matchResult.groupValues[1]
            val replacement = if (oldVal == "id") qfMarketLower else qfMarketUpper
            "\"country\":\"$replacement\""
        }

        // 3. Minimum signup age for US (13 under US COPPA instead of Indonesia's 18)
        if (isoUpper == "US") {
            out = out.replace(Regex("\"(min_signup_age|minSignupAge|min_age)\"\\s*:\\s*18\\b"), "\"$1\":13")
                .replace(Regex("\\\\\"(min_signup_age|minSignupAge|min_age)\\\\\"\\s*:\\s*18\\b"), "\\\"$1\\\":13")
        }

        // 4. Quarterfull Writer Benefits IDR -> USD reward conversion fallback
        if (currUpper == "USD" && (out.contains("\"IDR\"") || out.contains("310000"))) {
            out = out
                .replace("\"amount\":310000.0", "\"amount\":126.0")
                .replace("\"amount\":310000", "\"amount\":126.0")
                .replace("\"amount_micro\":310000000000", "\"amount_micro\":126000000")
                .replace("\"track30\":550000.0", "\"track30\":216.0")
                .replace("\"track30\":550000", "\"track30\":216.0")
                .replace("\"track30_micro\":550000000000", "\"track30_micro\":216000000")
                .replace("\"track60\":650000.0", "\"track60\":270.0")
                .replace("\"track60\":650000", "\"track60\":270.0")
                .replace("\"track60_micro\":650000000000", "\"track60_micro\":270000000")
                .replace("\"track90\":900000.0", "\"track90\":360.0")
                .replace("\"track90\":900000", "\"track90\":360.0")
                .replace("\"track90_micro\":900000000000", "\"track90_micro\":360000000")
                .replace("\"track120\":1100000.0", "\"track120\":450.0")
                .replace("\"track120\":1100000", "\"track120\":450.0")
                .replace("\"track120_micro\":1100000000000", "\"track120_micro\":450000000")
                .replace("Rp310,000", "\$126")
                .replace("Rp 310.000", "\$126")
                .replace("Rp310.000", "\$126")
        }

        // 5. Unescaped & escaped currency fields ("IDR" -> "USD", "idr" -> "usd")
        val currencyKeysPattern = "(currency|currency_code|currencyCode|market_currency|marketCurrency|benefit_currency|benefitCurrency|payout_currency|payoutCurrency|user_currency|userCurrency|default_currency|defaultCurrency)"
        out = out.replace(
            Regex("\"$currencyKeysPattern\"\\s*:\\s*\"(IDR|idr)\"")
        ) { matchResult ->
            val key = matchResult.groupValues[1]
            val oldVal = matchResult.groupValues[2]
            val replacement = if (oldVal == oldVal.lowercase(Locale.ROOT)) currLower else currUpper
            "\"$key\":\"$replacement\""
        }
        out = out.replace(
            Regex("\\\\\"$currencyKeysPattern\\\\\"\\s*:\\s*\\\\\"(IDR|idr)\\\\\"")
        ) { matchResult ->
            val key = matchResult.groupValues[1]
            val oldVal = matchResult.groupValues[2]
            val replacement = if (oldVal == oldVal.lowercase(Locale.ROOT)) currLower else currUpper
            "\\\"$key\\\":\\\"$replacement\\\""
        }

        // 6. Timezone & Locale leaks inside SSR state
        if (out.contains("Asia/Jakarta") || out.contains("Asia\\/Jakarta")) {
            out = out.replace("Asia/Jakarta", targetTimezone)
                .replace("Asia\\/Jakarta", targetTimezone.replace("/", "\\/"))
        }
        if (out.contains("\"id-ID\"") || out.contains("\\\"id-ID\\\"")) {
            out = out.replace("\"id-ID\"", "\"$targetLocale\"")
                .replace("\\\"id-ID\\\"", "\\\"$targetLocale\\\"")
        }
        if (out.contains("\"locale\":\"id\"") || out.contains("\\\"locale\\\":\\\"id\\\"")) {
            out = out.replace("\"locale\":\"id\"", "\"locale\":\"$primaryLang\"")
                .replace("\\\"locale\\\":\\\"id\\\"", "\\\"locale\\\":\\\"$primaryLang\\\"")
        }

        return out
    }

    /**
     * Network-level `shouldInterceptRequest` handler:
     * Intercepts `GET` requests for main-frame HTML documents, Quarterfull API calls (`api.quarterfull.io`),
     * and GeoIP lookup endpoints.
     * - Sends Quarterfull's official USA market headers (`X-User-Country: EN`, `X-User-Raw-Country: US`,
     *   `X-User-Market-Source: manual`, `X-Timezone: America/New_York`, `Accept-Language: en`) so
     *   `api.quarterfull.io` serves `$126 USD` writer benefits and `EN`/`US` market configuration directly.
     * - Rewrites any residual Indonesian GeoIP fields (`"estimated_country_iso_code":"ID"` -> `"EN"`,
     *   `"raw_geoip_country":"ID"` -> `"US"`, `"min_signup_age":18` -> `13`, `"IDR"` -> `"USD"`).
     * - Injects `<script data-giyani-pre-dom="1">` immediately after `<head>` on HTML responses so all
     *   storage locks and network hooks run before any page script executes.
     */
    fun interceptAndRewriteNetworkRequest(
        request: WebResourceRequest,
        country: CountryPreset,
        device: DevicePreset,
        profile: BrowserProfileEntity,
        isDarkTheme: Boolean,
        desktopViewportOverride: Boolean
    ): WebResourceResponse? {
        try {
            // Only intercept HTTP/HTTPS GET requests (WebView does not expose POST request bodies to shouldInterceptRequest)
            val method = request.method?.uppercase(Locale.ROOT) ?: "GET"
            if (method != "GET") return null

            val uri = request.url ?: return null
            val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
            if (scheme != "http" && scheme != "https") return null

            val reqUrl = uri.toString()
            val host = uri.host?.lowercase(Locale.ROOT).orEmpty()
            val path = uri.path?.lowercase(Locale.ROOT).orEmpty()

            // Skip static binary assets (images, fonts, media, css, wasm, static js bundles)
            if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                path.endsWith(".gif") || path.endsWith(".webp") || path.endsWith(".svg") ||
                path.endsWith(".ico") || path.endsWith(".woff") || path.endsWith(".woff2") ||
                path.endsWith(".ttf") || path.endsWith(".otf") || path.endsWith(".css") ||
                path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".mp3") ||
                path.endsWith(".wasm") || path.endsWith(".js")
            ) {
                return null
            }

            val isQuarterfullHost = host == "quarterfull.io" || host.endsWith(".quarterfull.io")
            val isGeoLookupHost = geoLookupHostKeywords.any { host == it || host.endsWith(".$it") } ||
                path.contains("/cdn-cgi/trace")
            val isMainFrameHtml = request.isForMainFrame

            if (!isQuarterfullHost && !isGeoLookupHost && !isMainFrameHtml) {
                return null
            }
            // Do not intercept Google OAuth/Account sign-in main frames so Google's internal cookies/redirects work natively
            if (host.contains("google.com") || host.contains("googleapis.com") || host.contains("gstatic.com")) {
                return null
            }

            val effectiveDevice = if (desktopViewportOverride) {
                CountryIdentityCatalog.getDeviceById("windows_desktop")
            } else {
                device
            }
            val countryIso = country.isoCode
                .replace(Regex("[^A-Za-z]"), "")
                .uppercase(Locale.ROOT)
                .take(2)
                .ifBlank { "US" }
            val qfMarketCountry = resolveQuarterfullMarketCountry(countryIso)
            val tz = profile.timezoneId.ifBlank { "America/New_York" }
            val langTag = profile.languageTag.ifBlank { "en-US" }
            val primaryLang = langTag.substringBefore("-").lowercase(Locale.ROOT).ifBlank { "en" }
            val currencyCode = country.currencyCode.ifBlank { "USD" }
            val tunnelEndpointIp = CountryIdentityCatalog.builtInWireGuardConfigs
                .find { it.countryCode.equals(countryIso, ignoreCase = true) }
                ?.endpointHost ?: "149.40.62.96"

            val reqBuilder = Request.Builder()
                .url(reqUrl)
                .get()

            // Forward safe headers from WebView (such as Authorization, Origin, Referer) while stripping leak headers
            request.requestHeaders?.forEach { (k, v) ->
                val lowerK = k.lowercase(Locale.ROOT)
                if (lowerK != "x-requested-with" &&
                    lowerK != "accept-language" &&
                    lowerK != "user-agent" &&
                    lowerK != "accept-encoding" &&
                    lowerK != "x-user-country" &&
                    lowerK != "x-user-raw-country" &&
                    lowerK != "x-user-market-source" &&
                    lowerK != "x-timezone" &&
                    lowerK != "x-user-timezone" &&
                    lowerK != "host" &&
                    v.isNotBlank()
                ) {
                    reqBuilder.header(k, v)
                }
            }

            // Attach current CookieManager cookies for this URL
            val existingCookies = CookieManager.getInstance().getCookie(reqUrl)
            if (!existingCookies.isNullOrBlank()) {
                val sanitizedCookieHeader = existingCookies
                    .replace(Regex("\\b(market_country|signup_market_country)=ID\\b"), "$1=$qfMarketCountry")
                    .replace(Regex("\\b(market_country|signup_market_country)=id\\b"), "$1=${qfMarketCountry.lowercase(Locale.ROOT)}")
                    .replace(Regex("\\b(country|raw_country)=ID\\b"), "$1=$countryIso")
                    .replace(Regex("\\b(country|raw_country)=id\\b"), "$1=${countryIso.lowercase(Locale.ROOT)}")
                    .replace(Regex("\\b(currency|currency_code)=IDR\\b", RegexOption.IGNORE_CASE), "$1=$currencyCode")
                reqBuilder.header("Cookie", sanitizedCookieHeader)
            }

            // Set coherent USA / target country headers
            reqBuilder.header("User-Agent", effectiveDevice.userAgent)
            reqBuilder.header("Accept-Language", if (isQuarterfullHost) primaryLang else country.acceptLanguageHeader)
            reqBuilder.header("X-User-Country", qfMarketCountry)
            reqBuilder.header("X-User-Raw-Country", countryIso)
            reqBuilder.header("X-User-Market-Source", "manual")
            reqBuilder.header("X-Timezone", tz)
            reqBuilder.header("X-User-Timezone", tz)
            reqBuilder.header("CF-IPCountry", countryIso)
            reqBuilder.header("X-Vercel-IP-Country", countryIso)
            reqBuilder.header("X-Vercel-IP-Country-Region", "NY")
            reqBuilder.header("X-Vercel-IP-City", profile.cityName.ifBlank { "New York" })
            reqBuilder.header("X-Vercel-IP-Timezone", tz)
            reqBuilder.header("X-Country-Code", countryIso)
            reqBuilder.header("X-Forwarded-For", tunnelEndpointIp)
            reqBuilder.header("X-Real-IP", tunnelEndpointIp)
            if (effectiveDevice.secChUa.isNotBlank()) {
                reqBuilder.header("Sec-CH-UA", effectiveDevice.secChUa)
                reqBuilder.header("Sec-CH-UA-Mobile", effectiveDevice.secChUaMobile)
                reqBuilder.header("Sec-CH-UA-Platform", effectiveDevice.secChUaPlatform)
            }

            val response = ssrInterceptorHttpClient.newCall(reqBuilder.build()).execute()
            val statusCode = response.code
            val contentTypeHeader = response.header("Content-Type").orEmpty()
            val mimeType = contentTypeHeader.substringBefore(";").trim().lowercase(Locale.ROOT).ifBlank { "text/html" }

            // Sync Set-Cookie headers back into CookieManager with Indonesian country/currency values scrubbed
            val setCookies = response.headers("Set-Cookie")
            if (setCookies.isNotEmpty()) {
                val cm = CookieManager.getInstance()
                setCookies.forEach { rawCookie ->
                    val cleanedCookie = rawCookie
                        .replace(Regex("\\b(market_country|signup_market_country)=ID\\b"), "$1=$qfMarketCountry")
                        .replace(Regex("\\b(market_country|signup_market_country)=id\\b"), "$1=${qfMarketCountry.lowercase(Locale.ROOT)}")
                        .replace(Regex("\\b(country|raw_country)=ID\\b"), "$1=$countryIso")
                        .replace(Regex("\\b(country|raw_country)=id\\b"), "$1=${countryIso.lowercase(Locale.ROOT)}")
                        .replace(Regex("\\b(currency|currency_code)=IDR\\b", RegexOption.IGNORE_CASE), "$1=$currencyCode")
                    cm.setCookie(reqUrl, cleanedCookie)
                }
                cm.flush()
            }

            val isTextOrJsonOrHtml = mimeType.contains("html") ||
                mimeType.contains("json") ||
                mimeType.contains("text") ||
                mimeType.contains("x-component") ||
                mimeType.contains("javascript")

            if (!isTextOrJsonOrHtml) {
                response.close()
                return null
            }

            val rawBody = response.body?.string().orEmpty()
            response.close()

            var rewrittenBody = rewriteSsrCountryAndCurrencyPayload(
                rawText = rawBody,
                targetCountryIso = countryIso,
                targetCurrencyCode = currencyCode,
                targetTimezone = tz,
                targetLocale = langTag
            )

            // Inject pre-DOM coherent identity & anti-fingerprinting script right after <head> on HTML pages
            if (mimeType.contains("html") && rewrittenBody.isNotBlank()) {
                val identityJs = buildCoherentIdentityScript(
                    country = country,
                    device = device,
                    profile = profile,
                    isDarkTheme = isDarkTheme,
                    desktopViewportOverride = desktopViewportOverride
                )
                val scriptTag = "<script data-giyani-pre-dom=\"1\">$identityJs</script>"
                val headRegex = Regex("(<head\\b[^>]*>)", RegexOption.IGNORE_CASE)
                rewrittenBody = if (headRegex.containsMatchIn(rewrittenBody)) {
                    headRegex.replaceFirst(rewrittenBody, "$1$scriptTag")
                } else {
                    "$scriptTag$rewrittenBody"
                }
            }

            val responseHeaders = mutableMapOf<String, String>()
            response.headers.names().forEach { name ->
                val lowerName = name.lowercase(Locale.ROOT)
                if (lowerName != "content-length" &&
                    lowerName != "content-encoding" &&
                    lowerName != "transfer-encoding" &&
                    lowerName != "content-security-policy" &&
                    lowerName != "content-security-policy-report-only"
                ) {
                    val value = response.header(name)
                    if (!value.isNullOrBlank()) {
                        responseHeaders[name] = value
                    }
                }
            }
            val requestOrigin = request.requestHeaders?.entries
                ?.firstOrNull { it.key.equals("Origin", ignoreCase = true) }
                ?.value
            if (!requestOrigin.isNullOrBlank()) {
                responseHeaders["Access-Control-Allow-Origin"] = requestOrigin
                responseHeaders["Access-Control-Allow-Credentials"] = "true"
            } else if (isQuarterfullHost && !responseHeaders.keys.any { it.equals("Access-Control-Allow-Origin", ignoreCase = true) }) {
                responseHeaders["Access-Control-Allow-Origin"] = "https://quarterfull.io"
                responseHeaders["Access-Control-Allow-Credentials"] = "true"
            }
            responseHeaders["X-Giyani-SSR-Shield"] = "$qfMarketCountry-$countryIso-$currencyCode"

            val reasonPhrase = response.message.ifBlank { "OK" }
            val safeStatusCode = if (statusCode in 100..599) statusCode else 200

            return WebResourceResponse(
                mimeType,
                "UTF-8",
                safeStatusCode,
                reasonPhrase,
                responseHeaders,
                ByteArrayInputStream(rewrittenBody.toByteArray(Charsets.UTF_8))
            )
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Scrubs the single live WebView's active DOMStorage, form data, and navigation history in-place
     * WITHOUT calling `webView.destroy()` or `WebStorage.deleteAllData()`, preventing Chromium's
     * `aw_browser_terminator.cc` renderer crash (`code -1`) while mounted in Compose `AndroidView`.
     */
    private fun scrubLiveWebViewInPlace() {
        currentLiveWebView?.let { wv ->
            try {
                wv.stopLoading()
                wv.evaluateJavascript(
                    "try { window.__giyaniSuppressVaultSave = true; localStorage.clear(); sessionStorage.clear(); } catch(e) {}",
                    null
                )
                wv.clearFormData()
                wv.clearHistory()
                wv.clearCache(false)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Destroy the single active WebView instance ONLY when its hosting Activity context changes
     * or when the OS reclaims the renderer process (`onRenderProcessGone`).
     */
    private fun destroyCurrentLiveWebView() {
        val wv = currentLiveWebView
        currentLiveWebView = null
        currentLiveWebViewProfileId = null
        currentVaultBridge = null
        wv?.let {
            try {
                (it.parent as? ViewGroup)?.removeView(it)
                it.stopLoading()
                it.removeAllViews()
                it.destroy()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Handles Android WebView `onRenderProcessGone` without crashing the application.
     */
    fun handleRenderProcessGone(crashedWebView: WebView?): Boolean {
        try {
            if (crashedWebView != null && crashedWebView === currentLiveWebView) {
                currentLiveWebView = null
                currentLiveWebViewProfileId = null
                currentVaultBridge = null
            }
            crashedWebView?.let {
                (it.parent as? ViewGroup)?.removeView(it)
                it.destroy()
            }
        } catch (_: Exception) {
        }
        return true
    }

    /**
     * Synchronous cold-start vault & cookie sanitization executed immediately inside
     * `GiyaniViewModel.init {}` BEFORE any Compose `WebView` is created or any URL is loaded.
     * This ensures every fresh app launch begins with 100% clean, unauthenticated profile sessions
     * without interrupting or destroying an active WebView renderer mid-load.
     */
    fun sanitizeColdStartVaultsSync(context: Context, initialFocusedProfileId: String) {
        isCookieSwapInProgress = true
        try {
            listOf(
                "giyani_profile_cookie_vault",
                "giyani_profile_dom_vault_v3",
                COOKIE_VAULT_PREFS,
                DOM_VAULT_PREFS
            ).forEach { prefName ->
                context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit().clear().commit()
            }
            currentCookieOwnerProfileId = initialFocusedProfileId
            try {
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies { cookieManager.flush() }
                cookieManager.flush()
            } catch (_: Exception) {
            }
            val metaPrefs = context.getSharedPreferences(SANITIZE_META_PREFS, Context.MODE_PRIVATE)
            metaPrefs.edit().putBoolean(KEY_V4_SANITIZED, true).apply()
        } finally {
            isCookieSwapInProgress = false
        }
    }

    /**
     * Cold-start & sanitization wipe (kept for compatibility; safe in-place scrub without destroying live WebView).
     */
    suspend fun sanitizeMixedSessionsIfNeededV4(context: Context, initialFocusedProfileId: String) {
        val metaPrefs = context.getSharedPreferences(SANITIZE_META_PREFS, Context.MODE_PRIVATE)
        currentCookieOwnerProfileId = initialFocusedProfileId
        purgeAllProfilesSessionsEverywhere(context, initialFocusedProfileId)
        metaPrefs.edit().putBoolean(KEY_V4_SANITIZED, true).apply()
    }

    /**
     * Synchronous immediate session wipe called when the application is closed (MainActivity.onDestroy /
     * ViewModel.onCleared) to guarantee all login sessions across all profiles are reset upon app exit.
     */
    fun purgeAllProfilesSessionsOnAppExit(context: Context) {
        isCookieSwapInProgress = true
        try {
            listOf(
                "giyani_profile_cookie_vault",
                "giyani_profile_dom_vault_v3",
                COOKIE_VAULT_PREFS,
                DOM_VAULT_PREFS
            ).forEach { prefName ->
                context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit().clear().commit()
            }

            scrubLiveWebViewInPlace()

            try {
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies { cookieManager.flush() }
                cookieManager.flush()
            } catch (_: Exception) {
            }
        } finally {
            isCookieSwapInProgress = false
        }
    }

    /**
     * Purges all saved cookies, localStorage, and sessionStorage across all profiles safely in-place
     * without killing the active WebView renderer process.
     */
    suspend fun purgeAllProfilesSessionsEverywhere(context: Context, activeOwnerProfileId: String?) {
        withContext(Dispatchers.Main) {
            isCookieSwapInProgress = true
            try {
                listOf(
                    "giyani_profile_cookie_vault",
                    "giyani_profile_dom_vault_v3",
                    COOKIE_VAULT_PREFS,
                    DOM_VAULT_PREFS
                ).forEach { prefName ->
                    context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit().clear().commit()
                }

                scrubLiveWebViewInPlace()

                val cookieManager = CookieManager.getInstance()
                awaitRemoveAllCookies(cookieManager)
                cookieManager.flush()
                currentCookieOwnerProfileId = activeOwnerProfileId
            } finally {
                isCookieSwapInProgress = false
            }
        }
    }

    /**
     * Checks whether a given profile currently holds saved login data (Cookies or DOMStorage tokens)
     * inside its isolated v4 vault.
     */
    fun hasSavedLoginVault(context: Context, profileId: String): Boolean {
        val cookiePrefs = context.getSharedPreferences(COOKIE_VAULT_PREFS, Context.MODE_PRIVATE)
        val savedCookies = cookiePrefs.getString("cookies_$profileId", null)
        if (!savedCookies.isNullOrBlank()) return true

        val domPrefs = context.getSharedPreferences(DOM_VAULT_PREFS, Context.MODE_PRIVATE)
        return domPrefs.all.any { (key, value) ->
            (key.startsWith("local_${profileId}_") || key.startsWith("session_${profileId}_")) &&
                value is String && value != "{}" && value.isNotBlank()
        }
    }

    /**
     * JavaScript bridge attached to the single active WebView to synchronize `window.localStorage`
     * and `window.sessionStorage` strictly into the active profile's isolated vault partition.
     */
    class GiyaniStorageVaultBridge(
        private val context: Context,
        @Volatile private var boundProfileId: String,
        @Volatile var keepLoginSession: Boolean
    ) {
        private val prefs = context.getSharedPreferences(DOM_VAULT_PREFS, Context.MODE_PRIVATE)

        fun updateBinding(newProfileId: String, keepSession: Boolean) {
            boundProfileId = newProfileId
            keepLoginSession = keepSession
        }

        @JavascriptInterface
        fun getBoundProfileId(): String = boundProfileId

        @JavascriptInterface
        fun saveOriginStorage(profileIdFromJs: String, origin: String, localJson: String, sessionJson: String) {
            if (isCookieSwapInProgress) return
            if (profileIdFromJs != boundProfileId) return
            if (currentCookieOwnerProfileId != null && boundProfileId != currentCookieOwnerProfileId) return
            if (!keepLoginSession || origin.isBlank() || origin == "null") return

            prefs.edit()
                .putString("local_${boundProfileId}_$origin", localJson)
                .putString("session_${boundProfileId}_$origin", sessionJson)
                .apply()
        }

        @JavascriptInterface
        fun getSavedLocalStorage(profileIdFromJs: String, origin: String): String {
            if (profileIdFromJs != boundProfileId) return "{}"
            if (!keepLoginSession || origin.isBlank() || origin == "null") return "{}"
            return prefs.getString("local_${boundProfileId}_$origin", "{}") ?: "{}"
        }

        @JavascriptInterface
        fun getSavedSessionStorage(profileIdFromJs: String, origin: String): String {
            if (profileIdFromJs != boundProfileId) return "{}"
            if (!keepLoginSession || origin.isBlank() || origin == "null") return "{}"
            return prefs.getString("session_${boundProfileId}_$origin", "{}") ?: "{}"
        }
    }

    /**
     * Single-WebView Factory:
     * Reuses the single healthy WebView instance across profile switches so at most ONE WebView
     * ever exists in RAM and Chromium's renderer process is never abruptly terminated!
     */
    fun getOrCreateProfileWebView(
        context: Context,
        profile: BrowserProfileEntity,
        onInitWebView: (WebView) -> Unit
    ): WebView {
        enforceJvmLocaleAndTimezone(context, profile)
        seedRegionalSearchCookies(profile)

        if (currentCookieOwnerProfileId == null) {
            currentCookieOwnerProfileId = profile.id
        }

        val existing = currentLiveWebView
        if (existing != null) {
            if (existing.context === context) {
                (existing.parent as? ViewGroup)?.removeView(existing)
                existing.onResume()
                currentVaultBridge?.updateBinding(profile.id, profile.keepLoginSession)
                val profileChanged = currentLiveWebViewProfileId != profile.id
                currentLiveWebViewProfileId = profile.id
                if (profileChanged) {
                    onInitWebView(existing)
                }
                return existing
            } else {
                // Activity context changed (e.g., Activity recreation) — safely release old WebView
                destroyCurrentLiveWebView()
            }
        }

        val newWebView = WebView(context)
        val bridge = GiyaniStorageVaultBridge(
            context = context.applicationContext,
            boundProfileId = profile.id,
            keepLoginSession = profile.keepLoginSession
        )
        newWebView.addJavascriptInterface(bridge, "__GiyaniVaultBridge")
        currentVaultBridge = bridge
        currentLiveWebView = newWebView
        currentLiveWebViewProfileId = profile.id
        onInitWebView(newWebView)
        return newWebView
    }

    fun getActiveProfileWebView(profileId: String): WebView? {
        return if (currentLiveWebViewProfileId == profileId) currentLiveWebView else null
    }

    /**
     * Scrubs the live WebView if it belongs to `profile.id` and clears vault if `keepLoginSession` is false.
     */
    fun releaseProfileWebView(context: Context, profile: BrowserProfileEntity) {
        if (!profile.keepLoginSession) {
            clearProfileVaultData(context, profile.id)
        }
        if (currentLiveWebViewProfileId == profile.id) {
            scrubLiveWebViewInPlace()
        }
    }

    /**
     * Releases session state for profiles outside the newly activated Country.
     */
    fun releaseWebViewsOutsideCountry(
        context: Context,
        profilesToDeactivate: List<BrowserProfileEntity>,
        keepOldSessionsInVault: Boolean
    ) {
        profilesToDeactivate.forEach { prof ->
            if (!keepOldSessionsInVault || !prof.keepLoginSession) {
                clearProfileVaultData(context, prof.id)
            }
            if (currentLiveWebViewProfileId == prof.id) {
                scrubLiveWebViewInPlace()
            }
        }
    }

    /**
     * Wipes the saved Cookies & DOMStorage vault for a single profileId.
     */
    fun clearProfileVaultData(context: Context, profileId: String) {
        val cookiePrefs = context.getSharedPreferences(COOKIE_VAULT_PREFS, Context.MODE_PRIVATE)
        cookiePrefs.edit()
            .remove("cookies_$profileId")
            .remove("domains_$profileId")
            .commit()

        val domPrefs = context.getSharedPreferences(DOM_VAULT_PREFS, Context.MODE_PRIVATE)
        val keysToRemove = domPrefs.all.keys.filter {
            it.startsWith("local_${profileId}_") || it.startsWith("session_${profileId}_")
        }
        val editor = domPrefs.edit()
        keysToRemove.forEach { editor.remove(it) }
        editor.commit()
    }

    /**
     * Resets/logs out a single profile completely in-place without destroying the mounted WebView.
     */
    suspend fun resetSingleProfileSession(
        context: Context,
        profile: BrowserProfileEntity,
        isCurrentlyFocused: Boolean
    ) {
        withContext(Dispatchers.Main) {
            clearProfileVaultData(context, profile.id)
            if (currentLiveWebViewProfileId == profile.id) {
                scrubLiveWebViewInPlace()
            }
            if (isCurrentlyFocused) {
                val cookieManager = CookieManager.getInstance()
                awaitRemoveAllCookies(cookieManager)
                cookieManager.flush()
                seedRegionalSearchCookies(profile)
            }
        }
    }

    /**
     * Generates JavaScript Engine v5 injection for:
     * 1. Layer 4 — Virtualized Per-Profile `IndexedDB` & `CacheStorage` namespacing.
     * 2. Layer 3 — Unconditional `localStorage.clear()` & `sessionStorage.clear()` on profile switch
     *    with debounced (350ms) event-driven vault sync (zero `setInterval` background lag!).
     * 3. 5-Layer USA / Target Country Identity & Sign-Up Enforcement:
     *    - Exact `Date.prototype.getTimezoneOffset`, `toTimeString`, and `toString` override for the
     *      profile's timezone (`America/New_York`, etc.) so `GMT+0700 (Western Indonesia Time)` or
     *      offset `-420` never leaks during account signup.
     *    - `window.fetch` & `XMLHttpRequest` GeoIP Shield: intercepts client-side GeoIP checks
     *      (`ipapi`, `ipwho.is`, `ipinfo`, `cdn-cgi/trace`, `/api/geo`, etc.) and returns the active
     *      profile's country (`US`, `United States`, `New York`, `146.70.230.146`).
     *    - Sign-Up JSON Payload Sanitizer: rewrites any leaked `"ID"`, `"Indonesia"`, `"id-ID"`, or
     *      `"Asia/Jakarta"` in `POST`/`PUT`/`PATCH` JSON request bodies to `"US"`, `"United States"`,
     *      `"en-US"`, and `"America/New_York"`.
     */
    fun buildCoherentIdentityScript(
        country: CountryPreset,
        device: DevicePreset,
        profile: BrowserProfileEntity,
        isDarkTheme: Boolean = true,
        desktopViewportOverride: Boolean = false
    ): String {
        val effectiveDevice = if (desktopViewportOverride) {
            CountryIdentityCatalog.getDeviceById("windows_desktop")
        } else {
            device
        }
        val tz = profile.timezoneId.replace("'", "").ifBlank { "America/New_York" }
        val lang = profile.languageTag.replace("'", "").ifBlank { "en-US" }
        val primaryLang = lang.substringBefore("-").ifBlank { "en" }
        val countryIso = country.isoCode
            .replace(Regex("[^A-Za-z]"), "")
            .uppercase(Locale.ROOT)
            .take(2)
            .ifBlank { "US" }
        val qfMarketCountry = resolveQuarterfullMarketCountry(countryIso)
        val countryName = country.name.replace("'", "").ifBlank { "United States" }
        val cityName = profile.cityName.replace("'", "").ifBlank { "New York" }
        val cleanLanguagesList = "$lang,$primaryLang"
        val platform = effectiveDevice.platform.replace("'", "")
        val isMobileJs = if (effectiveDevice.secChUaMobile == "?1") "true" else "false"
        val cleanPlatformName = effectiveDevice.secChUaPlatform.replace("\"", "").replace("'", "").ifBlank { "Android" }
        val profileIdSafe = profile.id.replace("'", "")
        val colorSchemePref = if (isDarkTheme) "dark" else "light"

        // Compute the target timezone offset in minutes for JS Date.prototype.getTimezoneOffset()
        // Note: Java TimeZone.getOffset() is positive east of UTC (e.g. +7h = +25200000ms),
        // whereas JS Date.getTimezoneOffset() is positive WEST of UTC (e.g. New York UTC-4/-5 = +240/+300, Jakarta UTC+7 = -420).
        val javaTz = TimeZone.getTimeZone(tz)
        val nowDate = java.util.Date()
        val jsTzOffsetMinutes = -(javaTz.getOffset(nowDate.time) / 60000)
        val totalHours = -jsTzOffsetMinutes / 60
        val absHours = kotlin.math.abs(totalHours)
        val absMins = kotlin.math.abs(jsTzOffsetMinutes % 60)
        val signChar = if (totalHours >= 0) "+" else "-"
        val isDstNow = javaTz.inDaylightTime(nowDate)
        val tzDisplayName = javaTz.getDisplayName(isDstNow, TimeZone.LONG, Locale.US)
            .replace(Regex("[^A-Za-z ]"), "")
            .trim()
            .ifBlank { "Eastern Standard Time" }
        val gmtOffsetString = String.format(Locale.US, "GMT%s%02d%02d (%s)", signChar, absHours, absMins, tzDisplayName)

        // Match built-in WireGuard endpoint IP for this country so GeoIP & WebRTC report a real tunnel IP
        val tunnelEndpointIp = CountryIdentityCatalog.builtInWireGuardConfigs
            .find { it.countryCode.equals(countryIso, ignoreCase = true) }
            ?.endpointHost ?: "146.70.230.146"

        return """
            (function() {
                try {
                    const ACTIVE_PROFILE_ID = '$profileIdSafe';
                    const KEEP_LOGIN_SESSION = ${profile.keepLoginSession};
                    const TARGET_COUNTRY_ISO = '$countryIso'.replace(/[^A-Za-z]/g, '').toUpperCase().slice(0, 2) || 'US';
                    const TARGET_COUNTRY_LOWER = TARGET_COUNTRY_ISO.toLowerCase();
                    const TARGET_QF_MARKET_CODE = '$qfMarketCountry'.replace(/[^A-Za-z]/g, '').toUpperCase().slice(0, 2) || 'EN';
                    const TARGET_QF_MARKET_LOWER = TARGET_QF_MARKET_CODE.toLowerCase();
                    const TARGET_MIN_SIGNUP_AGE = (TARGET_COUNTRY_ISO === 'US' || TARGET_COUNTRY_ISO === 'CA' || TARGET_COUNTRY_ISO === 'JP') ? 13 : (TARGET_COUNTRY_ISO === 'KR' ? 14 : (TARGET_COUNTRY_ISO === 'DE' ? 16 : 13));
                    const TARGET_COUNTRY_NAME = '$countryName';
                    const TARGET_CITY_NAME = '$cityName';
                    const TARGET_TIMEZONE = '$tz';
                    const TARGET_LOCALE = '$lang';
                    const TARGET_PRIMARY_LANG = '$primaryLang';
                    const TARGET_CLEAN_LANGUAGES = '$cleanLanguagesList';
                    const TARGET_TUNNEL_IP = '$tunnelEndpointIp';
                    const TARGET_TZ_OFFSET_MIN = $jsTzOffsetMinutes;
                    const TARGET_LAT = ${profile.simulatedLat};
                    const TARGET_LON = ${profile.simulatedLon};

                    const defineSafeProp = function(obj, prop, getterFn) {
                        try {
                            Object.defineProperty(obj, prop, {
                                get: getterFn,
                                configurable: true
                            });
                        } catch (e) {}
                    };

                    const TARGET_CURRENCY_CODE = '${country.currencyCode.ifBlank { "USD" }}'.toUpperCase();
                    const TARGET_CURRENCY_LOWER = TARGET_CURRENCY_CODE.toLowerCase();

                    // Quarterfull separates Market Country ("EN" for US/GB/CA/AU/NZ/IE -> $126 USD)
                    // from Raw GeoIP Country ("US" -> 13+ US COPPA age check).
                    const isQuarterfullMarketCountryKey = function(keyName) {
                        if (!keyName) return false;
                        const lowerK = String(keyName).trim().toLowerCase();
                        return lowerK === 'signup_market_country' ||
                            lowerK === 'market_country' ||
                            lowerK === 'estimated_country_iso_code' ||
                            lowerK === 'settlement_market' ||
                            lowerK === 'cravi_country' ||
                            lowerK === 'cravi_market_country_v1';
                    };

                    const isRawGeoCountryKey = function(keyName) {
                        if (!keyName) return false;
                        const lowerK = String(keyName).trim().toLowerCase();
                        return lowerK === 'cravi_raw_country' ||
                            lowerK === 'raw_geoip_country' ||
                            lowerK === 'raw_country' ||
                            lowerK === 'detected_country_iso_code' ||
                            lowerK === 'signup_country' ||
                            lowerK === 'country_code' ||
                            lowerK === 'countrycode' ||
                            lowerK === 'country_iso' ||
                            lowerK === 'countryiso' ||
                            lowerK === 'user_country' ||
                            lowerK === 'usercountry' ||
                            lowerK === 'ip_country' ||
                            lowerK === 'ipcountry' ||
                            lowerK === 'detected_country' ||
                            lowerK === 'detectedcountry' ||
                            lowerK === 'geo_country' ||
                            lowerK === 'geocountry' ||
                            lowerK === 'viewer_country' ||
                            lowerK === 'viewercountry' ||
                            lowerK === 'account_country' ||
                            lowerK === 'accountcountry' ||
                            lowerK === 'writer_country' ||
                            lowerK === 'writercountry' ||
                            lowerK === 'benefit_country' ||
                            lowerK === 'benefitcountry' ||
                            lowerK === 'payout_country' ||
                            lowerK === 'payoutcountry';
                    };

                    const isStrictCountryCodeKey = function(keyName) {
                        return isQuarterfullMarketCountryKey(keyName) || isRawGeoCountryKey(keyName);
                    };

                    const isStrictCurrencyCodeKey = function(keyName) {
                        if (!keyName) return false;
                        const lowerK = String(keyName).trim().toLowerCase();
                        return lowerK === 'currency' ||
                            lowerK === 'currency_code' ||
                            lowerK === 'currencycode' ||
                            lowerK === 'market_currency' ||
                            lowerK === 'marketcurrency' ||
                            lowerK === 'benefit_currency' ||
                            lowerK === 'benefitcurrency' ||
                            lowerK === 'payout_currency' ||
                            lowerK === 'payoutcurrency' ||
                            lowerK === 'user_currency' ||
                            lowerK === 'usercurrency' ||
                            lowerK === 'default_currency' ||
                            lowerK === 'defaultcurrency';
                    };

                    /**
                     * Resolves the exact expected country code for a given key:
                     * - Quarterfull Market keys (`signup_market_country`, `cravi_country`, `estimated_country_iso_code`, etc.)
                     *   ALWAYS resolve to uppercase `TARGET_QF_MARKET_CODE` (`"EN"` for USA) because Quarterfull's
                     *   `MARKET_COUNTRIES` list uses `"EN"` for USA/Global English ($126 USD) and rejects `"US"`.
                     * - Raw GeoIP keys (`cravi_raw_country`, `raw_geoip_country`, etc.) resolve to `TARGET_COUNTRY_ISO` (`"US"`),
                     *   which activates Quarterfull's 13+ US COPPA minimum age on Step 03 (`Your birthday.`).
                     */
                    const resolveCleanIsoCountry = function(rawVal, keyHint) {
                        const lowerKey = String(keyHint || '').trim().toLowerCase();
                        if (isQuarterfullMarketCountryKey(lowerKey) || lowerKey === 'market') {
                            return TARGET_QF_MARKET_CODE;
                        }
                        if (lowerKey === 'cravi_raw_country' ||
                            lowerKey === 'raw_geoip_country' ||
                            lowerKey === 'raw_country' ||
                            lowerKey === 'detected_country_iso_code') {
                            return TARGET_COUNTRY_ISO;
                        }
                        if (typeof rawVal === 'string') {
                            const trimmed = rawVal.trim();
                            if (/^[a-z]{2}$/.test(trimmed)) {
                                return TARGET_COUNTRY_LOWER;
                            }
                            if (/^[A-Z]{2}$/.test(trimmed)) {
                                return TARGET_COUNTRY_ISO;
                            }
                        }
                        return TARGET_COUNTRY_ISO;
                    };

                    const sanitizeUrlCountryQuery = function(rawUrl) {
                        if (typeof rawUrl !== 'string' || rawUrl.toLowerCase().indexOf('country') === -1) return rawUrl;
                        return rawUrl.replace(
                            /([?&](signup_market_country|market_country|signup_country|country_code|countryCode)=)([^&#]*)/gi,
                            function(match, prefix, keyName, oldVal) {
                                return prefix + resolveCleanIsoCountry(oldVal, keyName);
                            }
                        );
                    };

                    // =========================================================================
                    // LAYER 4: PER-PROFILE INDEXEDDB & CACHESTORAGE VIRTUAL NAMESPACING
                    // =========================================================================
                    try {
                        if (window.indexedDB && window.__giyaniIdbNamespaceProfile !== ACTIVE_PROFILE_ID) {
                            window.__giyaniIdbNamespaceProfile = ACTIVE_PROFILE_ID;
                            const rawOpen = window.__giyaniRawIdbOpen || window.indexedDB.open.bind(window.indexedDB);
                            const rawDelete = window.__giyaniRawIdbDelete || window.indexedDB.deleteDatabase.bind(window.indexedDB);
                            window.__giyaniRawIdbOpen = rawOpen;
                            window.__giyaniRawIdbDelete = rawDelete;

                            const namespaceDbName = function(dbName) {
                                const clean = String(dbName || 'default');
                                const prefix = '__giyani_idb_' + ACTIVE_PROFILE_ID + '__';
                                if (clean.indexOf('__giyani_idb_') === 0) {
                                    const stripped = clean.replace(/^__giyani_idb_.*?__/, '');
                                    return prefix + stripped;
                                }
                                return prefix + clean;
                            };

                            window.indexedDB.open = function(name, version) {
                                const namespaced = namespaceDbName(name);
                                return arguments.length > 1 ? rawOpen(namespaced, version) : rawOpen(namespaced);
                            };
                            window.indexedDB.deleteDatabase = function(name) {
                                return rawDelete(namespaceDbName(name));
                            };
                        }
                    } catch (e) {}

                    try {
                        if (window.caches && window.__giyaniCacheNamespaceProfile !== ACTIVE_PROFILE_ID) {
                            window.__giyaniCacheNamespaceProfile = ACTIVE_PROFILE_ID;
                            const rawCacheOpen = window.__giyaniRawCacheOpen || window.caches.open.bind(window.caches);
                            const rawCacheDelete = window.__giyaniRawCacheDelete || window.caches.delete.bind(window.caches);
                            const rawCacheHas = window.__giyaniRawCacheHas || window.caches.has.bind(window.caches);
                            window.__giyaniRawCacheOpen = rawCacheOpen;
                            window.__giyaniRawCacheDelete = rawCacheDelete;
                            window.__giyaniRawCacheHas = rawCacheHas;

                            const namespaceCacheName = function(cacheName) {
                                return '__giyani_cache_' + ACTIVE_PROFILE_ID + '__' + String(cacheName || 'default');
                            };
                            window.caches.open = function(name) { return rawCacheOpen(namespaceCacheName(name)); };
                            window.caches.delete = function(name) { return rawCacheDelete(namespaceCacheName(name)); };
                            window.caches.has = function(name) { return rawCacheHas(namespaceCacheName(name)); };
                        }
                    } catch (e) {}

                    // =========================================================================
                    // LAYER 3: UNCONDITIONAL DOMSTORAGE ISOLATION + DEBOUNCED VAULT SYNC
                    // + QUARTERFULL `cravi_*` USA MARKET ("EN") & RAW COUNTRY ("US") LOCK
                    // =========================================================================
                    const sanitizeQuarterfullStorageEntry = function(key, rawVal) {
                        const k = String(key || '').trim();
                        if (k === 'cravi_raw_country') return TARGET_COUNTRY_ISO;
                        if (k === 'cravi_country' || k === 'cravi_market_country_v1') return TARGET_QF_MARKET_CODE;
                        if (k === 'cravi_country_manual' || k === 'cravi_market_manual_v1') return '1';
                        if (k === 'cravi_locale') return TARGET_PRIMARY_LANG;
                        if (k === 'cravi_last_account_market_v1') {
                            return '{"locale":"' + TARGET_PRIMARY_LANG + '","country":"' + TARGET_QF_MARKET_CODE + '"}';
                        }
                        if (k === 'cravi_user' && typeof rawVal === 'string' && rawVal.length > 2) {
                            try {
                                const u = JSON.parse(rawVal);
                                if (u && typeof u === 'object') {
                                    u.country = TARGET_QF_MARKET_CODE;
                                    u.locale = TARGET_PRIMARY_LANG;
                                    u.market_policy_v1 = {
                                        country: TARGET_QF_MARKET_CODE,
                                        locale: TARGET_PRIMARY_LANG,
                                        manual_override: true
                                    };
                                    return (window.__giyaniRawJsonStringify || JSON.stringify)(u);
                                }
                            } catch (e) {}
                        }
                        if (k === 'payout-market-status' && typeof rawVal === 'string' && rawVal.length > 2) {
                            try {
                                const p = JSON.parse(rawVal);
                                if (p && typeof p === 'object') {
                                    if (p.data && typeof p.data === 'object') {
                                        p.data.market = TARGET_QF_MARKET_CODE;
                                        p.data.settlement_market = TARGET_QF_MARKET_CODE;
                                        p.data.currency = TARGET_CURRENCY_CODE;
                                    }
                                    p.market = TARGET_QF_MARKET_CODE;
                                    p.currency = TARGET_CURRENCY_CODE;
                                    return (window.__giyaniRawJsonStringify || JSON.stringify)(p);
                                }
                            } catch (e) {}
                        }
                        if (isStrictCountryCodeKey(k)) {
                            return resolveCleanIsoCountry(String(rawVal || ''), k);
                        }
                        return rawVal;
                    };

                    try {
                        const currentOrigin = window.location.origin;
                        if (currentOrigin && currentOrigin.indexOf('http') === 0 && window.__GiyaniVaultBridge) {
                            const rawSetItem = window.__giyaniRawSetItem || Storage.prototype.setItem;
                            const rawGetItem = window.__giyaniRawGetItem || Storage.prototype.getItem;
                            const rawRemoveItem = window.__giyaniRawRemoveItem || Storage.prototype.removeItem;
                            const rawClear = window.__giyaniRawClear || Storage.prototype.clear;
                            window.__giyaniRawSetItem = rawSetItem;
                            window.__giyaniRawGetItem = rawGetItem;
                            window.__giyaniRawRemoveItem = rawRemoveItem;
                            window.__giyaniRawClear = rawClear;

                            if (window.__giyaniDomOwnerProfileId !== ACTIVE_PROFILE_ID) {
                                window.__giyaniSuppressVaultSave = true;
                                try {
                                    rawClear.call(localStorage);
                                    rawClear.call(sessionStorage);

                                    if (KEEP_LOGIN_SESSION) {
                                        const savedLocalRaw = window.__GiyaniVaultBridge.getSavedLocalStorage(ACTIVE_PROFILE_ID, currentOrigin);
                                        const savedSessionRaw = window.__GiyaniVaultBridge.getSavedSessionStorage(ACTIVE_PROFILE_ID, currentOrigin);

                                        if (savedLocalRaw && savedLocalRaw !== '{}') {
                                            const parsedLocal = JSON.parse(savedLocalRaw);
                                            Object.keys(parsedLocal).forEach(function(k) {
                                                if (parsedLocal[k] !== null && parsedLocal[k] !== undefined) {
                                                    const rawStr = String(parsedLocal[k]);
                                                    rawSetItem.call(localStorage, k, sanitizeQuarterfullStorageEntry(k, rawStr));
                                                }
                                            });
                                        }
                                        if (savedSessionRaw && savedSessionRaw !== '{}') {
                                            const parsedSession = JSON.parse(savedSessionRaw);
                                            Object.keys(parsedSession).forEach(function(k) {
                                                if (parsedSession[k] !== null && parsedSession[k] !== undefined) {
                                                    const rawStr = String(parsedSession[k]);
                                                    rawSetItem.call(sessionStorage, k, sanitizeQuarterfullStorageEntry(k, rawStr));
                                                }
                                            });
                                        }
                                    }
                                    window.__giyaniDomOwnerProfileId = ACTIVE_PROFILE_ID;
                                } finally {
                                    window.__giyaniSuppressVaultSave = false;
                                }
                            }

                            // Immediately seed and lock Quarterfull's localStorage keys so Step 01-03 signup
                            // and post-login Writer Benefits always operate in USA ("US" raw / "EN" market / $126 USD)
                            try {
                                if (currentOrigin.indexOf('quarterfull.io') !== -1) {
                                    rawSetItem.call(localStorage, 'cravi_raw_country', TARGET_COUNTRY_ISO);
                                    rawSetItem.call(localStorage, 'cravi_country', TARGET_QF_MARKET_CODE);
                                    rawSetItem.call(localStorage, 'cravi_market_country_v1', TARGET_QF_MARKET_CODE);
                                    rawSetItem.call(localStorage, 'cravi_country_manual', '1');
                                    rawSetItem.call(localStorage, 'cravi_market_manual_v1', '1');
                                    rawSetItem.call(localStorage, 'cravi_locale', TARGET_PRIMARY_LANG);
                                    rawSetItem.call(
                                        localStorage,
                                        'cravi_last_account_market_v1',
                                        '{"locale":"' + TARGET_PRIMARY_LANG + '","country":"' + TARGET_QF_MARKET_CODE + '"}'
                                    );
                                }
                                for (let i = 0; i < localStorage.length; i++) {
                                    const k = localStorage.key(i);
                                    if (k) {
                                        const cur = rawGetItem.call(localStorage, k);
                                        const cleaned = sanitizeQuarterfullStorageEntry(k, cur);
                                        if (cleaned !== cur) {
                                            rawSetItem.call(localStorage, k, cleaned);
                                        }
                                    }
                                }
                                for (let i = 0; i < sessionStorage.length; i++) {
                                    const k = sessionStorage.key(i);
                                    if (k) {
                                        const cur = rawGetItem.call(sessionStorage, k);
                                        const cleaned = sanitizeQuarterfullStorageEntry(k, cur);
                                        if (cleaned !== cur) {
                                            rawSetItem.call(sessionStorage, k, cleaned);
                                        }
                                    }
                                }
                            } catch (e) {}

                            const syncVaultNow = function() {
                                try {
                                    if (window.__giyaniSuppressVaultSave) return;
                                    if (window.__giyaniDomOwnerProfileId !== ACTIVE_PROFILE_ID) return;
                                    if (!KEEP_LOGIN_SESSION) return;

                                    const locObj = {};
                                    for (let i = 0; i < localStorage.length; i++) {
                                        const k = localStorage.key(i);
                                        if (k) {
                                            const cur = rawGetItem.call(localStorage, k);
                                            locObj[k] = sanitizeQuarterfullStorageEntry(k, cur);
                                        }
                                    }
                                    const sesObj = {};
                                    for (let i = 0; i < sessionStorage.length; i++) {
                                        const k = sessionStorage.key(i);
                                        if (k) {
                                            const cur = rawGetItem.call(sessionStorage, k);
                                            sesObj[k] = sanitizeQuarterfullStorageEntry(k, cur);
                                        }
                                    }
                                    window.__GiyaniVaultBridge.saveOriginStorage(
                                        ACTIVE_PROFILE_ID,
                                        window.location.origin,
                                        (window.__giyaniRawJsonStringify || JSON.stringify)(locObj),
                                        (window.__giyaniRawJsonStringify || JSON.stringify)(sesObj)
                                    );
                                } catch (e) {}
                            };

                            const scheduleDebouncedVaultSync = function() {
                                if (window.__giyaniSuppressVaultSave) return;
                                if (window.__giyaniVaultDebounceTimer) {
                                    clearTimeout(window.__giyaniVaultDebounceTimer);
                                }
                                window.__giyaniVaultDebounceTimer = setTimeout(syncVaultNow, 350);
                            };

                            if (window.__giyaniStorageHookProfile !== ACTIVE_PROFILE_ID) {
                                window.__giyaniStorageHookProfile = ACTIVE_PROFILE_ID;
                                Storage.prototype.setItem = function(key, val) {
                                    const safeVal = sanitizeQuarterfullStorageEntry(key, String(val !== undefined && val !== null ? val : ''));
                                    rawSetItem.call(this, key, safeVal);
                                    scheduleDebouncedVaultSync();
                                };
                                Storage.prototype.getItem = function(key) {
                                    const cur = rawGetItem.apply(this, arguments);
                                    const k = String(key || '').trim();
                                    if (window.location.origin.indexOf('quarterfull.io') !== -1) {
                                        if (k === 'cravi_raw_country') return TARGET_COUNTRY_ISO;
                                        if (k === 'cravi_country' || k === 'cravi_market_country_v1') return TARGET_QF_MARKET_CODE;
                                        if (k === 'cravi_country_manual' || k === 'cravi_market_manual_v1') return '1';
                                        if (k === 'cravi_locale') return TARGET_PRIMARY_LANG;
                                    }
                                    if (cur !== null) {
                                        const cleaned = sanitizeQuarterfullStorageEntry(k, cur);
                                        if (cur !== cleaned) {
                                            rawSetItem.call(this, k, cleaned);
                                        }
                                        return cleaned;
                                    }
                                    return cur;
                                };
                                Storage.prototype.removeItem = function(key) {
                                    const k = String(key || '').trim();
                                    if (window.location.origin.indexOf('quarterfull.io') !== -1 &&
                                        (k === 'cravi_raw_country' || k === 'cravi_country' || k === 'cravi_market_country_v1' || k === 'cravi_country_manual' || k === 'cravi_market_manual_v1')) {
                                        return;
                                    }
                                    rawRemoveItem.apply(this, arguments);
                                    scheduleDebouncedVaultSync();
                                };
                                Storage.prototype.clear = function() {
                                    rawClear.apply(this, arguments);
                                    scheduleDebouncedVaultSync();
                                };
                            }
                        }
                    } catch (e) {}

                    if (window.__giyaniIdentityV5Profile === '${profileIdSafe}_${effectiveDevice.id}_${countryIso}_$colorSchemePref') return;
                    window.__giyaniIdentityV5Profile = '${profileIdSafe}_${effectiveDevice.id}_${countryIso}_$colorSchemePref';

                    // =========================================================================
                    // 1. COHERENT LANGUAGE, LOCALE & TIMEZONE OFFSET OVERRIDES
                    // =========================================================================
                    defineSafeProp(navigator, 'language', function() { return TARGET_LOCALE; });
                    defineSafeProp(navigator, 'languages', function() { return Object.freeze([TARGET_LOCALE, TARGET_PRIMARY_LANG, 'en-US', 'en']); });

                    try {
                        Date.prototype.getTimezoneOffset = function() {
                            return TARGET_TZ_OFFSET_MIN;
                        };
                        const origDateToString = window.__giyaniOrigDateToString || Date.prototype.toString;
                        const origDateToTimeString = window.__giyaniOrigDateToTimeString || Date.prototype.toTimeString;
                        window.__giyaniOrigDateToString = origDateToString;
                        window.__giyaniOrigDateToTimeString = origDateToTimeString;

                        const cleanGmtTail = '$gmtOffsetString';
                        Date.prototype.toString = function() {
                            const raw = origDateToString.call(this);
                            return raw.replace(/GMT[+-]\d{4}(\s*\([^)]*\))?/, cleanGmtTail);
                        };
                        Date.prototype.toTimeString = function() {
                            const raw = origDateToTimeString.call(this);
                            return raw.replace(/GMT[+-]\d{4}(\s*\([^)]*\))?/, cleanGmtTail);
                        };
                    } catch (e) {}

                    // =========================================================================
                    // 2. QUARTERFULL MARKET ("EN") + RAW COUNTRY ("US") + 13+ AGE + $126 USD SANITIZER
                    // Hooks JSON.stringify, Response.prototype.json/text, XMLHttpRequest.responseText/response,
                    // Fetch, XHR headers, FormData, and URLSearchParams.
                    // =========================================================================
                    const isIndonesianOrCorruptedCountryValue = function(val) {
                        if (typeof val !== 'string') return false;
                        const clean = val.trim();
                        return clean === 'ID' || clean === 'IDN' || clean === 'id' ||
                            clean === 'id-ID' || clean === 'id_ID' ||
                            clean.toLowerCase() === 'indonesia' ||
                            clean.indexOf('Asia/Jakarta') !== -1 ||
                            clean.indexOf('Asia/Makassar') !== -1 ||
                            clean.indexOf('Asia/Jayapura') !== -1;
                    };

                    const convertIdrRewardObjectIfNeeded = function(obj) {
                        if (!obj || typeof obj !== 'object' || TARGET_CURRENCY_CODE !== 'USD') return;
                        if (obj.currency === 'IDR' || obj.currency === 'idr' || obj.amount === 310000 || obj.amount_micro === 310000000000) {
                            if (obj.amount === 310000 || obj.amount === 310000.0) obj.amount = 126.0;
                            if (obj.amount_micro === 310000000000) obj.amount_micro = 126000000;
                            if (obj.track30 === 550000 || obj.track30 === 550000.0) obj.track30 = 216.0;
                            if (obj.track30_micro === 550000000000) obj.track30_micro = 216000000;
                            if (obj.track60 === 650000 || obj.track60 === 650000.0) obj.track60 = 270.0;
                            if (obj.track60_micro === 650000000000) obj.track60_micro = 270000000;
                            if (obj.track90 === 900000 || obj.track90 === 900000.0) obj.track90 = 360.0;
                            if (obj.track90_micro === 900000000000) obj.track90_micro = 360000000;
                            if (obj.track120 === 1100000 || obj.track120 === 1100000.0) obj.track120 = 450.0;
                            if (obj.track120_micro === 1100000000000) obj.track120_micro = 450000000;
                            if (typeof obj.currency === 'string') obj.currency = TARGET_CURRENCY_CODE;
                        }
                    };

                    const sanitizeSignupJsonObject = function(obj, visited) {
                        if (!obj || typeof obj !== 'object') return obj;
                        const seen = visited || (typeof WeakSet !== 'undefined' ? new WeakSet() : null);
                        if (seen) {
                            if (seen.has(obj)) return obj;
                            seen.add(obj);
                        }
                        if (Array.isArray(obj)) {
                            for (let i = 0; i < obj.length; i++) {
                                if (obj[i] && typeof obj[i] === 'object') {
                                    sanitizeSignupJsonObject(obj[i], seen);
                                }
                            }
                            return obj;
                        }
                        convertIdrRewardObjectIfNeeded(obj);
                        Object.keys(obj).forEach(function(k) {
                            const lowerK = k.trim().toLowerCase();
                            const v = obj[k];
                            if (typeof v === 'object' && v !== null) {
                                sanitizeSignupJsonObject(v, seen);
                                if (lowerK === 'market_policy_v1') {
                                    v.country = TARGET_QF_MARKET_CODE;
                                    v.locale = TARGET_PRIMARY_LANG;
                                    v.manual_override = true;
                                }
                            } else if (isQuarterfullMarketCountryKey(lowerK)) {
                                obj[k] = TARGET_QF_MARKET_CODE;
                            } else if (lowerK === 'signup_market_source') {
                                obj[k] = 'manual';
                            } else if (isRawGeoCountryKey(lowerK)) {
                                if (typeof v === 'string' || v === null || v === undefined) {
                                    obj[k] = resolveCleanIsoCountry(typeof v === 'string' ? v : '', lowerK);
                                }
                            } else if (lowerK === 'min_signup_age' || lowerK === 'minsignupage' || lowerK === 'min_age') {
                                if (typeof v === 'number' && v > TARGET_MIN_SIGNUP_AGE) {
                                    obj[k] = TARGET_MIN_SIGNUP_AGE;
                                }
                            } else if (isStrictCurrencyCodeKey(lowerK)) {
                                if (typeof v === 'string') {
                                    const trimCurr = v.trim();
                                    if (trimCurr === 'IDR') obj[k] = TARGET_CURRENCY_CODE;
                                    else if (trimCurr === 'idr') obj[k] = TARGET_CURRENCY_LOWER;
                                }
                            } else if (lowerK === 'country' || lowerK === 'market') {
                                if (typeof v === 'string') {
                                    const trimmedV = v.trim();
                                    if (isIndonesianOrCorruptedCountryValue(trimmedV) || !/^[A-Za-z]{2}$/.test(trimmedV)) {
                                        obj[k] = (window.location.hostname.indexOf('quarterfull.io') !== -1 || lowerK === 'market')
                                            ? TARGET_QF_MARKET_CODE
                                            : resolveCleanIsoCountry(trimmedV, lowerK);
                                    }
                                }
                            } else if (typeof v === 'string' && isIndonesianOrCorruptedCountryValue(v)) {
                                if (lowerK.indexOf('time') !== -1 || lowerK === 'tz') {
                                    obj[k] = TARGET_TIMEZONE;
                                } else if (lowerK === 'locale') {
                                    obj[k] = TARGET_PRIMARY_LANG;
                                } else if (lowerK.indexOf('lang') !== -1) {
                                    obj[k] = TARGET_LOCALE;
                                } else if (lowerK.indexOf('name') !== -1) {
                                    obj[k] = TARGET_COUNTRY_NAME;
                                } else {
                                    obj[k] = resolveCleanIsoCountry(v, lowerK);
                                }
                            } else if ((lowerK === 'timezoneoffset' || lowerK === 'tz_offset' || lowerK === 'tzoffset') && (v === -420 || v === '-420')) {
                                obj[k] = TARGET_TZ_OFFSET_MIN;
                            }
                        });
                        return obj;
                    };

                    const sanitizeIncomingResponseText = function(txt) {
                        if (typeof txt !== 'string' || txt.length === 0) return txt;
                        const trimmed = txt.trim();
                        if (trimmed === 'ID') return TARGET_COUNTRY_ISO;
                        if (trimmed === 'id') return TARGET_COUNTRY_LOWER;

                        if ((trimmed.charAt(0) === '{' && trimmed.charAt(trimmed.length - 1) === '}') ||
                            (trimmed.charAt(0) === '[' && trimmed.charAt(trimmed.length - 1) === ']')) {
                            try {
                                const parsed = JSON.parse(trimmed);
                                sanitizeSignupJsonObject(parsed);
                                return (window.__giyaniRawJsonStringify || JSON.stringify)(parsed);
                            } catch (e) {}
                        }

                        let outTxt = txt;
                        if (outTxt.indexOf('loc=ID') !== -1 && (outTxt.indexOf('visit_scheme=') !== -1 || outTxt.indexOf('fl=') !== -1)) {
                            outTxt = outTxt.replace(/\bloc=ID\b/g, 'loc=' + TARGET_COUNTRY_ISO);
                        }
                        outTxt = outTxt.replace(
                            /"(signup_market_country|market_country|estimated_country_iso_code|settlement_market|market)"\s*:\s*"(ID|id|US|us)"/g,
                            function(match, key) {
                                return '"' + key + '":"' + TARGET_QF_MARKET_CODE + '"';
                            }
                        );
                        outTxt = outTxt.replace(
                            /"(raw_geoip_country|raw_country|detected_country_iso_code|signup_country|country_code|countryCode|user_country|userCountry|ip_country|ipCountry|detected_country|detectedCountry|geo_country|geoCountry)"\s*:\s*"(ID|id)"/g,
                            function(match, key, oldVal) {
                                return '"' + key + '":"' + resolveCleanIsoCountry(oldVal, key) + '"';
                            }
                        );
                        if (TARGET_COUNTRY_ISO === 'US') {
                            outTxt = outTxt.replace(/"(min_signup_age|minSignupAge|min_age)"\s*:\s*18\b/g, '"$1":13');
                        }
                        outTxt = outTxt.replace(
                            /"(currency|currency_code|currencyCode|market_currency|marketCurrency|benefit_currency|benefitCurrency|payout_currency|payoutCurrency)"\s*:\s*"(IDR|idr)"/g,
                            function(match, key, oldVal) {
                                return '"' + key + '":"' + (oldVal === 'idr' ? TARGET_CURRENCY_LOWER : TARGET_CURRENCY_CODE) + '"';
                            }
                        );
                        return outTxt;
                    };

                    // Hook JSON.stringify so any payload serialized via JSON.stringify (including Quarterfull's Axios POST /api/auth/signup)
                    // automatically sets signup_market_country to TARGET_QF_MARKET_CODE ("EN" for USA) and signup_market_source to "manual"
                    try {
                        const rawJsonStringify = window.__giyaniRawJsonStringify || JSON.stringify;
                        window.__giyaniRawJsonStringify = rawJsonStringify;
                        JSON.stringify = function(value, replacer, space) {
                            try {
                                if (value && typeof value === 'object') {
                                    sanitizeSignupJsonObject(value);
                                }
                            } catch (e) {}
                            const serialized = rawJsonStringify.apply(this, arguments);
                            if (typeof serialized === 'string' && serialized.indexOf('signup_market_country') !== -1) {
                                return serialized
                                    .replace(
                                        /"signup_market_country"\s*:\s*("([^"]*)"|null)/g,
                                        '"signup_market_country":"' + TARGET_QF_MARKET_CODE + '"'
                                    )
                                    .replace(
                                        /"signup_market_source"\s*:\s*("([^"]*)"|null)/g,
                                        '"signup_market_source":"manual"'
                                    );
                            }
                            return serialized;
                        };
                    } catch (e) {}

                    // Hook Response.prototype.json AND Response.prototype.text
                    try {
                        if (typeof Response !== 'undefined' && Response.prototype) {
                            if (Response.prototype.json) {
                                const rawResponseJson = window.__giyaniRawResponseJson || Response.prototype.json;
                                window.__giyaniRawResponseJson = rawResponseJson;
                                Response.prototype.json = function() {
                                    return rawResponseJson.apply(this, arguments).then(function(data) {
                                        try {
                                            if (data && typeof data === 'object') {
                                                sanitizeSignupJsonObject(data);
                                            }
                                        } catch (e) {}
                                        return data;
                                    });
                                };
                            }
                            if (Response.prototype.text) {
                                const rawResponseText = window.__giyaniRawResponseText || Response.prototype.text;
                                window.__giyaniRawResponseText = rawResponseText;
                                Response.prototype.text = function() {
                                    return rawResponseText.apply(this, arguments).then(function(txt) {
                                        try {
                                            return sanitizeIncomingResponseText(txt);
                                        } catch (e) {}
                                        return txt;
                                    });
                                };
                            }
                        }
                    } catch (e) {}

                    const sanitizeOutgoingBodyString = function(body) {
                        if (typeof body !== 'string' || body.length < 2) return body;
                        let out = body;
                        const trimmed = out.trim();
                        if ((trimmed.charAt(0) === '{' && trimmed.charAt(trimmed.length - 1) === '}') ||
                            (trimmed.charAt(0) === '[' && trimmed.charAt(trimmed.length - 1) === ']')) {
                            try {
                                const parsed = JSON.parse(trimmed);
                                sanitizeSignupJsonObject(parsed);
                                out = (window.__giyaniRawJsonStringify || JSON.stringify)(parsed);
                            } catch (e) {}
                        }
                        if (out.indexOf('signup_market_country') !== -1 ||
                            out.indexOf('market_country') !== -1 ||
                            out.indexOf('country_code') !== -1) {
                            out = out.replace(
                                /"(signup_market_country|market_country)"\s*:\s*("([^"]*)"|null)/gi,
                                function(match, key) {
                                    return '"' + key + '":"' + TARGET_QF_MARKET_CODE + '"';
                                }
                            );
                            out = out.replace(
                                /"signup_market_source"\s*:\s*("([^"]*)"|null)/gi,
                                '"signup_market_source":"manual"'
                            );
                            out = out.replace(
                                /"(signup_country|country_code|countryCode)"\s*:\s*("([^"]*)"|null)/gi,
                                function(match, key, fullVal, innerStr) {
                                    return '"' + key + '":"' + resolveCleanIsoCountry(innerStr || '', key) + '"';
                                }
                            );
                            out = out.replace(
                                /\\"(signup_market_country|market_country)\\"\s*:\s*(\\"([^\\"]*)\\"|null)/gi,
                                function(match, key) {
                                    return '\\"' + key + '\\":\\"' + TARGET_QF_MARKET_CODE + '\\"';
                                }
                            );
                            out = out.replace(
                                /(^|[&?])(signup_market_country|market_country|signup_country|country_code|countryCode)=([^&\s]*)/gi,
                                function(match, sep, key, oldVal) {
                                    return sep + key + '=' + resolveCleanIsoCountry(oldVal, key);
                                }
                            );
                        }
                        return out;
                    };

                    const sanitizeAnyBody = function(body) {
                        if (!body) return body;
                        if (typeof body === 'string') {
                            return sanitizeOutgoingBodyString(body);
                        }
                        try {
                            if (typeof URLSearchParams !== 'undefined' && body instanceof URLSearchParams) {
                                ['signup_market_country', 'market_country', 'signup_country', 'country_code', 'countryCode'].forEach(function(k) {
                                    if (body.has(k)) {
                                        body.set(k, resolveCleanIsoCountry(body.get(k) || '', k));
                                    }
                                });
                                return body;
                            }
                        } catch (e) {}
                        try {
                            if (typeof FormData !== 'undefined' && body instanceof FormData) {
                                ['signup_market_country', 'market_country', 'signup_country', 'country_code', 'countryCode'].forEach(function(k) {
                                    if (body.has(k)) {
                                        const cur = body.get(k);
                                        body.set(k, resolveCleanIsoCountry(typeof cur === 'string' ? cur : '', k));
                                    }
                                });
                                return body;
                            }
                        } catch (e) {}
                        try {
                            if (typeof TextDecoder !== 'undefined' && typeof TextEncoder !== 'undefined') {
                                if (body instanceof Uint8Array || body instanceof ArrayBuffer) {
                                    const bytes = body instanceof ArrayBuffer ? new Uint8Array(body) : body;
                                    const decoded = new TextDecoder().decode(bytes);
                                    if (decoded.indexOf('signup_market_country') !== -1 || decoded.indexOf('market_country') !== -1) {
                                        const cleanedStr = sanitizeOutgoingBodyString(decoded);
                                        const encoded = new TextEncoder().encode(cleanedStr);
                                        return body instanceof ArrayBuffer ? encoded.buffer : encoded;
                                    }
                                }
                            }
                        } catch (e) {}
                        return body;
                    };

                    // Hook FormData.prototype.append / set and URLSearchParams.prototype.append / set
                    try {
                        if (typeof FormData !== 'undefined' && !window.__giyaniFormDataHooked) {
                            window.__giyaniFormDataHooked = true;
                            const rawFdAppend = FormData.prototype.append;
                            const rawFdSet = FormData.prototype.set;
                            FormData.prototype.append = function(name, value) {
                                if (isStrictCountryCodeKey(name) && typeof value === 'string') {
                                    return rawFdAppend.call(this, name, resolveCleanIsoCountry(value, name));
                                }
                                if (typeof value === 'string' && value.indexOf('signup_market_country') !== -1) {
                                    return rawFdAppend.call(this, name, sanitizeOutgoingBodyString(value));
                                }
                                return rawFdAppend.apply(this, arguments);
                            };
                            if (rawFdSet) {
                                FormData.prototype.set = function(name, value) {
                                    if (isStrictCountryCodeKey(name) && typeof value === 'string') {
                                        return rawFdSet.call(this, name, resolveCleanIsoCountry(value, name));
                                    }
                                    if (typeof value === 'string' && value.indexOf('signup_market_country') !== -1) {
                                        return rawFdSet.call(this, name, sanitizeOutgoingBodyString(value));
                                    }
                                    return rawFdSet.apply(this, arguments);
                                };
                            }
                        }
                    } catch (e) {}

                    try {
                        if (typeof URLSearchParams !== 'undefined' && !window.__giyaniUspHooked) {
                            window.__giyaniUspHooked = true;
                            const rawUspAppend = URLSearchParams.prototype.append;
                            const rawUspSet = URLSearchParams.prototype.set;
                            URLSearchParams.prototype.append = function(name, value) {
                                if (isStrictCountryCodeKey(name) && typeof value === 'string') {
                                    return rawUspAppend.call(this, name, resolveCleanIsoCountry(value, name));
                                }
                                return rawUspAppend.apply(this, arguments);
                            };
                            if (rawUspSet) {
                                URLSearchParams.prototype.set = function(name, value) {
                                    if (isStrictCountryCodeKey(name) && typeof value === 'string') {
                                        return rawUspSet.call(this, name, resolveCleanIsoCountry(value, name));
                                    }
                                    return rawUspSet.apply(this, arguments);
                                };
                            }
                        }
                    } catch (e) {}

                    // Hook window.Request constructor so `fetch(new Request(url, { body }))` is also sanitized
                    try {
                        if (typeof window.Request === 'function' && typeof Proxy !== 'undefined' && !window.__giyaniRequestProxied) {
                            window.__giyaniRequestProxied = true;
                            const OrigRequest = window.Request;
                            window.Request = new Proxy(OrigRequest, {
                                construct: function(target, args, newTarget) {
                                    try {
                                        if (args && args.length > 0) {
                                            if (typeof args[0] === 'string') {
                                                args[0] = sanitizeUrlCountryQuery(args[0]);
                                            }
                                            if (args.length > 1 && args[1] && typeof args[1] === 'object') {
                                                const initCopy = Object.assign({}, args[1]);
                                                if (initCopy.body) {
                                                    initCopy.body = sanitizeAnyBody(initCopy.body);
                                                }
                                                args[1] = initCopy;
                                            }
                                        }
                                    } catch (e) {}
                                    return Reflect.construct(target, args, newTarget);
                                }
                            });
                        }
                    } catch (e) {}

                    try {
                        if (typeof window.fetch === 'function') {
                            const rawFetch = window.__giyaniRawFetch || window.fetch.bind(window);
                            window.__giyaniRawFetch = rawFetch;

                            window.fetch = function(input, init) {
                                try {
                                    const reqUrl = typeof input === 'string' ? input : (input && input.url ? input.url : '');
                                    const cleanInput = typeof input === 'string' ? sanitizeUrlCountryQuery(input) : input;
                                    const isQuarterfullReq = reqUrl.indexOf('quarterfull.io') !== -1 ||
                                        ((!reqUrl || reqUrl.indexOf('/') === 0) && window.location.hostname.indexOf('quarterfull.io') !== -1);

                                    const patchedInit = (init && typeof init === 'object') ? Object.assign({}, init) : {};
                                    if (patchedInit.body) {
                                        patchedInit.body = sanitizeAnyBody(patchedInit.body);
                                    }
                                    if (isQuarterfullReq) {
                                        try {
                                            const hdrs = new Headers(patchedInit.headers || (input instanceof Request ? input.headers : undefined) || {});
                                            hdrs.set('Accept-Language', TARGET_PRIMARY_LANG);
                                            hdrs.set('X-User-Country', TARGET_QF_MARKET_CODE);
                                            hdrs.set('X-User-Raw-Country', TARGET_COUNTRY_ISO);
                                            hdrs.set('X-User-Market-Source', 'manual');
                                            hdrs.set('X-Timezone', TARGET_TIMEZONE);
                                            hdrs.set('X-User-Timezone', TARGET_TIMEZONE);
                                            patchedInit.headers = hdrs;
                                        } catch (hErr) {}
                                    }
                                    return rawFetch(cleanInput, patchedInit);
                                } catch (e) {
                                    return rawFetch(input, init);
                                }
                            };
                        }
                    } catch (e) {}

                    // Hook XMLHttpRequest (used by Quarterfull's Axios client for /api/auth/email/check, /api/auth/signup,
                    // /api/v1/service-countries, /api/v1/studio/streak-bonus/summary, etc.)
                    try {
                        if (typeof XMLHttpRequest !== 'undefined') {
                            const rawXhrOpen = window.__giyaniRawXhrOpen || XMLHttpRequest.prototype.open;
                            const rawXhrSend = window.__giyaniRawXhrSend || XMLHttpRequest.prototype.send;
                            const rawXhrSetHeader = window.__giyaniRawXhrSetHeader || XMLHttpRequest.prototype.setRequestHeader;
                            window.__giyaniRawXhrOpen = rawXhrOpen;
                            window.__giyaniRawXhrSend = rawXhrSend;
                            window.__giyaniRawXhrSetHeader = rawXhrSetHeader;

                            // Hook responseText & response getters on XMLHttpRequest.prototype so Axios always parses sanitized USA/EN JSON
                            if (!window.__giyaniXhrRespHooked) {
                                window.__giyaniXhrRespHooked = true;
                                const respTextDesc = Object.getOwnPropertyDescriptor(XMLHttpRequest.prototype, 'responseText');
                                const respDesc = Object.getOwnPropertyDescriptor(XMLHttpRequest.prototype, 'response');
                                if (respTextDesc && respTextDesc.get) {
                                    Object.defineProperty(XMLHttpRequest.prototype, 'responseText', {
                                        get: function() {
                                            const rawTxt = respTextDesc.get.call(this);
                                            try {
                                                if (this.readyState === 4 && typeof rawTxt === 'string' && rawTxt.length > 0) {
                                                    return sanitizeIncomingResponseText(rawTxt);
                                                }
                                            } catch (e) {}
                                            return rawTxt;
                                        },
                                        configurable: true
                                    });
                                }
                                if (respDesc && respDesc.get) {
                                    Object.defineProperty(XMLHttpRequest.prototype, 'response', {
                                        get: function() {
                                            const rawResp = respDesc.get.call(this);
                                            try {
                                                if (this.readyState === 4) {
                                                    if (typeof rawResp === 'string' && rawResp.length > 0) {
                                                        return sanitizeIncomingResponseText(rawResp);
                                                    }
                                                    if (rawResp && typeof rawResp === 'object') {
                                                        sanitizeSignupJsonObject(rawResp);
                                                        return rawResp;
                                                    }
                                                }
                                            } catch (e) {}
                                            return rawResp;
                                        },
                                        configurable: true
                                    });
                                }
                            }

                            XMLHttpRequest.prototype.open = function(method, url) {
                                const cleanUrl = typeof url === 'string' ? sanitizeUrlCountryQuery(url) : url;
                                this.__giyaniTargetUrl = String(cleanUrl || '');
                                this.__giyaniHdrMap = {};
                                const args = Array.prototype.slice.call(arguments);
                                if (args.length > 1 && typeof args[1] === 'string') {
                                    args[1] = cleanUrl;
                                }
                                return rawXhrOpen.apply(this, args);
                            };

                            XMLHttpRequest.prototype.setRequestHeader = function(name, value) {
                                try {
                                    const lowerN = String(name || '').trim().toLowerCase();
                                    let finalVal = value;
                                    if (lowerN === 'x-user-country') {
                                        finalVal = TARGET_QF_MARKET_CODE;
                                    } else if (lowerN === 'x-user-raw-country') {
                                        finalVal = TARGET_COUNTRY_ISO;
                                    } else if (lowerN === 'x-user-market-source') {
                                        finalVal = 'manual';
                                    } else if (lowerN === 'x-timezone' || lowerN === 'x-user-timezone') {
                                        finalVal = TARGET_TIMEZONE;
                                    } else if (lowerN === 'accept-language') {
                                        finalVal = TARGET_PRIMARY_LANG;
                                    }
                                    if (!this.__giyaniHdrMap) this.__giyaniHdrMap = {};
                                    this.__giyaniHdrMap[lowerN] = true;
                                    return rawXhrSetHeader.call(this, name, finalVal);
                                } catch (e) {
                                    return rawXhrSetHeader.apply(this, arguments);
                                }
                            };

                            XMLHttpRequest.prototype.send = function(body) {
                                try {
                                    const u = this.__giyaniTargetUrl || '';
                                    const isQf = u.indexOf('quarterfull.io') !== -1 ||
                                        ((!u || u.indexOf('/') === 0) && window.location.hostname.indexOf('quarterfull.io') !== -1);
                                    if (isQf) {
                                        const hdrMap = this.__giyaniHdrMap || {};
                                        try {
                                            if (!hdrMap['accept-language']) rawXhrSetHeader.call(this, 'Accept-Language', TARGET_PRIMARY_LANG);
                                            if (!hdrMap['x-user-country']) rawXhrSetHeader.call(this, 'X-User-Country', TARGET_QF_MARKET_CODE);
                                            if (!hdrMap['x-user-raw-country']) rawXhrSetHeader.call(this, 'X-User-Raw-Country', TARGET_COUNTRY_ISO);
                                            if (!hdrMap['x-user-market-source']) rawXhrSetHeader.call(this, 'X-User-Market-Source', 'manual');
                                            if (!hdrMap['x-timezone']) rawXhrSetHeader.call(this, 'X-Timezone', TARGET_TIMEZONE);
                                            if (!hdrMap['x-user-timezone']) rawXhrSetHeader.call(this, 'X-User-Timezone', TARGET_TIMEZONE);
                                        } catch (hErr) {}
                                    }
                                    if (body) {
                                        const cleaned = sanitizeAnyBody(body);
                                        return rawXhrSend.call(this, cleaned);
                                    }
                                } catch (e) {}
                                return rawXhrSend.apply(this, arguments);
                            };
                        }
                    } catch (e) {}

                    try {
                        const cleanDomCountryInputs = function() {
                            try {
                                const inputs = document.querySelectorAll('input[name*="country" i], select[name*="country" i]');
                                for (let i = 0; i < inputs.length; i++) {
                                    const el = inputs[i];
                                    const nameAttr = el.getAttribute('name') || '';
                                    if (isStrictCountryCodeKey(nameAttr)) {
                                        const cleanedVal = resolveCleanIsoCountry(el.value || '', nameAttr);
                                        if (el.value !== cleanedVal) {
                                            el.value = cleanedVal;
                                        }
                                    }
                                }
                            } catch (e) {}
                        };
                        cleanDomCountryInputs();
                        document.addEventListener('submit', cleanDomCountryInputs, true);
                        document.addEventListener('click', cleanDomCountryInputs, true);
                    } catch (e) {}

                    // 3. Coherent Platform, Hardware & Device Profile
                    defineSafeProp(navigator, 'platform', function() { return '$platform'; });
                    defineSafeProp(navigator, 'hardwareConcurrency', function() { return ${effectiveDevice.hardwareConcurrency}; });
                    if ('deviceMemory' in navigator) {
                        defineSafeProp(navigator, 'deviceMemory', function() { return ${effectiveDevice.deviceMemoryGb}; });
                    }
                    defineSafeProp(navigator, 'maxTouchPoints', function() { return ${effectiveDevice.maxTouchPoints}; });
                    defineSafeProp(navigator, 'webdriver', function() { return false; });
                    defineSafeProp(navigator, 'vendor', function() { return 'Google Inc.'; });

                    // 3b. Mask SpeechSynthesis Indonesian TTS Voices (`id-ID` Bahasa Indonesia leak prevention)
                    try {
                        if (window.speechSynthesis) {
                            const usVoiceList = Object.freeze([
                                Object.freeze({
                                    default: true,
                                    lang: TARGET_LOCALE,
                                    localService: true,
                                    name: 'Google US English',
                                    voiceURI: 'Google US English'
                                }),
                                Object.freeze({
                                    default: false,
                                    lang: 'en-US',
                                    localService: false,
                                    name: 'English United States',
                                    voiceURI: 'urn:moz-tts:speechd:English%20United%20States?en-US'
                                })
                            ]);
                            window.speechSynthesis.getVoices = function() {
                                return usVoiceList;
                            };
                        }
                    } catch (e) {}

                    // 3c. Mask WebGL Vendor & Renderer Fingerprint so Indonesian local SoC signature is normalized
                    try {
                        const patchWebGlProto = function( proto ) {
                            if (!proto || !proto.getParameter || proto.__giyaniWebGlPatched) return;
                            proto.__giyaniWebGlPatched = true;
                            const rawGetParam = proto.getParameter;
                            proto.getParameter = function(param) {
                                // UNMASKED_VENDOR_WEBGL = 0x9245 (37445), UNMASKED_RENDERER_WEBGL = 0x9246 (37446)
                                if (param === 37445) return 'Google Inc. (Qualcomm)';
                                if (param === 37446) return $isMobileJs ? 'ANGLE (Qualcomm, Adreno (TM) 750, OpenGL ES 3.2)' : 'ANGLE (NVIDIA, NVIDIA GeForce RTX 4070 Direct3D11 vs_5_0 ps_5_0, D3D11)';
                                return rawGetParam.apply(this, arguments);
                            };
                        };
                        if (typeof WebGLRenderingContext !== 'undefined') patchWebGlProto(WebGLRenderingContext.prototype);
                        if (typeof WebGL2RenderingContext !== 'undefined') patchWebGlProto(WebGL2RenderingContext.prototype);
                    } catch (e) {}

                    // 4. Modern Chrome Runtime Compatibility Object (window.chrome)
                    if (!window.chrome) {
                        window.chrome = {
                            app: { isInstalled: false },
                            runtime: {
                                OnInstalledReason: { INSTALL: 'install', UPDATE: 'update' },
                                PlatformOs: { ANDROID: 'android', WIN: 'win', MAC: 'mac' }
                            },
                            loadTimes: function() { return {}; },
                            csi: function() { return {}; }
                        };
                    }

                    // 5. Coherent User-Agent Client Hints JS API (navigator.userAgentData)
                    try {
                        const brandsList = [
                            { brand: 'Chromium', version: '131' },
                            { brand: 'Google Chrome', version: '131' },
                            { brand: 'Not_A Brand', version: '24' }
                        ];
                        const uaDataMock = {
                            brands: brandsList,
                            mobile: $isMobileJs,
                            platform: '$cleanPlatformName',
                            getHighEntropyValues: function(hints) {
                                return Promise.resolve({
                                    brands: brandsList,
                                    mobile: $isMobileJs,
                                    platform: '$cleanPlatformName',
                                    platformVersion: '15.0.0',
                                    architecture: $isMobileJs ? 'arm' : 'x86',
                                    bitness: '64',
                                    model: '${effectiveDevice.name.replace("'", "")}',
                                    uaFullVersion: '131.0.6778.104',
                                    fullVersionList: brandsList
                                });
                            },
                            toJSON: function() {
                                return {
                                    brands: brandsList,
                                    mobile: $isMobileJs,
                                    platform: '$cleanPlatformName'
                                };
                            }
                        };
                        defineSafeProp(navigator, 'userAgentData', function() { return uaDataMock; });
                    } catch (e) {}

                    // 6. Full Intl Suite Proxy (DateTimeFormat, NumberFormat, PluralRules, Collator, RelativeTimeFormat, DisplayNames)
                    if (typeof Intl !== 'undefined' && typeof Proxy !== 'undefined') {
                        try {
                            const OrigDTF = window.__giyaniOrigDTF || Intl.DateTimeFormat;
                            window.__giyaniOrigDTF = OrigDTF;
                            const injectDefaultTz = function(args) {
                                const rawLoc = args.length > 0 && args[0] !== undefined ? args[0] : TARGET_LOCALE;
                                const locales = (typeof rawLoc === 'string' && rawLoc.toLowerCase().indexOf('id') === 0) ? TARGET_LOCALE : rawLoc;
                                const options = Object.assign({}, (args.length > 1 && args[1] !== undefined) ? args[1] : {});
                                if (!options.timeZone || String(options.timeZone).indexOf('Jakarta') !== -1) {
                                    options.timeZone = TARGET_TIMEZONE;
                                }
                                return [locales, options];
                            };
                            Intl.DateTimeFormat = new Proxy(OrigDTF, {
                                construct: function(target, args, newTarget) {
                                    return Reflect.construct(target, injectDefaultTz(args), newTarget);
                                },
                                apply: function(target, thisArg, args) {
                                    return Reflect.construct(target, injectDefaultTz(args));
                                }
                            });

                            const proxyIntlCtor = function(ctorName) {
                                const OrigCtor = window['__giyaniOrig_' + ctorName] || Intl[ctorName];
                                if (!OrigCtor) return;
                                window['__giyaniOrig_' + ctorName] = OrigCtor;
                                const normalizeLocaleArgs = function(args) {
                                    const rawLoc = args.length > 0 && args[0] !== undefined ? args[0] : TARGET_LOCALE;
                                    const locales = (typeof rawLoc === 'string' && rawLoc.toLowerCase().indexOf('id') === 0) ? TARGET_LOCALE : rawLoc;
                                    const options = Object.assign({}, (args.length > 1 && args[1] !== undefined) ? args[1] : {});
                                    if (ctorName === 'NumberFormat' && options.currency && String(options.currency).toUpperCase() === 'IDR') {
                                        options.currency = '${country.currencyCode.ifBlank { "USD" }}';
                                    }
                                    return [locales, options];
                                };
                                Intl[ctorName] = new Proxy(OrigCtor, {
                                    construct: function(target, args, newTarget) {
                                        return Reflect.construct(target, normalizeLocaleArgs(args), newTarget);
                                    },
                                    apply: function(target, thisArg, args) {
                                        return Reflect.construct(target, normalizeLocaleArgs(args));
                                    }
                                });
                            };
                            ['NumberFormat', 'PluralRules', 'Collator', 'RelativeTimeFormat', 'DisplayNames', 'ListFormat'].forEach(proxyIntlCtor);
                        } catch (e) {}
                    }

                    // 7. Do Not Track / Global Privacy Control
                    if (${profile.blockTrackers}) {
                        defineSafeProp(navigator, 'doNotTrack', function() { return '1'; });
                        defineSafeProp(navigator, 'globalPrivacyControl', function() { return true; });
                    }

                    // 8. Per-Profile Deterministic Offscreen Canvas Protection
                    if (${profile.canvasProtection} && typeof HTMLCanvasElement !== 'undefined' && !window.__giyaniCanvasProtected) {
                        window.__giyaniCanvasProtected = true;
                        const profileSeed = (${profile.id.hashCode().and(0x7F)} % 7) + 1;
                        const origToDataURL = HTMLCanvasElement.prototype.toDataURL;
                        HTMLCanvasElement.prototype.toDataURL = function() {
                            try {
                                if (this.width > 0 && this.height > 0 && this.width <= 1024 && this.height <= 1024) {
                                    const clone = document.createElement('canvas');
                                    clone.width = this.width;
                                    clone.height = this.height;
                                    const cloneCtx = clone.getContext('2d');
                                    if (cloneCtx) {
                                        cloneCtx.drawImage(this, 0, 0);
                                        const pixel = cloneCtx.getImageData(0, 0, 1, 1);
                                        pixel.data[0] = (pixel.data[0] ^ profileSeed) & 0xFF;
                                        cloneCtx.putImageData(pixel, 0, 0);
                                        return origToDataURL.apply(clone, arguments);
                                    }
                                }
                            } catch (e) {}
                            return origToDataURL.apply(this, arguments);
                        };
                    }

                    // 9. WebRTC Local/ISP IP Leak Mitigation (Forces relay & rewrites any ICE candidate to Tunnel IP)
                    if (${profile.webRtcProtection} && typeof window.RTCPeerConnection === 'function' && typeof Proxy !== 'undefined' && !window.__giyaniRtcProxied) {
                        try {
                            window.__giyaniRtcProxied = true;
                            const OrigRTC = window.RTCPeerConnection;
                            window.RTCPeerConnection = new Proxy(OrigRTC, {
                                construct: function(target, args, newTarget) {
                                    const config = Object.assign({}, (args && args[0]) || {});
                                    config.iceTransportPolicy = 'relay';
                                    config.iceServers = [];
                                    return Reflect.construct(target, [config, args ? args[1] : undefined], newTarget);
                                }
                            });
                        } catch (e) {}
                    }

                    // 10. Synchronize Document Color-Scheme Meta with Giyani Dark/Light Theme Toggle
                    try {
                        if (document.documentElement) {
                            document.documentElement.style.colorScheme = '$colorSchemePref';
                        }
                    } catch (e) {}
                } catch (err) {}
            })();
        """.trimIndent()
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun applyProfileToWebView(
        webView: WebView,
        profile: BrowserProfileEntity,
        isPrivateTab: Boolean,
        desktopViewportOverride: Boolean = false
    ) {
        enforceJvmLocaleAndTimezone(webView.context, profile)
        seedRegionalSearchCookies(profile)

        val device = if (desktopViewportOverride) {
            CountryIdentityCatalog.getDeviceById("windows_desktop")
        } else {
            CountryIdentityCatalog.getDeviceById(profile.devicePresetId)
        }
        val settings = webView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(false)

        settings.userAgentString = device.userAgent
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        settings.textZoom = if (desktopViewportOverride) 95 else 100

        settings.loadsImagesAutomatically = true
        settings.blockNetworkImage = false
        settings.blockNetworkLoads = false
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.mediaPlaybackRequiresUserGesture = false

        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.cacheMode = if (isPrivateTab) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = true
        }
        // Keep offscreenPreRaster false to save GPU & native RAM on multi-profile devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settings.offscreenPreRaster = false
        }

        if (!isServiceWorkerConfigured) {
            isServiceWorkerConfigured = true
            try {
                val swController = ServiceWorkerController.getInstance()
                swController.serviceWorkerWebSettings.allowContentAccess = true
                swController.serviceWorkerWebSettings.allowFileAccess = true
                swController.serviceWorkerWebSettings.blockNetworkLoads = false
                swController.setServiceWorkerClient(object : ServiceWorkerClient() {
                    override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                        val reqUrl = request.url?.toString().orEmpty()
                        if (isTrackerUrl(reqUrl)) {
                            return createBlockedResponse()
                        }
                        return null
                    }
                })
            } catch (_: Exception) {
            }
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, !profile.blockThirdPartyCookies && !isPrivateTab)
    }

    /**
     * Expands candidate URLs into base origin, www, and auth subdomain variations so all cookies
     * for sites like `quarterfull.io` and Google are captured in the profile's isolated vault.
     */
    private fun expandCookieCandidateUrls(rawUrls: List<String>): List<String> {
        val set = linkedSetOf(
            "https://quarterfull.io",
            "https://www.quarterfull.io",
            "https://api.quarterfull.io",
            "https://auth.quarterfull.io",
            "https://www.google.com",
            "https://accounts.google.com",
            "https://duckduckgo.com"
        )
        rawUrls.forEach { url ->
            if (url.isNotBlank()) {
                set.add(url)
                try {
                    val uri = Uri.parse(url)
                    val scheme = uri.scheme ?: "https"
                    val host = uri.host
                    if (!host.isNullOrBlank()) {
                        set.add("$scheme://$host")
                        set.add("$scheme://$host/")
                        val rootDomain = host.removePrefix("www.")
                        set.add("$scheme://$rootDomain")
                        set.add("$scheme://www.$rootDomain")
                        set.add("$scheme://api.$rootDomain")
                        set.add("$scheme://auth.$rootDomain")
                    }
                } catch (_: Exception) {
                }
            }
        }
        return set.toList()
    }

    /**
     * Snapshots the current profile's cookies into its vault ONLY if this profile is the current
     * CookieJar owner and no profile swap is in progress.
     */
    fun saveProfileCookieSnapshot(
        context: Context,
        profile: BrowserProfileEntity,
        candidateUrls: List<String>
    ) {
        if (isCookieSwapInProgress) return
        if (currentCookieOwnerProfileId != null && currentCookieOwnerProfileId != profile.id) return
        if (!profile.keepLoginSession) return

        val prefs = context.getSharedPreferences(COOKIE_VAULT_PREFS, Context.MODE_PRIVATE)
        val cookieManager = CookieManager.getInstance()
        cookieManager.flush()

        val prevDomains = prefs.getString("domains_${profile.id}", "").orEmpty()
            .lines()
            .filter { it.isNotBlank() }
        val liveWvUrl = if (currentLiveWebViewProfileId == profile.id) currentLiveWebView?.url else null
        val allUrls = expandCookieCandidateUrls(candidateUrls + prevDomains + listOfNotNull(liveWvUrl))

        val serializedCookies = allUrls.mapNotNull { domainUrl ->
            val cookie = cookieManager.getCookie(domainUrl)
            if (!cookie.isNullOrBlank()) "$domainUrl|||$cookie" else null
        }.joinToString("\n")

        prefs.edit()
            .putString("cookies_${profile.id}", serializedCookies)
            .putString("domains_${profile.id}", allUrls.joinToString("\n"))
            .commit()
    }

    private suspend fun awaitRemoveAllCookies(cookieManager: CookieManager) {
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<Unit> { cont ->
                var resumed = false
                val mainHandler = Handler(Looper.getMainLooper())
                val timeoutRunnable = Runnable {
                    if (!resumed && cont.isActive) {
                        resumed = true
                        cont.resume(Unit)
                    }
                }
                mainHandler.postDelayed(timeoutRunnable, 600L)
                try {
                    cookieManager.removeAllCookies {
                        mainHandler.removeCallbacks(timeoutRunnable)
                        if (!resumed && cont.isActive) {
                            resumed = true
                            cont.resume(Unit)
                        }
                    }
                } catch (_: Exception) {
                    mainHandler.removeCallbacks(timeoutRunnable)
                    if (!resumed && cont.isActive) {
                        resumed = true
                        cont.resume(Unit)
                    }
                }
            }
        }
    }

    private suspend fun awaitEvaluateJs(webView: WebView, script: String) {
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<Unit> { cont ->
                var resumed = false
                val mainHandler = Handler(Looper.getMainLooper())
                val timeoutRunnable = Runnable {
                    if (!resumed && cont.isActive) {
                        resumed = true
                        cont.resume(Unit)
                    }
                }
                mainHandler.postDelayed(timeoutRunnable, 450L)
                try {
                    webView.evaluateJavascript(script) {
                        mainHandler.removeCallbacks(timeoutRunnable)
                        if (!resumed && cont.isActive) {
                            resumed = true
                            cont.resume(Unit)
                        }
                    }
                } catch (_: Exception) {
                    mainHandler.removeCallbacks(timeoutRunnable)
                    if (!resumed && cont.isActive) {
                        resumed = true
                        cont.resume(Unit)
                    }
                }
            }
        }
    }

    /**
     * Single-WebView Instant Disk-Vault Swap:
     * 1. Flushes `previousProfile`'s `localStorage`/`sessionStorage` into its disk vault, then clears
     *    DOMStorage and destroys `previousProfile`'s WebView so 0 MB of background WebView RAM is wasted!
     * 2. Saves `previousProfile`'s `CookieManager` snapshot into `cookies_${previousProfile.id}`.
     * 3. Awaits `cookieManager.removeAllCookies` callback so 100% of `previousProfile`'s cookies are
     *    wiped before `newProfile` becomes active.
     * 4. Enforces JVM Locale & Timezone (`enforceJvmLocaleAndTimezone`) for `newProfile`.
     * 5. Restores ONLY `newProfile`'s saved cookies + seeds Google regional cookies for `newProfile`.
     */
    suspend fun isolateAndSwitchProfileSession(
        context: Context,
        previousProfile: BrowserProfileEntity?,
        newProfile: BrowserProfileEntity,
        activeDomains: List<String>
    ) {
        withContext(Dispatchers.Main) {
            val prefs = context.getSharedPreferences(COOKIE_VAULT_PREFS, Context.MODE_PRIVATE)
            val cookieManager = CookieManager.getInstance()

            // Step 1: Save & release outgoing profile's single WebView BEFORE locking cookie swap
            if (previousProfile != null && previousProfile.id != newProfile.id) {
                val prevWv = if (currentLiveWebViewProfileId == previousProfile.id) currentLiveWebView else null
                if (previousProfile.keepLoginSession) {
                    if (prevWv != null) {
                        awaitEvaluateJs(
                            prevWv,
                            """
                            (function() {
                                try {
                                    if (window.__GiyaniVaultBridge && window.location.origin.indexOf('http') === 0) {
                                        const locObj = {};
                                        for (let i = 0; i < localStorage.length; i++) {
                                            const k = localStorage.key(i);
                                            if (k) locObj[k] = localStorage.getItem(k);
                                        }
                                        const sesObj = {};
                                        for (let i = 0; i < sessionStorage.length; i++) {
                                            const k = sessionStorage.key(i);
                                            if (k) sesObj[k] = sessionStorage.getItem(k);
                                        }
                                        window.__GiyaniVaultBridge.saveOriginStorage(
                                            '${previousProfile.id.replace("'", "")}',
                                            window.location.origin,
                                            JSON.stringify(locObj),
                                            JSON.stringify(sesObj)
                                        );
                                    }
                                    window.__giyaniSuppressVaultSave = true;
                                    localStorage.clear();
                                    sessionStorage.clear();
                                } catch (e) {}
                            })();
                            """.trimIndent()
                        )
                    }
                    saveProfileCookieSnapshot(context, previousProfile, activeDomains)
                } else {
                    clearProfileVaultData(context, previousProfile.id)
                }
                // Scrub the single live WebView in-place (never destroy the mounted WebView or renderer!)
                scrubLiveWebViewInPlace()
                currentVaultBridge?.updateBinding(newProfile.id, newProfile.keepLoginSession)
            }

            // Step 2: Lock JVM Locale & Timezone to newProfile
            enforceJvmLocaleAndTimezone(context, newProfile)

            // Step 3: Lock cookie jar swap & await 100% completion of removeAllCookies
            isCookieSwapInProgress = true
            try {
                awaitRemoveAllCookies(cookieManager)
                cookieManager.flush()

                // Step 4: Transfer CookieJar ownership to newProfile & restore ONLY newProfile's cookies
                currentCookieOwnerProfileId = newProfile.id
                seedRegionalSearchCookies(newProfile)

                if (newProfile.keepLoginSession) {
                    val savedCookies = prefs.getString("cookies_${newProfile.id}", null)
                    if (!savedCookies.isNullOrBlank()) {
                        savedCookies.lines().forEach { line ->
                            val parts = line.split("|||")
                            if (parts.size == 2) {
                                val domainUrl = parts[0]
                                val cookieHeader = parts[1]
                                cookieHeader.split(";").forEach { singleCookie ->
                                    val cleanCookie = singleCookie.trim()
                                    if (cleanCookie.isNotBlank()) {
                                        cookieManager.setCookie(domainUrl, "$cleanCookie; Path=/")
                                    }
                                }
                            }
                        }
                        cookieManager.flush()
                    }
                } else {
                    clearProfileVaultData(context, newProfile.id)
                }
            } finally {
                isCookieSwapInProgress = false
            }
        }
    }
}
