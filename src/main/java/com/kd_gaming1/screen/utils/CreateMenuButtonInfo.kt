package com.kd_gaming1.screen.utils

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import java.awt.Color

fun CreateMenuButtonInfo(text: String, action: () -> Unit) = UIContainer().constrain {
    x = CenterConstraint()
    y = SiblingConstraint()
    width = 64.pixels()
    height = 18.pixels()
}.also { button ->

    // Add text to button
    val buttonText = UIText(text).constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        textScale = 1.pixels()
        color = Color.WHITE.toConstraint()
    } childOf button

    val underline = UIRoundedRectangle(6f).constrain {
        x = CenterConstraint()
        y = CenterConstraint() + 8.pixels()
        width = CopyConstraintFloat().to(buttonText) as WidthConstraint + 16.pixels()
        height = 1.pixels()
        color = Color(252, 189, 56, 0).toConstraint()
    } childOf buttonText

    button.onMouseClick {
        action()
    }.onMouseEnter {
        buttonText.animate {
            setTextScaleAnimation(Animations.OUT_EXP, 0.3f, 1.pixels())
        }
        underline.animate {
            setColorAnimation(Animations.OUT_EXP, 0.5f, Color(252, 189, 56, 255).toConstraint())
        }
    }.onMouseLeave {
        buttonText.animate {
            setTextScaleAnimation(Animations.OUT_EXP, 0.3f, 1.pixels())
        }
        underline.animate {
            setColorAnimation(Animations.OUT_EXP, 0.5f, Color(252, 189, 56, 0).toConstraint())
        }
    }
}
