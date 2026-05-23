plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "1.0.+" apply false
}

stonecutter tasks {
    order("publishModrinth")
    order("publishCurseforge")
}

stonecutter active "26.1"

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
            replace("GuiGraphics", "GuiGraphicsExtractor")
            replace("renderContents", "extractContents")
            replace(".drawCenteredString", ".centeredText")
            replace(".renderOutline", ".outline")
            replace("super.render(", "super.extractRenderState(")
            replace(".render(", ".extractRenderState(")
            replace("Screens.getButtons(", "Screens.getWidgets(")
        }
        // UILib: render → extract for 26.1+
        string(current.parsed >= "26.1") {
            replace("renderBackground", "extractBackground")
            replace(".renderBase(", ".extractRenderStateBase(")
        }
    }
}