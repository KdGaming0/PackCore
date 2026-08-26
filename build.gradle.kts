plugins {
    id("dev.kikugie.loom-back-compat")
    id("me.modmuss50.mod-publish-plugin")
}

// DO NOT set group = ...!
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String
val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

// This can be used for publishing on Modrinth and Curseforge
val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }
repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    maven {
        url = uri("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    }
    exclusiveContent {
        forRepository {
            maven {
                url = uri("https://maven.azureaaron.net/releases")
            }
        }
        filter {
            includeGroup("net.azureaaron")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    implementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    implementation("maven.modrinth:midnightlib:${property("deps.midnightlib_version")}")

    include("maven.modrinth:midnightlib:${property("deps.midnightlib_version")}")
    implementation("net.azureaaron:hm-api:${property("deps.hm_api_version")}")

    include("net.azureaaron:hm-api:${property("deps.hm_api_version")}")
    implementation("maven.modrinth:ui-lib:${property("deps.uilib_version")}")

    // Optional integrations: needed at compile time; the mod degrades gracefully when they are absent.
    modCompileOnly("maven.modrinth:sodium:${property("deps.sodium_version")}")
    modCompileOnly("maven.modrinth:iris:${property("deps.iris_version")}")
    modCompileOnly("maven.modrinth:moreculling:${property("deps.moreculling_version")}")
    modCompileOnly("maven.modrinth:scamscreener:${property("deps.scamscreener_version")}")
    modCompileOnly("maven.modrinth:modmenu:${property("deps.modmenu_version")}")
    modCompileOnly("maven.modrinth:bobby:${property("deps.bobby_version")}")

    // Bobby bundles Configurate inside its jar, so Modrinth Maven does not expose it transitively.
    compileOnly("org.spongepowered:configurate-core:4.1.2")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")
    modRuntimeOnly("maven.modrinth:modmenu:${property("deps.modmenu_version")}")

    include(implementation(annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.5.3")!!)!!)
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }
    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDir = "../../run" // Shares the run directory between versions
    }
    log4jConfigs.from(file("log4j-dev.xml"))
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
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }
        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("minecraft", "mod.mc_compat")
            register("uilib_version", "deps.uilib_version")
            register("fabric_api", "deps.fabric_api")
            register("hm_api", "deps.hm_api_version")
            register("fabricloader", "deps.fabric_loader")
        }
        filesMatching("fabric.mod.json") { expand(props) }
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        from(loomx.modJar.map { it.archiveFile }, loomx.modSourcesJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
    test {
        useJUnitPlatform()
    }
}

if (sc.current.version in compatibleVersions) {
    val changelogFile = rootProject.file("CHANGELOG.md")
    val publishChangelog = if (changelogFile.exists()) changelogFile.readText() else "No changelog provided."
    publishMods {
        file.set(loomx.modJar.flatMap { it.archiveFile })
        additionalFiles.from(loomx.modSourcesJar.flatMap { it.archiveFile })
        displayName.set("${property("mod.name")} v${property("mod.version")} for mc${sc.current.version}")
        version.set("v${property("mod.version")}-mc${sc.current.version}")
        changelog.set(publishChangelog)
        type.set(STABLE)
        modLoaders.add("fabric")
        dryRun.set(
            providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null
                    || providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null
        )
        val modrinthId = providers.gradleProperty("publish.modrinth").orNull
        if (!modrinthId.isNullOrEmpty()) {
            modrinth {
                projectId.set(modrinthId)
                accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
                minecraftVersions.addAll(compatibleVersions)
                requires { slug = "P7dR8mSH" }
                requires { slug = "AOEDs9Al" } // UI Lib
                optional { slug = "mOgUt4GM" } // ModMenu
            }
        }
        val curseforgeId = providers.gradleProperty("publish.curseforge").orNull
        if (!curseforgeId.isNullOrEmpty()) {
            curseforge {
                projectId.set(curseforgeId)
                accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))
                minecraftVersions.addAll(compatibleVersions)
                client.set(true)
                requires { slug = "fabric-api" }
                requires { slug = "ui" }
                optional { slug = "modmenu" }
            }
        }
    }
}
