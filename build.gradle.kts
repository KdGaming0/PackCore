import dev.deftu.gradle.utils.version.MinecraftVersions

plugins {
    java
    id("dev.deftu.gradle.multiversion")
    id("dev.deftu.gradle.tools")
    id("dev.deftu.gradle.tools.resources")
    id("dev.deftu.gradle.tools.bloom")
    id("dev.deftu.gradle.tools.shadow")
    id("dev.deftu.gradle.tools.minecraft.loom")
    id("dev.deftu.gradle.tools.minecraft.releases")
}

repositories {
    maven("https://maven.wispforest.io/releases/")
    maven("https://api.modrinth.com/maven")
}

toolkitMultiversion {
    moveBuildsToRootProject.set(true)
}

val mcVersion = mcData.version

val midnightlibVersion = when (mcVersion) {
    MinecraftVersions.VERSION_1_21_8 -> project.properties["midnightlib_version_1_21_8"]
    MinecraftVersions.VERSION_1_21_5 -> project.properties["midnightlib_version_1_21_5"]
    else -> project.properties["midnightlib_version_1_21_5"]
}
val modmenuVersion = when (mcVersion) {
    MinecraftVersions.VERSION_1_21_8 -> project.properties["modmenu_version_1_21_8"]
    MinecraftVersions.VERSION_1_21_5 -> project.properties["modmenu_version_1_21_5"]
    else -> project.properties["modmenu_version_1_21_5"]
}
val owoVersion = when (mcVersion) {
    MinecraftVersions.VERSION_1_21_8 -> project.properties["owo_version_1_21_8"]
    MinecraftVersions.VERSION_1_21_5 -> project.properties["owo_version_1_21_5"]
    else -> project.properties["owo_version_1_21_5"]
}
val lavenderMdVersion = when (mcVersion) {
    MinecraftVersions.VERSION_1_21_8 -> project.properties["lavender_md_version_1_21_8"]
    MinecraftVersions.VERSION_1_21_5 -> project.properties["lavender_md_version_1_21_5"]
    else -> project.properties["lavender_md_version_1_21_5"]
}
val sodiumVersion = when (mcVersion) {
    MinecraftVersions.VERSION_1_21_8 -> project.properties["sodium_version_1_21_8"]
    MinecraftVersions.VERSION_1_21_5 -> project.properties["sodium_version_1_21_5"]
    else -> project.properties["sodium_version_1_21_5"]
}
val irisVersion = when (mcVersion) {
    MinecraftVersions.VERSION_1_21_8 -> project.properties["iris_version_1_21_8"]
    MinecraftVersions.VERSION_1_21_5 -> project.properties["iris_version_1_21_5"]
    else -> project.properties["iris_version_1_21_5"]
}

dependencies {
    modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")

    modImplementation("io.wispforest:owo-lib:$owoVersion")
    modImplementation(include("maven.modrinth:midnightlib:$midnightlibVersion")!!)
    modImplementation(include("io.wispforest.lavender-md:core:$lavenderMdVersion")!!)
    modImplementation(include("io.wispforest.lavender-md:owo-ui:$lavenderMdVersion")!!)

    modCompileOnly("maven.modrinth:sodium:$sodiumVersion")
    modCompileOnly("maven.modrinth:iris:$irisVersion")
    modRuntimeOnly("com.terraformersmc:modmenu:$modmenuVersion")
}