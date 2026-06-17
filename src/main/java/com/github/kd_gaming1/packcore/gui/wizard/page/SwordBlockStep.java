package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.SummaryRow;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.gui.wizard.WizardStep;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Step: sword block — toggles ScaleMe's sword block setting via reflection. */
public final class SwordBlockStep implements WizardStep {

    @Override public String id() { return "sword_block"; }
    @Override public int version() { return 1; }

    @Override
    public boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded("scaleme");
    }

    @Override
    public BaseWizardPage createPage(WizardState state, WizardNavigator navigator, int width, int height) {
        return new SwordBlockPage(state, navigator, width, height);
    }

    @Override
    public List<SummaryRow> summaryRows(WizardState state) {
        return List.of(SummaryRow.single(id(), "Sword Block",
                "gui.packcore.wizard.sword_block.", state.getSelection(SwordBlockPage.STATE_KEY)));
    }

    @Override
    public void apply(WizardState state) {
        String selectedId = state.getSelection(SwordBlockPage.STATE_KEY);
        if (selectedId == null) return;
        try {
            Class<?> configClass = Class.forName("com.github.kd_gaming1.scaleme.config.ScaleMeConfig");
            Field field = configClass.getDeclaredField("enableSwordBlock");
            field.setAccessible(true);
            field.setBoolean(null, selectedId.equals("enabled"));

            Class<?> midnightConfigClass = Class.forName("eu.midnightdust.lib.config.MidnightConfig");
            Method writeMethod = midnightConfigClass.getMethod("write", String.class);
            writeMethod.invoke(null, "scaleme");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to apply ScaleMe sword block setting", e);
        }
    }
}
