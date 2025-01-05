package com.github.kdgaming0.packcore.screen

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIImage
import gg.essential.elementa.constraints.ImageAspectConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus

class MainMenu : WindowScreen(ElementaVersion.V7) {
    init {
        UIImage.ofResource("/assets/packcore/textures/gui/background.png").constrain {
            x = 2.pixels()
            y = SiblingConstraint() + 5.pixels()

            width = ImageAspectConstraint()
            height = ImageAspectConstraint()
        } childOf window






    }
}