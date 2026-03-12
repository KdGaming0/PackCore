package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.component.OptionCardGrid;
import com.github.kd_gaming1.packcore.gui.wizard.BaseCardGridPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/** Step 1 — Main Menu Design chooser. */
public class MainMenuDesignPage extends BaseCardGridPage<MainMenuDesignPage.MenuDesignOption> {

    public static final String STATE_KEY = "mainMenuDesign";

    private static final int PREVIEW_WIDTH  = 320;
    private static final int PREVIEW_HEIGHT = 180;

    public MainMenuDesignPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override public Component getTitle()        { return Component.translatable("gui.packcore.wizard.page.main_menu_design.title"); }
    @Override protected String stateKey()        { return STATE_KEY; }
    @Override protected int columns()            { return 3; }
    @Override protected Component explanation()  { return Component.translatable("gui.packcore.wizard.page.main_menu_design.explanation"); }
    @Override protected List<MenuDesignOption> options() { return MenuDesignOption.all(); }

    @Override
    protected OptionCardGrid.CardDescriptor<MenuDesignOption> descriptor() {
        return OptionCardGrid.CardDescriptor.of(
                MenuDesignOption::id, MenuDesignOption::name, MenuDesignOption::description,
                MenuDesignOption::previewTexture, MenuDesignOption::previewTextureWidth, MenuDesignOption::previewTextureHeight
        );
    }

    public record MenuDesignOption(
            String id, Component name, Component description,
            Identifier previewTexture, int previewTextureWidth, int previewTextureHeight
    ) {
        public static List<MenuDesignOption> all() {
            return List.of(fromId("vanilla"), fromId("modern"), fromId("minimal"));
        }

        private static MenuDesignOption fromId(String id) {
            return new MenuDesignOption(
                    id,
                    Component.translatable("gui.packcore.wizard.menu_design." + id + ".name"),
                    Component.translatable("gui.packcore.wizard.menu_design." + id + ".desc"),
                    Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/wizard/menu_preview/" + id + ".png"),
                    PREVIEW_WIDTH, PREVIEW_HEIGHT
            );
        }
    }
}