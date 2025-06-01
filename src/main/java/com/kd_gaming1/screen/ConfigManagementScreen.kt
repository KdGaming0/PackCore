package com.kd_gaming1.screen

import com.kd_gaming1.screen.utils.CreateMenuButton
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.markdown.MarkdownComponent
import gg.essential.universal.UMinecraft
import java.awt.Color
import java.io.File
import java.nio.file.Paths

class ConfigManagementScreen : WindowScreen(ElementaVersion.V7) {

    // Paths to folders and files (extracted to reduce duplication & clarify usage).
    private val minecraftRoot = Paths.get("").toAbsolutePath().toString()
    private val skyblockFolder = File(minecraftRoot, "Skyblock Enhanced")

    private var createConfigZipWindow = false
    init {
        // Initialize the SkyblockEnhanced folder
        if (!skyblockFolder.exists()) {
            skyblockFolder.mkdirs()
        }

        // Left column with scrollable Markdown
        createLeftColumn()

        // Main container to hold UI for viewing & creating configs
        val mainContainer = createMainContainer()

        createColumnsForConfigTypes(mainContainer)
        createBottomButtons(mainContainer)
    }

    /**
     * Builds the left-hand column containing a scrollable Markdown help/guide section.
     * This method encapsulates all logic for producing the column's UI in one place.
     */
    private fun createLeftColumn() {
        val leftColumn = UIContainer().constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 30.percent()
            height = RelativeConstraint(1f)
        } childOf window

        val scrollComponent = createScrollingMarkdownComponent() childOf leftColumn
        val scrollContainer = createScrollBarTrack() childOf leftColumn

        // Introduce an outline for the scroll bar track
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

        // This container is the scrollable thumb itself
        val scrollBar = UIContainer().constrain {
            x = 1.pixels()
            y = 0.pixels()
            width = 6.pixels()
            height = 30.percent()
        } childOf scrollOutline

        createScrollBarBody(scrollBar)
        createScrollBarEvents(scrollBar)

        // Tie the vertical scroll bar mechanics to our Markdown scroll component.
        scrollComponent.setScrollBarComponent(
            scrollBar,
            isHorizontal = false,
            hideWhenUseless = true
        )
    }

    /**
     * Produces a ScrollComponent containing Markdown instructions for config management.
     * Extracted for clarity and to simplify createLeftColumn().
     */
    private fun createScrollingMarkdownComponent(): ScrollComponent {
        val scrollComponent = ScrollComponent().constrain {
            x = 10.pixels()
            y = 10.pixels()
            width = RelativeConstraint(1f) - 20.pixels()
            height = RelativeConstraint(1f) - 20.pixels()
        }

        MarkdownComponent(
            """
                # Work in progress...
                ### Config Management Guide
                
                This screen allows you to see and manage your zip archives with mod/MC configurations.
                
                ### Official Configs
                These are the default configurations that come with the mod pack:
                - Located in `Skyblock Enhanced/OfficialConfig
                - Cannot be deleted
                - Great starting point for new users
                
                ### Custom Configs
                Your personal saved configurations:
                - Located in `Skyblock Enhanced/CustomConfigs`
                - Can be deleted if no longer needed
                - Perfect for saving your custom setups
                
                ### Saving Configs
                To save your current settings:
                1. Click the **Save Current Config** button
                3. Select if you want to include the options for miencraft and optfine
                4. Select of you want to include all configs locateed in the configs folder
                5. Click **Save** to create the config file
                6. Give the zip archive a name 
                7. Confirm the creation of the zip archive
                
            """.trimIndent()
        ).constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = RelativeConstraint(1f)
        } childOf scrollComponent

        return scrollComponent
    }

    /**
     * Creates a container that acts as the track for the scroll bar (the vertical line on the right).
     * Also adds ornamental track markers for visual effect.
     */
    private fun createScrollBarTrack(): UIContainer {
        val scrollContainer = UIContainer().constrain {
            x = RelativeConstraint(1f) - 12.pixels()
            y = 0.pixels()
            width = 8.pixels()
            height = RelativeConstraint(1f)
        }

        // Decorative markers along the scroll track
        repeat(20) { index ->
            UIBlock(Color(255, 255, 255, 20)).constrain {
                x = 2.pixels()
                y = (index * 8).pixels()
                width = 4.pixels()
                height = 1.pixels()
            } childOf scrollContainer
        }
        return scrollContainer
    }

    /**
     * Creates the body of the scroll bar (the rounded rectangle we can see).
     */
    private fun createScrollBarBody(scrollBar: UIContainer) {
        UIRoundedRectangle(4f).constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 100.percent()
            height = 100.percent()
            color = Color(200, 200, 200, 100).toConstraint()
        } childOf scrollBar

        // Three horizontal "grip" lines
        repeat(3) { index ->
            UIBlock(Color(255, 255, 255, 80)).constrain {
                x = 1.5.pixels()
                y = CenterConstraint() + ((index - 1) * 4).pixels()
                width = 3.pixels()
                height = 1.pixels()
            } childOf scrollBar
        }
    }

    /**
     * Adds mouse enter/leave hover effects to the scroll bar for user feedback.
     */
    private fun createScrollBarEvents(scrollBar: UIContainer) {
        scrollBar.onMouseEnter {
            scrollBar.children.forEach { child ->
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
            scrollBar.children.forEach { child ->
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
    }

    /**
     * Builds the main container for the config management screen (right side area).
     * Returns the container so other methods can add child components (columns, buttons, etc.).
     */
    private fun createMainContainer(): UIContainer {
        val mainContainer = UIContainer().constrain {
            x = 30.percent()  // start after left column
            y = CenterConstraint()
            width = 65.percent()
            height = 85.percent()
        } childOf window

        // Background
        UIRoundedRectangle(8f).constrain {
            width = RelativeConstraint(1f)
            height = RelativeConstraint(1f)
            color = Color(20, 20, 20, 200).toConstraint()
        } childOf mainContainer

        // Title
        UIText("Config Management").constrain {
            x = CenterConstraint()
            y = 10.pixels()
            textScale = 2.pixels()
        } childOf mainContainer

        return mainContainer
    }

    /**
     * Builds the "two-column layout" for official and custom configs.
     * Each column is created with a helper function createConfigColumn().
     */
    private fun createColumnsForConfigTypes(mainContainer: UIContainer) {
        val columnsContainer = UIContainer().constrain {
            x = CenterConstraint()
            y = 40.pixels()
            width = 90.percent()
            height = RelativeConstraint(1f) - 60.pixels()
        } childOf mainContainer

        // Official configs column
        createConfigColumn(
            "Official Configs",
            "OfficialConfigs",
            5.pixels()
        ) childOf columnsContainer

        // Custom configs column
        createConfigColumn(
            "Custom Configs",
            "CustomConfigs",
            52.percent()
        ) childOf columnsContainer
    }

    /**
     * Creates the bottom row of buttons (e.g., "Save Current Config" and "Back").
     */
    private fun createBottomButtons(mainContainer: UIContainer) {
        val buttonContainer = UIContainer().constrain {
            x = CenterConstraint()
            y = RelativeConstraint(1f) - 40.pixels()
            width = ChildBasedSizeConstraint()
            height = 30.pixels()
        } childOf mainContainer

        CreateMenuButton("Save Current Config") {
            if (!createConfigZipWindow) {
                createConfigZipWindow = true
            CreateConfigZip().apply {
                constrain {
                    width = RelativeConstraint(1f)
                    height = RelativeConstraint(1f)
                }
                onWindowClose {
                    createConfigZipWindow = false
                    window.removeChild(this)
                }
            } childOf window
        }
        }.constrain {
            x = 0.pixels()
            y = CenterConstraint()
        } childOf buttonContainer

        CreateMenuButton("Back") {
            UMinecraft.getMinecraft().setScreen(null)
        }.constrain {
            x = SiblingConstraint(10f)
            y = CenterConstraint()
        } childOf buttonContainer
    }

    /**
     * Creates a UI column for either "Official Configs" or "Custom Configs".
     * Scans the specified folder and builds a scrollable list of .zip configs.
     *
     * - title: Display name for the column
     * - subfolderName: Subfolder inside "Skyblock Enhanced" (e.g., "OfficialConfigs")
     * - xPos: Horizontal constraint for column placement in the parent container
     */
    private fun createConfigColumn(title: String, subfolderName: String, xPos: XConstraint): UIContainer {
        val configFolder = File(skyblockFolder, subfolderName)
        if (!configFolder.exists()) {
            configFolder.mkdirs()
        }

        return UIContainer().constrain {
            x = xPos
            y = 0.pixels()
            width = 45.percent()
            height = RelativeConstraint(1f)
        }.also { column ->
            // Title
            UIText(title).constrain {
                x = CenterConstraint()
                y = 0.pixels()
                textScale = 1.2.pixels()
            } childOf column

            // Config list container
            val scrollComponent = ScrollComponent("No configs found").constrain {
                x = 0.pixels()
                y = 25.pixels()
                width = RelativeConstraint(1f)
                height = RelativeConstraint(1f) - 25.pixels()
            } childOf column

            // Populate list with .zip files (if any)
            val configFiles = configFolder.listFiles { file ->
                file.isFile && file.name.endsWith(".zip")
            }

            if (configFiles != null && configFiles.isNotEmpty()) {
                configFiles.forEach { configFile ->
                    createConfigEntry(configFile) childOf scrollComponent
                }
            }
        }
    }

    /**
     * Builds a single config entry row:
     * - Shows config name
     * - If in "CustomConfigs", adds a "Delete" button
     */
    private fun createConfigEntry(configFile: File): UIContainer {
        return UIContainer().constrain {
            x = 0.pixels()
            y = SiblingConstraint(5f)
            width = RelativeConstraint(1f)
            height = 30.pixels()
        }.also { container ->
            // Background
            UIRoundedRectangle(4f).constrain {
                width = RelativeConstraint(1f)
                height = RelativeConstraint(1f)
                color = Color(40, 40, 40, 150).toConstraint()
            } childOf container

            // Config name
            UIText(configFile.nameWithoutExtension).constrain {
                x = 5.pixels()
                y = CenterConstraint()
            } childOf container

            // Delete button if in CustomConfigs
            if (configFile.parentFile.name == "CustomConfigs") {
                createSmallButton("Delete") {
                    if (configFile.delete()) {
                        container.parent.removeChild(container)
                    }
                }.constrain {
                    x = 2.pixels(true)
                    y = CenterConstraint()
                } childOf container
            }
        }
    }

    /**
     * Creates a small button (50x20), used for actions like "Delete" within config entries.
     * Helps to reduce repetitive code blocks and improve consistency.
     */
    private fun createSmallButton(text: String, action: () -> Unit): UIContainer {
        return UIContainer().constrain {
            width = 50.pixels()
            height = 20.pixels()
        }.also { button ->
            UIRoundedRectangle(4f).constrain {
                width = RelativeConstraint(1f)
                height = RelativeConstraint(1f)
                color = Color(60, 60, 60, 150).toConstraint()
            }.onMouseEnter {
                animate {
                    setColorAnimation(Animations.OUT_EXP, 0.3f, Color(80, 80, 80, 150).toConstraint())
                }
            }.onMouseLeave {
                animate {
                    setColorAnimation(Animations.OUT_EXP, 0.3f, Color(60, 60, 60, 150).toConstraint())
                }
            }.onMouseClick { action() } childOf button

            UIText(text).constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                textScale = 0.8.pixels()
            } childOf button
        }
    }
}
