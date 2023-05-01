import java.util.Properties
import org.gradle.jvm.tasks.Jar

plugins {
    id("com.android.library")
    id("maven-publish")
    id("com.palantir.git-version") version "3.0.0"
}

val localProperties = Properties().apply {
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

android {
    namespace = "com.eidu.zip"
    compileSdk = 32
    defaultConfig {
        minSdk = 21
        targetSdk = 32
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {}
        }
    }
    externalNativeBuild {
        cmake {
            path = project.projectDir.resolve("CMakeLists.txt")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs", "stc/main/jni")
        }
    }
}

dependencies {
    api("com.eidu:archive-patcher:2.0.1")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}

configure<SourceSetContainer> {
    create("main") {
        java.srcDir("src/main/java")
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val gitVersion: groovy.lang.Closure<String> by extra

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/EIDU/archive-patcher-android")
                credentials {
                    username = System.getenv("GITHUB_USER") ?: localProperties.getProperty("githubUser")
                    password = System.getenv("GITHUB_TOKEN") ?: localProperties.getProperty("githubToken")
                }
            }
        }
        publications {
            create<MavenPublication>("maven") {
                groupId = "com.eidu"
                artifactId = "archive-patcher-android"
                version = gitVersion()

                from(components["release"])
                artifact(sourcesJar)
            }
        }
    }
}
