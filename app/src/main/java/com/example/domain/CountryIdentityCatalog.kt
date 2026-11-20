package com.example.domain

data class CityPreset(
    val name: String,
    val region: String,
    val timezoneId: String,
    val utcOffset: String,
    val defaultLat: Double,
    val defaultLon: Double
)

data class CountryPreset(
    val isoCode: String,
    val name: String,
    val flagEmoji: String,
    val defaultLanguage: String,
    val acceptLanguageHeader: String,
    val currencyCode: String,
    val currencySymbol: String,
    val dateFormat: String,
    val measurementSystem: String, // "Metric" or "Imperial"
    val defaultTimezone: String,
    val cities: List<CityPreset>,
    val vpnRegions: List<String>
)

data class DevicePreset(
    val id: String,
    val name: String,
    val category: String, // Brand / Form-Factor Category
    val userAgent: String,
    val browserName: String,
    val platform: String,
    val secChUa: String,
    val secChUaMobile: String,
    val secChUaPlatform: String,
    val viewportWidth: Int,
    val viewportHeight: Int,
    val devicePixelRatio: Float,
    val maxTouchPoints: Int,
    val hardwareConcurrency: Int,
    val deviceMemoryGb: Int,
    val useWideViewPort: Boolean
)

data class BuiltInWireGuardPreset(
    val id: String,
    val name: String,
    val serverTag: String,
    val countryCode: String,
    val cityName: String,
    val endpointHost: String,
    val endpointPort: Int,
    val tunnelAddressIpv4: String,
    val tunnelAddressIpv6: String?,
    val dnsServers: String,
    val publicKey: String,
    val privateKeyPlain: String,
    val allowedIps: String = "0.0.0.0/0, ::/0",
    val persistentKeepalive: Int = 25,
    val rawConfText: String
)

object CountryIdentityCatalog {
    val countries: List<CountryPreset> = listOf(
        CountryPreset(
            isoCode = "US",
            name = "United States",
            flagEmoji = "🇺🇸",
            defaultLanguage = "en-US",
            acceptLanguageHeader = "en-US,en;q=0.9",
            currencyCode = "USD",
            currencySymbol = "$",
            dateFormat = "MM/DD/YYYY",
            measurementSystem = "Imperial",
            defaultTimezone = "America/New_York",
            cities = listOf(
                CityPreset("New York", "US-East (NY)", "America/New_York", "UTC-05:00", 40.7128, -74.0060),
                CityPreset("Los Angeles", "US-West (CA)", "America/Los_Angeles", "UTC-08:00", 34.0522, -118.2437),
                CityPreset("Chicago", "US-Central (IL)", "America/Chicago", "UTC-06:00", 41.8781, -87.6298),
                CityPreset("Miami", "US-South (FL)", "America/New_York", "UTC-05:00", 25.7617, -80.1918),
                CityPreset("Dallas", "US-South Central (TX)", "America/Chicago", "UTC-06:00", 32.7767, -96.7970),
                CityPreset("Seattle", "US-Northwest (WA)", "America/Los_Angeles", "UTC-08:00", 47.6062, -122.3321),
                CityPreset("San Francisco", "Silicon Valley (CA)", "America/Los_Angeles", "UTC-08:00", 37.7749, -122.4194),
                CityPreset("Atlanta", "US-Southeast (GA)", "America/New_York", "UTC-05:00", 33.7490, -84.3880),
                CityPreset("Denver", "US-Mountain (CO)", "America/Denver", "UTC-07:00", 39.7392, -104.9903)
            ),
            vpnRegions = listOf("US-FREE#1", "US-FREE#2", "US-FREE#4", "US-FREE#27")
        ),
        CountryPreset(
            isoCode = "NL",
            name = "Netherlands",
            flagEmoji = "🇳🇱",
            defaultLanguage = "nl-NL",
            acceptLanguageHeader = "nl-NL,nl;q=0.9,en-US;q=0.8,en;q=0.7",
            currencyCode = "EUR",
            currencySymbol = "€",
            dateFormat = "DD-MM-YYYY",
            measurementSystem = "Metric",
            defaultTimezone = "Europe/Amsterdam",
            cities = listOf(
                CityPreset("Amsterdam", "North Holland (AMS)", "Europe/Amsterdam", "UTC+01:00", 52.3676, 4.9041),
                CityPreset("Rotterdam", "South Holland (RTM)", "Europe/Amsterdam", "UTC+01:00", 51.9244, 4.4777),
                CityPreset("The Hague", "South Holland (HAG)", "Europe/Amsterdam", "UTC+01:00", 52.0705, 4.3007),
                CityPreset("Utrecht", "Utrecht (UTR)", "Europe/Amsterdam", "UTC+01:00", 52.0907, 5.1214),
                CityPreset("Eindhoven", "North Brabant (EIN)", "Europe/Amsterdam", "UTC+01:00", 51.4416, 5.4697)
            ),
            vpnRegions = listOf("NL-FREE#2")
        ),
        CountryPreset(
            isoCode = "JP",
            name = "Japan",
            flagEmoji = "🇯🇵",
            defaultLanguage = "ja-JP",
            acceptLanguageHeader = "ja-JP,ja;q=0.9,en-US;q=0.7",
            currencyCode = "JPY",
            currencySymbol = "¥",
            dateFormat = "YYYY/MM/DD",
            measurementSystem = "Metric",
            defaultTimezone = "Asia/Tokyo",
            cities = listOf(
                CityPreset("Tokyo", "Kanto (TYO)", "Asia/Tokyo", "UTC+09:00", 35.6762, 139.6503),
                CityPreset("Osaka", "Kansai (KIX)", "Asia/Tokyo", "UTC+09:00", 34.6937, 135.5023),
                CityPreset("Yokohama", "Kanagawa (YOK)", "Asia/Tokyo", "UTC+09:00", 35.4437, 139.6380),
                CityPreset("Nagoya", "Chubu (NGO)", "Asia/Tokyo", "UTC+09:00", 35.1815, 136.9066),
                CityPreset("Fukuoka", "Kyushu (FUK)", "Asia/Tokyo", "UTC+09:00", 33.5902, 130.4017),
                CityPreset("Sapporo", "Hokkaido (CTS)", "Asia/Tokyo", "UTC+09:00", 43.0618, 141.3545)
            ),
            vpnRegions = listOf("JP-FREE#26")
        ),
        CountryPreset(
            isoCode = "CA",
            name = "Canada",
            flagEmoji = "🇨🇦",
            defaultLanguage = "en-CA",
            acceptLanguageHeader = "en-CA,en;q=0.9,fr-CA;q=0.8",
            currencyCode = "CAD",
            currencySymbol = "CA$",
            dateFormat = "YYYY-MM-DD",
            measurementSystem = "Metric",
            defaultTimezone = "America/Toronto",
            cities = listOf(
                CityPreset("Toronto", "Ontario (YYZ)", "America/Toronto", "UTC-05:00", 43.6532, -79.3832),
                CityPreset("Vancouver", "British Columbia (YVR)", "America/Vancouver", "UTC-08:00", 49.2827, -123.1207),
                CityPreset("Montreal", "Quebec (YUL)", "America/Toronto", "UTC-05:00", 45.5017, -73.5673),
                CityPreset("Calgary", "Alberta (YYC)", "America/Edmonton", "UTC-07:00", 51.0447, -114.0719),
                CityPreset("Ottawa", "Capital Region (YOW)", "America/Toronto", "UTC-05:00", 45.4215, -75.6972)
            ),
            vpnRegions = listOf("CA-FREE#34")
        ),
        CountryPreset(
            isoCode = "MX",
            name = "Mexico",
            flagEmoji = "🇲🇽",
            defaultLanguage = "es-MX",
            acceptLanguageHeader = "es-MX,es;q=0.9,en-US;q=0.8",
            currencyCode = "MXN",
            currencySymbol = "MX$",
            dateFormat = "DD/MM/YYYY",
            measurementSystem = "Metric",
            defaultTimezone = "America/Mexico_City",
            cities = listOf(
                CityPreset("Mexico City", "CDMX (MEX)", "America/Mexico_City", "UTC-06:00", 19.4326, -99.1332),
                CityPreset("Guadalajara", "Jalisco (GDL)", "America/Mexico_City", "UTC-06:00", 20.6597, -103.3496),
                CityPreset("Monterrey", "Nuevo León (MTY)", "America/Monterrey", "UTC-06:00", 25.6866, -100.3161),
                CityPreset("Cancún", "Quintana Roo (CUN)", "America/Cancun", "UTC-05:00", 21.1619, -86.8515),
                CityPreset("Tijuana", "Baja California (TIJ)", "America/Tijuana", "UTC-08:00", 32.5149, -117.0382)
            ),
            vpnRegions = listOf("MX-FREE#16")
        ),
        CountryPreset(
            isoCode = "CH",
            name = "Switzerland",
            flagEmoji = "🇨🇭",
            defaultLanguage = "de-CH",
            acceptLanguageHeader = "de-CH,de;q=0.9,fr-CH;q=0.8,en;q=0.7",
            currencyCode = "CHF",
            currencySymbol = "CHF",
            dateFormat = "DD.MM.YYYY",
            measurementSystem = "Metric",
            defaultTimezone = "Europe/Zurich",
            cities = listOf(
                CityPreset("Zurich", "Zurich (ZRH)", "Europe/Zurich", "UTC+01:00", 47.3769, 8.5417),
                CityPreset("Geneva", "Romandy (GVA)", "Europe/Zurich", "UTC+01:00", 46.2044, 6.1432),
                CityPreset("Basel", "Basel-Stadt (BSL)", "Europe/Zurich", "UTC+01:00", 47.5596, 7.5886),
                CityPreset("Bern", "Capital (BRN)", "Europe/Zurich", "UTC+01:00", 46.9480, 7.4474),
                CityPreset("Lausanne", "Vaud (QLS)", "Europe/Zurich", "UTC+01:00", 46.5197, 6.6323)
            ),
            vpnRegions = listOf("CH-FREE#11")
        ),
        CountryPreset(
            isoCode = "NO",
            name = "Norway",
            flagEmoji = "🇳🇴",
            defaultLanguage = "nb-NO",
            acceptLanguageHeader = "nb-NO,nb;q=0.9,no;q=0.8,en-US;q=0.7",
            currencyCode = "NOK",
            currencySymbol = "kr",
            dateFormat = "DD.MM.YYYY",
            measurementSystem = "Metric",
            defaultTimezone = "Europe/Oslo",
            cities = listOf(
                CityPreset("Oslo", "Eastern Norway (OSL)", "Europe/Oslo", "UTC+01:00", 59.9139, 10.7522),
                CityPreset("Bergen", "Vestland (BGO)", "Europe/Oslo", "UTC+01:00", 60.3913, 5.3221),
                CityPreset("Trondheim", "Trøndelag (TRD)", "Europe/Oslo", "UTC+01:00", 63.4305, 10.3951),
                CityPreset("Stavanger", "Rogaland (SVG)", "Europe/Oslo", "UTC+01:00", 58.9700, 5.7331)
            ),
            vpnRegions = listOf("NO-FREE#7")
        ),
        CountryPreset(
            isoCode = "ID",
            name = "Indonesia",
            flagEmoji = "🇮🇩",
            defaultLanguage = "id-ID",
            acceptLanguageHeader = "id-ID,id;q=0.9,en-US;q=0.8",
            currencyCode = "IDR",
            currencySymbol = "Rp",
            dateFormat = "DD/MM/YYYY",
            measurementSystem = "Metric",
            defaultTimezone = "Asia/Jakarta",
            cities = listOf(
                CityPreset("Jakarta", "WIB (DKI Jakarta)", "Asia/Jakarta", "UTC+07:00", -6.2088, 106.8456),
                CityPreset("Surabaya", "WIB (Jawa Timur)", "Asia/Jakarta", "UTC+07:00", -7.2575, 112.7521),
                CityPreset("Bandung", "WIB (Jawa Barat)", "Asia/Jakarta", "UTC+07:00", -6.9175, 107.6191),
                CityPreset("Medan", "WIB (Sumatera Utara)", "Asia/Jakarta", "UTC+07:00", 3.5952, 98.6722),
                CityPreset("Yogyakarta", "WIB (DIY)", "Asia/Jakarta", "UTC+07:00", -7.7956, 110.3695),
                CityPreset("Denpasar", "WITA (Bali)", "Asia/Makassar", "UTC+08:00", -8.6705, 115.2126),
                CityPreset("Makassar", "WITA (Sulawesi Selatan)", "Asia/Makassar", "UTC+08:00", -5.1477, 119.4327),
                CityPreset("Balikpapan", "WITA (Kalimantan Timur)", "Asia/Makassar", "UTC+08:00", -1.2379, 116.8529),
                CityPreset("Jayapura", "WIT (Papua)", "Asia/Jayapura", "UTC+09:00", -2.5337, 140.7181)
            ),
            vpnRegions = listOf("id-cgk-wg-01", "id-sub-wg-02")
        ),
        CountryPreset(
            isoCode = "DE",
            name = "Germany",
            flagEmoji = "🇩🇪",
            defaultLanguage = "de-DE",
            acceptLanguageHeader = "de-DE,de;q=0.9,en;q=0.8",
            currencyCode = "EUR",
            currencySymbol = "€",
            dateFormat = "DD.MM.YYYY",
            measurementSystem = "Metric",
            defaultTimezone = "Europe/Berlin",
            cities = listOf(
                CityPreset("Frankfurt", "Hesse (FRA)", "Europe/Berlin", "UTC+01:00", 50.1109, 8.6821),
                CityPreset("Berlin", "Berlin (BER)", "Europe/Berlin", "UTC+01:00", 52.5200, 13.4050),
                CityPreset("Munich", "Bavaria (MUC)", "Europe/Berlin", "UTC+01:00", 48.1351, 11.5820),
                CityPreset("Hamburg", "Northern Germany (HAM)", "Europe/Berlin", "UTC+01:00", 53.5511, 9.9937),
                CityPreset("Düsseldorf", "North Rhine-Westphalia (DUS)", "Europe/Berlin", "UTC+01:00", 51.2277, 6.7735)
            ),
            vpnRegions = listOf("de-fra-core-01", "de-ber-edge-02")
        ),
        CountryPreset(
            isoCode = "GB",
            name = "United Kingdom",
            flagEmoji = "🇬🇧",
            defaultLanguage = "en-GB",
            acceptLanguageHeader = "en-GB,en;q=0.9",
            currencyCode = "GBP",
            currencySymbol = "£",
            dateFormat = "DD/MM/YYYY",
            measurementSystem = "Metric",
            defaultTimezone = "Europe/London",
            cities = listOf(
                CityPreset("London", "England South (LON)", "Europe/London", "UTC+00:00", 51.5074, -0.1278),
                CityPreset("Manchester", "England North (MAN)", "Europe/London", "UTC+00:00", 53.4808, -2.2426),
                CityPreset("Birmingham", "West Midlands (BHX)", "Europe/London", "UTC+00:00", 52.4862, -1.8904),
                CityPreset("Edinburgh", "Scotland (EDI)", "Europe/London", "UTC+00:00", 55.9533, -3.1883),
                CityPreset("Glasgow", "Scotland West (GLA)", "Europe/London", "UTC+00:00", 55.8642, -4.2518)
            ),
            vpnRegions = listOf("gb-lon-wg-01", "gb-man-wg-02")
        ),
        CountryPreset(
            isoCode = "SG",
            name = "Singapore",
            flagEmoji = "🇸🇬",
            defaultLanguage = "en-SG",
            acceptLanguageHeader = "en-SG,en;q=0.9,zh-SG;q=0.8",
            currencyCode = "SGD",
            currencySymbol = "S$",
            dateFormat = "DD/MM/YYYY",
            measurementSystem = "Metric",
            defaultTimezone = "Asia/Singapore",
            cities = listOf(
                CityPreset("Singapore Central", "Marina Bay (SIN)", "Asia/Singapore", "UTC+08:00", 1.3521, 103.8198),
                CityPreset("Jurong West", "West Region (JUR)", "Asia/Singapore", "UTC+08:00", 1.3404, 103.7090),
                CityPreset("Changi", "East Region (CHG)", "Asia/Singapore", "UTC+08:00", 1.3644, 103.9915),
                CityPreset("Woodlands", "North Region (WDL)", "Asia/Singapore", "UTC+08:00", 1.4382, 103.7890)
            ),
            vpnRegions = listOf("sg-sin-core-01")
        ),
        CountryPreset(
            isoCode = "KR",
            name = "South Korea",
            flagEmoji = "🇰🇷",
            defaultLanguage = "ko-KR",
            acceptLanguageHeader = "ko-KR,ko;q=0.9,en-US;q=0.8",
            currencyCode = "KRW",
            currencySymbol = "₩",
            dateFormat = "YYYY.MM.DD",
            measurementSystem = "Metric",
            defaultTimezone = "Asia/Seoul",
            cities = listOf(
                CityPreset("Seoul", "Capital Area (ICN)", "Asia/Seoul", "UTC+09:00", 37.5665, 126.9780),
                CityPreset("Busan", "Yeongnam (PUS)", "Asia/Seoul", "UTC+09:00", 35.1796, 129.0756),
                CityPreset("Incheon", "Gyeonggi (INC)", "Asia/Seoul", "UTC+09:00", 37.4563, 126.7052),
                CityPreset("Daegu", "North Gyeongsang (TAE)", "Asia/Seoul", "UTC+09:00", 35.8714, 128.6014)
            ),
            vpnRegions = listOf("kr-sel-wg-01")
        ),
        CountryPreset(
            isoCode = "AU",
            name = "Australia",
            flagEmoji = "🇦🇺",
            defaultLanguage = "en-AU",
            acceptLanguageHeader = "en-AU,en;q=0.9",
            currencyCode = "AUD",
            currencySymbol = "A$",
            dateFormat = "DD/MM/YYYY",
            measurementSystem = "Metric",
            defaultTimezone = "Australia/Sydney",
            cities = listOf(
                CityPreset("Sydney", "New South Wales (SYD)", "Australia/Sydney", "UTC+10:00", -33.8688, 151.2093),
                CityPreset("Melbourne", "Victoria (MEL)", "Australia/Melbourne", "UTC+10:00", -37.8136, 144.9631),
                CityPreset("Brisbane", "Queensland (BNE)", "Australia/Brisbane", "UTC+10:00", -27.4705, 153.0260),
                CityPreset("Perth", "Western Australia (PER)", "Australia/Perth", "UTC+08:00", -31.9505, 115.8605)
            ),
            vpnRegions = listOf("au-syd-wg-01", "au-mel-wg-02")
        ),
        CountryPreset(
            isoCode = "FR",
            name = "France",
            flagEmoji = "🇫🇷",
            defaultLanguage = "fr-FR",
            acceptLanguageHeader = "fr-FR,fr;q=0.9,en-US;q=0.8",
            currencyCode = "EUR",
            currencySymbol = "€",
            dateFormat = "DD/MM/YYYY",
            measurementSystem = "Metric",
            defaultTimezone = "Europe/Paris",
            cities = listOf(
                CityPreset("Paris", "Île-de-France (CDG)", "Europe/Paris", "UTC+01:00", 48.8566, 2.3522),
                CityPreset("Marseille", "Provence (MRS)", "Europe/Paris", "UTC+01:00", 43.2965, 5.3698),
                CityPreset("Lyon", "Auvergne-Rhône-Alpes (LYS)", "Europe/Paris", "UTC+01:00", 45.7640, 4.8357),
                CityPreset("Nice", "Côte d'Azur (NCE)", "Europe/Paris", "UTC+01:00", 43.7102, 7.2620)
            ),
            vpnRegions = listOf("fr-par-wg-01")
        ),
        CountryPreset(
            isoCode = "BR",
            name = "Brazil",
            flagEmoji = "🇧🇷",
            defaultLanguage = "pt-BR",
            acceptLanguageHeader = "pt-BR,pt;q=0.9,en-US;q=0.8",
            currencyCode = "BRL",
            currencySymbol = "R$",
            dateFormat = "DD/MM/YYYY",
            measurementSystem = "Metric",
            defaultTimezone = "America/Sao_Paulo",
            cities = listOf(
                CityPreset("São Paulo", "Southeast (GRU)", "America/Sao_Paulo", "UTC-03:00", -23.5505, -46.6333),
                CityPreset("Rio de Janeiro", "Southeast (GIG)", "America/Sao_Paulo", "UTC-03:00", -22.9068, -43.1729),
                CityPreset("Brasília", "Federal District (BSB)", "America/Sao_Paulo", "UTC-03:00", -15.7975, -47.8919)
            ),
            vpnRegions = listOf("br-gru-wg-01")
        ),
        CountryPreset(
            isoCode = "IN",
            name = "India",
            flagEmoji = "🇮🇳",
            defaultLanguage = "en-IN",
            acceptLanguageHeader = "en-IN,en;q=0.9,hi-IN;q=0.8",
            currencyCode = "INR",
            currencySymbol = "₹",
            dateFormat = "DD/MM/YYYY",
            measurementSystem = "Metric",
            defaultTimezone = "Asia/Kolkata",
            cities = listOf(
                CityPreset("Mumbai", "Maharashtra (BOM)", "Asia/Kolkata", "UTC+05:30", 19.0760, 72.8777),
                CityPreset("New Delhi", "NCR (DEL)", "Asia/Kolkata", "UTC+05:30", 28.6139, 77.2090),
                CityPreset("Bengaluru", "Karnataka (BLR)", "Asia/Kolkata", "UTC+05:30", 12.9716, 77.5946),
                CityPreset("Hyderabad", "Telangana (HYD)", "Asia/Kolkata", "UTC+05:30", 17.3850, 78.4867)
            ),
            vpnRegions = listOf("in-bom-wg-01")
        ),
        CountryPreset(
            isoCode = "AE",
            name = "United Arab Emirates",
            flagEmoji = "🇦🇪",
            defaultLanguage = "ar-AE",
            acceptLanguageHeader = "ar-AE,ar;q=0.9,en-US;q=0.8",
            currencyCode = "AED",
            currencySymbol = "AED",
            dateFormat = "DD/MM/YYYY",
            measurementSystem = "Metric",
            defaultTimezone = "Asia/Dubai",
            cities = listOf(
                CityPreset("Dubai", "Dubai Emirate (DXB)", "Asia/Dubai", "UTC+04:00", 25.2048, 55.2708),
                CityPreset("Abu Dhabi", "Capital Emirate (AUH)", "Asia/Dubai", "UTC+04:00", 24.4539, 54.3773)
            ),
            vpnRegions = listOf("ae-dxb-wg-01")
        ),
        CountryPreset(
            isoCode = "MY",
            name = "Malaysia",
            flagEmoji = "🇲🇾",
            defaultLanguage = "ms-MY",
            acceptLanguageHeader = "ms-MY,ms;q=0.9,en-MY;q=0.8,en;q=0.7",
            currencyCode = "MYR",
            currencySymbol = "RM",
            dateFormat = "DD/MM/YYYY",
            measurementSystem = "Metric",
            defaultTimezone = "Asia/Kuala_Lumpur",
            cities = listOf(
                CityPreset("Kuala Lumpur", "Federal Territory (KUL)", "Asia/Kuala_Lumpur", "UTC+08:00", 3.1390, 101.6869),
                CityPreset("George Town", "Penang (PEN)", "Asia/Kuala_Lumpur", "UTC+08:00", 5.4141, 100.3288),
                CityPreset("Johor Bahru", "Johor (JHB)", "Asia/Kuala_Lumpur", "UTC+08:00", 1.4927, 103.7414)
            ),
            vpnRegions = listOf("my-kul-wg-01")
        )
    )

    val devicePresets: List<DevicePreset> = listOf(
        // 1. GOOGLE PIXEL
        DevicePreset(
            id = "google_pixel",
            name = "Google Pixel 9 Pro XL",
            category = "Google Pixel",
            userAgent = "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro XL) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 412,
            viewportHeight = 915,
            devicePixelRatio = 2.625f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 16,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "google_pixel_8a",
            name = "Google Pixel 8a",
            category = "Google Pixel",
            userAgent = "Mozilla/5.0 (Linux; Android 15; Pixel 8a) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 412,
            viewportHeight = 892,
            devicePixelRatio = 2.625f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 8,
            useWideViewPort = true
        ),

        // 2. SAMSUNG GALAXY
        DevicePreset(
            id = "samsung_galaxy",
            name = "Samsung Galaxy S24 Ultra",
            category = "Samsung",
            userAgent = "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 412,
            viewportHeight = 915,
            devicePixelRatio = 3.0f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 12,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "samsung_zfold6",
            name = "Samsung Galaxy Z Fold6",
            category = "Samsung",
            userAgent = "Mozilla/5.0 (Linux; Android 14; SM-F956B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 690,
            viewportHeight = 828,
            devicePixelRatio = 2.625f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 12,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "samsung_a55",
            name = "Samsung Galaxy A55 5G",
            category = "Samsung",
            userAgent = "Mozilla/5.0 (Linux; Android 14; SM-A556E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 384,
            viewportHeight = 854,
            devicePixelRatio = 2.8125f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 8,
            useWideViewPort = true
        ),

        // 3. XIAOMI / REDMI / POCO
        DevicePreset(
            id = "xiaomi_14",
            name = "Xiaomi 14 Ultra (HyperOS)",
            category = "Xiaomi / POCO",
            userAgent = "Mozilla/5.0 (Linux; Android 14; 24030PN60G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 393,
            viewportHeight = 873,
            devicePixelRatio = 3.0f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 16,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "redmi_note13_pro",
            name = "Redmi Note 13 Pro+ 5G",
            category = "Xiaomi / POCO",
            userAgent = "Mozilla/5.0 (Linux; Android 14; 23090RA98G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 393,
            viewportHeight = 873,
            devicePixelRatio = 3.0f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 12,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "poco_f6_pro",
            name = "POCO F6 Pro 5G",
            category = "Xiaomi / POCO",
            userAgent = "Mozilla/5.0 (Linux; Android 14; 23113RKC6G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 412,
            viewportHeight = 915,
            devicePixelRatio = 3.5f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 12,
            useWideViewPort = true
        ),

        // 4. OPPO / ONEPLUS / REALME
        DevicePreset(
            id = "oppo_find_x7",
            name = "OPPO Find X7 Ultra",
            category = "OPPO / OnePlus",
            userAgent = "Mozilla/5.0 (Linux; Android 14; PHY110) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 412,
            viewportHeight = 919,
            devicePixelRatio = 3.5f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 16,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "oppo_reno12_pro",
            name = "OPPO Reno12 Pro 5G",
            category = "OPPO / OnePlus",
            userAgent = "Mozilla/5.0 (Linux; Android 14; CPH2629) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 393,
            viewportHeight = 873,
            devicePixelRatio = 2.75f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 12,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "oneplus_12",
            name = "OnePlus 12 (OxygenOS 14)",
            category = "OPPO / OnePlus",
            userAgent = "Mozilla/5.0 (Linux; Android 14; CPH2581) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 412,
            viewportHeight = 919,
            devicePixelRatio = 3.5f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 16,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "realme_gt6",
            name = "Realme GT 6 5G",
            category = "Realme",
            userAgent = "Mozilla/5.0 (Linux; Android 14; RMX3851) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 412,
            viewportHeight = 915,
            devicePixelRatio = 3.0f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 12,
            useWideViewPort = true
        ),

        // 5. VIVO / iQOO
        DevicePreset(
            id = "vivo_x100_pro",
            name = "Vivo X100 Pro (Funtouch OS)",
            category = "Vivo / iQOO",
            userAgent = "Mozilla/5.0 (Linux; Android 14; V2309) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 412,
            viewportHeight = 915,
            devicePixelRatio = 3.0f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 16,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "iqoo_12_5g",
            name = "iQOO 12 5G (Snapdragon 8 Gen 3)",
            category = "Vivo / iQOO",
            userAgent = "Mozilla/5.0 (Linux; Android 14; I2220) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 412,
            viewportHeight = 915,
            devicePixelRatio = 3.0f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 16,
            useWideViewPort = true
        ),

        // 6. ASUS ROG / SONY / MOTOROLA / NOTHING / HONOR / HUAWEI / INFINIX / TECNO
        DevicePreset(
            id = "asus_rog_phone8",
            name = "ASUS ROG Phone 8 Pro",
            category = "Gaming & Flagship",
            userAgent = "Mozilla/5.0 (Linux; Android 14; ASUS_AI2401_A) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 412,
            viewportHeight = 915,
            devicePixelRatio = 2.625f,
            maxTouchPoints = 10,
            hardwareConcurrency = 8,
            deviceMemoryGb = 24,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "nothing_phone_2",
            name = "Nothing Phone (2) (Nothing OS)",
            category = "Gaming & Flagship",
            userAgent = "Mozilla/5.0 (Linux; Android 14; A065) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 412,
            viewportHeight = 915,
            devicePixelRatio = 2.625f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 12,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "sony_xperia_1vi",
            name = "Sony Xperia 1 VI",
            category = "Gaming & Flagship",
            userAgent = "Mozilla/5.0 (Linux; Android 14; XQ-EC54) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 393,
            viewportHeight = 851,
            devicePixelRatio = 2.75f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 12,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "motorola_edge50",
            name = "Motorola Edge 50 Ultra",
            category = "Gaming & Flagship",
            userAgent = "Mozilla/5.0 (Linux; Android 14; motorola edge 50 ultra) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 412,
            viewportHeight = 915,
            devicePixelRatio = 3.0f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 16,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "honor_magic6_pro",
            name = "Honor Magic6 Pro (MagicOS)",
            category = "Honor / Huawei",
            userAgent = "Mozilla/5.0 (Linux; Android 14; BVL-N49) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 412,
            viewportHeight = 915,
            devicePixelRatio = 3.0f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 12,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "huawei_pura70",
            name = "Huawei Pura 70 Ultra",
            category = "Honor / Huawei",
            userAgent = "Mozilla/5.0 (Linux; Android 12; HBP-LX9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 412,
            viewportHeight = 915,
            devicePixelRatio = 3.0f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 16,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "infinix_gt20_pro",
            name = "Infinix GT 20 Pro 5G",
            category = "Infinix / Tecno",
            userAgent = "Mozilla/5.0 (Linux; Android 14; Infinix X6871) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 393,
            viewportHeight = 873,
            devicePixelRatio = 2.75f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 12,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "tecno_camon30",
            name = "Tecno Camon 30 Premier 5G",
            category = "Infinix / Tecno",
            userAgent = "Mozilla/5.0 (Linux; Android 14; TECNO CL9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Mobile 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 412,
            viewportHeight = 915,
            devicePixelRatio = 3.0f,
            maxTouchPoints = 5,
            hardwareConcurrency = 8,
            deviceMemoryGb = 12,
            useWideViewPort = true
        ),

        // 7. APPLE iOS / iPadOS COMPATIBILITY
        DevicePreset(
            id = "iphone_compat",
            name = "iPhone 16 Pro Max (iOS 18.1)",
            category = "Apple iOS / iPadOS",
            userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1",
            browserName = "Mobile Safari 18.1",
            platform = "iPhone",
            secChUa = "",
            secChUaMobile = "?1",
            secChUaPlatform = "\"iOS\"",
            viewportWidth = 440,
            viewportHeight = 956,
            devicePixelRatio = 3.0f,
            maxTouchPoints = 5,
            hardwareConcurrency = 6,
            deviceMemoryGb = 8,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "iphone_15_pro",
            name = "iPhone 15 Pro (iOS 17.6)",
            category = "Apple iOS / iPadOS",
            userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Mobile/15E148 Safari/604.1",
            browserName = "Mobile Safari 17.6",
            platform = "iPhone",
            secChUa = "",
            secChUaMobile = "?1",
            secChUaPlatform = "\"iOS\"",
            viewportWidth = 393,
            viewportHeight = 852,
            devicePixelRatio = 3.0f,
            maxTouchPoints = 5,
            hardwareConcurrency = 6,
            deviceMemoryGb = 8,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "ipad_pro_m4",
            name = "iPad Pro 13\" M4 (iPadOS 18)",
            category = "Apple iOS / iPadOS",
            userAgent = "Mozilla/5.0 (iPad; CPU OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1",
            browserName = "iPad Safari 18.1",
            platform = "iPad",
            secChUa = "",
            secChUaMobile = "?0",
            secChUaPlatform = "\"iPadOS\"",
            viewportWidth = 1032,
            viewportHeight = 1376,
            devicePixelRatio = 2.0f,
            maxTouchPoints = 5,
            hardwareConcurrency = 10,
            deviceMemoryGb = 16,
            useWideViewPort = true
        ),

        // 8. PRIVACY & DESKTOP WORKSTATIONS
        DevicePreset(
            id = "android_generic",
            name = "Android Generic (Privacy Standardized)",
            category = "Privacy & Desktop",
            userAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            browserName = "Chrome Reduced UA 131",
            platform = "Linux armv81",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
            secChUaMobile = "?1",
            secChUaPlatform = "\"Android\"",
            viewportWidth = 393,
            viewportHeight = 851,
            devicePixelRatio = 2.75f,
            maxTouchPoints = 5,
            hardwareConcurrency = 4,
            deviceMemoryGb = 4,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "windows_desktop",
            name = "Windows 11 Desktop (Chrome 131)",
            category = "Privacy & Desktop",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            browserName = "Chrome Desktop 131",
            platform = "Win32",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?0",
            secChUaPlatform = "\"Windows\"",
            viewportWidth = 1440,
            viewportHeight = 900,
            devicePixelRatio = 1.25f,
            maxTouchPoints = 0,
            hardwareConcurrency = 8,
            deviceMemoryGb = 8,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "mac_desktop",
            name = "macOS Sequoia Desktop (Chrome 131)",
            category = "Privacy & Desktop",
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            browserName = "Chrome macOS 131",
            platform = "MacIntel",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?0",
            secChUaPlatform = "\"macOS\"",
            viewportWidth = 1440,
            viewportHeight = 900,
            devicePixelRatio = 2.0f,
            maxTouchPoints = 0,
            hardwareConcurrency = 10,
            deviceMemoryGb = 16,
            useWideViewPort = true
        ),
        DevicePreset(
            id = "linux_ubuntu_desktop",
            name = "Linux Ubuntu 24.04 Desktop (Chrome 131)",
            category = "Privacy & Desktop",
            userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            browserName = "Chrome Linux 131",
            platform = "Linux x86_64",
            secChUa = "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"131\"",
            secChUaMobile = "?0",
            secChUaPlatform = "\"Linux\"",
            viewportWidth = 1440,
            viewportHeight = 900,
            devicePixelRatio = 1.0f,
            maxTouchPoints = 0,
            hardwareConcurrency = 8,
            deviceMemoryGb = 16,
            useWideViewPort = true
        )
    )

    /**
     * The 10 real WireGuard configurations embedded directly into Giyani Browser.
     */
    val builtInWireGuardConfigs: List<BuiltInWireGuardPreset> = listOf(
        // 1. Freza Tampan (US-FREE#1)
        BuiltInWireGuardPreset(
            id = "wg_freza_us_1",
            name = "Freza Tampan • US-FREE#1",
            serverTag = "US-FREE#1",
            countryCode = "US",
            cityName = "New York",
            endpointHost = "146.70.230.146",
            endpointPort = 51820,
            tunnelAddressIpv4 = "10.2.0.2/32",
            tunnelAddressIpv6 = "2a07:b944::2:2/128",
            dnsServers = "10.2.0.1, 2a07:b944::2:1",
            publicKey = "gucaLaM/mgJQbHVvnZNtW+1L4Mi7E2mtTMrhS0K4miU=",
            privateKeyPlain = "OGuQXpfCP1OCDkHH8jRrYsKKyZbCheGGVZYkHNvUtW4=",
            allowedIps = "0.0.0.0/0, ::/0",
            persistentKeepalive = 25,
            rawConfText = """
                [Interface]
                # Key for Freza Tampan
                # Bouncing = 2
                # NAT-PMP (Penerusan Port) = off
                # VPN Accelerator = on
                PrivateKey = OGuQXpfCP1OCDkHH8jRrYsKKyZbCheGGVZYkHNvUtW4=
                Address = 10.2.0.2/32, 2a07:b944::2:2/128
                DNS = 10.2.0.1, 2a07:b944::2:1

                [Peer]
                # US-FREE#1
                PublicKey = gucaLaM/mgJQbHVvnZNtW+1L4Mi7E2mtTMrhS0K4miU=
                AllowedIPs = 0.0.0.0/0, ::/0
                Endpoint = 146.70.230.146:51820

                # Uncomment the following line (delete the # symbol) to connect to Proton VPN using IPv6.
                # Endpoint = [2a0d:5600:4f:23::10]:51820
                PersistentKeepalive = 25
            """.trimIndent()
        ),
        // 2. Freza USA 2 (US-FREE#2)
        BuiltInWireGuardPreset(
            id = "wg_freza_us_2",
            name = "Freza USA 2 • US-FREE#2",
            serverTag = "US-FREE#2",
            countryCode = "US",
            cityName = "Los Angeles",
            endpointHost = "89.187.185.161",
            endpointPort = 51820,
            tunnelAddressIpv4 = "10.2.0.2/32",
            tunnelAddressIpv6 = "2a07:b944::2:2/128",
            dnsServers = "10.2.0.1, 2a07:b944::2:1",
            publicKey = "ZEQHDxg/HbjznRvApyBWfUGs6T20Rvy0/DctZk6FvB4=",
            privateKeyPlain = "8FAtZSEaV8Uuwizy8tesUzdJWdYmIzvXNekVMtH2qns=",
            allowedIps = "0.0.0.0/0, ::/0",
            persistentKeepalive = 25,
            rawConfText = """
                [Interface]
                # Key for Freza USA 2
                # Bouncing = 1
                # NAT-PMP (Penerusan Port) = on
                # VPN Accelerator = on
                PrivateKey = 8FAtZSEaV8Uuwizy8tesUzdJWdYmIzvXNekVMtH2qns=
                Address = 10.2.0.2/32, 2a07:b944::2:2/128
                DNS = 10.2.0.1, 2a07:b944::2:1

                [Peer]
                # US-FREE#2
                PublicKey = ZEQHDxg/HbjznRvApyBWfUGs6T20Rvy0/DctZk6FvB4=
                AllowedIPs = 0.0.0.0/0, ::/0
                Endpoint = 89.187.185.161:51820

                # Uncomment the following line (delete the # symbol) to connect to Proton VPN using IPv6.
                # Endpoint = [2a02:6ea0:c822::10]:51820
                PersistentKeepalive = 25
            """.trimIndent()
        ),
        // 3. Freza USA 3 (US-FREE#4)
        BuiltInWireGuardPreset(
            id = "wg_freza_us_3",
            name = "Freza USA 3 • US-FREE#4",
            serverTag = "US-FREE#4",
            countryCode = "US",
            cityName = "Chicago",
            endpointHost = "146.70.230.130",
            endpointPort = 51820,
            tunnelAddressIpv4 = "10.2.0.2/32",
            tunnelAddressIpv6 = "2a07:b944::2:2/128",
            dnsServers = "10.2.0.1, 2a07:b944::2:1",
            publicKey = "g0gQj1lhsEuyJcJlzVyjq7aQmtC+/ca05vqOqvCQ/zI=",
            privateKeyPlain = "EFYfSXaWBv2KQL6Syb6Xfwz+kTWwOzswIDvgJi6jn1Y=",
            allowedIps = "0.0.0.0/0, ::/0",
            persistentKeepalive = 25,
            rawConfText = """
                [Interface]
                # Key for Freza USA 3
                # Bouncing = 3
                # NAT-PMP (Penerusan Port) = on
                # VPN Accelerator = on
                PrivateKey = EFYfSXaWBv2KQL6Syb6Xfwz+kTWwOzswIDvgJi6jn1Y=
                Address = 10.2.0.2/32, 2a07:b944::2:2/128
                DNS = 10.2.0.1, 2a07:b944::2:1

                [Peer]
                # US-FREE#4
                PublicKey = g0gQj1lhsEuyJcJlzVyjq7aQmtC+/ca05vqOqvCQ/zI=
                AllowedIPs = 0.0.0.0/0, ::/0
                Endpoint = 146.70.230.130:51820

                # Uncomment the following line (delete the # symbol) to connect to Proton VPN using IPv6.
                # Endpoint = [2a0d:5600:4f:22::10]:51820
                PersistentKeepalive = 25
            """.trimIndent()
        ),
        // 4. Freza USA 4 (US-FREE#27)
        BuiltInWireGuardPreset(
            id = "wg_freza_us_4",
            name = "Freza USA 4 • US-FREE#27",
            serverTag = "US-FREE#27",
            countryCode = "US",
            cityName = "Miami",
            endpointHost = "149.22.88.58",
            endpointPort = 51820,
            tunnelAddressIpv4 = "10.2.0.2/32",
            tunnelAddressIpv6 = null,
            dnsServers = "10.2.0.1",
            publicKey = "W0ke+9ooJLu/AnDM3pcbkn8e+PfjScXywBCsXdlEmwE=",
            privateKeyPlain = "eOe5M0HLwJj+dxJcmWLodwW4Q3eTYyzex1kZP521KlM=",
            allowedIps = "0.0.0.0/0, ::/0",
            persistentKeepalive = 25,
            rawConfText = """
                [Interface]
                # Key for Freza USA 4
                # Bouncing = 0
                # NAT-PMP (Penerusan Port) = on
                # VPN Accelerator = on
                PrivateKey = eOe5M0HLwJj+dxJcmWLodwW4Q3eTYyzex1kZP521KlM=
                Address = 10.2.0.2/32
                DNS = 10.2.0.1

                [Peer]
                # US-FREE#27
                PublicKey = W0ke+9ooJLu/AnDM3pcbkn8e+PfjScXywBCsXdlEmwE=
                AllowedIPs = 0.0.0.0/0, ::/0
                Endpoint = 149.22.88.58:51820

                PersistentKeepalive = 25
            """.trimIndent()
        ),
        // 5. Belanda 1 (NL-FREE#2)
        BuiltInWireGuardPreset(
            id = "wg_freza_nl_1",
            name = "Belanda 1 • NL-FREE#2",
            serverTag = "NL-FREE#2",
            countryCode = "NL",
            cityName = "Amsterdam",
            endpointHost = "149.36.51.3",
            endpointPort = 51820,
            tunnelAddressIpv4 = "10.2.0.2/32",
            tunnelAddressIpv6 = null,
            dnsServers = "10.2.0.1",
            publicKey = "zcfEQgUoPLK2kvDicsA/4B9F2AI7qfxX5ueDo/Zcjj4=",
            privateKeyPlain = "kHW2lH9il+ogceGCmA02MTxDkUEzLEvUV9073gZQrGI=",
            allowedIps = "0.0.0.0/0, ::/0",
            persistentKeepalive = 25,
            rawConfText = """
                [Interface]
                # Key for Belanda 1
                # Bouncing = 5
                # NAT-PMP (Penerusan Port) = on
                # VPN Accelerator = on
                PrivateKey = kHW2lH9il+ogceGCmA02MTxDkUEzLEvUV9073gZQrGI=
                Address = 10.2.0.2/32
                DNS = 10.2.0.1

                [Peer]
                # NL-FREE#2
                PublicKey = zcfEQgUoPLK2kvDicsA/4B9F2AI7qfxX5ueDo/Zcjj4=
                AllowedIPs = 0.0.0.0/0, ::/0
                Endpoint = 149.36.51.3:51820

                PersistentKeepalive = 25
            """.trimIndent()
        ),
        // 6. Jepang 1 (JP-FREE#26)
        BuiltInWireGuardPreset(
            id = "wg_freza_jp_1",
            name = "Jepang 1 • JP-FREE#26",
            serverTag = "JP-FREE#26",
            countryCode = "JP",
            cityName = "Tokyo",
            endpointHost = "212.102.51.110",
            endpointPort = 51820,
            tunnelAddressIpv4 = "10.2.0.2/32",
            tunnelAddressIpv6 = null,
            dnsServers = "10.2.0.1",
            publicKey = "mortbpQLtauCXw+rSFfEty9xZa1kEAeB9iB/c8sw6Wo=",
            privateKeyPlain = "gMSCCNBpbwYNzR4EndkOMTTGAylm19sQ4ijOgV8o4kI=",
            allowedIps = "0.0.0.0/0, ::/0",
            persistentKeepalive = 25,
            rawConfText = """
                [Interface]
                # Key for Jepang 1
                # Bouncing = 0
                # NAT-PMP (Penerusan Port) = on
                # VPN Accelerator = on
                PrivateKey = gMSCCNBpbwYNzR4EndkOMTTGAylm19sQ4ijOgV8o4kI=
                Address = 10.2.0.2/32
                DNS = 10.2.0.1

                [Peer]
                # JP-FREE#26
                PublicKey = mortbpQLtauCXw+rSFfEty9xZa1kEAeB9iB/c8sw6Wo=
                AllowedIPs = 0.0.0.0/0, ::/0
                Endpoint = 212.102.51.110:51820

                PersistentKeepalive = 25
            """.trimIndent()
        ),
        // 7. Kanada 1 (CA-FREE#34)
        BuiltInWireGuardPreset(
            id = "wg_freza_ca_1",
            name = "Kanada 1 • CA-FREE#34",
            serverTag = "CA-FREE#34",
            countryCode = "CA",
            cityName = "Toronto",
            endpointHost = "195.242.214.210",
            endpointPort = 51820,
            tunnelAddressIpv4 = "10.2.0.2/32",
            tunnelAddressIpv6 = "2a07:b944::2:2/128",
            dnsServers = "10.2.0.1, 2a07:b944::2:1",
            publicKey = "iKGECV5NZgXQ8mDSvupfeStGXBHDiO8VOOLs5U7KpGc=",
            privateKeyPlain = "IIU8kP0q+prsd8D1DXdFppLdFuzSpf2EWSs/tND9kF8=",
            allowedIps = "0.0.0.0/0, ::/0",
            persistentKeepalive = 25,
            rawConfText = """
                [Interface]
                # Key for Kanada 1
                # Bouncing = 1
                # NAT-PMP (Penerusan Port) = on
                # VPN Accelerator = on
                PrivateKey = IIU8kP0q+prsd8D1DXdFppLdFuzSpf2EWSs/tND9kF8=
                Address = 10.2.0.2/32, 2a07:b944::2:2/128
                DNS = 10.2.0.1, 2a07:b944::2:1

                [Peer]
                # CA-FREE#34
                PublicKey = iKGECV5NZgXQ8mDSvupfeStGXBHDiO8VOOLs5U7KpGc=
                AllowedIPs = 0.0.0.0/0, ::/0
                Endpoint = 195.242.214.210:51820

                # Uncomment the following line (delete the # symbol) to connect to Proton VPN using IPv6.
                # Endpoint = [2a0d:5600:19:42::10]:51820
                PersistentKeepalive = 25
            """.trimIndent()
        ),
        // 8. Meksiko 1 (MX-FREE#16)
        BuiltInWireGuardPreset(
            id = "wg_freza_mx_1",
            name = "Meksiko 1 • MX-FREE#16",
            serverTag = "MX-FREE#16",
            countryCode = "MX",
            cityName = "Mexico City",
            endpointHost = "84.20.27.53",
            endpointPort = 51820,
            tunnelAddressIpv4 = "10.2.0.2/32",
            tunnelAddressIpv6 = null,
            dnsServers = "10.2.0.1",
            publicKey = "mkI+cC9ggzfMdZy1cl3Fl01gPJJxsLXjshXAN8EedQ8=",
            privateKeyPlain = "wBVRyJndcxZAi80Ysd/Z30jU5VMizBPChk3q7bi6qnI=",
            allowedIps = "0.0.0.0/0, ::/0",
            persistentKeepalive = 25,
            rawConfText = """
                [Interface]
                # Key for Meksiko 1
                # Bouncing = 0
                # NAT-PMP (Penerusan Port) = on
                # VPN Accelerator = on
                PrivateKey = wBVRyJndcxZAi80Ysd/Z30jU5VMizBPChk3q7bi6qnI=
                Address = 10.2.0.2/32
                DNS = 10.2.0.1

                [Peer]
                # MX-FREE#16
                PublicKey = mkI+cC9ggzfMdZy1cl3Fl01gPJJxsLXjshXAN8EedQ8=
                AllowedIPs = 0.0.0.0/0, ::/0
                Endpoint = 84.20.27.53:51820

                PersistentKeepalive = 25
            """.trimIndent()
        ),
        // 9. Swiss 1 (CH-FREE#11)
        BuiltInWireGuardPreset(
            id = "wg_freza_ch_1",
            name = "Swiss 1 • CH-FREE#11",
            serverTag = "CH-FREE#11",
            countryCode = "CH",
            cityName = "Zurich",
            endpointHost = "135.136.39.34",
            endpointPort = 51820,
            tunnelAddressIpv4 = "10.2.0.2/32",
            tunnelAddressIpv6 = null,
            dnsServers = "10.2.0.1",
            publicKey = "L6Vcy4PWQOyzFsjOI0GNCDypvzZfTuuZ14qmR1sT5QE=",
            privateKeyPlain = "CN67Gx1WcjGtRiNPN6H6Zaeip05eusxk95d3RERLZWk=",
            allowedIps = "0.0.0.0/0, ::/0",
            persistentKeepalive = 25,
            rawConfText = """
                [Interface]
                # Key for swiss 1
                # Bouncing = 0
                # NAT-PMP (Penerusan Port) = on
                # VPN Accelerator = on
                PrivateKey = CN67Gx1WcjGtRiNPN6H6Zaeip05eusxk95d3RERLZWk=
                Address = 10.2.0.2/32
                DNS = 10.2.0.1

                [Peer]
                # CH-FREE#11
                PublicKey = L6Vcy4PWQOyzFsjOI0GNCDypvzZfTuuZ14qmR1sT5QE=
                AllowedIPs = 0.0.0.0/0, ::/0
                Endpoint = 135.136.39.34:51820

                PersistentKeepalive = 25
            """.trimIndent()
        ),
        // 10. Norwegia 1 (NO-FREE#7)
        BuiltInWireGuardPreset(
            id = "wg_freza_no_1",
            name = "Norwegia 1 • NO-FREE#7",
            serverTag = "NO-FREE#7",
            countryCode = "NO",
            cityName = "Oslo",
            endpointHost = "95.173.205.166",
            endpointPort = 51820,
            tunnelAddressIpv4 = "10.2.0.2/32",
            tunnelAddressIpv6 = "2a07:b944::2:2/128",
            dnsServers = "10.2.0.1, 2a07:b944::2:1",
            publicKey = "Z2pGk23p2uYtzedagHPvrF4jKm3cLpZkPedHWlQtkmk=",
            privateKeyPlain = "qJt6vCw5WkLF9yjAYpjTxTV4rpOqQCwLOBfh8xt4iWM=",
            allowedIps = "0.0.0.0/0, ::/0",
            persistentKeepalive = 25,
            rawConfText = """
                [Interface]
                # Key for Norwegia 1
                # Bouncing = 3
                # NAT-PMP (Penerusan Port) = on
                # VPN Accelerator = on
                PrivateKey = qJt6vCw5WkLF9yjAYpjTxTV4rpOqQCwLOBfh8xt4iWM=
                Address = 10.2.0.2/32, 2a07:b944::2:2/128
                DNS = 10.2.0.1, 2a07:b944::2:1

                [Peer]
                # NO-FREE#7
                PublicKey = Z2pGk23p2uYtzedagHPvrF4jKm3cLpZkPedHWlQtkmk=
                AllowedIPs = 0.0.0.0/0, ::/0
                Endpoint = 95.173.205.166:51820

                # Uncomment the following line (delete the # symbol) to connect to Proton VPN using IPv6.
                # Endpoint = [2a02:6ea0:5601:6473::10]:51820
                PersistentKeepalive = 25
            """.trimIndent()
        )
    )

    fun getCountryByCode(isoCode: String): CountryPreset {
        return countries.find { it.isoCode.equals(isoCode, ignoreCase = true) } ?: countries.first()
    }

    fun getDeviceById(id: String): DevicePreset {
        return devicePresets.find { it.id == id } ?: devicePresets.first()
    }
}
