import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")                  version "1.9.23"
    id("org.jetbrains.compose")    version "1.6.2"
    kotlin("plugin.serialization") version "1.9.23"
}

group   = "com.pokemonarena"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

val ktorVersion       = "2.3.9"
val koinVersion       = "3.5.3"
val exposedVersion    = "0.48.0"
val coroutinesVersion = "1.8.0"
val mockkVersion      = "1.13.10"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutinesVersion")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-compose:1.1.2")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:3.45.2.0")

    implementation("media.kamel:kamel-image:0.9.4")

    implementation("org.slf4j:slf4j-simple:2.0.12")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("app.cash.turbine:turbine:1.1.0")
}

compose.desktop {
    application {
        mainClass = "com.pokemonarena.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi, TargetFormat.Deb)
            packageName        = "PokemonArena"
            packageVersion     = "1.0.0"
            description        = "PokéArena Climática — Proyecto 2"
            vendor             = "UNS"
            windows {
                menuGroup      = "PokéArena"
                upgradeUuid    = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
        }
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.test { useJUnitPlatform() }
