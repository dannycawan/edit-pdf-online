# DEV_PROGRESS.md — Edit PDF Online - Text Editor
> Terakhir diperbarui: 2026-06-07

---

## Active Task

Memperbaiki alur Editor agar tombol Home/Main Tools dan Recent Files selalu memakai URI baru dari system PDF picker, bukan URI recent lama yang dapat kedaluwarsa.

## Current Status

- **Status**: Fresh picker fix selesai, local `clean assembleDebug` berhasil, dan GitHub Actions APK/AAB terbaru sukses.
- **Editor UI & Logic (Milestone 2-8)**: Selesai. Pengguna bisa membuka PDF, menambah overlay teks, blok penutup (cover), mengganti teks, membuat checkmark, dan menyimpannya (export via PdfBox). 
- **Layar Dukungan**: `HomeScreen`, `RecentFilesScreen`, `ToolsScreen`, dan `SettingsScreen` sudah diimplementasikan beserta ViewModel (Recent files menggunakan Room Database).
- **Navigation**: Seluruh rute sudah terhubung (`AppNavigation.kt`) menggunakan Compose Navigation dan SAF Picker terintegrasi.
- **Masalah Utama Lokal**: Editor sebelumnya dapat menerima URI lama langsung dari recent files dan menampilkan error akses file kedaluwarsa. Jalur tersebut sekarang membuka PDF picker ulang.
- **Lokalisasi & Strings**: Locale kini deteksi device (Bahasa Indonesia untuk ID, English untuk lainnya). Semua string hardcoded di UI telah dipindahkan ke `strings.xml` (EN & ID).
- **Analytics & Crashlytics**: Fungsi stub kini terhubung pada event kunci (open PDF, export PDF, errors).
- **GitHub Build**: `Android Build` run `27085606975` berhasil pada branch `main` untuk fresh picker patch; debug APK dan release AAB terbaru sudah diupload.
- **Blocked Eksternal**: Firebase real setup membutuhkan `google-services.json`; AdMob production test membutuhkan production ad unit/app IDs.

## What Has Been Confirmed

1. **Arsitektur**: Clean Architecture ringan berjalan baik dengan `EditorViewModel` dan `HomeViewModel`.
2. **PDF Engine**: Android PdfRenderer merender preview dengan benar. PdfBox-Android sudah dibungkus dengan baik di `PdfExportManager`. 
3. **Konfigurasi Tambahan**: FileProvider path sudah benar di XML dan AndroidManifest. Bug `settings.gradle.kts` (penamaan `dependencyResolution`) telah diperbaiki.
4. **Ads & Analytics**: Masih bersifat stub (siap dihubungkan saat file konfigurasi Firebase masuk).
5. **Signature Flow**: `SignatureScreen` sudah dibuat; user bisa menggambar tanda tangan, menyimpan PNG transparan, lalu signature ditempatkan kembali ke halaman PDF via `SavedStateHandle`.
6. **Default Language**: Perbaikan deteksi locale. Aplikasi sekarang mendeteksi bahasa perangkat; jika Indonesia (`in`/`id`), menggunakan Bahasa Indonesia, selain itu memaksa English via `LocaleUtils.applyAppLocale()`.
7. **Settings Actions**: Language, Theme, Help, Privacy Policy, dan Terms of Service membuka dialog dengan pesan yang telah disesuaikan (termasuk penjelasan locale otomatis).
8. **PDF Open Reliability**: Error "password-protected" palsu sudah ditangani dengan retry copy temp file, integrity check (size > 0), dan fallback langsung menggunakan SAF file descriptor.
9. **UI Strings**: Lebih dari 20 teks hardcoded telah diganti menggunakan resource `strings.xml`.
10. **Connected Functions**: CrashReporter dan AnalyticsTracker kini dipanggil saat error, open PDF, dan export PDF. Interstitial ad terhubung dengan frequency manager setelah export sukses.
11. **Fresh Editor URI**: Home hero, Main Tools, recent list di Home, dan `RecentFilesScreen` membuka `ActivityResultContracts.OpenDocument()` dengan MIME `application/pdf` sebelum navigasi ke Editor.
12. **Scope Guard**: PDF Tools, renderer, Gradle, export/save, dan conversion flow tidak diubah.

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
- [x] Force push local `main` ke `origin/main` pada commit `664d6c6`.
- [x] GitHub Actions push-to-main sukses (`Android Build` run `27085606975`).
- [x] Fix expired recent URI hanya di `AppNavigation.kt`.
- [x] Local `clean assembleDebug` sukses setelah fresh picker fix.

## In Progress

- Device QA fresh picker menggunakan APK artifact terbaru dari GitHub Actions.
- Sisa pekerjaan real Firebase/AdMob membutuhkan file/ID eksternal.

## Next Exact Steps

Berikut adalah milestone berikutnya berdasarkan kondisi saat ini:

1. **Device QA**: Install APK terbaru dari artifact GitHub dan test semua tombol/tool dengan PDF nyata.
2. **Milestone 12: Firebase Setup (external config needed)**
   - Tambahkan file `google-services.json`.
   - Uncomment kode dependency di build.gradle.
   - Hubungkan Stub di `AnalyticsTracker` dengan `FirebaseAnalytics`.
3. **Milestone 13: AdMob Production Validation (external IDs needed)**
   - Ganti test ID dengan production ID.
   - Test banner/interstitial di environment production-ready.
4. **Milestone 15: Device QA**
   - Install APK terbaru dan test export quality, large PDF handling, password-protected PDF, dan semua tool dengan file nyata.

## Files Already Read

| File | Alasan |
|------|--------|
| `build.gradle.kts` (root) | Plugin declarations, Firebase status |
| `settings.gradle.kts` | Project name, module structure |
| `app/build.gradle.kts` | Dependencies, SDK versions, namespace |
| `AndroidManifest.xml` | Permissions, activities, AdMob ID |
| `EditPdfApplication.kt` | Entry point, PdfBox init |
| `MainActivity.kt` | Single Activity, Compose setup |
| `AppNavigation.kt` | Routes, NavHost, screen wiring |
| `HomeScreen.kt` | Full UI composable, semua sections |
| `Color.kt`, `Theme.kt` | Material3 theme config |
| `AdFrequencyManager.kt` | Frequency cap logic |
| `InterstitialAdManager.kt` | Interstitial lifecycle |
| `BannerAdView.kt` | Banner composable + test IDs |
| `AnalyticsTracker.kt` | Analytics stub, semua event |
| `CrashReporter.kt` | Crash reporting stub |
| `RemoteConfigManager.kt` | Feature flags, ad controls, defaults |
| `AppDatabase.kt` | Room database singleton |
| `RecentFileDao.kt` | DAO queries (CRUD) |
| `RecentFile.kt` | Room entity definition |
| `RecentFileRepository.kt` | Repository wrapper |
| `EditorState.kt` | Editor state model, EditorTool enum, UndoAction |
| `PdfEditObject.kt` | Sealed class hierarchy (Text/Cover/Signature/Checkmark) |
| `PdfEditorEngine.kt` | Coordinate transform utility |
| `PdfExportManager.kt` | PDF export engine, draw methods |
| `PdfMergeManager.kt` | Merge + split/rotate/delete tools |
| `PdfConversionManager.kt` | Image to PDF dan PDF to Image |
| `PdfRendererManager.kt` | Android PdfRenderer wrapper |
| `PdfSignatureManager.kt` | Signature save/load |
| `FileUtils.kt` | SAF file utilities |
| `ShareUtils.kt` | Share intent utilities |
| `SignatureScreen.kt` | Layar baru untuk menggambar dan menyimpan signature PNG transparan |

## Files Modified

| File | Perubahan |
|------|-----------|
| `SYSTEM_MAP.md` | Baru dibuat — peta arsitektur lengkap |
| `DEV_PROGRESS.md` | Baru dibuat — catatan progres pengembangan |
| `MASTER_PLAN.md` | Update status Milestone 9, Milestone 10, renderer stabilization, force push main, dan validasi GitHub |
| `AppNavigation.kt` | Home/Main Tools dan Recent Files selalu memilih PDF baru sebelum membuka Editor; persist permission dibuat toleran terhadap provider non-persisten |
| `AppNavigation.kt` | Route SignatureScreen aktif dan return signature path via SavedStateHandle |
| `EditorScreen.kt` | Menerima signature path dan navigasi dari tool Sign |
| `EditorViewModel.kt` | Simpan pending tap, place signature, analytics tool selection |
| `SignatureScreen.kt` | Baru dibuat — canvas tanda tangan dan save PNG transparan |
| `strings.xml`, `values-in/strings.xml` | Tambah error string untuk save signature |
| `AndroidManifest.xml`, `ic_launcher.xml` | Fix launcher icon resource blocker |
| `PdfConversionManager.kt` | Baru dibuat — konversi image ke PDF dan PDF ke PNG |
| `ToolsScreen.kt` | PDF tools V1.5 aktif dengan SAF picker, dialog parameter, result/share |
| `ToolsViewModel.kt` | Baru dibuat — orkestrasi merge/split/rotate/delete/convert |
| `ShareUtils.kt` | Tambah share multi-file untuk hasil PDF to image |
| `HomeScreen.kt` | PDF tool cards diarahkan ke ToolsScreen; Sign/Fill Form membuka picker PDF |
| `SettingsScreen.kt` | Handler kosong dirapikan dan teks hardcoded dilokalisasi |
| `EditPdfApplication.kt`, `MainActivity.kt`, `LocaleUtils.kt` | Runtime locale device-aware: Bahasa Indonesia untuk device ID, English untuk lainnya |
| `FileUtils.kt` | Validasi PDF dibuat lebih toleran untuk SAF provider |
| `AppNavigation.kt`, `ToolsScreen.kt` | Persist read permission untuk URI file yang dipilih |
| `strings.xml`, `values-in/strings.xml` | Tambah dialog Settings dan perbaiki teks export saving |

## Important Functions / Flows Touched

- `EditPdfApplication.onCreate()` → init PdfBox
- `MainActivity.onCreate()` → setContent Compose
- `AppNavigation()` → Routes & NavHost
- `HomeScreen()` → UI utama (hero, tools grid, bottom nav)
- `PdfRendererManager.openPdf/renderPage` → render PDF ke Bitmap
- `PdfExportManager.exportPdf` → apply overlay ke PDF baru
- `PdfMergeManager.mergePdfs` + `PdfPageToolManager.*` → tools PDF
- `RecentFileRepository.addOrUpdateRecentFile` → CRUD recent files
- `AdFrequencyManager.canShowInterstitial` → frequency cap logic
- `InterstitialAdManager.showInterstitialIfReady` → ad lifecycle
- `RemoteConfigManager.*` → feature flags & config
- `SignatureScreen()` → draw signature, clear, save transparent PNG
- `EditorViewModel.placeSignatureAtPendingTap()` → menambahkan SignatureObject setelah SignatureScreen selesai
- `ToolsViewModel.*` → menjalankan merge/split/rotate/delete/image-to-pdf/pdf-to-image
- `PdfConversionManager.*` → konversi image/PDF

## Decisions Made

1. Dokumentasi ditulis dalam Bahasa Indonesia sesuai permintaan user.
2. `SYSTEM_MAP.md` mengikuti format yang diminta: Project Summary, Core Logic Flow, Clean Tree, Module Map, Data & Config, External Integrations, Risks/Blind Spots.
3. Semua file dibaca secara targeted (blok fungsi relevan), tidak full scan file besar secara membabi buta.
4. Exclusion list diterapkan: `.gradle`, `build`, `.git`, `node_modules`, dll diabaikan.

## Errors / Blockers

- Tidak ada blocker kode yang terkonfirmasi setelah compile/build lokal debug sukses.
- Local `:app:bundleRelease --no-daemon` timeout setelah 6 menit pada mesin ini; validasi AAB dilanjutkan via GitHub Actions.
- Warning lokal tersisa: Gradle mencetak `[CXX1101] NDK at C:\Users\User\AppData\Local\Android\Sdk\ndk\27.0.12077973 did not have a source.properties file`, tetapi `:app:assembleDebug` tetap sukses.
- Percobaan memakai `GRADLE_USER_HOME` di workspace gagal saat unzip Gradle distribution (`NoSuchFileException` pada zip hasil download) dan meninggalkan folder sementara `.gradle-user-home/`.

## Validation Status

- **Build**: Local `clean assembleDebug` passed. GitHub Actions `Android Build` run `27085606975` pada commit `cd567f8` juga passed; build debug APK, release AAB, dan kedua upload artifact semuanya `success`.
- **GitHub Artifacts**: `edit-pdf-online-debug-apk` id `7461740645`, size 31,054,416 bytes, digest `sha256:c28e98a1fc50e1ef7ce08bca1cb9ff621823851ec48f574540fc5c4efbcc58dc`; `edit-pdf-online-release-aab` id `7461740973`, size 13,548,966 bytes, digest `sha256:0cf37137fe6b863451f63a6ad4708da08d2bc813196d642dc52e9c73b338f3dd`.
- **Test**: Not run (tidak ada test files ditemukan)
- **Lint**: Not run
- **Manual Check**: Passed untuk static wiring: tidak ada `onClick = {}` kosong tersisa di source `ui`, Settings menu sudah punya dialog, PDF picker now persists read permission.

## Do Not Repeat

- Tidak perlu mengulang full scan seluruh file proyek — semua file sudah terdokumentasi di SYSTEM_MAP.md dan daftar di atas.
- Jangan ulang analisis dependency — sudah tercatat di `app/build.gradle.kts` dan `gradle/libs.versions.toml`.

## Resume Note for Next Agent

Aplikasi sudah menyelesaikan fresh picker fix di `AppNavigation.kt`. Home hero, Main Tools, recent list di Home, dan layar Recent Files tidak lagi membuka URI database secara langsung; semua meminta user memilih PDF baru sebelum masuk Editor. PDF Tools dan renderer tidak disentuh. Local `clean assembleDebug` dan GitHub Actions run `27085606975` sukses. Lanjut berikutnya: install APK artifact `7461740645` untuk device QA.
