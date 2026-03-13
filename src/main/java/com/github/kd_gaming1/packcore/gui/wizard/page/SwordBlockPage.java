package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.component.OptionCardGrid;
import com.github.kd_gaming1.packcore.gui.wizard.BaseCardGridPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/** Sword Block toggle — lets the user enable or disable ScaleMe's sword block animation. */
public class SwordBlockPage extends BaseCardGridPage<SwordBlockPage.SwordBlockOption> {

    public static final String STATE_KEY = "swordBlock";

    public SwordBlockPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override public Component getTitle()       { return Component.translatable("gui.packcore.wizard.page.sword_block.title"); }
    @Override protected String stateKey()       { return STATE_KEY; }
    @Override protected int columns()           { return 2; }
    @Override protected Component explanation() { return Component.translatable("gui.packcore.wizard.page.sword_block.explanation"); }
    @Override protected List<SwordBlockOption> options() { return SwordBlockOption.all(); }

    @Override
    protected OptionCardGrid.CardDescriptor<SwordBlockOption> descriptor() {
        return OptionCardGrid.CardDescriptor.of(
                SwordBlockOption::id, SwordBlockOption::name, SwordBlockOption::description,
                SwordBlockOption::previewTexture, SwordBlockOption::previewTextureWidth, SwordBlockOption::previewTextureHeight
        );
    }

    public record SwordBlockOption(
            String id, Component name, Component description,
            Identifier previewTexture, int previewTextureWidth, int previewTextureHeight
    ) {
        public static List<SwordBlockOption> all() {
            return List.of(fromId("enabled"), fromId("disabled"));
        }

        private static SwordBlockOption fromId(String id) {
            return new SwordBlockOption(
                    id,
                    Component.translatable("gui.packcore.wizard.sword_block." + id + ".name"),
                    Component.translatable("gui.packcore.wizard.sword_block." + id + ".desc"),
                    Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/sprites/wizard/sword_block_preview/" + id + ".png"),
                    320, 180
            );
        }
    }
}