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
