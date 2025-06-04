package com.kd_gaming1.screen

import com.kd_gaming1.config.ModConfig
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import java.awt.Color

class ConfigGui : WindowScreen(ElementaVersion.V7) {
    init {

        // Main container for all content
        val mainContainer = UIContainer().constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf window

        // Single title with the new text
        UIText("SkyBlock Enhanced Configs").constrain {
            x = CenterConstraint()
            y = 15.pixels()
            textScale = 2.5.pixels()
            color = Color(229, 160, 0).toConstraint()
        } childOf mainContainer

        UIText("Delivered by PackCore").constrain {
            x = CenterConstraint()
            y = SiblingConstraint(2f)
            textScale = 1.5.pixels()
            color = Color(170, 170, 170).toConstraint()
        } childOf mainContainer

        // Settings container with a semi-transparent background
        val settingsContainer = UIRoundedRectangle(6f).constrain {
            x = CenterConstraint()
            y = 65.pixels()
            width = RelativeConstraint(0.6f)
            height = RelativeConstraint(0.7f)
            color = Color(20, 20, 20, 180).toConstraint()
        } childOf mainContainer

        // Scroll component to handle many options
        val scrollComponent = ScrollComponent(
            innerPadding = 10f
        ).constrain {
            x = 10.pixels()
            y = 10.pixels()
            width = RelativeConstraint(1f) - 30.pixels() // Leave room for scroll bar
            height = RelativeConstraint(1f) - 55.pixels()
        } childOf settingsContainer

        // Inner container for settings
        val innerContainer = UIContainer().constrain {
            width = RelativeConstraint(1f)
            height = ChildBasedSizeConstraint()
        } childOf scrollComponent

        // Create scroll container
        val scrollContainer = UIContainer().constrain {
            x = RelativeConstraint(1f) - 12.pixels()
            y = 0.pixels()
            width = 8.pixels()
            height = RelativeConstraint(1f) - 55.pixels()
        } childOf settingsContainer

        // Create track markers
        repeat(20) { index ->
            UIBlock(Color(255, 255, 255, 20)).constrain {
                x = 2.pixels()
                y = (index * 8).pixels()
                width = 4.pixels()
                height = 1.pixels()
            } childOf scrollContainer
        }

        // Scroll outline
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

        // Scroll bar
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

        // Add the three lines in the scroll bar
        repeat(3) { index ->
            UIBlock(Color(255, 255, 255, 80)).constrain {
                x = 1.5.pixels()
                y = CenterConstraint() + ((index - 1) * 4).pixels()
                width = 3.pixels()
                height = 1.pixels()
            } childOf scrollBar
        }

        // Scroll bar hover effects
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

        // Connect scroll bar to scroll component
        scrollComponent.setScrollBarComponent(
            scrollBar,
            isHorizontal = false,
            hideWhenUseless = true
        )

        // Create settings with improved styling
        createToggleSetting(
            innerContainer,
            "Ask To Set Default Config",
            "When enabled, you will promoted to copy configs from the archiver folder to the Minecraft instance folder. This allows you to easily enable configs you have saved with the help of /packcore archive - if you have not archived any configs, this will apply the default configs that comes with the modpack.",
            ModConfig.getPromptSetDefaultConfig()
        ) { newValue ->
            ModConfig.setPromptSetDefaultConfig(newValue)
        }

        createToggleSetting(
            innerContainer,
            "Enable Custom Main Menu",
            "Use a custom main menu with a skyblock-themed background. This provides a better Skyblock Enhanced experience. Recommended",
            ModConfig.getEnableCustomMenu()
        ) { newValue ->
            ModConfig.setEnableCustomMenu(newValue)
        }

        // Save & Close button
        UIRoundedRectangle(4f).constrain {
            x = CenterConstraint()
            y = RelativeConstraint(1f) - 35.pixels()
            width = 120.pixels()
            height = 25.pixels()
            color = Color(229, 160, 0, 180).toConstraint()
        }.also { button ->
            UIText("Save & Close").constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                color = Color.WHITE.toConstraint()
            } childOf button

            button.onMouseClick {
                ModConfig.saveConfig()
                displayScreen(null)
            }
            button.onMouseEnter {
                button.setColor(Color(252, 189, 56))
            }
            button.onMouseLeave {
                button.setColor(Color(229, 160, 0, 180))
            }
        } childOf settingsContainer
    }

    private fun createToggleSetting(
        parent: UIContainer,
        title: String,
        description: String,
        initialValue: Boolean,
        onToggle: (Boolean) -> Unit
    ) {
        val container = UIContainer().constrain {
            x = CenterConstraint()
            y = SiblingConstraint(2f) // Further reduced padding between options
            width = 100.percent()
            height = ChildBasedSizeConstraint()
        } childOf parent

        // Setting title with improved styling
        UIText(title).constrain {
            x = 0.pixels()
            y = 0.pixels()
            color = Color.WHITE.toConstraint()
        } childOf container

        // Setting description using UIWrappedText
        UIWrappedText(description).constrain {
            x = 0.pixels()
            y = SiblingConstraint(1f) // Reduced padding between title and description
            width = RelativeConstraint(0.8f)
            textScale = 0.8.pixels()
            color = Color(170, 170, 170).toConstraint()
        } childOf container

        // Toggle button with improved styling
        var isEnabled = initialValue
        val toggleButton = UIRoundedRectangle(6f).constrain {
            x = 0.pixels(true)
            y = 4.pixels()
            width = 46.pixels()
            height = 22.pixels()
            color = (if (isEnabled) Color(60, 160, 60, 180) else Color(100, 100, 100, 180)).toConstraint()
        }.also { button ->
            // Slider with rounded corners
            val slider = UIRoundedRectangle(5f).constrain {
                x = if (isEnabled) 24.pixels() else 2.pixels()
                y = 2.pixels()
                width = 18.pixels()
                height = 18.pixels()
                color = Color.WHITE.toConstraint()
            } childOf button

            button.onMouseClick {
                isEnabled = !isEnabled
                onToggle(isEnabled)

                if (isEnabled) {
                    button.setColor(Color(60, 160, 60, 180))
                    slider.animate {
                        setXAnimation(
                            Animations.OUT_EXP,
                            0.2f,
                            24.pixels()
                        )
                    }
                } else {
                    button.setColor(Color(100, 100, 100, 180))
                    slider.animate {
                        setXAnimation(
                            Animations.OUT_EXP,
                            0.2f,
                            2.pixels()
                        )
                    }
                }
            }
        } childOf container
    }
}