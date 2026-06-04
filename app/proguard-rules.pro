# ProGuard rules for Edit PDF Online - Text Editor
# Purpose: Configure code shrinking rules for release builds
# Caller: Android build system during release builds

# PdfBox-Android
-keep class com.tom_roush.pdfbox.** { *; }
-keep class org.apache.fontbox.** { *; }
-keep class org.apache.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn org.apache.fontbox.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.commons.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# AdMob
-keep class com.google.android.gms.ads.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }

# Compose
-dontwarn androidx.compose.**
