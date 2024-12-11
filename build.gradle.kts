import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    application
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.shadow)
    alias(libs.plugins.ksp)
    alias(libs.plugins.openapi.generator)
}

group = "me.santio"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
//   Adventure API
    implementation(libs.bundles.adventure.api)

//   JDA
    implementation(libs.jda)
    implementation(libs.jda.kts)

//   Client
    implementation(libs.gson)
    implementation(libs.bundles.ktor.client)

//    Auto Service
    ksp(libs.autoservice.ksp)
    implementation(libs.autoservice.google)

//   Database
    implementation(libs.flyway.core)
    implementation(libs.sqlite)
    implementation(libs.iron)
    ksp(libs.iron)

//   Environment
    implementation(libs.dotenv)
    implementation(libs.bundles.log4j)
}

kotlin {
    jvmToolchain(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

sourceSets {
    main {
        kotlin.srcDir("${project.layout.buildDirectory.get()}/generated/src/main/kotlin")
        resources.srcDir("${project.layout.buildDirectory.get()}/generated/src/main/resources")
    }
}

tasks.withType<GenerateTask> {
    generatorName.set("kotlin")
    remoteInputSpec.set("https://minehut.santio.me/api.yaml")
    outputDir.set("${project.layout.buildDirectory.get()}/generated")
    cleanupOutput.set(true)
    additionalProperties.set(
        mapOf(
            "apiSuffix" to "",
            "artifactId" to "minehut-api",
            "groupId" to "me.santio.sdk.minehut",
            "idea" to "true",
            "library" to "jvm-ktor",
            "serializationLibrary" to "gson",
            "packageName" to "me.santio.sdk.minehut"
        )
    )
    openapiNormalizer.set(
        mapOf(
            "SET_TAGS_FOR_ALL_OPERATIONS" to "minehut"
        )
    )

    dependsOn("clean")
}

tasks.register("createMigration") {
    doLast {

        val timestamp = System.currentTimeMillis() / 1000
        val path = "${timestamp}_.sql"

        val migrationDir = File(project.projectDir.toString(), "src/main/resources/db/migration")
        if (!migrationDir.exists()) {
            migrationDir.mkdirs()
        }

        val migrationFile = File(migrationDir, path)
        migrationFile.writeText(
            """
         |-- Create your migration here
         |
         |CREATE TABLE IF NOT EXISTS example (
         |   id INTEGER PRIMARY KEY,
         |   name TEXT
         |);
         |
         |-- Seeding
         |INSERT INTO example (id, name) VALUES (1, 'Hello');
         """.trimMargin()
        )
    }
}

application {
    mainClass.set("me.santio.minehututils.MinehutUtilsKt")
}
