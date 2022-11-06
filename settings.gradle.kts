rootProject.name = "telegranloader"

pluginManagement {
    plugins {
    }
    resolutionStrategy {
    }
    repositories {
        maven(url = "./maven-repo")
        gradlePluginPortal()
        maven {
            url = uri("https://repo.spring.io/release")
        }
        maven {
            url = uri("https://repo.spring.io/milestone")
        }
    }
}