import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import com.android.build.gradle.BaseExtension

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("com.github.recloudstream:gradle:-SNAPSHOT")
        // AGGIORNATO: Passiamo a Kotlin 2.0.0
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

fun Project.cloudstream(configuration: CloudstreamExtension.() -> Unit) = 
    (extensions.getByName("cloudstream") as CloudstreamExtension).configuration()

fun Project.android(configuration: BaseExtension.() -> Unit) = 
    (extensions.getByName("android") as BaseExtension).configuration()

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    cloudstream {
        // Assicurati che questi dati siano corretti
        setRepo("elia1986", "SXXX", "github")
    }

    android {
        namespace = "com.Chatrubate"
        compileSdkVersion(34)

        defaultConfig {
            minSdk = 21
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        
        // AGGIUNTO: Necessario per Kotlin 2.0+
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    dependencies {
        val implementation by configurations
        val cloudstream by configurations
        
        cloudstream("com.lagradost:cloudstream3:pre-release")
        
        implementation("com.github.Blatzar:NiceHttp:0.4.11")
        implementation("org.jsoup:jsoup:1.15.3")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    }
}

tasks.register("make") {
    dependsOn("makePluginsJson")
}
