package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.github.kd_gaming1.packcore.gui.component.OptionSelectList;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Set;

/**
 * Wizard step — lets the user pick a Caxton font resource pack, or keep the default font.
 *
 * <p>Shows a warning banner when the user has selected a shader-based performance profile
 * on the Performance page, or when Iris shaders are already active, since Caxton fonts
 * are incompatible with shaders.
 */
public class CaxtonFontPage extends BaseWizardPage {

    public static final String STATE_KEY = "caxtonFont";

    private static final Component PAGE_TITLE =
            Component.translatable("gui.packcore.wizard.page.caxton_font.title");

    private static final int PADDING = 16;
    private static final int LABEL_GAP = 8;
    private static final int COLOR_WARNING = 0xFFFF5555;
    private static final int COLOR_WARNING_BG = 0x99330000;
    private static final int WARNING_HEIGHT = 24;
    private static final int WARNING_PADDING = 4;
    private static final int COLOR_LABEL = 0xFFCCCCCC;

    /**
     * Performance-page profile IDs that enable shaders.
     * Keep in sync with {@link PerformancePage.PerformanceProfile#all()}.
     */
    private static final Set<String> SHADER_PROFILE_IDS = Set.of(
            "quality_performance_shaders",
            "quality_quality_shaders"
    );

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

        int contentOffsetY = labelHeight;
        int contentHeight = availableHeight - labelHeight;

        // ── Shader conflict warning banner ────────────────────────────────────
        // Show the warning when the user picked a shader profile on the Performance page,
        // OR when Iris shaders are already active in the running game.
        boolean shaderSelected = isShaderProfileSelected();
        boolean shadersLive = isIrisShadersActiveNow();

        if (shaderSelected || shadersLive) {
            Component warningText = Component.literal(
                    "⚠  " + (shaderSelected
                            ? "You selected a shader profile — Caxton fonts are incompatible with shaders and will render incorrectly."
                            : "Shaders are active — Caxton fonts are incompatible with shaders and will render incorrectly."));

            int textWidth = font.width(warningText.getString());
            int bannerHeight = (textWidth > availableWidth - WARNING_PADDING * 2)
                    ? (font.lineHeight * 2 + WARNING_PADDING * 3)
                    : WARNING_HEIGHT;

            ShaderWarningBanner banner = new ShaderWarningBanner(
                    0, contentOffsetY, availableWidth, bannerHeight, warningText);
            container.addComponent(banner);

            contentOffsetY += bannerHeight + LABEL_GAP;
            contentHeight -= bannerHeight + LABEL_GAP;
        }

        OptionSelectList<FontOption> fontList = new OptionSelectList<>(
                0, contentOffsetY,
                availableWidth, contentHeight,
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

    /**
     * Checks the wizard state to see if the user chose a shader-based performance profile.
     */
    private boolean isShaderProfileSelected() {
        String perfSelection = state.getSelection(PerformancePage.STATE_KEY);
        return perfSelection != null && SHADER_PROFILE_IDS.contains(perfSelection);
    }

    /**
     * Reflection check: is an Iris shader pack currently in use?
     * Uses {@code Iris.isPackInUseQuick()} which returns true when the active
     * pipeline is an {@code IrisRenderingPipeline} (i.e. shaders are loaded).
     */
    private static boolean isIrisShadersActiveNow() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) return false;
        try {
            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            return (boolean) irisClass.getMethod("isPackInUseQuick").invoke(null);
        } catch (Exception ignored) {
            return false;
        }
    }

    // ── ShaderWarningBanner ───────────────────────────────────────────────────

    private static class ShaderWarningBanner extends EmptyComponent {

        private final Component text;

        ShaderWarningBanner(int x, int y, int width, int height, Component text) {
            super(x, y, width, height);
            this.text = text;
        }

        //? if >=26.1 {
        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                           float partialTick, int parentWidth, int parentHeight) {
        //?} else {
        /*@Override
        public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                           float partialTick, int parentWidth, int parentHeight) {
         *///?}
            int x = getTotalX(), y = getTotalY(), w = getWidth(), h = getHeight();
            var font = Minecraft.getInstance().font;

            graphics.fill(x, y, x + w, y + h, COLOR_WARNING_BG);
            graphics.fill(x, y, x + 3, y + h, COLOR_WARNING);

            int textX = x + 3 + WARNING_PADDING;
            int maxTextWidth = w - 3 - WARNING_PADDING * 2;
            List<FormattedCharSequence> lines = font.split(text, maxTextWidth);

            int totalTextHeight = lines.size() * font.lineHeight + (lines.size() - 1) * 2;
            int textY = y + (h - totalTextHeight) / 2;
            for (FormattedCharSequence line : lines) {
                //? if >=26.1 {
                graphics.text(font, line, textX, textY, COLOR_WARNING, false);
                //?} else {
                 /*graphics.drawString(font, line, textX, textY, COLOR_WARNING, false);
                *///?}
                textY += font.lineHeight + 2;
            }

            super.extractRenderState(graphics, mouseX, mouseY, partialTick, parentWidth, parentHeight);
        }
    }
}