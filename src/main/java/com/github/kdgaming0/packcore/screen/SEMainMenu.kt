package com.github.kdgaming0.packcore.screen

import com.github.kdgaming0.packcore.screen.components.UIPanorama
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen

import com.github.kdgaming0.packcore.screen.utils.CreateMenuButton
import com.github.kdgaming0.packcore.screen.utils.CreateMenuButtonInfo
import com.github.kdgaming0.packcore.screen.utils.CreateMenuButtonJoinServer
import com.github.kdgaming0.packcore.screen.utils.CreateWebsiteButton
import com.github.kdgaming0.packcore.utils.CheckForUpdates
import com.github.kdgaming0.packcore.utils.ModpackInfo
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.markdown.MarkdownComponent
import gg.essential.universal.UMinecraft
import net.minecraftforge.fml.common.Loader;
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.gui.GuiOptions
import net.minecraft.client.gui.GuiSelectWorld
import java.awt.Color

class SEMainMenu : WindowScreen(ElementaVersion.V7) {
    val versions = CheckForUpdates.checkForUpdates()
    val currentVersion = versions[0] ?: "Unknown"
    val latestVersion = versions[1] ?: "Unknown"
    val changeLog = versions[2] ?: "No change log available."

    private var isInfoPanelVisible = false
    private var optifineWindowOpen = false
    private var optifineGuide: OptifineGuide? = null

        init {
            // Set the background image
            val background = UIPanorama().constrain {
                width = RelativeConstraint(1f)
                height = RelativeConstraint(1f)
            } childOf window

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
            val title = UIImage.ofResource("/assets/packcore/textures/gui/SkyBlock-Enhanced-v5.png").constrain {
                x = CenterConstraint()
                y = 15.pixels()
                width = 180.pixels()
                height = ImageAspectConstraint()
            } childOf buttonContainer

            val buttonContainer2 = UIContainer().constrain {
                x = CenterConstraint()
                y = 60.pixels()
                width = ChildBasedMaxSizeConstraint()
                height = 100.percent()
            } childOf buttonBackgroundEffect

                // Add buttons
                CreateMenuButtonJoinServer("Join Hypixel", "mc.hypixel.net", 25565) {
                } childOf buttonContainer2

                CreateMenuButton("Singleplayer") {
                    UMinecraft.getMinecraft().displayGuiScreen(GuiSelectWorld(this))
                } childOf buttonContainer2

                CreateMenuButton("Multiplayer") {
                    UMinecraft.getMinecraft().displayGuiScreen(GuiMultiplayer(this))
                } childOf buttonContainer2

            val buttonContainer3 = UIContainer().constrain {
                x = CenterConstraint()
                y = SiblingConstraint(padding = 10f)
                width = ChildBasedMaxSizeConstraint()
                height = 100.percent()
            } childOf buttonContainer2

                CreateMenuButton("Options") {
                    UMinecraft.getMinecraft().displayGuiScreen(GuiOptions(this, UMinecraft.getMinecraft().gameSettings))
                } childOf buttonContainer3

                CreateMenuButton("Mod Options") {
                    // Set menu later
                } childOf buttonContainer3

            val buttonContainer4 = UIContainer().constrain {
                x = CenterConstraint()
                y = SiblingConstraint(padding = 10f)
                width = ChildBasedMaxSizeConstraint()
                height = 100.percent()
            } childOf buttonContainer3

                CreateMenuButton("Quit Game") {
                    UMinecraft.getMinecraft().shutdown()
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
            CreateWebsiteButton("Modrinth", "https://modrinth.com/modpack/skyblock-enhanced") childOf websiteContainer
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

            UIText("Mods (${Loader.instance().getModList().size}/${Loader.instance().getModList().size})").constrain {
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

            if (!Loader.isModLoaded("optifine")) {
                val optifineGuide = CreateMenuButtonInfo("Optifine Guide") {
                    if (!optifineWindowOpen) {
                        optifineWindowOpen = true
                        OptifineGuide().apply {
                            constrain {
                                width = RelativeConstraint(1f)
                                height = RelativeConstraint(1f)
                            }

                            onWindowClose {
                                optifineWindowOpen = false
                                window.removeChild(this)
                            }
                        } childOf window
                    }
                }.constrain {
                    x = 100.pixels(true)
                    y = 5.pixels()
                } childOf window
            }

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
                x = 15.pixels(true)
                y = 5.pixels()
            } childOf window

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

    override fun afterInitialization() {
        if (!Loader.isModLoaded("optifine") && !optifineWindowOpen && optifineGuide == null) {
            optifineGuide = OptifineGuide().apply {
                constrain {
                    width = RelativeConstraint(1f)
                    height = RelativeConstraint(1f)
                }

                onWindowClose {
                    optifineWindowOpen = false
                    window.removeChild(optifineGuide!!)
                    optifineGuide = null
                }
            } childOf window
        }
    }
}