# SYSTEM_MAP.md — Edit PDF Online - Text Editor
> Terakhir diperbarui: 2026-08-02

---

## Project Summary

- **Tujuan aplikasi**: Aplikasi Android untuk membuka, mengedit (teks, tanda tangan, checkmark, cover), merge, split, rotate, dan mengekspor file PDF.
- **Tech stack utama**:
  - **Runtime**: Android (minSdk 24, targetSdk 35, compileSdk 35)
  - **Bahasa**: Kotlin + Jetpack Compose (Material3)
  - **PDF Engine**: PdfBox-Android (edit/export/merge/split), Android PdfRenderer (rendering preview)
  - **Database**: Room (SQLite) — entity `recent_files`
  - **Storage**: DataStore Preferences (belum digunakan aktif)
  - **Ads**: Google AdMob (banner + interstitial)
  - **Analytics**: Firebase Analytics (stub, belum aktif)
  - **Crash Reporting**: Firebase Crashlytics (stub, belum aktif)
  - **Remote Config**: Firebase Remote Config (stub, lokal defaults)
  - **Navigation**: Jetpack Navigation Compose
  - **Image Loading**: Coil Compose
  - **Build**: Gradle Kotlin DSL + Version Catalog (`libs.versions.toml`)
- **Pola arsitektur**: Single-Activity + Compose Navigation, pendekatan Clean Architecture ringan (data/domain/ui layers), tanpa DI framework (manual instantiation).

## Audit Delta 2026-06-23

- Semua `PdfEditObject` adalah overlay di atas halaman asli; aplikasi belum mengubah content stream text asli secara langsung.
- `TextObject`: add/move/delete/export aktif. Edit dan resize object yang sudah ditempatkan belum terhubung.
- `CoverObject`: draw/move/delete/export aktif. Resize handle belum interaktif.
- `SignatureObject`: draw/save/place/move/delete/resize/export aktif; resize menjaga aspect ratio dan masuk undo/redo.
- `CheckmarkObject`: add/move/delete/export aktif; resize belum tersedia.
- `EditorOverlay.SelectionHandles()` saat ini hanya menggambar satu titik indikator dan tidak menerima input gesture.
- `UndoAction.ModifyObject` sudah dimodelkan, tetapi flow move/resize/edit belum memasukkannya ke undo stack.
- Tool `REPLACE` menggambar cover, membuka dialog input, lalu membuat replacement `TextObject` pada area pilihan.
- `BannerAdView` dipasang pada Home/Tools/Recent; Editor tidak memuat banner.
- `InterstitialAdManager` tersedia tetapi belum dipanggil untuk preload/show; flow sukses hanya menaikkan counter `AdFrequencyManager`.
- Unit test `AdFrequencyManager` mencakup threshold, cooldown, disabled config, dan reset; instrumentation test belum ada.

---

## Core Logic Flow (Function-Level Flowchart)

### Flow 1: Buka & Edit PDF
```
User tap "Open PDF" (HomeScreen)
  -> SAF file picker (implemented via ActivityResultContracts)
    -> AppNavigation.editorRoute(uri, toolId?) // Uri.encode() once; do not Uri.decode() again
      -> EditorScreen (diimplementasi dengan EditorViewModel)
        -> PdfRendererManager.openPdf(uri)         // 3 strategies:
           1. Copy SAF file ke temp + PdfRenderer   // primary
           2. Direct SAF FD + PdfRenderer            // fallback 1
           3. PdfBox validation (page count only)    // fallback 2 jika PdfRenderer SecurityException
        -> PdfRendererManager.renderPage(pageIndex)  // render ke Bitmap @ scale 2.5f (null jika PdfBox fallback aktif)
        -> EditorScreen.LaunchedEffect(initialTool, totalPages) // auto-select tool setelah PDF load
        -> User menambah overlay (Text/Cover/Signature/Checkmark)
          -> EditorState.editObjects di-update
        -> PdfExportManager.exportPdf(sourceUri, editObjects, outputStream)
          -> drawCoverObject / drawTextObject / drawSignatureObject / drawCheckmarkObject
        -> ShareUtils.sharePdf(file) / save via SAF
```

### Flow 1a: Buat & Tempatkan Signature
```
User pilih tool Sign di EditorScreen
  -> User tap posisi di halaman PDF
    -> EditorViewModel.storePendingTap(...)
    -> AppNavigation navigate ke SignatureScreen
      -> User menggambar signature di Compose Canvas
      -> PdfSignatureManager.saveSignature(bitmap) // PNG transparan di filesDir/signatures
      -> SignatureScreen mengembalikan signature_path via SavedStateHandle
    -> EditorScreen menerima signatureImagePath
    -> EditorViewModel.placeSignatureAtPendingTap(path)
    -> SignatureObject ditambahkan ke current page
```

### Flow 2: Merge PDF
```
User pilih file PDF (ToolsScreen SAF picker)
  -> PdfMergeManager.mergePdfs(uris, outputFile)
    -> PDFMergerUtility (PdfBox-Android)
  -> Result<File>
```

### Flow 3: Split / Rotate / Delete Pages
```
User pilih file PDF + pengaturan (ToolsScreen)
  -> PdfPageToolManager.splitPdf(sourceUri, pageRanges, outputDir)
  -> PdfPageToolManager.rotatePages(sourceUri, pageIndices, degrees, outputFile)
  -> PdfPageToolManager.deletePages(sourceUri, pageIndices, outputFile)
    -> PDDocument (PdfBox-Android)
  -> Result<File> / Result<List<File>>
```

### Flow 3a: Image/PDF Conversion
```
User pilih Image to PDF / PDF to Image (ToolsScreen)
  -> SAF picker image/pdf
    -> ToolsViewModel.imageToPdf(...) / pdfToImages(...)
      -> PdfConversionManager.imageToPdf(...) // PdfBox PDDocument + LosslessFactory
      -> PdfConversionManager.pdfToImages(...) // Android PdfRenderer + PNG output
    -> Result dialog + ShareUtils.shareFiles(...)
```

### Flow 4: Recent Files
```
HomeScreen / RecentFilesScreen menampilkan metadata recent dari Room
  -> User tap recent item
    -> AppNavigation membuka ActivityResultContracts.OpenDocument()
      -> User memilih PDF baru
        -> persistReadPermission(context, uri)
        -> Routes.editorRoute(uri.toString())
        -> EditorScreen menerima URI fresh
```

### Flow 5: Interstitial Ad
```
Setelah export/action sukses
  -> AdFrequencyManager.recordSuccessfulAction()
  -> AdFrequencyManager.canShowInterstitial()
    -> cek isInterstitialAfterExportEnabled (RemoteConfigManager)
    -> cek actionCount >= frequency
    -> cek elapsed >= minSeconds
  -> InterstitialAdManager.showInterstitialIfReady(activity)
    -> InterstitialAd.show(activity)
```

---

## Clean Tree

```
edit pdf online/
├── store-assets/                          # Play Store assets
│   ├── ic_launcher-playstore.png          # Hi-res icon 512x512
│   └── feature_graphic.png                # Feature graphic 1024x500
├── build.gradle.kts                       # Root build script
├── settings.gradle.kts                    # Project settings
├── gradle.properties                      # Gradle properties
├── gradle/
│   └── libs.versions.toml                 # Version catalog
├── app/
│   ├── build.gradle.kts                   # App module build config
│   ├── proguard-rules.pro                 # ProGuard rules
│   └── src/main/
│       ├── AndroidManifest.xml            # App manifest
│       ├── res/
│       │   ├── drawable/
│       │   │   ├── ic_launcher.xml         # Legacy vector icon (red + PDF)
│       │   │   ├── ic_launcher_foreground.xml # Adaptive icon foreground
│       │   │   └── ic_launcher_background.xml # Adaptive icon background (red)
│       │   ├── mipmap-anydpi-v26/
│       │   │   ├── ic_launcher.xml         # Adaptive icon definition
│       │   │   └── ic_launcher_round.xml   # Adaptive icon round variant
│       │   ├── mipmap-mdpi/               # 48x48 fallback PNGs
│       │   ├── mipmap-hdpi/               # 72x72 fallback PNGs
│       │   ├── mipmap-xhdpi/              # 96x96 fallback PNGs
│       │   ├── mipmap-xxhdpi/             # 144x144 fallback PNGs
│       │   ├── mipmap-xxxhdpi/            # 192x192 fallback PNGs
│       │   ├── values/
│       │   │   ├── strings.xml            # String resources (EN)
│       │   │   ├── colors.xml             # XML color definitions
│       │   │   └── themes.xml             # XML theme (non-Compose)
│       │   └── values-in/                 # Bahasa Indonesia strings
│       └── java/com/editpdf/online/
│           ├── EditPdfApplication.kt      # Application class
│           ├── MainActivity.kt            # Single Activity host
│           ├── ads/
│           │   ├── AdFrequencyManager.kt  # Frequency cap logic
│           │   ├── BannerAdView.kt        # Banner ad composable
│           │   └── InterstitialAdManager.kt # Interstitial lifecycle
│           ├── analytics/
│           │   ├── AnalyticsTracker.kt    # Firebase Analytics (stub)
│           │   └── CrashReporter.kt       # Crashlytics (stub)
│           ├── config/
│           │   └── RemoteConfigManager.kt # Remote Config (stub + defaults)
│           ├── data/
│           │   ├── local/
│           │   │   ├── AppDatabase.kt     # Room database (singleton)
│           │   │   └── RecentFileDao.kt   # DAO for recent_files
│           │   ├── model/
│           │   │   └── RecentFile.kt      # Room entity recent_files
│           │   └── repository/
│           │       └── RecentFileRepository.kt # CRUD wrapper
│           ├── domain/
│           │   └── model/
│           │       ├── EditorState.kt     # Editor state + EditorTool enum
│           │       └── PdfEditObject.kt   # Sealed class: Text/Cover/Signature/Checkmark
│           ├── pdf/
│           │   ├── PdfEditorEngine.kt     # Coordinate transform (screen <-> PDF)
│           │   ├── PdfExportManager.kt    # Export overlay ke PDF baru
│           │   ├── PdfMergeManager.kt     # Merge + PdfPageToolManager (split/rotate/delete)
│           │   ├── PdfRendererManager.kt  # Android PdfRenderer wrapper
│           │   └── PdfSignatureManager.kt # Simpan/load signature PNG
│           ├── ui/
│           │   ├── home/
│           │   │   └── HomeScreen.kt      # Home screen composable
│           │   ├── navigation/
│           │   │   └── AppNavigation.kt   # NavHost + Routes
│           │   ├── signature/
│           │   │   └── SignatureScreen.kt # Canvas gambar tanda tangan + save PNG
│           │   └── theme/
│           │       ├── Color.kt           # Compose color tokens
│           │       ├── Theme.kt           # MaterialTheme setup
│           │       └── Type.kt            # Typography definitions
│           └── utils/
│               ├── FileUtils.kt           # SAF file utilities
│               └── ShareUtils.kt          # Share intent utilities
```

---

## Module Map (The Chapters)

### Entry Points

| File | Fungsi/Class Utama | Peran |
|------|-------------------|-------|
| `EditPdfApplication.kt` | `EditPdfApplication.attachBaseContext()`, `EditPdfApplication.onCreate()` | Menerapkan runtime locale device-aware dan inisialisasi PdfBox-Android resource loader saat app start |
| `MainActivity.kt` | `MainActivity.attachBaseContext()`, `MainActivity.onCreate()` | Menerapkan runtime locale device-aware, lalu host Compose content dengan theme + navigation |
| `AppNavigation.kt` | `AppNavigation()`, `Routes` object | Definisi semua route & NavHost; startDestination = HOME |

### UI Layer

| File | Fungsi/Class Utama | Peran |
|------|-------------------|-------|
| `HomeScreen.kt` | `HomeScreen()`, `HeroSection()`, `MainToolsGrid()`, `PdfToolsGrid()`, `BottomNavBar()` | Layar utama: hero CTA, grid tool editing, grid tool PDF, recent files, bottom nav |
| `SignatureScreen.kt` | `SignatureScreen()`, `SignaturePad()`, `createSignatureBitmap()` | Layar gambar tanda tangan: menangkap stroke, menyimpan PNG transparan, lalu kembali ke editor |
| `ToolsScreen.kt` | `ToolsScreen()`, tool dialogs, result/error dialogs | UI tools V1.5: picker SAF, dialog split/rotate/delete, share hasil |
| `ToolsViewModel.kt` | `mergePdfs()`, `splitPdf()`, `rotatePdf()`, `deletePages()`, `imageToPdf()`, `pdfToImages()` | State dan orkestrasi PDF tools, analytics stub, ad action frequency |
| `Color.kt` | Konstanta warna (PrimaryNavy, SecondaryBlue, dll) | Token warna untuk seluruh app |
| `Theme.kt` | `EditPdfOnlineTheme()` | Material3 light theme setup |
| `Type.kt` | `AppTypography` | Definisi tipografi app |

### PDF Engine

| File | Fungsi/Class Utama | Peran |
|------|-------------------|-------|
| `PdfRendererManager.kt` | `openPdf()`, `renderPage()`, `closePdf()`, `getPageDimensions()`, `openWithPdfBox()` | Render PDF ke Bitmap via Android PdfRenderer; fallback ke PdfBox untuk validasi + page count jika PdfRenderer gagal (SecurityException). Copy SAF file ke temp dengan retry & delay progresif. |
| `PdfExportManager.kt` | `exportPdf()`, `exportToFile()`, `drawTextObject()`, `drawCoverObject()`, `drawSignatureObject()`, `drawCheckmarkObject()` | Menulis semua overlay edit ke PDF baru via PdfBox-Android |
| `PdfEditorEngine.kt` | `screenToPdf()`, `pdfToScreen()`, `screenSizeToPdf()` | Konversi koordinat antara screen space (Compose) dan PDF space (PdfBox) |
| `PdfMergeManager.kt` | `PdfMergeManager.mergePdfs()`, `PdfPageToolManager.splitPdf()`, `.rotatePages()`, `.deletePages()` | Tools merge, split, rotate, delete halaman PDF |
| `PdfConversionManager.kt` | `imageToPdf()`, `pdfToImages()` | Konversi gambar ke PDF dan PDF ke PNG per halaman |
| `PdfSignatureManager.kt` | `saveSignature()`, `loadSignature()`, `getSavedSignatures()`, `deleteSignature()` | Simpan/load bitmap tanda tangan sebagai PNG di internal storage |

### Domain Models

| File | Fungsi/Class Utama | Peran |
|------|-------------------|-------|
| `EditorState.kt` | `EditorState`, `EditorTool` enum, `UndoAction` sealed class | State editor: halaman aktif, tool aktif, overlay objects, undo/redo stack |
| `PdfEditObject.kt` | `PdfEditObject` sealed class: `TextObject`, `CoverObject`, `SignatureObject`, `CheckmarkObject` | Model overlay editing yang ditempatkan di atas halaman PDF |

### Data Layer

| File | Fungsi/Class Utama | Peran |
|------|-------------------|-------|
| `AppDatabase.kt` | `AppDatabase` (Room, singleton) | Database Room `edit_pdf_online.db`, menyediakan DAO |
| `RecentFileDao.kt` | `getAllRecentFiles()`, `getRecentFiles()`, `insertRecentFile()`, `updateRecentFile()`, `deleteRecentFile()` | DAO CRUD untuk tabel `recent_files` |
| `RecentFile.kt` | `RecentFile` data class (Room Entity) | Entity: id, fileName, fileUri, fileSizeBytes, pageCount, lastOpenedTime, thumbnailPath, isExported |
| `RecentFileRepository.kt` | `addOrUpdateRecentFile()`, `markAsExported()`, `deleteRecentFile()` | Repository wrapper — cek existing by URI, insert/update |

### Ads & Monetisasi

| File | Fungsi/Class Utama | Peran |
|------|-------------------|-------|
| `BannerAdView.kt` | `BannerAdView()` composable, `TEST_BANNER_AD_UNIT_ID`, `TEST_INTERSTITIAL_AD_UNIT_ID` | Banner ad wrapper untuk Compose via AndroidView |
| `InterstitialAdManager.kt` | `loadInterstitial()`, `showInterstitialIfReady()` | Lifecycle load/show interstitial dengan frequency cap |
| `AdFrequencyManager.kt` | `canShowInterstitial()`, `recordSuccessfulAction()`, `recordInterstitialShown()` | Frequency cap: setiap N aksi + minimum M detik antar interstitial |

### Config & Analytics

| File | Fungsi/Class Utama | Peran |
|------|-------------------|-------|
| `RemoteConfigManager.kt` | `fetchConfig()`, property getters (isBannerHomeEnabled, interstitialFrequency, dll) | Remote Config stub dengan default values lokal; kontrol fitur & ads |
| `AnalyticsTracker.kt` | `trackOpenPdfClicked()`, `trackExportSuccess()`, `trackToolUsed()`, dll | Firebase Analytics stub — semua event di-log ke Logcat |
| `CrashReporter.kt` | `logError()`, `logMessage()`, `setCustomKey()` | Crashlytics stub — log ke Logcat |

### Utilities

| File | Fungsi/Class Utama | Peran |
|------|-------------------|-------|
| `FileUtils.kt` | `getFileName()`, `getFileSize()`, `generateOutputFileName()`, `createTempOutputFile()`, `isPdfFile()`, `isFileTooLarge()` | Utility SAF file operations; validasi PDF memakai header atau metadata picker |
| `LocaleUtils.kt` | `applyAppLocale()` | Menggunakan Bahasa Indonesia untuk device locale ID dan English untuk locale lain |
| `ShareUtils.kt` | `sharePdf()`, `sharePdfUri()` | Share PDF via Android Intent/FileProvider |

### Editor File Picker Policy

- Home hero dan semua Main Tools memakai satu launcher `OpenDocument` dengan MIME `application/pdf`.
- URI hasil picker boleh melewati Navigation route hanya dalam bentuk encoded route argument. Navigation Compose sudah mengembalikan argumen yang decoded satu kali; jangan panggil `Uri.decode()` lagi sebelum `EditorScreen`.
- Recent item di Home dan `RecentFilesScreen` membuka picker ulang; URI yang tersimpan hanya menjadi metadata recent dan tidak dipakai langsung untuk membuka Editor.
- `persistReadPermission()` mengabaikan provider yang hanya memberi akses sementara atau tidak mendukung permission persisten.
- PDF Tools tetap memakai flow miliknya sendiri dan memakai URI langsung di `ToolsScreen`/`ToolsViewModel`, sehingga tidak terdampak bug encoding route Editor.

---

## Data & Config

### Config Locations
- **Version catalog**: `gradle/libs.versions.toml`
- **App build config**: `app/build.gradle.kts` (namespace = `com.editpdf.online`)
- **String resources**: `app/src/main/res/values/strings.xml` (EN), `app/src/main/res/values-in/` (ID)
- **Colors XML**: `app/src/main/res/values/colors.xml`
- **Remote Config defaults**: hardcoded di `RemoteConfigManager.kt` (defaults map)
- **AdMob App ID**: `AndroidManifest.xml` meta-data (test ID: `ca-app-pub-3940256099942544~3347511713`)

### Skema Data
- **Database**: `edit_pdf_online.db` (Room, version 1)
- **Tabel `recent_files`**:
  | Kolom | Tipe | Keterangan |
  |-------|------|------------|
  | id | String (PK) | UUID |
  | fileName | String | Nama file display |
  | fileUri | String | SAF URI |
  | fileSizeBytes | Long | Ukuran file |
  | pageCount | Int | Jumlah halaman |
  | lastOpenedTime | Long | Timestamp terakhir dibuka |
  | lastExportedTime | Long? | Timestamp terakhir diekspor |
  | thumbnailPath | String? | Path thumbnail (belum digunakan) |
  | isExported | Boolean | Flag sudah pernah diekspor |

### Migration/Seed
- Tidak ada migration file eksplisit; menggunakan `fallbackToDestructiveMigration()`.

### Folder Output/Runtime Artifacts
- **Temp PDF files**: `context.cacheDir/temp_pdf_*.pdf` (PdfRendererManager)
- **Export output**: `context.cacheDir/exports/` (FileUtils.createTempOutputFile)
- **PDF tools output**: `context.cacheDir/pdf_tools/` (merge/split/rotate/delete/convert output)
- **Signatures**: `context.filesDir/signatures/*.png` (PdfSignatureManager)

---

## External Integrations

| Service | Modul Pemanggil | Status |
|---------|----------------|--------|
| Google AdMob (Banner) | `BannerAdView.kt` | Aktif (test IDs) |
| Google AdMob (Interstitial) | `InterstitialAdManager.kt` | Aktif (test IDs) |
| Firebase Analytics | `AnalyticsTracker.kt` | Stub (log ke Logcat) |
| Firebase Crashlytics | `CrashReporter.kt` | Stub (log ke Logcat) |
| Firebase Remote Config | `RemoteConfigManager.kt` | Stub (local defaults) |
| PdfBox-Android | `PdfExportManager.kt`, `PdfMergeManager.kt` | Aktif |
| Android PdfRenderer | `PdfRendererManager.kt` | Aktif |
| Android SAF (Storage Access Framework) | `FileUtils.kt`, semua PDF managers | Aktif |
| FileProvider | `ShareUtils.kt`, `AndroidManifest.xml`, `file_paths.xml` | Aktif untuk share exported PDF |

## Build & Release Status

- **Remote utama**: `origin/main`.
- **Push terakhir (2026-08-02)**: Commit icon merah + mipmap + store-assets + .md update → push ke `origin/main`.
- **GitHub Actions**: Workflow `android-build.yml` otomatis build APK (debug) + AAB (release) saat push ke main.
- **Play Store Assets**: Icon merah (#E30613) + tulisan PDF sudah dibuat. Adaptive icon + mipmap fallback tersedia.
- **AndroidManifest**: Icon merujuk `@mipmap/ic_launcher` dan `@mipmap/ic_launcher_round`.
- **Store Assets**: `store-assets/ic_launcher-playstore.png` (512x512), `store-assets/feature_graphic.png` (1024x500).

---

## Risks / Blind Spots

1. **Firebase belum dikonfigurasi** — semua Firebase dependencies di-comment; `google-services.json` belum ada.
2. **Local Android SDK warning** — `:app:assembleDebug` sudah berhasil pada 2026-06-05, tetapi Gradle masih dapat mencetak warning NDK `source.properties` hilang di `C:\Users\User\AppData\Local\Android\Sdk\ndk\27.0.12077973`.
3. **DataStore Preferences** ada di dependency tapi belum digunakan secara aktif (saat ini Room yang dipakai untuk recent files).
4. **Coil Compose** ada di dependency tapi belum digunakan (disiapkan untuk load signature PNG).
5. **ProGuard rules** hanya komentar default — belum ada rules untuk PdfBox-Android atau AdMob.
6. **Runtime QA PDF Tools** masih perlu dicoba di perangkat dengan PDF nyata untuk export quality, large PDF, dan file password-protected.
7. **Launcher icon** sekarang memakai adaptive icon merah (#E30613) + tulisan "PDF", dengan mipmap fallback PNG untuk Android < 8.0. Aset Play Store (512x512 hi-res icon dan 1024x500 feature graphic) tersedia di `store-assets/`.
8. **PdfBox rendering unavailable** — PdfBox-Android 2.0.27.0 tidak menyediakan API konversi `BufferedImage` ke `Bitmap` Android. Saat PdfBox fallback aktif, preview halaman tidak tampil, tetapi navigasi, page count, dan export tetap berfungsi.
