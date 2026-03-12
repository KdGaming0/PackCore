package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.component.OptionCardGrid;
import com.github.kd_gaming1.packcore.gui.wizard.BaseCardGridPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/** Step 4 — Item Background Design chooser. */
public class ItemBackgroundPage extends BaseCardGridPage<ItemBackgroundPage.ItemBackgroundOption> {

    public static final String STATE_KEY = "itemBackground";

    public ItemBackgroundPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override public Component getTitle()        { return Component.translatable("gui.packcore.wizard.page.item_background.title"); }
    @Override protected String stateKey()        { return STATE_KEY; }
    @Override protected int columns()            { return 3; }
    @Override protected Component explanation()  { return Component.translatable("gui.packcore.wizard.page.item_background.explanation"); }
    @Override protected List<ItemBackgroundOption> options() { return ItemBackgroundOption.all(); }

    @Override
    protected OptionCardGrid.CardDescriptor<ItemBackgroundOption> descriptor() {
        return OptionCardGrid.CardDescriptor.of(
                ItemBackgroundOption::id, ItemBackgroundOption::name, ItemBackgroundOption::description,
                ItemBackgroundOption::previewTexture, ItemBackgroundOption::previewTextureWidth, ItemBackgroundOption::previewTextureHeight
        );
    }

    public record ItemBackgroundOption(
            String id, Component name, Component description,
            Identifier previewTexture, int previewTextureWidth, int previewTextureHeight
    ) {
        public static List<ItemBackgroundOption> all() {
            return List.of(fromId("none"), fromId("circle"), fromId("square"));
        }

        private static ItemBackgroundOption fromId(String id) {
            return new ItemBackgroundOption(
                    id,
                    Component.translatable("gui.packcore.wizard.item_background." + id + ".name"),
                    Component.translatable("gui.packcore.wizard.item_background." + id + ".desc"),
                    Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/wizard/item_background_preview/" + id + ".png"),
                    320, 180
            );
        }
    }
}