package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.GiyaniDestination
import com.example.viewmodel.GiyaniUiState
import com.example.viewmodel.GiyaniViewModel

data class GuideChapter(
    val id: String,
    val category: String,
    val stepNumber: String,
    val title: String,
    val summary: String,
    val bulletPoints: List<String>,
    val codeSnippetTitle: String? = null,
    val codeSnippet: String? = null,
    val actionLabel: String? = null,
    val actionDestination: GiyaniDestination? = null,
    val practiceTemplateConf: Boolean = false
)

object UserGuideCatalog {
    const val SAMPLE_WIREGUARD_TEMPLATE = """[Interface]
# Template Latihan WireGuard Giyani Browser
PrivateKey = Y29udG9oUHJpdmF0ZUtleVVudHVrTGF0aWhhbkdpeWFuaUJyb3dzZXI=
Address = 10.66.66.2/32, fd42:42:42::2/128
DNS = 1.1.1.1, 1.0.0.1

[Peer]
PublicKey = Y29udG9oUHVibGljS2V5U2VydmVyVnBuV2lyZUd1YXJkUmVzbWk=
Endpoint = 198.51.100.42:51820
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25"""

    const val VPS_INSTALL_COMMANDS = """curl -O https://raw.githubusercontent.com/angristan/wireguard-install/master/wireguard-install.sh
chmod +x wireguard-install.sh
sudo ./wireguard-install.sh
# Setelah selesai, tampilkan file .conf untuk disalin ke Giyani Browser:
cat ~/wg0-client-giyani-browser.conf"""

    val chapters: List<GuideChapter> = listOf(
        GuideChapter(
            id = "chapter_chrome_panels",
            category = "PEMULA",
            stepNumber = "BAB 1",
            title = "Cara Pakai Tampilan Chrome (Left-Panel & Right-Panel)",
            summary = "Mengenal tombol buka-tutup (toggle) Panel Kiri, Panel Kanan, dan Bar Multi-Profile di bagian atas layar browser.",
            bulletPoints = listOf(
                "Baris Paling Atas (Multi-Profile Strip): Menampilkan semua profile yang sedang dibuka bersamaan dalam 1 negara. Ketuk nama profile untuk berpindah akun secara instan.",
                "Baris Tengah (Chrome Tab Strip): Menampilkan tab yang sedang aktif. Ketuk tombol '+' untuk membuka tab baru atau '×' untuk menutup tab.",
                "Tombol Kiri '[2P]' (Toggle Left-Panel): Ketuk tombol di sebelah kiri kotak alamat web untuk memunculkan/menyembunyikan Panel Kiri (Manajemen Profile & Tab).",
                "Tombol Kanan 'Tune' (Toggle Right-Panel): Ketuk ikon pengaturan di sebelah kanan kotak alamat web untuk memunculkan/menyembunyikan Panel Kanan (Pilih Negara, Kota, Device Preset, & Sakelar VPN).",
                "Menutup Panel: Cukup ketuk area layar gelap di tengah atau tombol '×' di pojok atas panel."
            ),
            actionLabel = "Buka Layar Utama Chrome Browser",
            actionDestination = GiyaniDestination.BROWSER_WEBVIEW
        ),
        GuideChapter(
            id = "chapter_multi_profile",
            category = "PROFILE",
            stepNumber = "BAB 2",
            title = "Workflow Multi-Profile (Nama & Email Tanpa Password)",
            summary = "Cara membuat, mengedit, menghapus, menggunakan profile, dan membuka banyak profile sekaligus dalam 1 Negara atau 1 VPN.",
            bulletPoints = listOf(
                "Buat Profile Tanpa Password: Buka Left-Panel ('[2P]') lalu ketuk 'Buat Profile Baru'. Cukup isi Nama & Email beserta Negara, Kota, dan Device Preset.",
                "Identitas Browser Tersendiri: Tiap profile memiliki Cookies, penyimpanan login, riwayat browsing, bookmark, zona waktu, bahasa, dan User-Agent yang terpisah total.",
                "Buka Banyak Profile Bersamaan (Syarat 1 Negara / 1 VPN): Anda bebas menekan 'Buka Bersama' pada banyak profile selama semuanya berada di 1 Negara yang sama (misal: sama-sama US).",
                "Pindah ke Profile Negara Lain: Jika Anda memilih profile dari negara berbeda (misal dari US pindah ke Jepang), ketuk 'Pindah Negara & Use'. Aplikasi otomatis menyesuaikan sesi aktif ke negara baru tersebut.",
                "Edit & Hapus Profile: Ketuk ikon Pensil (Edit) pada kartu profile untuk mengubah Nama, Email, Negara, atau Perangkat, dan ikon Tempat Sampah untuk menghapus profile."
            ),
            actionLabel = "Buka Manajemen Profile",
            actionDestination = GiyaniDestination.PROFILES_PRIVACY
        ),
        GuideChapter(
            id = "chapter_wireguard_basics",
            category = "VPN",
            stepNumber = "BAB 3",
            title = "Mengenal WireGuard VPN & Template Latihan (.conf)",
            summary = "Penjelasan bahasa awam tentang isi konfigurasi WireGuard dan template siap pakai untuk berlatih Import di Giyani Browser.",
            bulletPoints = listOf(
                "Apa itu File .conf WireGuard? Hanya teks singkat berisi 2 bagian: [Interface] (kunci rahasia & IP virtual HP Anda) dan [Peer] (kunci publik & alamat server VPN tujuan).",
                "Keamanan Android Keystore: Saat Anda menekan 'Encrypt & Import', Giyani Browser langsung mengenkripsi PrivateKey Anda ke dalam chip keamanan Android Keystore (AES-256-GCM).",
                "Per-App VPN (Khusus Browser): Hanya lalu lintas Giyani Browser yang melewati terowongan VPN. Aplikasi lain di HP Anda (WhatsApp, M-Banking, Ojek Online) tetap memakai jaringan lokal normal.",
                "Fail-Closed Kill Switch: Jika diaktifkan, Giyani Browser otomatis menahan koneksi web apabila server VPN terputus agar IP asli Anda tidak bocor.",
                "Latihan Sekarang: Anda bisa menyalin contoh template di bawah ini atau langsung menekan tombol 'Praktikkan Import Template ke VPN Engine'."
            ),
            codeSnippetTitle = "Contoh Template Standar WireGuard (.conf):",
            codeSnippet = SAMPLE_WIREGUARD_TEMPLATE,
            actionLabel = "Praktikkan: Muat Template Latihan & Buka VPN",
            actionDestination = GiyaniDestination.VPN_MANAGER,
            practiceTemplateConf = true
        ),
        GuideChapter(
            id = "chapter_free_commercial_vpn",
            category = "VPN",
            stepNumber = "BAB 4",
            title = "Cara Pasang VPN Gratis & Komersial (ProtonVPN / WARP / Mullvad)",
            summary = "Langkah demi langkah mendapatkan file WireGuard (.conf) asli dari penyedia VPN gratis maupun berbayar.",
            bulletPoints = listOf(
                "1. ProtonVPN (100% Gratis — Rekomendasi Pemula): Daftar akun gratis di account.protonvpn.com -> Buka menu 'Downloads' -> Gulir ke 'WireGuard configuration' -> Pilih platform Android & pilih server FREE (US, Japan, atau Netherlands) -> Klik 'Create' & Download file .conf.",
                "2. Cloudflare WARP Gratis (wgcf): Gunakan alat open-source 'wgcf' (jalankan 'wgcf register' lalu 'wgcf generate') untuk mendapatkan file 'wgcf-profile.conf' jaringan Cloudflare.",
                "3. Mullvad / Windscribe / IVPN: Masuk ke dashboard akun -> Buka 'WireGuard Config Generator' -> Pilih negara & kota -> Unduh file .conf.",
                "4. Cara Memasukkan ke Giyani Browser: Buka file .conf yang sudah diunduh dengan aplikasi teks/catatan -> Salin (Copy) seluruh isinya -> Buka menu 'VPN' di Giyani Browser -> Ketuk 'Import .conf' -> Tempel (Paste) -> Ketuk 'Encrypt & Import' -> Ketuk 'Connect'!"
            ),
            actionLabel = "Buka Layar VPN untuk Import .conf",
            actionDestination = GiyaniDestination.VPN_MANAGER
        ),
        GuideChapter(
            id = "chapter_vps_self_hosted",
            category = "VPS",
            stepNumber = "BAB 5",
            title = "Membuat Server WireGuard Sendiri di VPS (5 Menit)",
            summary = "Panduan membuat server VPN pribadi di VPS Ubuntu/Debian (DigitalOcean, Vultr, Linode, IDCloudHost) dan menghubungkannya ke Giyani Browser.",
            bulletPoints = listOf(
                "Langkah 1: Siapkan 1 VPS murah (Ubuntu 22.04/24.04 atau Debian 12) di lokasi negara yang Anda inginkan (misal Singapura, Tokyo, atau Frankfurt).",
                "Langkah 2: Login ke terminal SSH VPS Anda, lalu jalankan perintah instalasi otomatis di bawah ini.",
                "Langkah 3: Tekan Enter untuk pilihan default dan beri nama client 'giyani-browser'.",
                "Langkah 4: Tampilkan isi file konfigurasi dengan 'cat ~/wg0-client-giyani-browser.conf', salin seluruh teksnya, lalu tempelkan ke menu 'Import .conf' di Giyani Browser."
            ),
            codeSnippetTitle = "Perintah Terminal VPS Ubuntu / Debian:",
            codeSnippet = VPS_INSTALL_COMMANDS,
            actionLabel = "Buka Layar VPN Engine",
            actionDestination = GiyaniDestination.VPN_MANAGER
        ),
        GuideChapter(
            id = "chapter_tabs_downloads_audit",
            category = "FITUR",
            stepNumber = "BAB 6",
            title = "Tab Groups, Download Manager (Pause/Resume) & Audit Diagnostik",
            summary = "Cara mengelompokkan tab berwarna, menjeda/melanjutkan unduhan file besar, dan membaca hasil pemeriksaan privasi.",
            bulletPoints = listOf(
                "Tab Groups Manager: Buka menu 'Tabs' -> Ketuk 'New Group' untuk membuat grup tab berwarna -> Ketuk kepala grup untuk melipat/membuka (Expand/Collapse) -> Ketuk ikon Folder Panah pada tab untuk memindahkannya ke grup lain.",
                "Download Manager (Pause & Resume): Buka menu 'Downloads' -> Setiap file yang diunduh menampilkan ukuran, kecepatan, dan sisa waktu (ETA). Anda bebas menekan 'Pause' dan 'Resume' kapan saja tanpa mengulang dari 0%, lalu klik 'Open File' saat selesai.",
                "Halaman Audit (Identity Diagnostics): Menampilkan 17 pemeriksaan asli. Jika status IP tertulis 'Mismatch', itu tanda jujur bahwa Anda memilih negara (misal US) tetapi belum menyalakan VPN US. Nyalakan VPN negara tersebut agar status berubah menjadi 'Protected'."
            ),
            actionLabel = "Buka Halaman Audit Diagnostik",
            actionDestination = GiyaniDestination.DIAGNOSTICS
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserGuideScreen(
    uiState: GiyaniUiState,
    viewModel: GiyaniViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(GiyaniDestination.BROWSER_WEBVIEW)
    }

    val clipboardManager = LocalClipboardManager.current
    var selectedCategory by remember { mutableStateOf("SEMUA") }
    val expandedMap = remember {
        mutableStateMapOf<String, Boolean>().apply {
            UserGuideCatalog.chapters.forEach { put(it.id, true) }
        }
    }

    val filteredChapters = remember(selectedCategory) {
        if (selectedCategory == "SEMUA") {
            UserGuideCatalog.chapters
        } else {
            UserGuideCatalog.chapters.filter { it.category == selectedCategory }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("user_guide_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "Panduan Pengguna",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Panduan Pengguna & Setup VPN",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Petunjuk lengkap langkah demi langkah untuk pengguna awam hingga mahir",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("guide_category_filter_row")
        ) {
            val categories = listOf(
                "SEMUA" to "Semua Bab (6)",
                "PEMULA" to "Tampilan Chrome & Panel",
                "PROFILE" to "Multi-Profile",
                "VPN" to "Cara Atur WireGuard VPN",
                "VPS" to "Buat Server VPS Sendiri",
                "FITUR" to "Tab, Download & Audit"
            )
            items(categories, key = { it.first }) { (catKey, catLabel) ->
                FilterChip(
                    selected = selectedCategory == catKey,
                    onClick = { selectedCategory = catKey },
                    label = { Text(catLabel) },
                    modifier = Modifier.testTag("guide_filter_chip_$catKey")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("user_guide_lazy_column"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            // Quick Summary Card for Beginners
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Ringkasan Cepat 3 Langkah untuk Pengguna Baru:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "1. Ketuk tombol '[2P]' di kiri kotak alamat web untuk membuka Panel Kiri & membuat Profile (cukup Nama & Email tanpa password).\n" +
                                "2. Ketuk tombol 'Tune' di kanan kotak alamat web untuk membuka Panel Kanan & memilih Negara, Kota, serta Perangkat.\n" +
                                "3. Buka menu 'VPN' untuk mengimpor konfigurasi WireGuard (.conf) dari ProtonVPN Gratis, Cloudflare WARP, atau VPS Anda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Interactive Chapters List
            items(filteredChapters, key = { it.id }) { chapter ->
                val isExpanded = expandedMap[chapter.id] ?: true

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("guide_chapter_card_${chapter.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Chapter Header (Clickable Accordion)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedMap[chapter.id] = !isExpanded }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = chapter.stepNumber,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = chapter.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = chapter.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Tutup Bab" else "Buka Bab",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider()

                                // Step-by-step Bullet Points
                                chapter.bulletPoints.forEach { point ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 6.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = point,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // Optional Code / Config Template Box with Copy Button
                                if (!chapter.codeSnippet.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = chapter.codeSnippetTitle ?: "Konfigurasi:",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(chapter.codeSnippet))
                                                viewModel.showMessage("Teks konfigurasi berhasil disalin ke Clipboard!")
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .height(32.dp)
                                                .testTag("copy_snippet_button_${chapter.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Salin",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Salin Teks", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = chapter.codeSnippet,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }

                                // Interactive Practice / Shortcut Button
                                if (chapter.actionLabel != null && chapter.actionDestination != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = {
                                            if (chapter.practiceTemplateConf) {
                                                viewModel.importWireGuardConfig(
                                                    profileName = "Contoh Template WireGuard (${uiState.selectedCountry.isoCode})",
                                                    countryCode = uiState.selectedCountry.isoCode,
                                                    cityName = uiState.selectedCity.name,
                                                    rawConfText = UserGuideCatalog.SAMPLE_WIREGUARD_TEMPLATE,
                                                    browserOnly = true,
                                                    failClosed = false
                                                )
                                            }
                                            viewModel.navigateTo(chapter.actionDestination)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("chapter_action_button_${chapter.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(chapter.actionLabel)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
