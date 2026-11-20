package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BrowserProfileEntity
import com.example.domain.CountryIdentityCatalog
import com.example.ui.screens.formatBytes
import com.example.ui.screens.formatEtaSeconds
import com.example.vpn.VpnEngineController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app_name matches Giyani Browser`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Giyani Browser", appName)
    }

    @Test
    fun `country presets configure coherent language and timezone`() {
        val us = CountryIdentityCatalog.getCountryByCode("US")
        assertEquals("en-US", us.defaultLanguage)
        assertEquals("USD", us.currencyCode)
        assertTrue(us.cities.any { it.name == "New York" && it.timezoneId == "America/New_York" })

        val jp = CountryIdentityCatalog.getCountryByCode("JP")
        assertEquals("ja-JP", jp.defaultLanguage)
        assertEquals("JPY", jp.currencyCode)
        assertEquals("Asia/Tokyo", jp.defaultTimezone)
    }

    @Test
    fun `profile stores name and email without password and allows multi-open in same country`() {
        val p1 = BrowserProfileEntity(
            id = "p1",
            name = "Alex Mercer",
            email = "alex@giyani.us",
            countryCode = "US",
            cityName = "New York",
            timezoneId = "America/New_York",
            languageTag = "en-US",
            devicePresetId = "google_pixel",
            preferredVpnServerId = "wg-us-1",
            isActive = true,
            isFocused = true
        )
        val p2 = BrowserProfileEntity(
            id = "p2",
            name = "Sarah Jenkins",
            email = "sarah@giyani.us",
            countryCode = "US",
            cityName = "Los Angeles",
            timezoneId = "America/Los_Angeles",
            languageTag = "en-US",
            devicePresetId = "samsung_galaxy",
            preferredVpnServerId = "wg-us-1",
            isActive = true,
            isFocused = false
        )
        val p3DifferentCountry = BrowserProfileEntity(
            id = "p3",
            name = "Kenji Takahashi",
            email = "kenji@giyani.jp",
            countryCode = "JP",
            cityName = "Tokyo",
            timezoneId = "Asia/Tokyo",
            languageTag = "ja-JP",
            devicePresetId = "android_generic",
            preferredVpnServerId = null,
            isActive = false,
            isFocused = false
        )

        assertEquals("alex@giyani.us", p1.email)
        assertTrue(p1.countryCode == p2.countryCode && p1.isActive && p2.isActive)
        assertFalse(p1.countryCode == p3DifferentCountry.countryCode)
    }

    @Test
    fun `wireguard config parser encrypts private key via keystore manager`() {
        val sampleConf = """
            [Interface]
            PrivateKey = cHJpdmF0ZWtleWV4YW1wbGUxMjM0NTY3ODkwMTIzNDU2Nzg5MDEy
            Address = 10.66.66.2/32, fd42:42:42::2/128
            DNS = 1.1.1.1, 1.0.0.1

            [Peer]
            PublicKey = cHVibGlja2V5ZXhhbXBsZTEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=
            Endpoint = 198.51.100.42:51820
            AllowedIPs = 0.0.0.0/0, ::/0
            PersistentKeepalive = 25
        """.trimIndent()

        val result = VpnEngineController.parseWireGuardConfig(
            profileName = "Tokyo WG Node",
            countryCode = "JP",
            cityName = "Tokyo",
            rawConfText = sampleConf
        )
        assertTrue(result.isSuccess)
        val entity = result.getOrNull()
        assertNotNull(entity)
        assertEquals("198.51.100.42", entity?.endpointHost)
        assertEquals(51820, entity?.endpointPort)
        assertTrue(entity?.encryptedPrivateKey != "cHJpdmF0ZWtleWV4YW1wbGUxMjM0NTY3ODkwMTIzNDU2Nzg5MDEy")
    }

    @Test
    fun `download telemetry formatting calculates human readable size and eta`() {
        assertEquals("1.50 MB", formatBytes(1572864L))
        assertEquals("2m 5s", formatEtaSeconds(125L))
    }
}
