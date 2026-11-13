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
        // Kakao SDK is distributed only from Kakao's repository; restrict it to Kakao artifacts.
        maven("https://devrepo.kakao.com/nexus/content/groups/public/") {
            content { includeGroupByRegex("com\\.kakao\\..*") }
        }
        maven("https://devrepo.kakao.com/nexus/repository/kakaomap-releases/") {
            content { includeGroup("com.kakao.maps.open") }
        }
    }
}

rootProject.name = "KkokKkok"
include(":app")
