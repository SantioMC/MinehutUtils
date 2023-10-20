plugins {
    application
    kotlin("jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("app.cash.sqldelight") version "2.0.0"
}

group = "me.santio"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

sqldelight {
    databases {
        create("Minehut") {
            packageName.set("me.santio.minehututils.db")
        }
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation(kotlin("stdlib-jdk8"))

//    JDA
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation("net.dv8tion:JDA:5.0.0-beta.15") {
        exclude("opus-java")
    }

//    Unirest
    implementation("com.konghq:unirest-java-core:4.0.12")
    implementation("com.konghq:unirest-object-mappers-gson:4.0.12")
    implementation("com.google.code.gson:gson:2.10.1")

//    Libraries
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.github.SantioMC.Coffee:jda:626e4be055")
    implementation("app.cash.sqldelight:sqlite-driver:2.1.0-SNAPSHOT")

//    Adventure API
    implementation("net.kyori:adventure-text-minimessage:4.14.0")
    implementation("net.kyori:adventure-text-serializer-plain:4.14.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.14.0")
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("me.santio.minehututils.MinehutUtilsKt")
}