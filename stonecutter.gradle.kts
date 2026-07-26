plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "2.1.+" apply false
}

stonecutter active "26.1"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    constants["release"] = property("mod.id") != "template"
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String
}

val releaseVersions = listOf(
    "26.1"
)

stonecutter tasks {
    order("publishMods")
}

tasks.register("publishToAllPlatforms") {
    group       = "publishing"
    description = "Publish all release groups to Modrinth and CurseForge sequentially."
    dependsOn(releaseVersions.map { ":$it:publishMods" })
}

gradle.projectsEvaluated {
    releaseVersions.zipWithNext().forEach { (prev, next) ->
        project(":$next").tasks.named("publishMods") {
            mustRunAfter(":$prev:publishMods")
        }
    }
}
