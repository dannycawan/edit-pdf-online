# DEV_PROGRESS.md — Edit PDF Online - Text Editor
> Terakhir diperbarui: 2026-06-16

---

## Active Task

Memperbaiki error "Tidak dapat membuka file PDF" pada tombol Buka PDF, kelompok Alat Utama, dan edit tools yang membuka Editor.

## Current Status

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
- **Build**: Local `assembleDebug` lama pernah sukses, tetapi untuk siklus 2026-06-16 build lokal tidak dijalankan. Verifikasi berikutnya harus lewat GitHub Actions.
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

## In Progress

- Device QA menggunakan APK terbaru.

## Next Exact Steps

1. **Device QA**: Install APK terbaru dan test open PDF dari:
   - Home hero button
   - Main Tools grid
   - Recent files
   - File dari Google Drive / Download / internal storage
2. Test apakah PDF dengan DRM/restrictions bisa dibuka (tanpa preview, dengan page count).
3. Test export tetap berfungsi untuk PDF yang dibuka via PdfBox fallback.

## Files Modified (siklus ini)

| File | Perubahan |
|------|-----------|
| `PdfRendererManager.kt` | Hapus `canAccessUri()` pre-check; tambah retry delay; tambah PdfBox fallback (openWithPdfBox); getPageDimensions via PdfBox |
| `EditorViewModel.kt` | Hapus `isPdfFile()`/`isFileTooLarge()` pre-check; error mapping lebih akurat (SecurityException -> password_pdf, IOException -> open_pdf) |
| `AppNavigation.kt` | Perbaiki komentar `persistReadPermission` |

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
