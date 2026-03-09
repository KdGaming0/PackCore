package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import com.github.kd_gaming1.packcore.gui.component.OptionCardGrid;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * Step 1 — Main Menu Design chooser.
 */
public class MainMenuDesignPage extends BaseWizardPage {

    private static final Component PAGE_TITLE = Component.translatable("gui.packcore.wizard.page.main_menu_design.title");
    private static final Component PAGE_SUBTITLE = Component.translatable("gui.packcore.wizard.page.main_menu_design.subtitle");
    private static final Component EXPLANATION = Component.translatable("gui.packcore.wizard.page.main_menu_design.explanation");

    public static final String STATE_KEY = "mainMenuDesign";

    private static final int OUTER_PADDING = 16;
    private static final int GRID_COLUMNS = 3;
    private static final int CARD_GAP = 10;
    private static final int EXPLANATION_GAP = 8;
    private static final int COLOR_EXPLANATION = 0xFFAAAAAA;

    private static final int PREVIEW_WIDTH = 320;
    private static final int PREVIEW_HEIGHT = 180;

    public MainMenuDesignPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override public Component getTitle() { return PAGE_TITLE; }
    @Override public Component getSubtitle() { return PAGE_SUBTITLE; }
    @Override public boolean validate() { return true; }
    @Override public void onExit() { }
    @Override public boolean canSkip() { return true; }

    @Override
    public void onEnter() {
        this.clearComponents();

        int availableWidth = getWidth() - OUTER_PADDING * 2;

        MultiLineTextComponent explanation = new MultiLineTextComponent(
                OUTER_PADDING, OUTER_PADDING, availableWidth, EXPLANATION, COLOR_EXPLANATION
        );
        this.addComponent(explanation);

        int gridTop = OUTER_PADDING + explanation.getHeight() + EXPLANATION_GAP;
        int gridHeight = getHeight() - gridTop - OUTER_PADDING;

        OptionCardGrid<MenuDesignOption> grid = new OptionCardGrid<>(
                OUTER_PADDING, gridTop,
                availableWidth, gridHeight,
                GRID_COLUMNS, CARD_GAP,
                MenuDesignOption.all(),
                OptionCardGrid.CardDescriptor.of(
                        MenuDesignOption::id,
                        MenuDesignOption::name,
                        MenuDesignOption::description,
                        MenuDesignOption::previewTexture,
                        MenuDesignOption::previewTextureWidth,
                        MenuDesignOption::previewTextureHeight
                ),
                state.getSelection(STATE_KEY),
                selected -> state.setSelection(STATE_KEY, selected != null ? selected.id() : null)
        );

        this.addComponent(grid);
    }

    public record MenuDesignOption(
            String id,
            Component name,
            Component description,
            Identifier previewTexture,
            int previewTextureWidth,
            int previewTextureHeight
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