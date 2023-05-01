// Top-level build file where you can add configuration options common to all sub-projects/modules.

val localProperties = java.util.Properties().apply {
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.3.1")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/EIDU/archive-patcher-android")
            credentials {
                username =
                    System.getenv("GITHUB_USER")
                                ?: localProperties.getProperty("githubReadPackagesUser")
                                ?: throw NoSuchFieldException(
                            "Missing property: ${"githubReadPackagesUser"}",
                        )
                password =
                    System.getenv("GITHUB_TOKEN")
                                ?: localProperties.getProperty("githubReadPackagesToken")
                                ?: throw NoSuchFieldException(
                            "Missing property: ${"githubReadPackagesToken"}",
                        )
            }
        }
    }
}
