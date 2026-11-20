package com.example.diagnostics

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.webkit.WebSettings
import androidx.core.content.ContextCompat
import com.example.data.BrowserProfileEntity
import com.example.domain.CountryIdentityCatalog
import com.example.vpn.VpnConnectionState
import com.example.vpn.VpnTunnelStatus

enum class DiagnosticStatus(val label: String) {
    AVAILABLE("Available"),
    PROTECTED("Protected"),
    RESTRICTED("Restricted"),
    UNAVAILABLE("Unavailable"),
    MISMATCH("Mismatch")
}

data class DiagnosticMetricItem(
    val id: String,
    val category: String,
    val title: String,
    val measuredValue: String,
    val status: DiagnosticStatus,
    val detailNote: String
)

object IdentityDiagnosticsEngine {

    fun evaluateDiagnostics(
        context: Context,
        profile: BrowserProfileEntity,
        vpnState: VpnConnectionState
    ): List<DiagnosticMetricItem> {
        val country = CountryIdentityCatalog.getCountryByCode(profile.countryCode)
        val device = CountryIdentityCatalog.getDeviceById(profile.devicePresetId)

        val defaultWebViewUa = try {
            WebSettings.getDefaultUserAgent(context)
        } catch (_: Exception) {
            "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36"
        }

        val displayMetrics = context.resources.displayMetrics
        val actualScreenPx = "${displayMetrics.widthPixels}x${displayMetrics.heightPixels} (Physical) → ${device.viewportWidth}x${device.viewportHeight} (Viewport Preset)"
        val actualCpuCores = Runtime.getRuntime().availableProcessors()

        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val ipTelemetry = vpnState.publicIpTelemetry
        val ipMatchesCountry = ipTelemetry.detectedCountryCode.equals(country.isoCode, ignoreCase = true)

        val systemDnsServers = getSystemDnsServers(context)
        val activeDnsDisplay = if (vpnState.status == VpnTunnelStatus.CONNECTED) {
            "VPN Tunnel DNS: ${vpnState.configuredDns}"
        } else {
            "System Network DNS: ${systemDnsServers.ifEmpty { "Auto / DHCP" }}"
        }

        return listOf(
            // 1. User-Agent
            DiagnosticMetricItem(
                id = "user_agent",
                category = "Browser Identity",
                title = "User-Agent String",
                measuredValue = device.userAgent,
                status = DiagnosticStatus.PROTECTED,
                detailNote = "Configured via ${device.name} preset. Native WebView UA ($defaultWebViewUa) is masked."
            ),
            // 2. Browser Version & Client Hints
            DiagnosticMetricItem(
                id = "browser_version",
                category = "Browser Identity",
                title = "Browser Version & Sec-CH-UA",
                measuredValue = "${device.browserName} | ${device.secChUa.ifBlank { "Sec-CH-UA Suppressed (Safari/iOS Compat)" }}",
                status = DiagnosticStatus.AVAILABLE,
                detailNote = "HTTP Client Hints headers synchronized with active User-Agent."
            ),
            // 3. Platform
            DiagnosticMetricItem(
                id = "platform",
                category = "Browser Identity",
                title = "Navigator Platform",
                measuredValue = "${device.platform} (Host OS: Android ${Build.VERSION.RELEASE})",
                status = DiagnosticStatus.PROTECTED,
                detailNote = "navigator.platform & Sec-CH-UA-Platform aligned to preset (${device.secChUaPlatform})."
            ),
            // 4. Language & Accept-Language
            DiagnosticMetricItem(
                id = "language",
                category = "Country & Locale",
                title = "Language & Accept-Language",
                measuredValue = "${profile.languageTag} (Header: ${country.acceptLanguageHeader})",
                status = DiagnosticStatus.PROTECTED,
                detailNote = "Synchronized with ${country.name} (${country.isoCode}) preset in HTTP headers & navigator.languages."
            ),
            // 5. Timezone
            DiagnosticMetricItem(
                id = "timezone",
                category = "Country & Locale",
                title = "Timezone (Intl.DateTimeFormat)",
                measuredValue = "${profile.timezoneId} (${profile.cityName}, ${country.name})",
                status = DiagnosticStatus.PROTECTED,
                detailNote = "Configured automatically from selected city (${profile.cityName})."
            ),
            // 6. Screen Dimensions
            DiagnosticMetricItem(
                id = "screen_dimensions",
                category = "Hardware & Viewport",
                title = "Screen & Viewport Dimensions",
                measuredValue = actualScreenPx,
                status = DiagnosticStatus.AVAILABLE,
                detailNote = "WebView viewport width & overview scaling configured to match ${device.name}."
            ),
            // 7. Device Pixel Ratio
            DiagnosticMetricItem(
                id = "device_pixel_ratio",
                category = "Hardware & Viewport",
                title = "Device Pixel Ratio (DPR)",
                measuredValue = "Preset: ${device.devicePixelRatio}x | Physical Display: ${displayMetrics.density}x",
                status = DiagnosticStatus.AVAILABLE,
                detailNote = "Runtime CSS pixel ratio reported by display density."
            ),
            // 8. Hardware Concurrency
            DiagnosticMetricItem(
                id = "hardware_concurrency",
                category = "Hardware & Viewport",
                title = "Hardware Concurrency (CPU Cores)",
                measuredValue = "${device.hardwareConcurrency} Logical Cores (Host Hardware: $actualCpuCores cores)",
                status = DiagnosticStatus.PROTECTED,
                detailNote = "Standardized via navigator.hardwareConcurrency override to prevent CPU core count fingerprinting."
            ),
            // 9. Device Memory
            DiagnosticMetricItem(
                id = "device_memory",
                category = "Hardware & Viewport",
                title = "Device Memory (RAM Tier)",
                measuredValue = "${device.deviceMemoryGb} GB Bucket",
                status = DiagnosticStatus.PROTECTED,
                detailNote = "Capped to standard W3C power-of-two bucket (${device.deviceMemoryGb} GB) to reduce entropy."
            ),
            // 10. WebGL Information
            DiagnosticMetricItem(
                id = "webgl_info",
                category = "Fingerprinting Surface",
                title = "WebGL Renderer & Vendor Exposure",
                measuredValue = "Hardware GPU (${Build.HARDWARE} / ${Build.BOARD})",
                status = DiagnosticStatus.RESTRICTED,
                detailNote = "Android WebView Limitation: Native GLSL driver strings are managed by the OS Chromium layer; full GPU driver virtualization requires Phase 4 Custom Chromium."
            ),
            // 11. Canvas Exposure Status
            DiagnosticMetricItem(
                id = "canvas_exposure",
                category = "Fingerprinting Surface",
                title = "Canvas 2D Readback Protection",
                measuredValue = if (profile.canvasProtection) "Deterministic Session Bit-Flip Active" else "Unmodified Canvas Readback",
                status = if (profile.canvasProtection) DiagnosticStatus.PROTECTED else DiagnosticStatus.AVAILABLE,
                detailNote = if (profile.canvasProtection) {
                    "HTMLCanvasElement.toDataURL applies deterministic, non-flickering noise per profile."
                } else {
                    "Canvas protection is disabled for this profile."
                }
            ),
            // 12. Audio Fingerprinting Exposure
            DiagnosticMetricItem(
                id = "audio_exposure",
                category = "Fingerprinting Surface",
                title = "WebAudio Context Diagnostics",
                measuredValue = "User-Gesture Gated (Autoplay Blocked)",
                status = DiagnosticStatus.RESTRICTED,
                detailNote = "Media playback requires explicit user gesture; background AudioContext oscillator fingerprinting is restricted."
            ),
            // 13. WebRTC Exposure Status
            DiagnosticMetricItem(
                id = "webrtc_exposure",
                category = "Network & Location",
                title = "WebRTC ICE Candidate Policy",
                measuredValue = if (profile.webRtcProtection) "Relay-Only (STUN Local IP Masking)" else "Default ICE Candidates",
                status = if (profile.webRtcProtection) DiagnosticStatus.PROTECTED else DiagnosticStatus.AVAILABLE,
                detailNote = if (profile.webRtcProtection) {
                    "RTCPeerConnection forced to iceTransportPolicy='relay' to prevent local LAN IP leakage."
                } else {
                    "WebRTC peer connections can inspect local network interfaces."
                }
            ),
            // 14. Geolocation Permission & Transparency
            DiagnosticMetricItem(
                id = "geolocation_permission",
                category = "Network & Location",
                title = "Geolocation & Location Distinction",
                measuredValue = buildString {
                    append("OS GPS Permission: ")
                    append(if (hasFineLocation || hasCoarseLocation) "Granted" else "Not Granted")
                    append(" | Browser Policy: ${profile.geolocationPolicy}")
                    if (profile.geolocationPolicy == "SIMULATED_TESTING") {
                        append(" (Testing Lat/Lon: ${profile.simulatedLat}, ${profile.simulatedLon})")
                    }
                },
                status = when (profile.geolocationPolicy) {
                    "BLOCKED" -> DiagnosticStatus.PROTECTED
                    "SIMULATED_TESTING" -> DiagnosticStatus.RESTRICTED
                    else -> DiagnosticStatus.AVAILABLE
                },
                detailNote = "Strictly distinguishes Actual OS GPS, Browser W3C Geolocation Permission, Simulated Dev Testing Location, and VPN IP Location."
            ),
            // 15. Public IP
            DiagnosticMetricItem(
                id = "public_ip",
                category = "Network & Location",
                title = "Public IPv4 / IPv6 Address",
                measuredValue = "${ipTelemetry.publicIp} (${ipTelemetry.asnOrg})",
                status = when {
                    vpnState.status == VpnTunnelStatus.FAIL_CLOSED_BLOCKING -> DiagnosticStatus.PROTECTED
                    ipTelemetry.error != null -> DiagnosticStatus.UNAVAILABLE
                    vpnState.status == VpnTunnelStatus.CONNECTED -> DiagnosticStatus.PROTECTED
                    else -> DiagnosticStatus.AVAILABLE
                },
                detailNote = if (vpnState.status == VpnTunnelStatus.CONNECTED) {
                    "Routed via Per-App WireGuard tunnel (${vpnState.assignedTunnelIpv4})."
                } else {
                    "Direct ISP network connection (VPN tunnel is not active)."
                }
            ),
            // 16. IP-Derived Country vs Selected Country Preset
            DiagnosticMetricItem(
                id = "ip_country_match",
                category = "Network & Location",
                title = "IP-Derived Country vs Selected Preset",
                measuredValue = "IP Location: ${ipTelemetry.detectedCountryName} (${ipTelemetry.detectedCountryCode}) | Selected Preset: ${country.name} (${country.isoCode})",
                status = when {
                    ipTelemetry.detectedCountryCode == "--" -> DiagnosticStatus.UNAVAILABLE
                    ipMatchesCountry -> DiagnosticStatus.PROTECTED
                    else -> DiagnosticStatus.MISMATCH
                },
                detailNote = if (ipMatchesCountry) {
                    "Public IP geolocation matches the selected ${country.name} identity preset."
                } else {
                    "Transparency Notice: Selecting ${country.name} configures locale/timezone headers, NOT your physical IP. Connect to a ${country.name} WireGuard VPN endpoint to align your IP country."
                }
            ),
            // 17. DNS Configuration Status
            DiagnosticMetricItem(
                id = "dns_status",
                category = "Network & Location",
                title = "DNS Resolver Status",
                measuredValue = activeDnsDisplay,
                status = if (vpnState.status == VpnTunnelStatus.CONNECTED) DiagnosticStatus.PROTECTED else DiagnosticStatus.RESTRICTED,
                detailNote = if (vpnState.status == VpnTunnelStatus.CONNECTED) {
                    "DNS queries from Giyani Browser are routed inside the WireGuard tunnel."
                } else {
                    "Using system DNS resolver. Connect VPN to enforce encrypted tunnel DNS."
                }
            )
        )
    }

    private fun getSystemDnsServers(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNet = cm?.activeNetwork ?: return ""
            val linkProps = cm.getLinkProperties(activeNet) ?: return ""
            linkProps.dnsServers.joinToString(", ") { it.hostAddress ?: "" }
        } catch (_: Exception) {
            ""
        }
    }
}
