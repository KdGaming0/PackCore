package com.github.kd_gaming1.packcore.lavendermd;

import io.wispforest.lavendermd.compiler.OwoUICompiler;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CustomLavenderCompiler extends OwoUICompiler {

    public CustomLavenderCompiler() {
        super();
    }

    @Override
    public void visitImage(Identifier image, String description, boolean fit) {
        if (fit) {
            this.append(Containers.stack(Sizing.fill(100), Sizing.content())
                    .child(Components.texture(image, 0, 0, 256, 256, 256, 256)
                            .blend(true)
                            .tooltip(Text.literal(description))
                            .sizing(Sizing.fill(100), Sizing.content()))
                    .horizontalAlignment(HorizontalAlignment.CENTER));
        } else {
            super.visitImage(image, description, fit);
        }
    }
}
