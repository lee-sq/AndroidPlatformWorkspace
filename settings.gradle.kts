pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "AndroidPlatformWorkspace"
include(":foundation-core")

include(":device-api-base")
project(":device-api-base").projectDir = file("device/api/base")

include(":device-api-scale")
project(":device-api-scale").projectDir = file("device/api/scale")

include(":device-api-cabinet")
project(":device-api-cabinet").projectDir = file("device/api/cabinet")

include(":device-core")
project(":device-core").projectDir = file("device/core")

include(":device-driver-scale")
project(":device-driver-scale").projectDir = file("device/driver/scale")

include(":device-driver-cabinet")
project(":device-driver-cabinet").projectDir = file("device/driver/cabinet")

include(":device-starter-scale")
project(":device-starter-scale").projectDir = file("device/starter/scale")

include(":device-starter-cabinet")
project(":device-starter-cabinet").projectDir = file("device/starter/cabinet")

include(":device-test-app")
project(":device-test-app").projectDir = file("device/test-app")

include(":device-cabinet-test-app")
project(":device-cabinet-test-app").projectDir = file("device/cabinet-test-app")
