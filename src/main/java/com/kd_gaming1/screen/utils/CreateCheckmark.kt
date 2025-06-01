package com.kd_gaming1.screen.utils

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import java.awt.Color
import kotlin.reflect.KMutableProperty0

fun CreateCheckmark(text: String, isChecked: KMutableProperty0<Boolean>) = UIContainer().constrain {
    x = CenterConstraint()
    y = SiblingConstraint()
    width = 128.pixels()
    height = 20.pixels()
}.also { button ->

    val horizontalContainer = UIContainer().constrain {
        x = 0.pixels()
        y = CenterConstraint()
        width = 100.percent()
        height = 100.percent()
    } childOf button

    val checkbox = UIBlock().constrain {
        x = 0.pixels()
        y = CenterConstraint()
        width = 15.pixels()
        height = 15.pixels()
        color = Color.DARK_GRAY.toConstraint()
    }.effect(
        OutlineEffect(
            Color(0, 0, 0, 100),
            1f,
            drawInsideChildren = true
        )
    ) childOf horizontalContainer

    val checkmark = UIText("✓").constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        color = Color.WHITE.toConstraint()
    }

    val buttonText = UIText(text).constrain {
        x = SiblingConstraint(padding = 7f)
        y = CenterConstraint()
        textScale = 1.pixels()
        color = Color.WHITE.toConstraint()
    } childOf horizontalContainer

    val underline = UIRoundedRectangle(6f).constrain {
        x = CenterConstraint()
        y = CenterConstraint() + 8.pixels()
        width = CopyConstraintFloat().to(buttonText) as WidthConstraint + 16.pixels()
        height = 1.pixels()
        color = Color(252, 189, 56, 0).toConstraint()
    } childOf buttonText

    button.onMouseClick {
        isChecked.set(!isChecked.get())
        if (isChecked.get()) {
            checkbox.addChild(checkmark)
        } else {
            checkbox.removeChild(checkmark)
        }
    }.onMouseEnter {
        underline.animate {
            setColorAnimation(Animations.OUT_EXP, 0.5f, Color(252, 189, 56, 255).toConstraint())
        }
    }.onMouseLeave {
        underline.animate {
            setColorAnimation(Animations.OUT_EXP, 0.5f, Color(252, 189, 56, 0).toConstraint())
        }
    }

    if (isChecked.get()) {
        checkbox.addChild(checkmark)
    }
}