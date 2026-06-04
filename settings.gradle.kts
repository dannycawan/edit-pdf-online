/**
 * Purpose: Project settings for Edit PDF Online - Text Editor
 * Caller: Gradle build system
 * Dependencies: None
 * Main Functions: Repository configuration, module inclusion
 * Side Effects: Configures dependency resolution for entire project
 */
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "EditPdfOnline"
include(":app")
