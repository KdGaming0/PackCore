plugins {
    id("fabric-loom")
    id("me.modmuss50.mod-publish-plugin")
}

version = "${property("mod.version")}+${stonecutter.current.version}"
base.archivesName = property("mod.id") as String

repositories {
    mavenCentral()
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    exclusiveContent {
        forRepository { maven("https://maven.azureaaron.net/releases") }
        filter { includeGroup("net.azureaaron") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${stonecutter.current.version}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    modImplementation("maven.modrinth:midnightlib:${property("deps.midnightlib_version")}")
    include("maven.modrinth:midnightlib:${property("deps.midnightlib_version")}")

    modImplementation("maven.modrinth:ui-lib:${property("deps.uilib_version")}")

    modImplementation("net.azureaaron:hm-api:${property("deps.hm_api_version")}")
    include("net.azureaaron:hm-api:${property("deps.hm_api_version")}")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")
    modRuntimeOnly("maven.modrinth:modmenu:${property("deps.modmenu_version")}")
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
    val java = JavaVersion.VERSION_21
    targetCompatibility = java
    sourceCompatibility = java
}

tasks {
    processResources {
        inputs.property("id", project.property("mod.id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("hm_api", project.property("deps.hm_api"))
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft", project.property("mod.mc_dep"))
        inputs.property("fabric_api", project.property("deps.fabric_api"))
        inputs.property("fabricloader", project.property("deps.fabric_loader"))

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "hm_api" to project.property("deps.hm_api"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep"),
            "fabric_api" to project.property("deps.fabric_api"),
            "fabricloader" to project.property("deps.fabric_loader")
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

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        from(
            remapJar.map { it.archiveFile },
            remapSourcesJar.map { it.archiveFile }
        )
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

publishMods {
    file = tasks.remapJar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.remapSourcesJar.map { it.archiveFile.get() })
    displayName = "${property("mod.name")} ${property("mod.version")} for ${stonecutter.current.version}"
    version = property("mod.version") as String
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add("fabric")

    dryRun = providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null
            || providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.add(stonecutter.current.version)
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
        minecraftVersions.add(stonecutter.current.version)
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