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

dependencies {
    modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")

    modImplementation("io.wispforest:owo-lib:${project.properties["owo_version"]}")
    modImplementation(include("maven.modrinth:midnightlib:${project.properties["midnightlib_version"]}")!!)
    modImplementation(include("io.wispforest.lavender-md:core:${project.properties["lavender_md_version"]}")!!)
    modImplementation(include("io.wispforest.lavender-md:owo-ui:${project.properties["lavender_md_version"]}")!!)

    modCompileOnly("maven.modrinth:sodium:mc1.21.6-0.6.13-fabric")
    modCompileOnly("maven.modrinth:iris:1.9.1+1.21.7-fabric")
    modRuntimeOnly("com.terraformersmc:modmenu:${project.properties["modmenu_version"]}")
}