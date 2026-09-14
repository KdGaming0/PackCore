package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.gui.component.OptionSelectList;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class PingOffsetMinerPage extends BaseWizardPage {
    public static final String STATE_KEY = "pingOffsetMiner";
    private static final int PADDING = 16;

    public PingOffsetMinerPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override public Component getTitle() { return Component.translatable("gui.packcore.wizard.page.ping_offset_miner.title"); }
    @Override public boolean validate() { return true; }
    @Override public void onExit() {}

    @Override
    public void onEnter() {
        clearComponents();
        if (state.getSelection(STATE_KEY) == null) {
            try {
                state.setSelection(STATE_KEY, PingOffsetMinerStep.isEnabled() ? "enabled" : "disabled");
            } catch (ReflectiveOperationException e) {
                PackCore.LOGGER.warn("Could not read Ping Offset Miner setting", e);
            }
        }

        int width = getWidth() - PADDING * 2;
        MultiLineTextComponent intro = new MultiLineTextComponent(PADDING, PADDING, width,
                Component.translatable("gui.packcore.wizard.page.ping_offset_miner.explanation"), 0xFF777777);
        addComponent(intro);
        int listY = PADDING + intro.getHeight() + 10;
        addComponent(new OptionSelectList<>(PADDING, listY, width, Math.max(1, getHeight() - listY - PADDING),
                List.of("enabled", "disabled"),
                OptionSelectList.RowDescriptor.of(
                        id -> id,
                        id -> Component.translatable("gui.packcore.wizard.ping_offset_miner." + id + ".name"),
                        id -> Component.translatable("gui.packcore.wizard.ping_offset_miner." + id + ".desc")),
                state.getSelection(STATE_KEY), selected -> state.setSelection(STATE_KEY, selected)));
    }
}
