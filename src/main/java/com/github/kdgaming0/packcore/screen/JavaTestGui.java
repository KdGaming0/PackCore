package com.github.kdgaming0.packcore.screen;

import gg.essential.elementa.ElementaVersion;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.WindowScreen;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.UIImage;
import gg.essential.elementa.components.UIText;
import gg.essential.elementa.constraints.CenterConstraint;
import gg.essential.elementa.constraints.ChildBasedSizeConstraint;
import gg.essential.elementa.constraints.PixelConstraint;
import gg.essential.elementa.constraints.RelativeConstraint;
import gg.essential.elementa.constraints.animation.AnimatingConstraints;
import gg.essential.elementa.constraints.animation.Animations;
import gg.essential.elementa.effects.ScissorEffect;

import java.awt.*;

public class JavaTestGui extends WindowScreen {

    UIImage background = UIImage.ofResource("/assets/packcore/textures/gui/background.png");
    UIText title = new UIText("Skyblock Enhanced");
    UIBlock buttonBlock = new UIBlock(new Color(64, 64, 255));


    public JavaTestGui() {
        super(ElementaVersion.V7);
        background
                .setX(new PixelConstraint(0f)) // Start from left edge
                .setY(new PixelConstraint(0f)) // Start from top edge
                .setWidth(new RelativeConstraint(1f)) // 100% of window width (1.0 = 100%)
                .setHeight(new RelativeConstraint(1f)) // 100% of window height (1.0 = 100%)
                .setChildOf(getWindow());

        title
                .setX(new CenterConstraint())
                .setY(new PixelConstraint(15f))
                .setTextScale(new PixelConstraint(1.5f))
                .setColor(new Color(255, 255, 255))
                .setChildOf(background);


    }
}
