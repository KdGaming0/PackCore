package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.component.OptionCardGrid;
import com.github.kd_gaming1.packcore.gui.wizard.BaseCardGridPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/** Step 3 — Tab Design chooser. */
public class TabDesignPage extends BaseCardGridPage<TabDesignPage.TabDesignOption> {

    public static final String STATE_KEY = "tabDesign";

    public TabDesignPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override public Component getTitle()        { return Component.translatable("gui.packcore.wizard.page.tab_design.title"); }
    @Override protected String stateKey()        { return STATE_KEY; }
    @Override protected int columns()            { return 2; }
    @Override protected Component explanation()  { return Component.translatable("gui.packcore.wizard.page.tab_design.explanation"); }
    @Override protected List<TabDesignOption> options() { return TabDesignOption.all(); }

    @Override
    protected OptionCardGrid.CardDescriptor<TabDesignOption> descriptor() {
        return OptionCardGrid.CardDescriptor.of(
                TabDesignOption::id, TabDesignOption::name, TabDesignOption::description,
                TabDesignOption::previewTexture, TabDesignOption::previewTextureWidth, TabDesignOption::previewTextureHeight
        );
    }

    public record TabDesignOption(
            String id, Component name, Component description,
            Identifier previewTexture, int previewTextureWidth, int previewTextureHeight
    ) {
        public static List<TabDesignOption> all() {
            return List.of(fromId("compact"), fromId("fancy"));
        }

        private static TabDesignOption fromId(String id) {
            return new TabDesignOption(
                    id,
                    Component.translatable("gui.packcore.wizard.tab_design." + id + ".name"),
                    Component.translatable("gui.packcore.wizard.tab_design." + id + ".desc"),
                    Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/wizard/tab_preview/" + id + ".png"),
                    320, 180
            );
        }
    }
}