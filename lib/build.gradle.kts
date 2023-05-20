import java.util.Properties
import org.gradle.jvm.tasks.Jar

plugins {
    id("com.android.library")
    id("maven-publish")
    id("signing")
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
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

signing {
    useInMemoryPgpKeys(
        System.getenv("MAVEN_CENTRAL_KEY_ID"),
        System.getenv("MAVEN_CENTRAL_KEY"),
        System.getenv("MAVEN_CENTRAL_KEY_PASSWORD")
    )
    sign(publishing.publications)
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.2.2")

    api("com.eidu:archive-patcher:3.0.0")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}

configure<SourceSetContainer> {
    create("main") {
        java.srcDir("src/main/java")
    }
}

tasks.register<Javadoc>("javadoc") {
    val variant = android.libraryVariants.first { it.name == "release" }
    description = "Generates Javadoc for ${variant.name}."
    source = fileTree(variant.sourceSets.first { it.name == "main" }.javaDirectories.first())
    classpath = files(variant.javaCompile.classpath.files) +
            files("${android.sdkDirectory}/platforms/${android.compileSdkVersion}/android.jar")
    (options as StandardJavadocDocletOptions).apply {
        source = "8" // workaround for https://bugs.openjdk.java.net/browse/JDK-8212233
        links(
            "https://docs.oracle.com/javase/7/docs/api/",
            "https://d.android.com/reference/"
        )
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named<Javadoc>("javadoc"))
}

val gitVersion: groovy.lang.Closure<String> by extra

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "MavenCentral"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = System.getenv("MAVEN_CENTRAL_USERNAME")
                    password = System.getenv("MAVEN_CENTRAL_PASSWORD")
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
                artifact(javadocJar)

                pom {
                    name.value("archive-patcher-android")
                    description.value("Android-compatible wrapper for archive-patcher")
                    url.value("https://github.com/EIDU/archive-patcher-android")
                    licenses {
                        license {
                            name.value("GNU Public License 2.0")
                            url.value("https://raw.githubusercontent.com/EIDU/archive-patcher-android/main/LICENSE")
                        }
                    }
                    developers {
                        developer {
                            id.value("berlix")
                            name.value("Felix Engelhardt")
                            url.value("https://github.com/berlix/")
                        }
                    }
                    scm {
                        url.value("https://github.com/EIDU/archive-patcher-android")
                        connection.value("scm:git:git://github.com/EIDU/archive-patcher-android.git")
                        developerConnection.value("scm:git:ssh://git@github.com/EIDU/archive-patcher-android.git")
                    }
                }
            }
        }
    }
}
