plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "1.0.+" apply false
}

stonecutter tasks {
    order("publishModrinth")
    order("publishCurseforge")
}

stonecutter active "1.21.11"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    swaps["mod_version"] = "\"" + property("mod.version") + "\";"
    swaps["minecraft"] = "\"" + node.metadata.version + "\";"
    constants["release"] = property("mod.id") != "template"
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String

    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
        string(current.parsed >= "26.1") {
            replace("classTweaker v1 named", "classTweaker v1 official")
        }
        // UILib: render → extract for 26.1+
        regex(current.parsed >= "26.1") {
            replace("render([A-Z])" to "extract$1", "extract([A-Z])" to "render$1")
        }
    }
}