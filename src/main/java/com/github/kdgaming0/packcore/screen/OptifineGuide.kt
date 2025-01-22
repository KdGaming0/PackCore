package com.github.kdgaming0.packcore.screen

import com.github.kdgaming0.packcore.screen.utils.CreateMenuButton
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.markdown.MarkdownComponent
import java.awt.Color

class OptifineGuide : UIContainer() {
    private var closeCallback: () -> Unit = {}

    fun onWindowClose(callback: () -> Unit): OptifineGuide {
        closeCallback = callback
        return this
    }

    init {
        val infoPanel = UIRoundedRectangle(6f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 95.percent()
            height = 95.percent()
            color = Color(0, 0, 0, 200).toConstraint()
        } childOf this

        val title = UIWrappedText("PackCore have detected that you don't have Optifine").constrain {
            x = CenterConstraint()
            y = 5.pixels()
            textScale = 1.25.pixels()
            color = Color.WHITE.toConstraint()
        } childOf infoPanel

        val guideBox = ScrollComponent().constrain {
            x = CenterConstraint()
            y = 25.pixels()
            width = 90.percent()
            height = 100.percent() - 60.pixels()
        } childOf infoPanel

        CreateMenuButton("Close") {
            parent.removeChild(this)
            closeCallback()
        }.constrain {
            x = CenterConstraint()
            y = 2.pixels(true)
        } childOf infoPanel

        // Create checkbox container
        val checkboxContainer = UIContainer().constrain {
            x = 5.pixels()
            y = 5.pixels(true)  // Position above the Close button
            width = ChildBasedSizeConstraint()
            height = 20.pixels()
        } childOf infoPanel

        val checkmark = UIText("✓").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            color = Color.BLACK.toConstraint()
        }

        val checkbox = UIBlock().constrain {
            x = 0.pixels()
            y = CenterConstraint()
            width = 15.pixels()
            height = 15.pixels()
            color = Color.LIGHT_GRAY.toConstraint()
        }.onMouseClick {

        }.effect (
            OutlineEffect(
                Color(0, 0, 0, 100),
                1f,
                drawInsideChildren = true
            )
        ) childOf checkboxContainer

        CreateMenuButton("Don't show this again") {
        }.constrain {
            x = SiblingConstraint(padding = 2f)
            y = CenterConstraint()
        } childOf checkboxContainer


        val guideInstructionsText = MarkdownComponent(
            """
            ## Optifine install guide
            1. Download Optifine from [here](https://optifine.net/downloads)
            2. Open the downloaded file
            3. Click install
            4. Open the Minecraft launcher
            5. Click the installations tab
            6. Click the new installation button
            7. Name the installation
            8. Select the version you installed Optifine for
            9. Click create
            10. Select the new installation
            11. Click play
            12. Enjoy Optifine!
            13. If you have any issues, please contact support
            """.trimIndent()
        ).constrain {
            x = CenterConstraint()
            width = 100.percent() - 10.pixels()
        } childOf guideBox

        // Create a container for the scroll bar system
        val scrollContainer = UIContainer().constrain {
            x = 0.pixels(true)
            y = 0.pixels()
            width = 8.pixels()
            height = 100.percent()
        } childOf infoPanel

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
        guideBox.setScrollBarComponent(
            scrollBar,
            isHorizontal = false, // false for vertical scrolling
            hideWhenUseless = true // hide when content doesn't need scrolling
        )
    }
}