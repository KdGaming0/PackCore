package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.component.OptionCardGrid;
import com.github.kd_gaming1.packcore.gui.wizard.BaseCardGridPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/** Step 5 — Storage Design chooser. */
public class StorageDesignPage extends BaseCardGridPage<StorageDesignPage.StorageDesignOption> {

    public static final String STATE_KEY = "storageDesign";

    public StorageDesignPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override public Component getTitle()        { return Component.translatable("gui.packcore.wizard.page.storage_design.title"); }
    @Override protected String stateKey()        { return STATE_KEY; }
    @Override protected int columns()            { return 2; }
    @Override protected Component explanation()  { return Component.translatable("gui.packcore.wizard.page.storage_design.explanation"); }
    @Override protected List<StorageDesignOption> options() { return StorageDesignOption.all(); }

    @Override
    protected OptionCardGrid.CardDescriptor<StorageDesignOption> descriptor() {
        return OptionCardGrid.CardDescriptor.of(
                StorageDesignOption::id, StorageDesignOption::name, StorageDesignOption::description,
                StorageDesignOption::previewTexture, StorageDesignOption::previewTextureWidth, StorageDesignOption::previewTextureHeight
        );
    }

    public record StorageDesignOption(
            String id, Component name, Component description,
            Identifier previewTexture, int previewTextureWidth, int previewTextureHeight
    ) {
        public static List<StorageDesignOption> all() {
            return List.of(fromId("overlay"), fromId("vanilla"));
        }

        private static StorageDesignOption fromId(String id) {
            return new StorageDesignOption(
                    id,
                    Component.translatable("gui.packcore.wizard.storage_design." + id + ".name"),
                    Component.translatable("gui.packcore.wizard.storage_design." + id + ".desc"),
                    Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/sprites/wizard/storage_preview/" + id + ".png"),
                    320, 180
            );
        }
    }
}