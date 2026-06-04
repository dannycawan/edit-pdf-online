/**
 * Purpose: Root build script for Edit PDF Online - Text Editor
 * Caller: Gradle build system
 * Dependencies: AGP, Kotlin, KSP plugins via version catalog
 * Main Functions: Plugin declarations for the project
 * Side Effects: None at root level
 *
 * Note: Google Services and Firebase Crashlytics plugins are declared
 * but not applied at root level. They will be applied in app/build.gradle.kts
 * once google-services.json is added.
 */
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    // Firebase plugins - uncomment when google-services.json is added
    // alias(libs.plugins.google.services) apply false
    // alias(libs.plugins.firebase.crashlytics) apply false
}
