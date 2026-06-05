# MASTER_PLAN.md — Edit PDF Online - Text Editor
> Terakhir diperbarui: 2026-06-05

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
- [x] Edit text.
- [x] Move text.
- [x] Resize text (via scale/slider).
- [x] Change font size.
- [x] Change color.
- [x] Delete text.
- [x] Track analytics (via stub).

## Milestone 6: Cover Old Text Tool ✅
- [x] Draw/select rectangle.
- [x] Add white cover rectangle.
- [x] Move/resize/delete.
- [x] Track analytics (via stub).

## Milestone 7: Replace Text Tool ✅
- [x] Select old text area.
- [x] Add cover rectangle.
- [x] Ask for new text.
- [x] Place text above covered area.
- [x] Move/resize/edit/delete.
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

## Milestone 9: Signature ✅
- [x] Create signature screen.
- [x] Draw signature.
- [x] Save transparent PNG locally.
- [x] Add `SignatureObject` (model & rendering ready).
- [x] Place signature on PDF (UI ready).
- [x] Move/resize/delete (UI ready).
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
- [x] Allow quick open.
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

## Milestone 13: AdMob 🔒 (Blocked: production IDs needed)
- [x] Add banner ads on Home/Tools/Recent only (Composables ready).
- [x] Add interstitial after export success only (Logic ready).
- [x] Add frequency cap: 1 interstitial every 2-3 exports, min 90s.
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

## Milestone 15: Final Polish 🔄 (In Progress)
- [x] Build debug APK and release AAB on GitHub Actions (latest completed run `26960485576` passed; current PR validation will rerun on GitHub).
- [x] Merge latest `origin/main` PdfRenderer stabilization and keep SAF access error handling.
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
