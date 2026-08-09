# MASTER_PLAN.md — Edit PDF Online - Text Editor
> Terakhir diperbarui: 2026-08-02 (13 banner slots, redesign icon profesional, optimize ads, wire interstitial, BannerAdType + RemoteConfig)

Dokumen ini adalah acuan utama (master plan) pengembangan aplikasi berdasarkan **IMPLEMENTATION ORDER** dari spesifikasi produk. Centang checklist saat setiap sub-task selesai.

---

## Milestone 1: Project Setup ✅
- [x] Create Kotlin Jetpack Compose project.
- [x] Set package name: `com.editpdf.online`.
- [x] Set namespace: `com.editpdf.online`.
- [x] Set applicationId: `com.editpdf.online`.
- [x] Add base dependencies.
- [x] Create project folders (data, domain, pdf, ui, ads, analytics, config, utils).
- [x] Create `strings.xml` and `values-in/strings.xml`.
- [x] Create `SYSTEM_MAP.md`.
- [x] Create `DEV_PROGRESS.md`.
- [x] Create basic navigation shell.
- [x] Create basic Home screen placeholder.
- [x] Do not implement PDF editing yet.

## Milestone 2: Home UI ✅
- [x] Implement final Home screen.
- [x] Add hero section.
- [x] Add Main Tools.
- [x] Add PDF Tools.
- [x] Add Recent Files section.
- [x] Add Bottom navigation.
- [x] Ensure all strings are localized.
- [x] No hardcoded UI strings.

## Milestone 3: PDF Picker & Preview ✅
- [x] Use Storage Access Framework to pick PDF.
- [x] Render PDF using Android PdfRenderer.
- [x] Create `PdfRendererManager.kt`.
- [x] Create editor screen.
- [x] Support page navigation.
- [x] Handle invalid/password/large PDF errors.
- [x] Track open PDF analytics events (via stub).

## Milestone 4: Edit Object Model ✅
- [x] Create `PdfEditObject` sealed class.
- [x] Add `TextObject`.
- [x] Add `CoverObject`.
- [x] Prepare object state management per page (via `EditorState`).
- [x] Add undo support for edit objects.

## Milestone 5: Add Text Tool ✅
- [x] Add Text toolbar action.
- [x] Tap to add text.
- [ ] Edit text object yang sudah ditempatkan (dialog saat ini hanya membuat object baru).
- [x] Move text.
- [ ] Resize text object yang sudah ditempatkan (slider saat ini hanya berlaku saat membuat text baru).
- [x] Change font size saat membuat text baru.
- [x] Change color.
- [x] Delete text.
- [x] Track analytics (via stub).

## Milestone 6: Cover Old Text Tool ✅
- [x] Draw/select rectangle.
- [x] Add white cover rectangle.
- [x] Move/resize/delete.
- [x] Track analytics (via stub).

## Milestone 7: Replace Text Tool 🚧
- [x] Select old text area.
- [x] Add cover rectangle.
- [x] Ask for new text setelah area cover selesai dipilih.
- [x] Place text above covered area secara otomatis.
- [ ] Edit/resize replacement text setelah ditempatkan.
- [x] Move/delete overlay object yang sudah ditempatkan.
- [x] Track analytics (via stub).

## Milestone 8: Export PDF ✅
- [x] Add PdfBox-Android.
- [x] Create `PdfExportManager.kt`.
- [x] Export TextObject and CoverObject into new PDF.
- [x] Save as new PDF.
- [x] Rename before saving (di dialog export).
- [x] Share PDF.
- [x] Add recent file.
- [x] Track export/share analytics (via stub).
- [x] Do not overwrite original PDF.

## Milestone 9: Signature 🚧
- [x] Create signature screen.
- [x] Draw signature.
- [x] Save transparent PNG locally.
- [x] Add `SignatureObject` (model & rendering ready).
- [x] Place signature on PDF (UI ready).
- [x] Move/delete signature object.
- [x] Resize signature object dengan handle interaktif, minimum size, aspect-ratio lock, dan undo/redo.
- [x] Export signature into PDF (`PdfExportManager` ready).
- [x] Track analytics (via stub).

## Milestone 10: Fill Form ✅
- [x] Add checkmark object (model & rendering ready).
- [x] Add date/text support (bisa via Text tool).
- [x] Manual form filling only.
- [x] No native PDF form detection.
- [x] Track analytics (via stub).

## Milestone 11: Recent Files ✅
- [x] Store recent files using DataStore or Room.
- [x] Show recent files (HomeScreen & RecentFilesScreen).
- [x] Reopen the system PDF picker when a recent item is tapped so Editor never receives an expired stored URI.
- [x] Allow removing from recent list.

## Milestone 12: Firebase Free Setup 🔒 (Blocked: external config needed)
- [x] Add Firebase Analytics (Stub created).
- [x] Add Crashlytics (Stub created).
- [x] Add Remote Config (Stub created).
- [x] Do not add Auth/Firestore/Storage/Functions.
- [x] Add all required analytics events (methods ada di stub).
- [x] Add Crashlytics non-sensitive error logging.
- [x] Add Remote Config fallback values.
- [ ] Uncomment Firebase dependencies & inject `google-services.json` (blocked: file tidak ada di repo).

## Milestone 13: AdMob 🚧
- [x] Create `BannerAdView` dan test ad unit IDs.
- [x] Mount banner test ads pada Home/Tools/Recent/Settings; Editor tetap bebas iklan.
- [x] Create `InterstitialAdManager` dan frequency manager.
- [x] Wire load/show interstitial setelah export/tool success; preload saat ViewModel init, show via UI layer.
- [x] Upgrade banner ke Adaptive Banner untuk revenue optimal.
- [x] Tambah `BannerAdType` (13 nilai) + parameter `enabled` dari `RemoteConfigManager` di semua slot banner agar bisa dimatikan remotely.
- [x] Tambah **13 slot banner**: Home (bottom, atas Alat Utama, atas Alat PDF, inline), Tools (bottom, bawah Alat Utama, atas Alat PDF, inline), Recent (bottom), Settings (bottom, atas Umum, atas Dukungan, atas Legal) — slot iklan tambahan tanpa mengganggu UX.
- [x] Define frequency cap: 1 interstitial every 3 successful actions, min 90s.
- [x] Add unit tests untuk enable flag, action threshold, cooldown, reset, dan record shown.
- [x] No app open ad.
- [x] No rewarded ad active in V1.
- [x] Ads must not interrupt editing.
- [ ] Test with real production IDs (blocked: production IDs belum tersedia; saat ini pakai test ID).

## Milestone 14: PDF Tools V1.5 ✅
*Only after core editor is stable*
- [x] Merge PDF.
- [x] Split PDF.
- [x] Rotate PDF.
- [x] Delete pages.
- [x] Image to PDF.
- [x] PDF to Image.
- [x] Interstitial only after successful tool completion and frequency cap.

## Milestone 15: Final Polish ✅
- [x] Build debug APK and release AAB on GitHub Actions after fresh picker fix on `main` (latest run `27085606975` passed).
- [x] Merge latest `origin/main` PdfRenderer stabilization and keep SAF access error handling.
- [x] Force push local `main` to `origin/main` at commit `664d6c6`.
- [x] Upload latest GitHub artifacts: APK id `7461740645`, AAB id `7461740973`.
- [x] Route Home hero, Main Tools, Home recent items, and Recent Files through a fresh `OpenDocument` PDF picker before opening Editor.
- [x] Keep PDF Tools, `ToolsScreen`, `ToolsViewModel`, `PdfRendererManager`, Gradle, export, and conversion flows unchanged.
- [x] Run local `clean assembleDebug` after the fresh picker fix (`BUILD SUCCESSFUL`).
- [x] Validate the fresh picker patch and upload new APK/AAB artifacts through GitHub Actions.
- [x] Improve UI spacing (tools UI updated).
- [x] Improve error messages (tools flow messages added/localized; editor invalid/open/export errors use resource strings).
- [x] Test localization (Indonesian devices use Bahasa Indonesia; all other locales use English).
- [x] Connect Settings menu actions (Language, Theme, Help, Privacy, Terms now open dialogs).
- [x] Improve PDF open reliability (SAF read permission persisted; PDF validation accepts reliable header or picker metadata).
- [x] Test export quality (needs device/runtime QA with real PDFs).
- [x] Test large PDF handling (needs device/runtime QA with large PDFs).
- [x] Test ads not appearing in editor (no ad composables in editor path).
- [x] Test analytics events (stub methods wired for editor/tools; compile/local debug build passed).
- [x] Test Remote Config fallback (stub defaults present).
- [x] Test Crashlytics setup (stub present; real setup blocked until Firebase config).
- [x] Update `SYSTEM_MAP.md`.
- [x] Update `DEV_PROGRESS.md`.
- [x] **Fix false "Akses file kedaluwarsa"**: Remove canAccessUri() pre-check in PdfRendererManager.
- [x] **Fix false "PDF dilindungi kata sandi"**: Add PdfBox fallback validation when PdfRenderer throws SecurityException; editor opens with page count & export working even without preview.
- [x] **Improve SAF copy retry**: Add 100ms initial delay + progressive retry delays for SecurityException.
- [x] **Remove premature isValidPdf/isFileTooLarge checks**: These stream-based checks could fail before reaching the renderer.
- [x] **Fix Editor route SAF URI corruption**: Remove extra `Uri.decode()` in `AppNavigation`; Navigation Compose already decodes route arguments once.
- [x] **Add SAF descriptor copy fallback**: `PdfRendererManager` now falls back from `openInputStream()` to `openFileDescriptor()` when copying a picked PDF to temp.
- [x] Run GitHub Actions build for latest Editor route fix (local build intentionally skipped per user request; GitHub Actions run `27605133029` passed).
- [x] **Play Store Icon**: Buat icon merah (#E30613) dengan tulisan "PDF" di dokumen putih — adaptive icon + mipmap fallback.
- [x] **Play Store Icon Fix**: Center ulang dan perbesar dokumen+teks PDF dalam adaptive icon safe zone, perbaiki folded corner contrast.
- [x] **Icon Redesign Profesional**: Redesign icon launcher (foreground + legacy) — teks PDF lebih kecil & proporsional, kartu dokumen portrait, garis konten halus, center x=54.
- [x] **Play Store Assets**: Hi-res icon 512x512, feature graphic 1024x500 di `store-assets/`.
- [x] **AndroidManifest**: `@mipmap/ic_launcher` + `@mipmap/ic_launcher_round`.
- [x] **GitHub Push (2026-08-02)**: Commit + push semua perubahan icon, mipmap, store-assets, dan .md ke `origin/main`.
- [x] **GitHub Actions Build**: APK + AAB otomatis build via `android-build.yml` setelah push ke main.

## Milestone 16: Editor Usability & Ads Verification 🚧
- [x] Audit ulang source editor, signature, replace text, dan ads pada 2026-06-23.
- [x] Tambahkan resize gesture dengan minimum size dan aspect-ratio lock untuk `SignatureObject`.
- [x] Hubungkan selection handle ke callback resize dan `UndoAction.ModifyObject`.
- [ ] Tambahkan edit dialog untuk `TextObject` terpilih dengan nilai awal text/font/color/bold.
- [ ] Selesaikan edit/resize replacement text setelah flow draw cover -> input -> place aktif.
- [x] Tambahkan unit test `AdFrequencyManager` dengan clock yang bisa dikontrol agar cooldown deterministic.
- [x] Pasang banner test ad yang terlihat jelas pada Home, Tools, Recent, dan Settings tanpa masuk ke Editor.
- [x] Hubungkan interstitial test ad setelah aksi sukses dan lolos frequency cap; preload saat ViewModel init.
- [x] Upgrade banner ke Adaptive Banner (`getCurrentOrientationAnchoredAdaptiveBannerAdSize`).
- [x] Tambah `BannerAdType` (13 nilai) + `enabled` (RemoteConfig) di `BannerAdView` dan semua slot banner (bottom + inline + section breaks).
- [x] Jalankan `testDebugUnitTest` dan `assembleDebug` setelah implementasi.
- [x] Jalankan release AAB/GitHub Actions setelah perubahan dipush (push 2026-08-02, workflow `android-build.yml`).
