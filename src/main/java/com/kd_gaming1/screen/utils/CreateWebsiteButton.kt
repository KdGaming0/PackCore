package com.kd_gaming1.screen.utils

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import java.awt.Color
import net.minecraft.util.Util

fun CreateWebsiteButton(text: String, url: String) = UIContainer().constrain {
    x = SiblingConstraint()
    width = 38.pixels()
    height = 12.pixels()
}.also { button ->
    val underline = UIRoundedRectangle(6f).constrain {
        x = CenterConstraint()
        y = CenterConstraint() + 5.pixels()
        width = 36.pixels()
        height = 1.pixels()
        color = Color(252, 189, 56, 0).toConstraint()
    } childOf button

    UIText(text).constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        textScale = 0.7.pixels()
        color = Color.WHITE.toConstraint()
    } childOf button

    button.onMouseEnter {
        underline.animate {
            setColorAnimation(Animations.OUT_EXP, 0.5f, Color(252, 189, 56, 255).toConstraint())
        }
    }.onMouseLeave {
        underline.animate {
            setColorAnimation(Animations.OUT_EXP, 0.5f, Color(252, 189, 56, 0).toConstraint())
        }
    }.onMouseClick {
        try {
            Util.getOperatingSystem().open(url)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}