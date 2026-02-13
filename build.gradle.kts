plugins {
    id("fabric-loom")
    id("me.modmuss50.mod-publish-plugin")
}

version = "${property("mod.version")}+${stonecutter.current.version}"
base.archivesName = property("mod.id") as String

repositories {
    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.wispforest.io/releases/")
    maven("https://jitpack.io")
    maven("https://maven.midnightdust.eu/releases")
    maven("https://repo.hypixel.net/repository/Hypixel/")
    exclusiveContent {
        forRepository { maven("https://maven.azureaaron.net/releases") }
        filter { includeGroup("net.azureaaron") }
    }
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    mavenLocal()
}

dependencies {
    minecraft("com.mojang:minecraft:${stonecutter.current.version}")
    mappings("net.fabricmc:yarn:${property("deps.yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    modImplementation("eu.midnightdust:midnightlib:${property("deps.midnightlib_version")}")
    include("eu.midnightdust:midnightlib:${property("deps.midnightlib_version")}")

    modImplementation("com.terraformersmc:modmenu:${property("deps.modmenu_version")}")
    modRuntimeOnly("com.terraformersmc:modmenu:${property("deps.modmenu_version")}")
    modImplementation("io.wispforest:owo-lib:${property("deps.owo_version")}")
    modImplementation("io.wispforest.lavender-md:core:${property("deps.lavender_md_version")}")
    include("io.wispforest.lavender-md:core:${property("deps.lavender_md_version")}")
    modImplementation("io.wispforest.lavender-md:owo-ui:${property("deps.lavender_md_version")}")
    include("io.wispforest.lavender-md:owo-ui:${property("deps.lavender_md_version")}")
    modImplementation("net.azureaaron:hm-api:1.0.1+1.21.2")
    include("net.azureaaron:hm-api:1.0.1+1.21.2")

    modImplementation("maven.modrinth:sodium:${property("deps.sodium_version")}")
    modImplementation("maven.modrinth:iris:${property("deps.iris_version")}")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.1")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
}

// Add this mixin configuration block
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
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft", project.property("mod.mc_dep"))
        inputs.property("owo_version", project.property("deps.owo_version"))
        inputs.property("iris_version", project.property("deps.iris_version"))
        inputs.property("fabric_loader", project.property("deps.fabric_loader"))
        inputs.property("sodium_version", project.property("deps.sodium_version"))
        inputs.property("modmenu_version", project.property("deps.modmenu_version"))
        inputs.property("midnightlib_version", project.property("deps.midnightlib_version"))
        inputs.property("lavender_md_version", project.property("deps.lavender_md_version"))

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep"),
            "owo_version" to project.property("deps.owo_version"),
            "iris_version" to project.property("deps.iris_version"),
            "fabric_loader" to project.property("deps.fabric_loader"),
            "sodium_version" to project.property("deps.sodium_version"),
            "modmenu_version" to project.property("deps.modmenu_version"),
            "lavender_md_version" to project.property("deps.lavender_md_version"),
            "midnightlib_version" to project.property("deps.midnightlib_version")
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

stonecutter {
    replacements.string(current.parsed >= "1.21.11") {
        replace("ParentComponent", "ParentUIComponent")
    }

    replacements.regex(current.parsed >= "1.21.11") {
        replace("\\bComponent\\b" to "UIComponent", "\\bUIComponent\\b" to "Component")
        replace("\\bComponents\\b" to "UIComponents", "\\bUIComponents\\b" to "Components")
        replace("\\bContainers\\b" to "UIContainers", "\\bUIContainers\\b" to "Containers")
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
            slug = "ccKDOlHs" // OwO Lib
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
            slug = "owo-lib"
        }
        optional {
            slug = "modmenu"
        }
    }
}