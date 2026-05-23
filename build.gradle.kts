plugins {
    // This plugin applies the correct loom variant based on the Minecraft version
    id("dev.kikugie.loom-back-compat")
    `maven-publish`
    id("me.modmuss50.mod-publish-plugin")
}

// DO NOT set group = ...!
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

// This can be used for publishing on Modrinth and Curseforge
val compatibleVersions: List<String> =
    sc.properties.rawOrNull("mod", "mc_releases")?.asList().orEmpty().map { it.toString() }

repositories {
    mavenCentral()
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    strictMaven("https://maven.daqem.com/releases", "DAQEM Studios", "com.daqem", "com.daqem.uilib")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    exclusiveContent {
        forRepository { maven("https://maven.azureaaron.net/releases") }
        filter { includeGroup("net.azureaaron") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${stonecutter.current.version}")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    modImplementation("maven.modrinth:midnightlib:${property("deps.midnightlib_version")}")
    include("maven.modrinth:midnightlib:${property("deps.midnightlib_version")}")

    modImplementation("com.daqem.uilib:uilib-fabric:${property("deps.uilib_version")}")

    modImplementation("maven.modrinth:modmenu:${property("deps.modmenu_version")}")
    modCompileOnly("maven.modrinth:scamscreener:${property("deps.scamscreener_version")}")
    modCompileOnly("maven.modrinth:scaleme:${property("deps.scaleme_version")}")
    modCompileOnly("maven.modrinth:moreculling:${property("deps.moreculling_version")}")

    modImplementation("net.azureaaron:hm-api:${property("deps.hm_api_version")}")
    include("net.azureaaron:hm-api:${property("deps.hm_api_version")}")

    modImplementation("maven.modrinth:sodium:${property("deps.sodium_version")}")
    modImplementation("maven.modrinth:iris:${property("deps.iris_version")}")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")
    modRuntimeOnly("maven.modrinth:modmenu:${property("deps.modmenu_version")}")

    modRuntimeOnly("maven.modrinth:scamscreener:${property("deps.scamscreener_version")}")
}

loom {
    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDir = "../../run" // Shares the run directory between versions
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        inputs.property("id", project.property("mod.id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft", project.property("mod.mc_dep"))
        inputs.property("ui_lib", project.property("deps.uilib_version"))
        inputs.property("hm_api", project.property("deps.hm_api_version"))
        inputs.property("fabric_api", project.property("deps.fabric_api"))
        inputs.property("iris_version", project.property("deps.iris_version"))
        inputs.property("fabricloader", project.property("deps.fabric_loader"))
        inputs.property("uilib_version", project.property("deps.uilib_version"))
        inputs.property("sodium_version", project.property("deps.sodium_version"))
        inputs.property("modmenu_version", project.property("deps.modmenu_version"))

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep"),
            "ui_lib" to project.property("deps.uilib_version"),
            "hm_api" to project.property("deps.hm_api_version"),
            "fabric_api" to project.property("deps.fabric_api"),
            "iris_version" to project.property("deps.iris_version"),
            "fabricloader" to project.property("deps.fabric_loader"),
            "uilib_version" to project.property("deps.uilib_version"),
            "sodium_version" to project.property("deps.sodium_version"),
            "modmenu_version" to project.property("deps.modmenu_version")
        )

        filesMatching("fabric.mod.json") {
            expand(props)
        }
    }

    jar {
        from("LICENSE") {
            rename { fileName -> "${fileName}_${project.property("mod.id")}" }
        }
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/
    register<Copy>("buildAndCollect") {
        group = "build"
        // loomx.mod(Sources)Jar returns the jar task for the applied loom variant
        from(loomx.modJar.map { it.archiveFile }, loomx.modSourcesJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

publishMods {
    file = loomx.modJar.flatMap { it.archiveFile }
    additionalFiles.from(loomx.modSourcesJar.flatMap { it.archiveFile })
    displayName = "${property("mod.name")} ${property("mod.version")} for ${sc.current.version}"
    version = property("mod.version") as String
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add("fabric")

    dryRun = providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null
            || providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        compatibleVersions.forEach { minecraftVersions.add(it) }
        requires {
            slug = "P7dR8mSH" // Fabric API
        }
        requires {
            slug = "AOEDs9Al" // UI Lib
        }
        optional {
            slug = "mOgUt4GM" // ModMenu
        }
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        compatibleVersions.forEach { minecraftVersions.add(it) }
        requires {
            slug = "fabric-api"
        }
        requires {
            slug = "ui"
        }
        optional {
            slug = "modmenu"
        }
    }
}
