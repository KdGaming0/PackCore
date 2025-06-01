package com.kd_gaming1.screen

import com.kd_gaming1.config.ModConfig
import com.kd_gaming1.screen.utils.CreateCheckmark
import com.kd_gaming1.screen.utils.CreateMenuButton
import gg.essential.elementa.components.*
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.components.inspector.Inspector
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import java.awt.Color

class CreateConfigZip : UIContainer() {
    private var closeCallback: () -> Unit = {}

    fun onWindowClose(callback: () -> Unit): CreateConfigZip {
        closeCallback = callback
        return this
    }

    init {

        val backgroundPanel = UIRoundedRectangle(6f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 95.percent()
            height = 95.percent()
            color = Color(0, 0, 0, 200).toConstraint()
        } childOf this

        // Top title text
        UIText("Create config zip with selected items").constrain {
            x = CenterConstraint()
            y = 5.pixels()
            textScale = 1.25.pixels()
            color = Color.WHITE.toConstraint()
        } childOf backgroundPanel

        // Scroll box with list of items in from the root folder of minecraft in tree format
        val scrollBox = ScrollComponent().constrain {
            x = CenterConstraint()
            y = SiblingConstraint()
            width = 90.percent()
            height = 100.percent() - 60.pixels()
        } childOf backgroundPanel

        // Adding a scroll bar to the scroll box
        val scrollContainer = UIContainer().constrain {
            x = 0.pixels(true)
            y = 0.pixels()
            width = 8.pixels()
            height = 100.percent()
        } childOf scrollBox

        repeat(20) { index ->
            UIBlock(Color(255, 255, 255, 20)).constrain {
                x = 2.pixels()
                y = (index * 8).pixels()
                width = 4.pixels()
                height = 1.pixels()
            } childOf scrollContainer
        }

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
        scrollBox.setScrollBarComponent(
            scrollBar,
            isHorizontal = false, // false for vertical scrolling
            hideWhenUseless = true // hide when content doesn't need scrolling
        )


        // Create confirmation pop-up
        val confirmPopUp = UIRoundedRectangle(4f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 50.percent()
            height = 50.percent()
            color = Color(30, 30, 30, 100).toConstraint()
        } childOf backgroundPanel

        confirmPopUp.hide(true)

        CreateMenuButton("Close") {
            parent.removeChild(this)
            closeCallback()
        }.constrain {
            x = CenterConstraint() + 25.percent()
            y = 2.pixels(true)
        } childOf backgroundPanel

        CreateMenuButton("Archive Selected") {
            confirmPopUp.unhide()
        }.constrain {
            x = CenterConstraint() - 25.percent()
            y = 5.pixels(true)
        } childOf backgroundPanel

        UIText("Work in progress don't work!").constrain {
            x = CenterConstraint()
            y = 10.pixels(true)
            textScale = 1.pixels()
            color = Color.WHITE.toConstraint()
        } childOf confirmPopUp

        val confirmText = UIWrappedText("Are you sure you want to create a config zip with the selected items?").constrain {
            x = CenterConstraint()
            y = 7.pixels()
            width = RelativeConstraint(0.9f)
            textScale = 1.pixels()
            color = Color.WHITE.toConstraint()
        } childOf confirmPopUp

        val giveNameText = UIWrappedText("Please give a name for the zip archive").constrain {
            x = CenterConstraint()
            y = SiblingConstraint(padding = 7f)
            width = RelativeConstraint(0.9f)
            textScale = 1.pixels()
            color = Color.WHITE.toConstraint()
        } childOf confirmPopUp

        val textInputBox = UIBlock ().constrain {
            x = CenterConstraint()
            y = SiblingConstraint(padding = 7f)
            width = 90.percent()
            height = ChildBasedSizeConstraint() + 5.pixels()
            color = Color(0,0,0,50).toConstraint()
        } effect (
                OutlineEffect(
                    Color(0, 0, 0),
                    1f,
                    drawAfterChildren = true
                )
        ) childOf confirmPopUp

        val inputName = UITextInput("Write name for zip archive").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = RelativeConstraint(1f) - 6.pixels()
        } childOf textInputBox

        textInputBox.onMouseClick { inputName.grabWindowFocus() }

        CreateMenuButton("Yes") {
            val zipFileName = inputName.getText()
            if (zipFileName.isEmpty()) {
                inputName.setText("Please write name here! Click to edit.")
                giveNameText.setColor(Color.RED)
                // close menu

            } else {
                // Call function to create config zip
            }
            giveNameText.setColor(Color.RED)
        }.constrain {
            x = 15.pixels()
            y = 5.pixels(true)
            width = 30.pixels()
        } childOf confirmPopUp

        CreateMenuButton("No") {
            confirmPopUp.hide(true)
            giveNameText.setColor(Color.RED)
        }.constrain {
            x = 15.pixels(true)
            y = 5.pixels(true)
            width = 30.pixels()
        } childOf confirmPopUp

        }
    }
