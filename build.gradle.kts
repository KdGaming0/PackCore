import org.apache.commons.lang3.SystemUtils

plugins {
    idea
    java
    kotlin("jvm") version "2.0.21"
    // Loom for Forge mods
    id("gg.essential.loom") version "0.10.0.+"
    // Pack200 for Forge dependencies
    id("dev.architectury.architectury-pack200") version "0.1.3"
    // Shadow for relocating (important for Forge + Elementa)
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

// -------------------------------------------------------------------------------------------------
// Constants
// -------------------------------------------------------------------------------------------------
val baseGroup: String by project
val mcVersion: String by project
val version: String by project
val modid: String by project

// You can adjust these to the proper versions you want to use.
val elementaVersion = "2.0.0"            // Replace with latest Elementa version if desired
val ucVersion = "1.0.3"                 // Replace with latest UniversalCraft version if desired

val mixinGroup = "$baseGroup.mixin"
val transformerFile = file("src/main/resources/accesstransformer.cfg")

// -------------------------------------------------------------------------------------------------
// Toolchains
// -------------------------------------------------------------------------------------------------
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

// -------------------------------------------------------------------------------------------------
// Minecraft configuration (Forge via Loom)
// -------------------------------------------------------------------------------------------------
loom {
    log4jConfigs.from(file("log4j2.xml"))
    launchConfigs {
        "client" {
            property("mixin.debug", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
            arg("--tweakClass", "com.github.kdgaming0.packcore.tweaker.TestTweaker")
        }
    }
    runConfigs {
        "client" {
            if (SystemUtils.IS_OS_MAC_OSX) {
                // This argument causes a crash on macOS
                vmArgs.remove("-XstartOnFirstThread")
            }
        }
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        mixinConfig("mixins.$modid.json")
        if (transformerFile.exists()) {
            println("Installing access transformer")
            accessTransformer(transformerFile)
        }
    }
    mixin {
        defaultRefmapName.set("mixins.$modid.refmap.json")
    }
}

sourceSets.main {
    // Ensures resources go into the same place as compiled classes
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
}

// -------------------------------------------------------------------------------------------------
// Repositories
// -------------------------------------------------------------------------------------------------
repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    // DevAuth repository (if you need it for debugging/logging in with alt)
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    // Essential's public Maven for Elementa
    maven(url = "https://repo.essential.gg/repository/maven-public")
}

// -------------------------------------------------------------------------------------------------
// Configurations
// -------------------------------------------------------------------------------------------------
val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

// -------------------------------------------------------------------------------------------------
// Dependencies
// -------------------------------------------------------------------------------------------------
dependencies {
    // Minecraft + Forge version
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    // Sponge Mixin library (if you're using mixins)
    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")

    // Add DevAuth for debugging if desired
    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.2.1")

    // ---------------------------------------------------------------------------------------------
    // Elementa + UniversalCraft for 1.8.9 Forge
    // ---------------------------------------------------------------------------------------------
    // The core Elementa library
    shadowImpl("gg.essential:elementa:676")

    // UniversalCraft for 1.8.9 Forge
    // Use 'modImplementation' if you need Loom integration; otherwise 'implementation' is typically fine.
    // With Forge, you'll need to relocate both Elementa and UniversalCraft inside your final jar.
    shadowImpl("gg.essential:universalcraft-1.8.9-forge:369")
}

// -------------------------------------------------------------------------------------------------
// Task Configurations
// -------------------------------------------------------------------------------------------------
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<org.gradle.jvm.tasks.Jar> {
    archiveBaseName.set(modid)
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"
        // If you don't want mixins, remove these lines:
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["MixinConfigs"] = "mixins.$modid.json"
        if (transformerFile.exists()) {
            this["FMLAT"] = "${modid}_at.cfg"
        }
    }
}

// Process resources (to replace placeholders in mcmod.info, etc.)
tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mcversion", mcVersion)
    inputs.property("modid", modid)
    inputs.property("basePackage", baseGroup)

    filesMatching(listOf("mcmod.info", "mixins.$modid.json")) {
        expand(inputs.properties)
    }

    // Access transformer file rename
    rename("accesstransformer.cfg", "META-INF/${modid}_at.cfg")
}

// -------------------------------------------------------------------------------------------------
// Remap + Shadow Jar
// -------------------------------------------------------------------------------------------------
val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    from(tasks.shadowJar)
    input.set(tasks.shadowJar.get().archiveFile)
}

tasks.jar {
    archiveClassifier.set("without-deps")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
}

tasks.shadowJar {
    // This is critical for Forge + Elementa to avoid conflicts.
    // Make sure you relocate both Elementa and UniversalCraft into your own package.
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    archiveClassifier.set("non-obfuscated-with-deps")
    configurations = listOf(shadowImpl)

    // Relocate Elementa + UniversalCraft to avoid conflicts with other mods
    relocate("gg.essential.elementa", "${baseGroup}.deps.elementa")
    relocate("gg.essential.universalcraft", "${baseGroup}.deps.universalcraft")

    doLast {
        configurations.forEach {
            println("Copying dependencies into mod: ${it.files}")
        }
    }
}

tasks.assemble.get().dependsOn(tasks.remapJar)