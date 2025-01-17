package com.github.kdgaming0.packcore.screen.utils

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import java.awt.Color

fun CreateWebsiteButton(text: String, url: String) = UIContainer().constrain {
    x = SiblingConstraint(padding = 1f)
    width = 40.pixels()
    height = 12.pixels()
}.also { button ->
    // Create reference to underline component that we can animate
    val underline = UIRoundedRectangle(6f).constrain {
        x = CenterConstraint()
        y = CenterConstraint() + 5.pixels()
        width = 38.pixels()
        height = 1.pixels()
        color = Color(252, 189, 56, 0).toConstraint()
    } childOf button

    // Add text to button
    UIText(text).constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        textScale = 0.7.pixels()
        color = Color.WHITE.toConstraint()
    } childOf button

    // Move hover animations to the button
    button.onMouseEnter {
        // Animate the underline from the button's hover
        underline.animate {
            setColorAnimation(Animations.OUT_EXP, 0.5f, Color(252, 189, 56, 255).toConstraint())
        }
    }.onMouseLeave {
        // Reset underline animation when mouse leaves button
        underline.animate {
            setColorAnimation(Animations.OUT_EXP, 0.5f, Color(252, 189, 56, 0).toConstraint())
        }
    }.onMouseClick {
        try {
            val desktop = java.awt.Desktop.getDesktop()
            desktop.browse(java.net.URI(url))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}