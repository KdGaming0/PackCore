package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.component.OptionCardGrid;
import com.github.kd_gaming1.packcore.gui.wizard.BaseCardGridPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * Dungeon Routes chooser — lets the user pick between Skyblocker's built-in
 * dungeon secret waypoints or Stella's dungeon routes.
 */
public class DungeonRoutesPage extends BaseCardGridPage<DungeonRoutesPage.DungeonRoutesOption> {

    public static final String STATE_KEY = "dungeonRoutes";

    private static final int PREVIEW_WIDTH  = 320;
    private static final int PREVIEW_HEIGHT = 180;

    public DungeonRoutesPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override public Component getTitle()        { return Component.translatable("gui.packcore.wizard.page.dungeon_routes.title"); }
    @Override protected String stateKey()        { return STATE_KEY; }
    @Override protected int columns()            { return 2; }
    @Override protected Component explanation()  { return Component.translatable("gui.packcore.wizard.page.dungeon_routes.explanation"); }
    @Override protected List<DungeonRoutesOption> options() { return DungeonRoutesOption.all(); }

    @Override
    protected OptionCardGrid.CardDescriptor<DungeonRoutesOption> descriptor() {
        return OptionCardGrid.CardDescriptor.of(
                DungeonRoutesOption::id, DungeonRoutesOption::name, DungeonRoutesOption::description,
                DungeonRoutesOption::previewTexture, DungeonRoutesOption::previewTextureWidth, DungeonRoutesOption::previewTextureHeight,
                DungeonRoutesOption::recommended
        );
    }

    public record DungeonRoutesOption(
            String id, Component name, Component description,
            Identifier previewTexture, int previewTextureWidth, int previewTextureHeight,
            boolean recommended
    ) {
        public static List<DungeonRoutesOption> all() {
            return List.of(fromId("skyblocker_waypoints", true), fromId("stella", false));
        }

        private static DungeonRoutesOption fromId(String id, boolean recommended) {
            return new DungeonRoutesOption(
                    id,
                    Component.translatable("gui.packcore.wizard.dungeon_routes." + id + ".name"),
                    Component.translatable("gui.packcore.wizard.dungeon_routes." + id + ".desc"),
                    Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/sprites/wizard/dungeon_routes_preview/" + id + ".png"),
                    PREVIEW_WIDTH, PREVIEW_HEIGHT,
                    recommended
            );
        }
    }
}
