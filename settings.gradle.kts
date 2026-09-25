pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "DJVideoMixerPro"
include(":app")
include(":core-audio")
include(":core-dsp")
include(":core-stems")
include(":core-video")
include(":core-db")
include(":native")
