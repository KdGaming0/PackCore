package com.kd_gaming1.screen

import com.kd_gaming1.screen.utils.CreateMenuButton
import com.kd_gaming1.screen.utils.CreateMenuButtonInfo
import com.kd_gaming1.screen.utils.CreateMenuButtonJoinServer
import com.kd_gaming1.screen.utils.CreateWebsiteButton
import com.kd_gaming1.utils.CheckForUpdates
import com.kd_gaming1.utils.ModpackInfo
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.markdown.MarkdownComponent
import gg.essential.universal.UMinecraft
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.gui.screen.option.OptionsScreen
import net.minecraft.client.gui.screen.world.SelectWorldScreen
import java.awt.Color

class SEMainMenu : WindowScreen(ElementaVersion.V7) {
    val versions = CheckForUpdates.checkForUpdates()
    val currentVersion = versions[0] ?: "Unknown"
    val latestVersion = versions[1] ?: "Unknown"
    val changeLog = versions[2] ?: "No change log available."

    private var isInfoPanelVisible = false

    // Check if ModMenu is installed
    private val isModMenuInstalled = FabricLoader.getInstance().isModLoaded("modmenu")

    init {

        // Button Container with Buttons
        val buttonContainer = UIContainer().constrain {
            x = 5.pixels()
            y = 0.pixels()
            width = ChildBasedMaxSizeConstraint() + 10.percent()
            height = 100.percent()
        } childOf window

        val buttonBackgroundEffect = UIBlock().constrain {
            x = CenterConstraint()
            y = 0.pixels()
            width = ChildBasedMaxSizeConstraint()
            height = 100.percent()
            color = Color(40, 40, 40, 150).toConstraint()
        } childOf buttonContainer

        // Main Title
        val title = UIImage.ofResource("/assets/packcore/textures/gui/SkyBlock-Enhanced-v6.png").constrain {
            x = CenterConstraint()
            y = 15.pixels()
            width = 180.pixels()
            height = ImageAspectConstraint()
        } childOf buttonContainer

        val buttonContainer2 = UIContainer().constrain {
            x = CenterConstraint()
            y = 70.pixels()
            width = ChildBasedMaxSizeConstraint()
            height = 100.percent()
        } childOf buttonBackgroundEffect

        // Add buttons
        CreateMenuButtonJoinServer("Join Hypixel", "mc.hypixel.net", 25565) {
        } childOf buttonContainer2

        CreateMenuButton("Singleplayer") {
            // Updated for Minecraft 1.21.5 - use setScreen instead of displayGuiScreen
            UMinecraft.getMinecraft().setScreen(SelectWorldScreen(this))
        } childOf buttonContainer2

        CreateMenuButton("Multiplayer") {
            // Updated for Minecraft 1.21.5 - use setScreen instead of displayGuiScreen
            UMinecraft.getMinecraft().setScreen(MultiplayerScreen(this))
        } childOf buttonContainer2

        val buttonContainer3 = UIContainer().constrain {
            x = CenterConstraint()
            y = SiblingConstraint(padding = 10f)
            width = ChildBasedMaxSizeConstraint()
            height = 100.percent()
        } childOf buttonContainer2

        CreateMenuButton("Options") {
            // Updated for Minecraft 1.21.5 - use setScreen and options instead of gameSettings
            UMinecraft.getMinecraft().setScreen(OptionsScreen(this, UMinecraft.getMinecraft().options))
        } childOf buttonContainer3

        // Only show Mods button if ModMenu is installed
        if (isModMenuInstalled) {
            CreateMenuButton("Mods") {
                try {
                    // Dynamically load ModMenu screen to avoid compile-time dependency
                    val modMenuScreenClass = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen")
                    val constructor = modMenuScreenClass.getConstructor(net.minecraft.client.gui.screen.Screen::class.java)
                    val modsScreen = constructor.newInstance(this) as net.minecraft.client.gui.screen.Screen
                    UMinecraft.getMinecraft().setScreen(modsScreen)
                } catch (e: Exception) {
                    // This should not happen if ModMenu is properly installed, but just in case
                    println("Failed to open ModMenu screen: ${e.message}")
                }
            } childOf buttonContainer3
        }

        val buttonContainer4 = UIContainer().constrain {
            x = CenterConstraint()
            y = SiblingConstraint(padding = 10f)
            width = ChildBasedMaxSizeConstraint()
            height = 100.percent()
        } childOf buttonContainer3

        CreateMenuButton("Quit Game") {
            // Updated for Minecraft 1.21.5 - use close instead of shutdown
            UMinecraft.getMinecraft().scheduleStop()
        } childOf buttonContainer4

        // Container for the social/websites buttons
        val websiteContainer = UIContainer().constrain {
            x = CenterConstraint()
            y = 0.pixels(true) + 20.pixels()
            width = ChildBasedSizeConstraint()
            height = ChildBasedSizeConstraint()
        } childOf buttonContainer
        // Add buttons
        CreateWebsiteButton("GitHub", "https://github.com/KdGaming0/SkyBlock-Enhanced-Modpack") childOf websiteContainer
        CreateWebsiteButton("Modrinth", "https://modrinth.com/project/skyblock-enhanced-modern-edition") childOf websiteContainer
        CreateWebsiteButton("Help", "https://github.com/KdGaming0/SkyBlock-Enhanced-Modpack/discussions") childOf websiteContainer

        val infobox = UIContainer().constrain {
            x = CenterConstraint()
            y = SiblingConstraint(padding = 3f)
            width = ChildBasedSizeConstraint()
            height = ChildBasedSizeConstraint()
        } childOf buttonContainer4

        UIText("SkyBlock Enhanced Version: ${ModpackInfo.getCurrentVersion()}").constrain {
            x = CenterConstraint()
            y = SiblingConstraint(padding = 3f)
            textScale = 0.6.pixels()
            color = Color(229, 160, 0).toConstraint()
        } childOf infobox

        // Updated to show only loaded mods count for Fabric
        val loadedMods = FabricLoader.getInstance().allMods.filter { !it.metadata.id.startsWith("fabric") && it.metadata.id != "minecraft" && it.metadata.id != "java" }
        UIText("Mods (${loadedMods.size}/${FabricLoader.getInstance().allMods.size})").constrain {
            x = CenterConstraint()
            y = SiblingConstraint(padding = 3f)
            textScale = 0.6.pixels()
            color = Color.LIGHT_GRAY.toConstraint()
        } childOf infobox

        // Update info
        val infoBox = UIContainer().constrain {
            x = 10.pixels(true)
            y = 10.pixels(true)
            width = 50.percent()
            height = 75.percent()
        } childOf window

        val infoPanel = UIRoundedRectangle(6f).constrain {
            x = RelativeConstraint(1f) + 110.percent()
            y = 5.pixels(true)
            width = 100.percent()
            height = 100.percent()
            color = Color(0, 0, 0, 120).toConstraint()
        } childOf infoBox

        val labeBox = UIContainer().constrain {
            x = 15.pixels(true)
            y = 5.pixels()
            width = ChildBasedSizeConstraint()
            height = ChildBasedMaxSizeConstraint()
        } childOf window
        val infoButton = CreateMenuButtonInfo("See What's New") {
            if (isInfoPanelVisible) {
                infoPanel.animate {
                    setXAnimation(Animations.OUT_EXP, 0.5f, RelativeConstraint(1f) + 110.percent())
                }
            } else {
                infoPanel.animate {
                    setXAnimation(Animations.OUT_EXP, 0.45f, 0.pixels(true))
                }
            }
            isInfoPanelVisible = !isInfoPanelVisible
        }.constrain {
            x = SiblingConstraint(padding = 0f, true)
            y = CenterConstraint()
        } childOf labeBox

        fun showInfoPanel() {
            infoPanel.animate {
                setXAnimation(Animations.OUT_EXP, 0.45f, 0.pixels(true))
            }
            isInfoPanelVisible = true
        }

        val versionText = UIWrappedText("Current Version: $currentVersion - Latest Version: $latestVersion").constrain {
            x = 5.pixels()
            y = 5.pixels()
            width = 100.percent() - 10.pixels()
            textScale = 1.pixels()
            color = Color.WHITE.toConstraint()
        } childOf infoPanel

        val updateMessage: String
        val updateMessageColor: Color

        if (currentVersion == latestVersion || currentVersion > latestVersion) {
            updateMessage = "You are up to date! Change log for current version."
            updateMessageColor = Color.WHITE
        } else {
            updateMessage = "A new update is available! Change log for the latest version."
            updateMessageColor = Color.RED // Change this color to make it stand out
            showInfoPanel()
        }

        val updateText = UIWrappedText(updateMessage).constrain {
            x = 5.pixels()
            y = SiblingConstraint(padding = 5f)
            width = 100.percent() - 10.pixels()
            textScale = 1.pixels()
            color = updateMessageColor.toConstraint()
        } childOf infoPanel

        val changelogDivider = UIRoundedRectangle(6f).constrain {
            x = CenterConstraint()
            y = SiblingConstraint(padding = 5f)
            width = 90.percent()
            height = 1.pixels()
            color = Color(255, 255, 255).toConstraint()
        } childOf infoPanel

        val changeLogComponent = ScrollComponent().constrain {
            x = 5.pixels()
            y = SiblingConstraint(padding = 5f)
            width = 90.percent()
            height = 100.percent() - 60.pixels()
        } childOf infoPanel

        val changeLogText = MarkdownComponent(changeLog.trimIndent()).constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 100.percent()
            height = ChildBasedSizeConstraint()
            textScale = 0.1.pixels()
            color = Color.WHITE.toConstraint()
        } childOf changeLogComponent

        // Create a container for the scroll bar system
        val scrollContainer = UIContainer().constrain {
            x = 0.pixels(true)
            y = 0.pixels()
            width = 8.pixels()
            height = 100.percent()
        } childOf infoPanel

        // Create subtle track markers to indicate scrollable area
        repeat(20) { index ->
            UIBlock(Color(255, 255, 255, 20)).constrain {
                x = 2.pixels()
                y = (index * 8).pixels()
                width = 4.pixels()
                height = 1.pixels()
            } childOf scrollContainer
        }

        //Scroll bar
        val scrollOutline = UIRoundedRectangle(4f).constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 8.pixels()
            height = 100.percent()
            color = Color(30, 30, 30, 100).toConstraint()
        } childOf scrollContainer

        scrollOutline effect OutlineEffect(
            Color(0, 0, 0, 40),
            1f,
            drawInsideChildren = true
        )

        val scrollBar = UIContainer().constrain {
            x = 1.pixels()
            y = 0.pixels()
            width = 6.pixels()
            height = 30.percent()
        } childOf scrollOutline

        val scrollBarBody = UIRoundedRectangle(4f).constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 100.percent()
            height = 100.percent()
            color = Color(200, 200, 200, 100).toConstraint()
        } childOf scrollBar

        repeat(3) { index ->
            UIBlock(Color(255, 255, 255, 80)).constrain {
                x = 1.5.pixels()
                y = CenterConstraint() + ((index - 1) * 4).pixels()
                width = 3.pixels()
                height = 1.pixels()
            } childOf scrollBar
        }

        scrollBar.onMouseEnter {
            children.forEach { child ->
                if (child is UIBlock) {
                    child.animate {
                        setColorAnimation(
                            Animations.OUT_EXP,
                            0.3f,
                            Color(252, 189, 56, 255).toConstraint()
                        )
                    }
                }
            }
        }.onMouseLeave {
            children.forEach { child ->
                if (child is UIBlock) {
                    child.animate {
                        setColorAnimation(
                            Animations.OUT_EXP,
                            0.3f,
                            Color(255, 255, 255, 80).toConstraint()
                        )
                    }
                }
            }
        }
        changeLogComponent.setScrollBarComponent(
            scrollBar,
            isHorizontal = false, // false for vertical scrolling
            hideWhenUseless = true // hide when content doesn't need scrolling
        )
    }
}