package com.github.kdgaming0.packcore.screen

import com.github.kdgaming0.packcore.screen.components.UIPanorama
import com.github.kdgaming0.packcore.config.ModConfig
import com.github.kdgaming0.packcore.screen.utils.CreateMenuButton
import com.github.kdgaming0.packcore.screen.utils.CreateMenuButtonInfo
import com.github.kdgaming0.packcore.screen.utils.CreateMenuButtonJoinServer
import com.github.kdgaming0.packcore.screen.utils.CreateWebsiteButton
import com.github.kdgaming0.packcore.utils.CheckForUpdates
import com.github.kdgaming0.packcore.utils.ModpackInfo
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.elementa.markdown.MarkdownComponent
import gg.essential.universal.UMinecraft
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.gui.GuiOptions
import net.minecraft.client.gui.GuiSelectWorld
import net.minecraftforge.fml.common.Loader
import java.awt.Color

/**
 * Main menu screen for SkyBlock Enhanced modpack.
 * Provides navigation to game modes, settings, and modpack information.
 */
class SEMainMenu : WindowScreen(ElementaVersion.V7) {

    // Version information retrieved once during initialization
    private val versionInfo = CheckForUpdates.checkForUpdates()
    private val currentVersion = versionInfo.getOrNull(0) ?: "Unknown"
    private val latestVersion = versionInfo.getOrNull(1) ?: "Unknown"
    private val changeLog = versionInfo.getOrNull(2) ?: "No change log available."

    // State management
    private var isInfoPanelVisible = false
    private var isOptifineWindowOpen = false
    private var optifineGuide: OptifineGuide? = null

    // Store references to animated components
    private lateinit var infoPanel: UIRoundedRectangle

    // UI color constants
    private companion object {
        val BACKGROUND_COLOR = Color(40, 40, 40, 150)
        val VERSION_COLOR = Color(229, 160, 0)
        val INFO_PANEL_COLOR = Color(0, 0, 0, 120)
        val DIVIDER_COLOR = Color.WHITE
        val SCROLL_TRACK_COLOR = Color(30, 30, 30, 100)
        val SCROLL_BAR_COLOR = Color(200, 200, 200, 100)
        val SCROLL_INDICATOR_COLOR = Color(255, 255, 255, 80)
        val SCROLL_HOVER_COLOR = Color(252, 189, 56, 255)

        // Layout constants
        const val BUTTON_SPACING = 10f
        const val ANIMATION_DURATION = 0.45f
        const val INFO_PANEL_OFFSET = 110f
    }

    init {
        setupUI()
    }

    /**
     * Main UI setup orchestration method
     */
    private fun setupUI() {
        createBackground()
        createMainButtonContainer()
        createInfoPanel()
        createControlButtons()
    }

    /**
     * Creates the panoramic background
     */
    private fun createBackground() {
        UIPanorama().constrain {
            width = RelativeConstraint(1f)
            height = RelativeConstraint(1f)
        } childOf window
    }

    /**
     * Creates the main container with navigation buttons
     */
    private fun createMainButtonContainer() {
        val buttonContainer = UIContainer().constrain {
            x = 5.pixels()
            y = 0.pixels()
            width = ChildBasedMaxSizeConstraint() + 10.percent()
            height = 100.percent()
        } childOf window

        // Background panel for buttons
        val backgroundPanel = UIBlock().constrain {
            x = CenterConstraint()
            y = 0.pixels()
            width = ChildBasedMaxSizeConstraint() - 25.pixels()
            height = 100.percent()
            color = BACKGROUND_COLOR.toConstraint()
        } childOf buttonContainer

        createTitleAndButtons(backgroundPanel)
        createWebsiteButtons(buttonContainer)
        createVersionInfo(buttonContainer)
    }

    /**
     * Creates the title logo and navigation buttons
     */
    private fun createTitleAndButtons(parent: UIComponent) {
        // Main title logo
        UIImage.ofResource("/assets/packcore/textures/gui/SkyBlock-Enhanced-v5.png").constrain {
            x = CenterConstraint()
            y = 15.pixels()
            width = 180.pixels()
            height = ImageAspectConstraint()
        } childOf parent

        // Primary navigation buttons
        val primaryButtonContainer = createButtonContainer(parent, 70.pixels())
        createPrimaryButtons(primaryButtonContainer)

        // Secondary navigation buttons
        val secondaryButtonContainer = createButtonContainer(primaryButtonContainer, SiblingConstraint(BUTTON_SPACING))
        createSecondaryButtons(secondaryButtonContainer)

        // Exit button
        val exitButtonContainer = createButtonContainer(secondaryButtonContainer, SiblingConstraint(BUTTON_SPACING))
        createExitButton(exitButtonContainer)
    }

    /**
     * Helper method to create button containers with consistent styling
     */
    private fun createButtonContainer(parent: UIComponent, yConstraint: YConstraint): UIContainer {
        return UIContainer().constrain {
            x = CenterConstraint()
            y = yConstraint
            width = ChildBasedMaxSizeConstraint()
            height = 100.percent()
        } childOf parent
    }

    /**
     * Creates primary navigation buttons (server connection and game modes)
     */
    private fun createPrimaryButtons(container: UIContainer) {
        CreateMenuButtonJoinServer("Join Hypixel", "mc.hypixel.net", 25565) {
            // Connection logic handled by the button component
        } childOf container

        CreateMenuButton("Singleplayer") {
            UMinecraft.getMinecraft().displayGuiScreen(GuiSelectWorld(this))
        } childOf container

        CreateMenuButton("Multiplayer") {
            UMinecraft.getMinecraft().displayGuiScreen(GuiMultiplayer(this))
        } childOf container
    }

    /**
     * Creates secondary navigation buttons (settings and options)
     */
    private fun createSecondaryButtons(container: UIContainer) {
        CreateMenuButton("Options") {
            UMinecraft.getMinecraft().displayGuiScreen(
                GuiOptions(this, UMinecraft.getMinecraft().gameSettings)
            )
        } childOf container

        CreateMenuButton("PackCore Options") {
            displayScreen(ConfigGui())
        } childOf container
    }

    /**
     * Creates the exit game button
     */
    private fun createExitButton(container: UIContainer) {
        CreateMenuButton("Quit Game") {
            UMinecraft.getMinecraft().shutdown()
        } childOf container
    }

    /**
     * Creates website/social media buttons
     */
    private fun createWebsiteButtons(parent: UIContainer) {
        val websiteContainer = UIContainer().constrain {
            x = CenterConstraint()
            y = 0.pixels(true) + 20.pixels()
            width = ChildBasedSizeConstraint()
            height = ChildBasedSizeConstraint()
        } childOf parent

        val websiteButtons = listOf(
            "GitHub" to "https://github.com/KdGaming0/SkyBlock-Enhanced-Modpack",
            "Modrinth" to "https://modrinth.com/project/9JTbeXjU",
            "Discord" to "https://discord.gg/pdwxyjTta7",
            "Help" to "https://github.com/KdGaming0/SkyBlock-Enhanced-Modpack/discussions"
        )

        websiteButtons.forEach { (name, url) ->
            CreateWebsiteButton(name, url) childOf websiteContainer
        }
    }

    /**
     * Creates version and mod information display
     */
    private fun createVersionInfo(parent: UIContainer) {
        val infoContainer = UIContainer().constrain {
            x = CenterConstraint()
            y = 0.pixels(true) + 60.pixels()
            width = ChildBasedSizeConstraint()
            height = ChildBasedSizeConstraint()
        } childOf parent

        val modCount = Loader.instance().modList.size

        UIText("SkyBlock Enhanced Version: ${ModpackInfo.getCurrentVersion()}").constrain {
            x = CenterConstraint()
            y = 0.pixels()
            textScale = 0.6.pixels()
            color = VERSION_COLOR.toConstraint()
        } childOf infoContainer

        UIText("Mods ($modCount/$modCount)").constrain {
            x = CenterConstraint()
            y = SiblingConstraint(3f)
            textScale = 0.6.pixels()
            color = Color.LIGHT_GRAY.toConstraint()
        } childOf infoContainer
    }

    /**
     * Creates the sliding information panel
     */
    private fun createInfoPanel() {
        val infoContainer = UIContainer().constrain {
            x = 10.pixels(true)
            y = 10.pixels(true)
            width = 50.percent()
            height = 75.percent()
        } childOf window

        infoPanel = createInfoPanelBackground(infoContainer)
        populateInfoPanel(infoPanel)
    }

    /**
     * Creates the background for the info panel
     */
    private fun createInfoPanelBackground(parent: UIContainer): UIRoundedRectangle {
        return UIRoundedRectangle(6f).constrain {
            x = RelativeConstraint(1f) + INFO_PANEL_OFFSET.percent()
            y = 5.pixels(true)
            width = 100.percent()
            height = 100.percent()
            color = INFO_PANEL_COLOR.toConstraint()
        } childOf parent
    }

    /**
     * Populates the info panel with version information and changelog
     */
    private fun populateInfoPanel(panel: UIRoundedRectangle) {
        createVersionDisplay(panel)
        createUpdateMessage(panel)
        createChangelogSection(panel)
        createScrollBar(panel)
    }

    /**
     * Creates version information display in the info panel
     */
    private fun createVersionDisplay(panel: UIRoundedRectangle) {
        UIWrappedText("Current Version: $currentVersion - Latest Version: $latestVersion").constrain {
            x = 5.pixels()
            y = 5.pixels()
            width = 100.percent() - 10.pixels()
            textScale = 1.pixels()
            color = Color.WHITE.toConstraint()
        } childOf panel
    }

    /**
     * Creates update status message
     */
    private fun createUpdateMessage(panel: UIRoundedRectangle) {
        val (message, color) = getUpdateMessageAndColor()

        UIWrappedText(message).constrain {
            x = 5.pixels()
            y = SiblingConstraint(5f)
            width = 100.percent() - 10.pixels()
            textScale = 1.pixels()
        } childOf panel
    }

    /**
     * Determines update message and color based on version comparison
     */
    private fun getUpdateMessageAndColor(): Pair<String, Color> {
        return if (isUpToDate()) {
            "You are up to date! Change log for current version." to Color.WHITE
        } else {
            "A new update is available! Change log for the latest version." to Color.RED
        }
    }

    /**
     * Creates the changelog section with scrollable content
     */
    private fun createChangelogSection(panel: UIRoundedRectangle) {
        // Divider
        UIRoundedRectangle(6f).constrain {
            x = CenterConstraint()
            y = SiblingConstraint(5f)
            width = 90.percent()
            height = 1.pixels()
            color = DIVIDER_COLOR.toConstraint()
        } childOf panel

        // Scrollable changelog
        val scrollComponent = ScrollComponent().constrain {
            x = 5.pixels()
            y = SiblingConstraint(5f)
            width = 90.percent()
            height = 100.percent() - 60.pixels()
        } childOf panel

        MarkdownComponent(changeLog.trimIndent()).constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 100.percent()
            height = ChildBasedSizeConstraint()
            textScale = 0.1.pixels()
            color = Color.WHITE.toConstraint()
        } childOf scrollComponent
    }

    /**
     * Creates custom scroll bar for the info panel
     */
    private fun createScrollBar(panel: UIRoundedRectangle) {
        // Find the scroll component that was created in createChangelogSection
        val scrollComponent = panel.children.filterIsInstance<ScrollComponent>().firstOrNull() ?: return

        val scrollContainer = UIContainer().constrain {
            x = 0.pixels(true)
            y = 0.pixels()
            width = 8.pixels()
            height = 100.percent()
        } childOf panel

        val scrollBar = createScrollBarComponent(scrollContainer)

        // Connect the scroll bar to the actual scroll component
        scrollComponent.setScrollBarComponent(
            scrollBar, hideWhenUseless = true, isHorizontal = false
        )
    }

    /**
     * Creates the actual scrollbar component
     */
    private fun createScrollBarComponent(container: UIContainer): UIContainer {
        val scrollBar = UIContainer().constrain {
            x = 1.pixels()
            y = 0.pixels()
            width = 6.pixels()
            height = 30.percent()
        } childOf container

        // Create the draggable scroll bar background
        UIRoundedRectangle(3f).constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 100.percent()
            height = 100.percent()
            color = SCROLL_BAR_COLOR.toConstraint()
        } childOf scrollBar

        repeat(3) { index ->
            UIBlock(SCROLL_INDICATOR_COLOR).constrain {
                x = 1.5.pixels()
                y = CenterConstraint() + ((index - 1) * 4).pixels()
                width = 3.pixels()
                height = 1.pixels()
            } childOf scrollBar
        }

        scrollBar.onMouseEnter {
            children.filterIsInstance<UIBlock>().forEach { child ->
                child.animate {
                    setColorAnimation(Animations.OUT_EXP, 0.3f, SCROLL_HOVER_COLOR.toConstraint())
                }
            }
        }.onMouseLeave {
            children.filterIsInstance<UIBlock>().forEach { child ->
                child.animate {
                    setColorAnimation(Animations.OUT_EXP, 0.3f, SCROLL_INDICATOR_COLOR.toConstraint())
                }
            }
        }

        return scrollBar
    }

    /**
     * Creates control buttons (info panel toggle and optifine guide)
     */
    private fun createControlButtons() {
        val controlContainer = UIContainer().constrain {
            x = 15.pixels(true)
            y = 5.pixels()
            width = ChildBasedSizeConstraint()
            height = ChildBasedMaxSizeConstraint()
        } childOf window

        createInfoToggleButton(controlContainer)

        if (!isOptiFineInstalled()) {
            createOptifineGuideButton(controlContainer)
        }
    }

    /**
     * Creates the button to toggle the info panel
     */
    private fun createInfoToggleButton(parent: UIContainer) {
        CreateMenuButtonInfo("See What's New") {
            toggleInfoPanel()
        }.constrain {
            x = 0.pixels(true)
            y = CenterConstraint()
        } childOf parent
    }

    /**
     * Creates the Optifine guide button (only if Optifine is not installed)
     */
    private fun createOptifineGuideButton(parent: UIContainer) {
        CreateMenuButtonInfo("Optifine Guide") {
            openOptifineGuide()
        }.constrain {
            x = SiblingConstraint(20f, true)
            y = CenterConstraint()
        } childOf parent
    }

    /**
     * Toggles the visibility of the info panel with animation
     */
    private fun toggleInfoPanel() {
        val targetX = if (isInfoPanelVisible) {
            RelativeConstraint(1f) + INFO_PANEL_OFFSET.percent()
        } else {
            0.pixels(true)
        }

        infoPanel.animate {
            setXAnimation(Animations.OUT_EXP, ANIMATION_DURATION, targetX)
        }

        isInfoPanelVisible = !isInfoPanelVisible
    }

    /**
     * Shows the info panel automatically (used for update notifications)
     */
    private fun showInfoPanel() {
        if (!isInfoPanelVisible) {
            toggleInfoPanel()
        }
    }

    /**
     * Opens the Optifine installation guide
     */
    private fun openOptifineGuide() {
        if (!isOptifineWindowOpen) {
            isOptifineWindowOpen = true

            optifineGuide = OptifineGuide().apply {
                constrain {
                    width = RelativeConstraint(1f)
                    height = RelativeConstraint(1f)
                }

                onWindowClose {
                    closeOptifineGuide()
                }
            } childOf window
        }
    }

    /**
     * Closes the Optifine guide window
     */
    private fun closeOptifineGuide() {
        optifineGuide?.let { guide ->
            isOptifineWindowOpen = false
            window.removeChild(guide)
            optifineGuide = null
        }
    }

    /**
     * Checks if the current version is up to date
     */
    private fun isUpToDate(): Boolean {
        return currentVersion == latestVersion || currentVersion > latestVersion
    }

    /**
     * Checks if OptiFine is installed by attempting to load its class
     */
    private fun isOptiFineInstalled(): Boolean {
        return try {
            Class.forName("optifine.OptiFineClassTransformer")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    override fun afterInitialization() {
        // Show update notification if not up to date
        if (!isUpToDate()) {
            showInfoPanel()
        }

        // Show Optifine guide if conditions are met
        if (shouldShowOptifineGuide()) {
            openOptifineGuide()
        }
    }

    /**
     * Determines if the Optifine guide should be shown automatically
     */
    private fun shouldShowOptifineGuide(): Boolean {
        return !isOptiFineInstalled() &&
                ModConfig.getShowOptifineGuide() &&
                !isOptifineWindowOpen &&
                optifineGuide == null
    }
}