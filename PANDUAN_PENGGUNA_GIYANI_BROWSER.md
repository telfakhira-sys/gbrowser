# 📘 PANDUAN LENGKAP PENGGUNAAN GIYANI BROWSER & PENGATURAN WIREGUARD VPN
*(Disusun Khusus agar Mudah Dipahami oleh Pengguna Awam dari Nol)*

Selamat datang di **Giyani Browser**!
Giyani Browser adalah aplikasi peramban (*browser*) Android bergaya **Google Chrome APK** yang dirancang agar Anda bisa menjelajah internet dengan **Identitas Negara yang Rapi**, **Banyak Profile Sekaligus (Nama & Email Tanpa Password)**, serta **VPN WireGuard Khusus Browser (Per-App VPN)** tanpa perlu pusing dengan istilah teknis yang rumit.

---

## DAFTAR ISI
1. [Bab 1: Mengenal Tampilan Chrome Giyani Browser (Left-Panel & Right-Panel)](#bab-1-mengenal-tampilan-chrome-giyani-browser-left-panel--right-panel)
2. [Bab 2: Workflow Multi-Profile (Nama & Email Tanpa Password + Buka Banyak Profile 1 Negara/VPN)](#bab-2-workflow-multi-profile-nama--email-tanpa-password--buka-banyak-profile-1-negaravpn)
3. [Bab 3: Memahami Perbedaan "Identitas Negara Browser" vs "Lokasi IP VPN"](#bab-3-memahami-perbedaan-identitas-negara-browser-vs-lokasi-ip-vpn)
4. [Bab 4: Panduan Lengkap Mengatur WireGuard VPN (Dari Nol untuk Pemula)](#bab-4-panduan-lengkap-mengatur-wireguard-vpn-dari-nol-untuk-pemula)
   - [4.1 Penjelasan Bahasa Awam: Apa Itu WireGuard & Arti Isi File `.conf`?](#41-penjelasan-bahasa-awam-apa-itu-wireguard--arti-isi-file-conf)
   - [4.2 Contoh Template Konfigurasi WireGuard Siap Pakai (Untuk Latihan Import)](#42-contoh-template-konfigurasi-wireguard-siap-pakai-untuk-latihan-import)
   - [4.3 Cara 1: Mendapatkan WireGuard Gratis dari ProtonVPN (Paling Mudah untuk Awam)](#43-cara-1-mendapatkan-wireguard-gratis-dari-protonvpn-paling-mudah-untuk-awam)
   - [4.4 Cara 2: Menggunakan Cloudflare WARP Gratis atau Mullvad VPN](#44-cara-2-menggunakan-cloudflare-warp-gratis-atau-mullvad-vpn)
   - [4.5 Cara 3: Membuat Server WireGuard Pribadi di VPS Sendiri (5 Menit)](#45-cara-3-membuat-server-wireguard-pribadi-di-vps-sendiri-5-menit)
   - [4.6 Langkah Memasukkan (Import) WireGuard ke Giyani Browser](#46-langkah-memasukkan-import-wireguard-ke-giyani-browser)
5. [Bab 5: Cara Menggunakan Tab Groups (Pengelompokan Tab Berwarna)](#bab-5-cara-menggunakan-tab-groups-pengelompokan-tab-berwarna)
6. [Bab 6: Cara Menggunakan Download Manager (Pause, Resume, & Background Download)](#bab-6-cara-menggunakan-download-manager-pause-resume--background-download)
7. [Bab 7: Membaca Halaman Audit / Identity Diagnostics & Tanya-Jawab (FAQ)](#bab-7-membaca-halaman-audit--identity-diagnostics--tanya-jawab-faq)

---

## Bab 1: Mengenal Tampilan Chrome Giyani Browser (Left-Panel & Right-Panel)

Saat membuka **Giyani Browser**, Anda langsung disambut layar utama bergaya **Google Chrome APK** yang bersih dan cepat.

Di bagian atas layar browser terdapat **3 baris kontrol utama**:
1. **Baris Paling Atas (Multi-Profile Strip)**:
   - Menampilkan semua **Profile yang sedang aktif/dibuka bersamaan** (misalnya: `🇺🇸 Alex Mercer` dan `🇺🇸 Sarah Jenkins`).
   - Cukup **ketuk nama profile** di baris ini untuk berpindah akun/identitas secara instan tanpa menutup tab!
2. **Baris Tengah (Chrome Horizontal Tab Strip)**:
   - Menampilkan tab-tab web yang sedang terbuka di profile tersebut.
   - Ketuk tombol **`+`** di ujung kanan untuk membuka tab baru, atau **`×`** pada tab untuk menutupnya.
3. **Baris Omnibox (Kotak Alamat Web + Tombol Toggle Panel Kiri & Kanan)**:
   - **Tombol Kiri `[2P]` (Toggle Left-Panel)**: Ketuk tombol ini untuk **memunculkan atau menyembunyikan Panel Kiri**. Panel Kiri berisi **Daftar Profile** (Buat Profile, Edit, Hapus, Use Profile, Buka Bersama) serta daftar Tab & Grup Tab.
   - **Kotak Alamat Web (Omnibox)**: Ketik alamat situs (contoh: `wikipedia.org`) atau kata kunci pencarian, lalu tekan **Go / Enter**.
   - **Tombol Kotak Angka `[3]`**: Menunjukkan jumlah tab terbuka. Ketuk untuk membuka layar penuh **Tab Groups Manager**.
   - **Tombol Kanan `Tune / Pengaturan` (Toggle Right-Panel)**: Ketuk tombol ikon pengaturan di kanan kotak alamat untuk **memunculkan atau menyembunyikan Panel Kanan**. Panel Kanan berisi pengaturan cepat **Negara, Kota/Timezone, Identitas HP/Laptop (Device Preset), Tombol Connect WireGuard VPN, dan Proteksi Privasi**.

> **Tips Mudah**: Untuk menutup *Left-Panel* atau *Right-Panel*, Anda cukup mengetuk area layar web yang gelap di tengah atau menekan tombol **`×`** di pojok atas panel.

---

## Bab 2: Workflow Multi-Profile (Nama & Email Tanpa Password + Buka Banyak Profile 1 Negara/VPN)

Di Giyani Browser, Anda bisa membuat banyak profil pengguna (seperti profil kerja, riset, belanja, atau pengujian wilayah) **tanpa perlu memasukkan password sama sekali**.

### A. Cara Membuat Profile Baru
1. Buka **Panel Kiri (Left-Panel)** dengan mengetuk tombol **`[2P]`** di sebelah kiri kotak alamat web (atau buka menu **Profiles** di bar navigasi bawah).
2. Ketuk tombol hijau **"Buat Profile Baru (Nama & Email)"**.
3. Isi:
   - **Nama Profile** (contoh: `Rina Tokopedia` atau `Andi Riset Jepang`).
   - **Email Profile** (contoh: `rina@bisnis.id` — *hanya sebagai label identitas profil, tidak perlu password*).
   - **Negara Identitas Browser** (contoh: `🇺🇸 United States`, `🇯🇵 Japan`, `🇮🇩 Indonesia`, `🇩🇪 Germany`, `🇬🇧 UK`, `🇸🇬 Singapore`, `🇨🇭 Switzerland`).
   - **Kota & Timezone** (contoh: pilih `Jakarta` untuk zona waktu `Asia/Jakarta`).
   - **Identitas Perangkat (Device Preset)** (contoh: `Google Pixel 9 Pro`, `Samsung Galaxy S24 Ultra`, `iPhone Compatibility`, atau `Windows 11 Desktop`).
4. Ketuk **"Buat & Gunakan Profile"**.

### B. Setiap Profile Memiliki Identitas & Penyimpanan Terisolasi
- Setiap profile memiliki **Cookies, Login Situs, Riwayat Browsing, Bookmark, Tab Groups, Zona Waktu, Bahasa, dan Jenis Perangkat (User-Agent)** yang terpisah total.
- Jika Anda login ke sebuah situs di *Profile A*, situs tersebut tidak akan mengetahui aktivitas Anda di *Profile B*.

### C. Aturan Membuka Banyak Profile Sekaligus (Syarat: 1 Negara atau 1 VPN)
- **Bisa Aktif Bersamaan**: Anda dapat mengaktifkan/membuka **2, 3, atau lebih profile sekaligus** asalkan profile-profile tersebut berada di **1 Negara yang sama** atau menggunakan **1 jalur VPN yang sama**.
  - *Contoh*: `Alex Mercer (US - New York)` dan `Sarah Jenkins (US - Los Angeles)` sama-sama bernegara **United States (US)**. Anda bisa menekan tombol **"Buka Bersama" / "Aktifkan Bersamaan"** pada keduanya. Keduanya akan muncul di bar atas Chrome sehingga Anda bisa berganti-ganti dalam 1 ketukan!
- **Jika Berbeda Negara**: Jika saat ini Anda sedang membuka sesi negara **US**, lalu Anda ingin membuka profile `Kenji Takahashi (JP - Japan)`:
  - Anda tidak bisa mencampur sesi **US** dan **JP** secara bersamaan agar tidak terjadi kebocoran identitas jaringan/VPN.
  - Cukup tekan tombol **"Pindah Negara & Use"** pada profile `Kenji Takahashi`. Aplikasi akan otomatis menonaktifkan sementara sesi US dan mengaktifkan lingkungan negara **Jepang (JP)** untuk Anda.

---

## Bab 3: Memahami Perbedaan "Identitas Negara Browser" vs "Lokasi IP VPN"

Bagi pengguna awam, penting untuk memahami cara kerja Giyani Browser yang **jujur dan transparan**:

1. **Apa yang Terjadi Saat Anda Memilih Negara (Tanpa Menyalakan VPN)?**
   - Saat Anda memilih negara (misalnya **Jepang 🇯🇵**), Giyani Browser langsung mengatur:
     - Bahasa web (`ja-JP`),
     - Jam & Zona Waktu browser (`Asia/Tokyo`),
     - Format Tanggal (`YYYY/MM/DD`) & Mata Uang (`JPY ¥`).
   - **NAMUN**, alamat IP internet Anda masih berasal dari provider internet asli Anda (misalnya Telkomsel/IndiHome di Indonesia).
2. **Apa yang Terjadi Saat Anda Menyalakan WireGuard VPN?**
   - Saat Anda menghubungkan **WireGuard VPN** ke server negara tersebut (misalnya server VPN Tokyo, Jepang), barulah **Alamat IP Publik** dan jalur DNS browser Anda berubah menjadi **Jepang**!
   - Keunggulan Giyani Browser: VPN kita menggunakan teknologi **Per-App VPN (Browser-Only)**. Artinya, **hanya lalu lintas Giyani Browser yang lewat VPN**, sedangkan aplikasi lain di HP Anda (seperti WhatsApp, Gojek, Mobile Banking) tetap berjalan normal menggunakan jaringan lokal HP Anda!

---

## Bab 4: Panduan Lengkap Mengatur WireGuard VPN (Dari Nol untuk Pemula)

Giyani Browser menggunakan protokol VPN modern bernama **WireGuard** yang sangat cepat, hemat baterai, dan aman. Kunci rahasia VPN Anda disimpan di dalam brankas perangkat keras **Android Keystore (AES-256-GCM)** sehingga tidak bisa dicuri.

### 4.1 Penjelasan Bahasa Awam: Apa Itu WireGuard & Arti Isi File `.conf`?

Konfigurasi WireGuard hanyalah sebuah teks pendek (biasanya berakhiran `.conf`) yang berisi **2 bagian**:

1. **`[Interface]` (Kartu Identitas HP Anda di Dalam Terowongan VPN)**:
   - `PrivateKey`: Kunci rahasia milik HP Anda (seperti PIN ATM, jangan dibagikan ke orang lain; Giyani Browser otomatis mengenkripsinya).
   - `Address`: Alamat IP lokal virtual HP Anda di dalam terowongan VPN (contoh: `10.66.66.2/32`).
   - `DNS`: Server penerjemah alamat web yang aman (contoh: `1.1.1.1, 1.0.0.1` milik Cloudflare).
2. **`[Peer]` (Alamat Server VPN Tujuan Anda)**:
   - `PublicKey`: Kunci publik milik server VPN tujuan.
   - `Endpoint`: Alamat IP atau domain server VPN beserta nomor port-nya (contoh: `us-nyc-wg-01.vpnprovider.net:51820`).
   - `AllowedIPs`: `0.0.0.0/0, ::/0` (artinya seluruh lalu lintas internet Giyani Browser diarahkan melewati terowongan VPN ini).

---

### 4.2 Contoh Template Konfigurasi WireGuard Siap Pakai (Untuk Latihan Import)

Anda bisa menyalin teks template di bawah ini untuk mencoba fitur **Import `.conf`** di menu **VPN** Giyani Browser (atau cukup tekan tombol **"Gunakan Template Latihan"** di layar Panduan/VPN di dalam aplikasi):

```ini
[Interface]
# Contoh Template Konfigurasi WireGuard Giyani Browser
PrivateKey = Y29udG9oUHJpdmF0ZUtleVVudHVrTGF0aWhhbkdpeWFuaUJyb3dzZXI=
Address = 10.66.66.2/32, fd42:42:42::2/128
DNS = 1.1.1.1, 1.0.0.1

[Peer]
PublicKey = Y29udG9oUHVibGljS2V5U2VydmVyVnBuV2lyZUd1YXJkUmVzbWk=
Endpoint = 198.51.100.42:51820
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25
```

> **Catatan Jujur**: Template di atas menggunakan alamat IP contoh dokumentasi (`198.51.100.42`) agar Anda bisa berlatih cara mengimpor dan melihat bagaimana fitur **Fail-Closed Kill Switch** dan **Android Keystore** bekerja. Untuk terhubung ke internet nyata melalui VPN, ikuti salah satu dari **3 Cara Mudah** di bawah ini untuk mendapatkan file `.conf` asli!

---

### 4.3 Cara 1: Mendapatkan WireGuard Gratis dari ProtonVPN (Paling Mudah untuk Awam)

**ProtonVPN** menyediakan server WireGuard **100% Gratis** (tanpa batas kuota, tersedia server **United States 🇺🇸**, **Japan 🇯🇵**, dan **Netherlands 🇳🇱**).

**Langkah-langkahnya:**
1. Buat akun gratis di situs resmi: `https://account.protonvpn.com/signup` (pilih paket **Proton Free**).
2. Setelah masuk ke dashboard ProtonVPN, buka menu **Downloads** → gulir ke bawah ke bagian **WireGuard configuration**.
3. Beri nama konfigurasi (misalnya: `Giyani-HP`), pilih platform **Android / GNU Linux**, lalu pilih salah satu server **FREE** (misalnya: **US-FREE** atau **JP-FREE**).
4. Klik tombol **Create** lalu klik **Download** untuk mengunduh file `.conf` (atau buka file tersebut dengan aplikasi catatan/teks lalu **Salin / Copy** seluruh isinya).
5. Buka **Giyani Browser** → masuk ke menu **VPN** → ketuk **"Import .conf"** → tempel (*Paste*) teks tadi → ketuk **"Encrypt & Import"** → lalu ketuk **"Connect"**!

---

### 4.4 Cara 2: Menggunakan Cloudflare WARP Gratis atau Mullvad VPN

#### Opsi A: Cloudflare WARP Gratis (Menggunakan Generator `wgcf`)
Jika Anda ingin koneksi super cepat menggunakan jaringan Cloudflare terdekat:
1. Di komputer atau Termux, unduh alat gratis open-source `wgcf` dari GitHub (`ViRb3/wgcf`).
2. Jalankan dua perintah singkat:
   ```bash
   wgcf register
   wgcf generate
   ```
3. Perintah di atas menghasilkan file `wgcf-profile.conf`. Salin isi file tersebut dan **Import `.conf`** ke dalam Giyani Browser!

#### Opsi B: Mullvad VPN / IVPN / Windscribe (Privasi Tingkat Tinggi)
1. Masuk ke halaman akun penyedia VPN Anda (misalnya Mullvad atau Windscribe).
2. Masuk ke menu **WireGuard Configuration Generator**.
3. Pilih negara & kota tujuan (misalnya: *Germany - Frankfurt* atau *Singapore*).
4. Unduh file `.conf` dan tempelkan ke menu **Import `.conf`** di Giyani Browser.

---

### 4.5 Cara 3: Membuat Server WireGuard Pribadi di VPS Sendiri (5 Menit)

Jika Anda menyewa VPS murah (seperti DigitalOcean, Vultr, Linode, AWS Lightsail, atau IDCloudHost seharga ~$3–$5/bulan) dengan sistem operasi **Ubuntu 22.04 / 24.04** atau **Debian 12**, Anda bisa membuat server WireGuard pribadi dalam 5 menit:

1. Login ke terminal SSH VPS Anda.
2. Jalankan perintah instalasi otomatis resmi berikut:
   ```bash
   curl -O https://raw.githubusercontent.com/angristan/wireguard-install/master/wireguard-install.sh
   chmod +x wireguard-install.sh
   sudo ./wireguard-install.sh
   ```
3. Tekan **Enter** untuk semua pilihan default, lalu masukkan nama client: `giyani-browser`.
4. Setelah selesai, tampilkan isi konfigurasi dengan perintah:
   ```bash
   cat ~/wg0-client-giyani-browser.conf
   ```
5. Salin teks yang muncul di layar terminal, lalu tempelkan ke tombol **"Import .conf"** di Giyani Browser!

---

### 4.6 Langkah Memasukkan (Import) WireGuard ke Giyani Browser

Setelah Anda memiliki teks `.conf` dari salah satu cara di atas:
1. Buka **Giyani Browser**, ketuk menu **VPN** di bagian bawah (atau buka **Right-Panel** lalu ketuk **Konfigurasi**).
2. Ketuk tombol hijau **"Import .conf"**.
3. Masukkan **Nama Label** (contoh: `ProtonVPN Jepang Gratis`).
4. Tempelkan (*Paste*) teks konfigurasi WireGuard Anda ke kotak **"Paste WireGuard Configuration"**.
5. Pastikan dua sakelar di bawahnya menyala:
   - **Per-App (Giyani Browser Only)**: Menyala (`ON`) agar hanya Giyani Browser yang melewati VPN.
   - **Fail-Closed Kill Switch**: Menyala (`ON`) jika Anda ingin Giyani Browser otomatis memblokir halaman web apabila koneksi ke server VPN terputus (mencegah kebocoran IP asli).
6. Ketuk **"Encrypt & Import"**.
7. Kartu server VPN Anda akan muncul di daftar. Ketuk tombol **"Connect"** dan setujui izin VPN Android saat muncul pop-up!

---

## Bab 5: Cara Menggunakan Tab Groups (Pengelompokan Tab Berwarna)

Agar puluhan tab Anda tidak berantakan, gunakan fitur **Tab Groups**:
1. Ketuk tombol **angka jumlah tab** di sebelah kanan kotak alamat web, atau pilih menu **Tabs** di navigasi bawah.
2. **Membuat Grup Baru**:
   - Ketuk tombol **"New Group"**.
   - Ketik nama grup (contoh: `Belanja Jepang`, `Riset Kerja`, atau `Berita Harian`).
   - Pilih warna identitas grup (*Emerald*, *Cyan*, *Amber*, *Violet*, *Rose*, atau *Cobalt*), lalu ketuk **"Create Group"**.
3. **Menampilkan / Menyembunyikan Isi Grup (Expand / Collapse)**:
   - Ketuk kepala (*header*) grup berwarna untuk melipat (*collapse*) atau membuka (*expand*) daftar tab di dalamnya.
4. **Memindahkan Tab Antar-Grup**:
   - Pada setiap kartu tab, ketuk ikon **Folder Panah (`Move Tab`)** untuk memindahkan tab tersebut ke grup lain atau ke bagian *Ungrouped Tabs*.

---

## Bab 6: Cara Menggunakan Download Manager (Pause, Resume, & Background Download)

Giyani Browser dilengkapi mesin pengunduh latar belakang (*Background Foreground Service*) yang mendukung **Jeda (Pause)** dan **Lanjutkan (Resume)**:
1. Saat Anda mengunduh file apa pun di dalam browser (atau menekan tombol **"+ Add URL"** di menu **Downloads**), unduhan langsung berjalan di latar belakang.
2. **Informasi Real-Time**:
   - Menampilkan persentase (`%`), ukuran file yang sudah terunduh vs total ukuran (`MB`), kecepatan unduh (`KB/s` atau `MB/s`), serta **Estimasi Waktu Tersisa (ETA)**.
3. **Pause & Resume Tanpa Mengulang dari Nol**:
   - Ketuk **"Pause"** untuk menghentikan sementara unduhan.
   - Ketuk **"Resume"** kapan saja untuk melanjutkan unduhan tepat dari byte terakhir yang tersimpan!
4. **Membuka File & Lokasi Penyimpanan**:
   - Setelah berstatus **COMPLETED**, ketuk **"Open File"** untuk langsung membuka dokumen/PDF/gambar tersebut, atau ketuk **"Export / Location"** untuk membagikannya ke aplikasi File Manager HP Anda.

---

## Bab 7: Membaca Halaman Audit / Identity Diagnostics & Tanya-Jawab (FAQ)

Buka menu **Audit** di navigasi bawah untuk melihat **17 Pengukuran Diagnostik Asli (Real-Time)**:
- **Status `Protected` (Hijau)**: Parameter identitas Anda (seperti *User-Agent*, *Bahasa*, *Timezone*, *Canvas 2D*, *WebRTC*) sudah terlindungi dan sesuai dengan preset negara profile Anda.
- **Status `Mismatch` (Merah/Oranye)**: Biasanya muncul pada baris *"IP-Derived Country vs Selected Preset"* apabila Anda memilih negara (misalnya **US**), tetapi Anda **belum menyalakan VPN US**. Begitu Anda menghubungkan VPN ke server negara yang sama, statusnya otomatis berubah menjadi **`Protected`**!

### Tanya-Jawab Singkat (FAQ Pengguna Awam)
- **T: Apakah saya perlu membuat password saat menambah Profile baru?**
  - **J:** **Tidak perlu.** Cukup isi Nama dan Email sebagai penanda identitas profile.
- **T: Mengapa saya bisa membuka 2 profile US bersamaan, tapi saat mengaktifkan profile Jepang muncul peringatan?**
  - **J:** Karena jaringan VPN bekerja per koneksi negara. Selama profile-profile Anda berada di **1 Negara yang sama** (atau menggunakan VPN yang sama), Anda bebas membuka banyak profile sekaligus. Jika ingin memakai profile negara lain, cukup klik **"Pindah Negara & Use"**.
- **T: Bagaimana jika browser tiba-tiba menampilkan layar merah "Fail-Closed Kill Switch Active"?**
  - **J:** Itu artinya Anda menyalakan koneksi VPN dengan fitur *Fail-Closed Kill Switch*, namun server VPN yang Anda masukkan sedang tidak aktif/tidak terjangkau. Fitur ini melindungi Anda agar browser tidak bocor menggunakan IP asli. Anda cukup menekan **"Disable Kill Switch"** jika ingin kembali memakai jaringan internet biasa.
