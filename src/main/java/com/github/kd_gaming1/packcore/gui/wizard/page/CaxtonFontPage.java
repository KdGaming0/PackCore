package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.github.kd_gaming1.packcore.gui.component.OptionSelectList;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;

import java.util.List;

/**
 * Wizard step — lets the user pick a Caxton font resource pack, or keep the default font.
 */
public class CaxtonFontPage extends BaseWizardPage {

    public static final String STATE_KEY = "caxtonFont";

    private static final Component PAGE_TITLE =
            Component.translatable("gui.packcore.wizard.page.caxton_font.title");

    private static final int PADDING = 16;
    private static final int LABEL_GAP = 8;
    private static final int COLOR_LABEL = 0xFFCCCCCC;

    // ── Font options ──────────────────────────────────────────────────────────

    public record FontOption(String id, String packId, Component name, Component description) {

        public static final String NONE_ID = "none";

        public static List<FontOption> all() {
            return List.of(
                    none(),
                    fromId("open_sans", "caxton:opensans"),
                    fromId("inter", "caxton:inter")
            );
        }

        private static FontOption none() {
            return new FontOption(
                    NONE_ID,
                    null,
                    Component.translatable("gui.packcore.wizard.caxton_font.none.name"),
                    Component.translatable("gui.packcore.wizard.caxton_font.none.desc"));
        }

        private static FontOption fromId(String id, String packId) {
            return new FontOption(
                    id,
                    packId,
                    Component.translatable("gui.packcore.wizard.caxton_font." + id + ".name"),
                    Component.translatable("gui.packcore.wizard.caxton_font." + id + ".desc"));
        }
    }

    public CaxtonFontPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override
    public Component getTitle() {
        return PAGE_TITLE;
    }

    @Override
    public boolean validate() {
        return true;
    }

    @Override
    public void onExit() {}

    @Override
    public void onEnter() {
        clearComponents();

        // Preselect current enabled Caxton pack (if any)
        String enabledPackId = Minecraft.getInstance()
                .getResourcePackRepository()
                .getSelectedPacks()
                .stream()
                .map(Pack::getId)
                .filter(id -> FontOption.all().stream().anyMatch(opt -> id.equals(opt.packId())))
                .findFirst()
                .orElse(null);

        if (enabledPackId == null) {
            // Explicitly select "none" so it shows as selected in the UI
            state.setSelection(STATE_KEY, FontOption.NONE_ID);
        } else {
            String optionId = FontOption.all().stream()
                    .filter(opt -> enabledPackId.equals(opt.packId()))
                    .map(FontOption::id)
                    .findFirst()
                    .orElse(FontOption.NONE_ID);
            state.setSelection(STATE_KEY, optionId);
        }

        int availableWidth = getWidth() - PADDING * 2;
        int availableHeight = getHeight() - PADDING * 2;

        var font = Minecraft.getInstance().font;
        int labelHeight = font.lineHeight + LABEL_GAP;

        EmptyComponent container = new EmptyComponent(PADDING, PADDING, availableWidth, availableHeight);

        container.addComponent(new TextComponent(
                0, 0,
                Component.translatable("gui.packcore.wizard.caxton_font.label"),
                COLOR_LABEL));

        int listHeight = availableHeight - labelHeight;

        OptionSelectList<FontOption> fontList = new OptionSelectList<>(
                0, labelHeight,
                availableWidth, listHeight,
                FontOption.all(),
                OptionSelectList.RowDescriptor.of(
                        FontOption::id,
                        FontOption::name,
                        FontOption::description),
                state.getSelection(STATE_KEY),
                selected -> {
                    if (selected == null) return;
                    state.setSelection(STATE_KEY, selected.id());
                });

        container.addComponent(fontList);
        addComponent(container);
    }
}