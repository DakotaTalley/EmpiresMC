plugins {
    id("net.fabricmc.fabric-loom")
    kotlin("jvm")
    `maven-publish`
}

version = property("mod_version") as String
group = property("maven_group") as String

repositories {
    // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    // Fabric API. Technically optional, but the project depends on it from phase 1 onward.
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

    // Kotlin entrypoint support, bundles the Kotlin stdlib and kotlinx libraries as a mod dependency.
    implementation("net.fabricmc:fabric-language-kotlin:${property("fabric_language_kotlin_version")}")

    // Lets plain JUnit tests run mixin-modified classes and bootstrap Minecraft's registries (fabric DEV-009).
    testImplementation("net.fabricmc:fabric-loader-junit:${property("loader_version")}")
}

tasks.test {
    useJUnitPlatform()
}

// Every Kotlin source must carry the MPL-2.0 Exhibit A notice. Unlike LGPL/MIT — where the root
// LICENSE covers the whole tree — MPL-2.0 §1.4 defines Covered Software by the notice attached to
// each file, so a source file without the header is arguably not covered by the project license at
// all. Mozilla's directory-level fallback (FAQ Q22) only applies where an in-file notice is
// "impossible or impractical", which is true of the Phase 9 art but not of a .kt file.
//
// This is enforced rather than documented because the failure is silent: a source missing the
// header compiles, passes every test, and ships in the jar. The gap would surface only when
// someone forks — the one moment it matters. Wired into `check`, so `./gradlew build` and CI both
// catch it with no workflow change.
val licenseHeaderSources = fileTree("src") { include("**/*.kt") }
val licenseHeaderMarker = layout.buildDirectory.file("checks/license-headers.txt")
val licenseHeaderRoot = projectDir

val checkLicenseHeaders = tasks.register("checkLicenseHeaders") {
    group = "verification"
    description = "Fails if any Kotlin source is missing the MPL-2.0 Exhibit A header."

    inputs.files(licenseHeaderSources)
        .withPropertyName("sources")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(licenseHeaderMarker)

    doLast {
        // Match on the distinctive first line only — the URL and wrapping vary between comment
        // styles, and matching the whole block would reject a valid but reflowed notice.
        val notice = "This Source Code Form is subject to the terms of the Mozilla Public"
        val missing = licenseHeaderSources.files
            .filter { file -> file.useLines { lines -> lines.take(10).none { notice in it } } }
            .map { it.toRelativeString(licenseHeaderRoot) }
            .sorted()

        if (missing.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("${missing.size} Kotlin source(s) missing the MPL-2.0 Exhibit A header:")
                    missing.forEach { appendLine("  $it") }
                    appendLine()
                    appendLine("MPL-2.0 §1.4 defines Covered Software by the notice attached to each file,")
                    appendLine("so a source without this header may not be covered by the project license.")
                    appendLine("Add to the top of each file, above the package declaration:")
                    appendLine()
                    appendLine("/*")
                    appendLine(" * This Source Code Form is subject to the terms of the Mozilla Public")
                    appendLine(" * License, v. 2.0. If a copy of the MPL was not distributed with this")
                    appendLine(" * file, You can obtain one at https://mozilla.org/MPL/2.0/.")
                    appendLine(" */")
                },
            )
        }

        licenseHeaderMarker.get().asFile.apply {
            parentFile.mkdirs()
            writeText("${licenseHeaderSources.files.size} sources checked\n")
        }
    }
}

tasks.check {
    dependsOn(checkLicenseHeaders)
}

fabricApi {
    configureTests {
        // Separate source set + mod id from `main` so gametest code never ships in the mod jar.
        createSourceSet = true
        modId = "empiresmc-test"
        enableGameTests = true
        enableClientGameTests = false
        // Agrees to the Minecraft EULA (https://aka.ms/MinecraftEULA) so Loom can spin up a real
        // dedicated server for the gametest run — required boilerplate for this feature to run at all.
        eula = true
    }
}

tasks.processResources {
    val version = project.version
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

java {
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present. If you remove this line, sources will not be generated.
    withSourcesJar()
}

tasks.jar {
    val projectName = project.name
    inputs.property("projectName", projectName)

    from("LICENSE") {
        rename { "${it}_${projectName}" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {
        // Add repositories to publish to here.
    }
}
