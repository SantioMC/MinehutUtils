plugins {
    kotlin("jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    application
}

group = "me.santio"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
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

//    Generic Dependencies
    implementation("com.konghq:unirest-java-core:4.0.12")
    implementation("com.konghq:unirest-object-mappers-gson:4.0.12")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.github.SantioMC.Coffee:jda:a8fb1430ce")

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