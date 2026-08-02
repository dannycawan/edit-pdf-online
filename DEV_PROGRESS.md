# DEV_PROGRESS.md — Edit PDF Online - Text Editor
> Terakhir diperbarui: 2026-08-02 (Play Store readiness, icon merah + PDF, push ke GitHub, build APK + AAB)

---

## Active Task

Play Store readiness: pembuatan icon app merah + tulisan PDF, adaptive icon, mipmap assets, feature graphic, dan update dokumentasi.

## Current Status

- **Implementasi 2026-06-23**: mapping Alat Utama Edit Teks diubah dari `COVER` menjadi `REPLACE` pada Home dan Tools.
- **Signature**: create/save/place/export/move/delete/resize aktif. Handle kanan bawah menjaga aspect ratio, minimum size, dan mencatat `UndoAction.ModifyObject`.
- **Text**: tambah text baru, font size/color/bold saat pembuatan, move, delete, dan export tersedia. Dialog belum dapat membuka serta mengubah `TextObject` yang sudah dipilih.
- **Replace Text**: draw area sekarang membuat cover, membuka dialog teks baru, dan menempatkan text pada titik kiri atas area pilihan. Edit ulang object terpilih masih pending.
- **Undo/redo**: model `UndoAction.ModifyObject` tersedia, namun move/resize/edit belum mendorong modify action ke undo stack.
- **Ads**: banner test ad aktif pada Home, Tools, dan Recent; Editor tetap tanpa ads. Interstitial masih belum di-load/show dari flow sukses.
- **Tests**: empat unit test `AdFrequencyManager` aktif dan lulus untuk threshold, cooldown, disabled config, dan reset.
- **Validation**: `testDebugUnitTest` dan `assembleDebug` lulus pada salinan build 2026-06-23.

- **Fix Alat Utama**: Root cause ditemukan — semua 4 card di `MainToolsGrid` memanggil `onOpenPdf` yang SAMA, dan route editor tidak membawa informasi tool yang dipilih.
  1. Route editor sekarang memiliki parameter opsional `tool`: `editor?uri={uri}&tool={tool}`.
  2. `HomeScreen.MainToolsGrid` sekarang mengirim tool ID yang berbeda untuk setiap card (COVER, SIGN, CHECKMARK, TEXT).
  3. `ToolsScreen` yang ada di `AppNavigation` juga meneruskan tool ID yang sesuai ke route editor (edit_text→COVER, sign→SIGN, fill_form→CHECKMARK, add_text→TEXT).
  4. `EditorScreen` menerima parameter `initialTool: String` dan auto-select tool via `LaunchedEffect(initialTool, state.totalPages)` setelah PDF berhasil dimuat.
- **Fix PDF Preview**: Background canvas PDF diganti dari abu-abu (`SurfaceVariant`) ke hitam gelap (`0xFF212121`), seperti PDF viewer profesional. Render scale dinaikkan dari 2.0f ke 2.5f.
- **Fix Navigation Bar Editor**: `PageNavigationBar` diubah ke dark background hitam dengan tombol biru untuk konsistensi dengan dark canvas area.
- **Fix Toolbar Bottom**: Selected tool state lebih jelas dengan background biru translucent dan teks semi-bold.

- **SecurityException Fix**: Root cause ditemukan dan diperbaiki:
  1. `canAccessUri()` di `PdfRendererManager` melakukan `openInputStream()` sebagai pre-check — pada beberapa device/provider SAF, URI baru dari picker belum siap dibaca dalam milidetik pertama, menyebabkan false "Akses file kedaluwarsa".
  2. `openFromFile()` / `openFromSafDirect()` menganggap semua `SecurityException` sebagai "password-protected" — pada kenyataannya PdfRenderer Android melempar SecurityException untuk PDF dengan enkripsi/DRM tertentu yang TIDAK memerlukan password.
  3. `FileUtils.isPdfFile()` dan `FileUtils.isFileTooLarge()` membuka stream URI sebelum renderer — kegagalan di sini membuat PDF ditolak sebelum dicoba.
- **PdfBox Fallback**: Ketika PdfRenderer gagal dengan SecurityException, PdfBox-Android digunakan sebagai fallback untuk validasi dan mendapatkan page count. Editor tetap bisa dibuka dengan navigasi halaman, dan export (via PdfBox) tetap berfungsi.
- **Peningkatan Retry**: `copyToTempFileWithRetry()` sekarang menunggu 100ms sebelum attempt pertama dan melakukan retry dengan delay progresif saat `SecurityException`.
- **Error Message Mapping**: `EditorViewModel` sekarang hanya menampilkan "password-protected" untuk SecurityException yang benar-benar tidak bisa diatasi (PdfBox juga gagal). IOException ditampilkan sebagai "Tidak dapat membuka PDF".
- **Root cause baru (2026-06-16)**: Flow Alat Utama berbeda dari Alat PDF. Alat PDF memakai URI langsung di `ToolsScreen`/`ToolsViewModel`, sedangkan Alat Utama memasukkan URI ke route `editor?uri=...`. Navigation Compose sudah decode argumen route sekali, tetapi `AppNavigation` melakukan `Uri.decode()` lagi. Ini merusak SAF document ID seperti `primary%3ADownload%2Ffile.pdf` menjadi `primary:Download/file.pdf`, sehingga `ContentResolver` gagal membuka file valid.
- **Fix route Editor**: `AppNavigation` tidak lagi melakukan double-decode URI sebelum mengirim ke `EditorScreen`.
- **Fix pembacaan SAF tambahan**: `PdfRendererManager.copyToTempFileWithRetry()` sekarang mencoba copy lewat `openInputStream()` dan fallback `openFileDescriptor()`/`AutoCloseInputStream`.
- **Build**: Local `assembleDebug` tidak dijalankan untuk siklus 2026-06-16. Verifikasi dilakukan lewat GitHub Actions; run `27605133029` passed.
- **Riwayat Perbaikan**: Lihat bagian Errors / Blockers di bawah.

## What Has Been Confirmed

1. **Arsitektur**: Clean Architecture ringan berjalan baik dengan `EditorViewModel` dan `HomeViewModel`.
2. **PDF Engine**: Android PdfRenderer merender preview dengan benar. PdfBox-Android sudah dibungkus dengan baik di `PdfExportManager` dan sekarang sebagai fallback di `PdfRendererManager`.
3. **Konfigurasi Tambahan**: FileProvider path sudah benar di XML dan AndroidManifest.
4. **Ads & Analytics**: Masih bersifat stub.
5. **Signature Flow**: SignatureScreen sudah berfungsi.
6. **Default Language**: Deteksi locale device-aware.
7. **PDF Open Reliability**: Retry copy temp file, integrity check, dan PdfBox fallback untuk file yang ditolak PdfRenderer.
8. **UI Strings**: Semua hardcoded text sudah diganti string resources.

## Work Completed

- [x] Membuat `MASTER_PLAN.md` berdasarkan spesifikasi super prompt.
- [x] Mengupdate `SYSTEM_MAP.md` dengan alur aplikasi yang baru.
- [x] Milestone 1-8: Setup Project, Home UI, Picker, Render, Editor Models, Editor UI, Tools, Export PDF (Selesai semua).
- [x] Milestone 11: Implementasi Recent Files.
- [x] Milestone 13: AdMob (Siap pakai logic frequency & interstitial manager).
- [x] Fix Typo gradle file: `dependencyResolutionManagement`.
- [x] Milestone 9: Signature screen, draw signature, save transparent PNG, place signature on PDF.
- [x] Milestone 10: Track Fill Form/checkmark tool analytics via stub.
- [x] Milestone 14: Merge, Split, Rotate, Delete Pages, Image to PDF, PDF to Image UI + logic.
- [x] Fix resource blocker: tambah launcher icon vector dan update Manifest dari `@mipmap` ke `@drawable`.
- [x] Fix default language agar otomatis menggunakan Bahasa Indonesia di perangkat ID, dan English di device lain.
- [x] Fix Settings menu yang sebelumnya kosong dan lokalisasi pesan dialog.
- [x] Fix validasi PDF dan error `SecurityException` (password-protected palsu) di `PdfRendererManager` dengan retry, integrity check, dan direct FD fallback.
- [x] Ganti semua teks bahasa Inggris yang di-hardcode di UI dengan string resources (`strings.xml` EN & ID).
- [x] Hubungkan stub Analytics, Crashlytics, dan Interstitial Ad pada event utama.
- [x] Build lokal sukses.
- [x] Force push local `main` ke `origin/main`.
- [x] GitHub Actions push-to-main sukses.
- [x] Fix expired recent URI hanya di `AppNavigation.kt`.
- [x] **Fix false "Akses file kedaluwarsa"**: Hapus `canAccessUri()` pre-check; langsung copy + retry.
- [x] **Fix false "PDF dilindungi kata sandi"**: PdfBox fallback untuk PDF yang ditolak PdfRenderer; error message lebih akurat.
- [x] **Fix false "Tidak dapat membuka file PDF" pada Alat Utama**: Hapus double `Uri.decode()` di `AppNavigation` agar SAF URI tidak rusak saat melewati route Editor.
- [x] **Perkuat copy SAF Editor**: Tambah fallback copy temp via `openFileDescriptor()` ketika `openInputStream()` gagal.
- [x] **Fix Alat Utama tidak mengaktifkan tool**: Route editor kini memiliki param `tool`; setiap tool card mengirim toolId berbeda; EditorScreen auto-select tool via `LaunchedEffect`.
- [x] **Improve PDF Preview**: Background canvas hitam gelap seperti viewer profesional; render scale dinaikkan ke 2.5f.
- [x] **Improve Editor UI**: PageNavigationBar dark theme; ToolButton selected state lebih jelas.
- [x] **Play Store Icon**: Buat icon app baru warna merah (#E30613) dengan tulisan "PDF" di dokumen putih.
- [x] **Adaptive Icon**: Buat `ic_launcher_foreground.xml` dan `ic_launcher_background.xml` untuk Android 8.0+.
- [x] **Mipmap Assets**: Buat folder mipmap-mdpi sampai mipmap-xxxhdpi dengan ic_launcher dan ic_launcher_round PNG.
- [x] **AndroidManifest Update**: Ubah icon dari `@drawable` ke `@mipmap/ic_launcher` dan `@mipmap/ic_launcher_round`.
- [x] **Play Store Assets**: Generate hi-res icon 512x512 dan feature graphic 1024x500 di folder `store-assets/`.
- [x] **Dokumentasi**: Update ketiga file .md (DEV_PROGRESS, MASTER_PLAN, SYSTEM_MAP) ke tanggal 2026-08-02.
- [x] **GitHub Push**: Semua perubahan icon, adaptive icon, mipmap, store-assets, dan .md di-commit dan di-push ke `origin/main`.
- [x] **GitHub Actions**: Build APK + AAB otomatis berjalan via workflow `android-build.yml` setelah push ke main.

## In Progress

- Edit selected `TextObject` dan resize text/cover/checkmark.
- Integrasi load/show interstitial setelah aksi sukses.

## Next Exact Steps

1. Ubah `TextInputDialog` agar mendukung mode add/edit dan prefill selected `TextObject`.
2. Generalisasi resize untuk Text/Cover/Checkmark bila dibutuhkan.
3. Hubungkan load/show interstitial setelah aksi sukses dan lolos frequency cap.
4. Jalankan release AAB/GitHub Actions dan device QA untuk Replace, Signature resize, banner, serta export.
5. Upload ke Play Store Console dengan icon dan feature graphic yang sudah dibuat.

## Files Modified (siklus 2 — 2026-06-16)

| File | Perubahan |
|------|-----------|
| `AppNavigation.kt` | Tambah param `tool` ke route editor; `pendingToolId` pakai `mutableStateOf`; tool mapping (edit_text→COVER, sign→SIGN, fill_form→CHECKMARK, add_text→TEXT) |
| `HomeScreen.kt` | `onOpenPdfWithTool` callback baru; setiap tool card kirim toolId berbeda |
| `EditorScreen.kt` | Terima `initialTool: String`; auto-select via LaunchedEffect; dark canvas background; PageNavigationBar dark theme; ToolButton selected state lebih jelas |
| `PdfRendererManager.kt` | Naikkan render scale default dari 2.0f ke 2.5f |

## Errors / Blockers

- ~~Error "Akses file telah kedaluwarsa"~~ → **FIXED**: dihapus `canAccessUri()` pre-check.
- ~~Error "PDF ini mungkin dilindungi kata sandi" (setelah fix pertama)~~ → **FIXED**: ditambah PdfBox fallback + error mapping akurat.
- **PdfBox rendering unavailable**: PdfBox-Android 2.0.27.0 tidak menyediakan API konversi `BufferedImage` ke `Bitmap` Android. Saat PdfBox fallback aktif, preview halaman tidak ditampilkan (loading spinner), tetapi navigasi halaman, page count, dan export tetap berfungsi.
- `javax.imageio.ImageIO` tidak tersedia di classpath Android (dibuang oleh build tools), sehingga render-to-PNG via PdfBox tidak bisa dilakukan.

## Do Not Repeat

- Jangan tambah pre-check akses URI sebelum benar-benar mencoba copy — `openInputStream()` bisa gagal sementara setelah picker.
- Jangan asumsikan SecurityException = password-protected — PdfRenderer throw SecurityException untuk berbagai alasan (enkripsi, DRM, restricted PDF).
- Jangan gunakan `javax.imageio.ImageIO` — tidak tersedia di Android classpath.

## Resume Note for Next Agent

Semua error "Akses file kedaluwarsa" dan "PDF dilindungi kata sandi" palsu sudah diperbaiki. `PdfRendererManager` sekarang:
1. Tidak lagi melakukan pre-check `canAccessUri()` — langsung copy dan retry.
2. Jika PdfRenderer gagal dengan SecurityException, fallback ke PdfBox untuk validasi dan page count.
3. Error message lebih akurat.

Jika PdfBox fallback aktif, preview halaman tidak tampil (PdfBox rendering ke Bitmap tidak didukung di Android). Untuk mengaktifkan preview penuh via PdfBox, perlu upgrade ke PdfBox-Android versi lebih baru atau menambah library konversi `BufferedImage` ke `Bitmap`.
