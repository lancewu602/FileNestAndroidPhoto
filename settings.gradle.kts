pluginManagement {
    repositories {
        // 用于加载 Gradle 插件的仓库
        google()
        mavenCentral()
        gradlePluginPortal() // Gradle 插件门户
        maven { url = uri("https://jitpack.io") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // 禁止项目级单独配置仓库
    repositories {
        // 所有模块共享的依赖仓库
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "FileNestAndroidPhoto"
include(":app")
