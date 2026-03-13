package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.gui.component.MultiSelectList;
import com.github.kd_gaming1.packcore.gui.component.MarkdownComponent;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.util.JvmArgs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Step 6 — Resource Pack chooser.
 */
public class ResourcePackPage extends BaseWizardPage {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ResourcePackPage");

    private static final Component PAGE_TITLE = Component.translatable("gui.packcore.wizard.page.resource_pack.title");

    private static final int PADDING = 16;
    private static final int COLUMN_GAP = 14;
    private static final int SCROLL_BAR_WIDTH = 8;

    /** Minimum thread stack size required by Hypixel+ (4 MB). */
    private static final long XSS_THRESHOLD_BYTES = 4L * 1024 * 1024;

    private static final String FALLBACK_MARKDOWN = "*No resource pack guide found.*";
    private static final Path MARKDOWN_PATH = PackCore.PACKCORE_DIR.resolve("markdown").resolve("resource_packs.md");

    public ResourcePackPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override public Component getTitle() { return PAGE_TITLE; }
    @Override public boolean validate() { return true; }
    @Override public void onExit() { }

    @Override
    public void onEnter() {
        this.clearComponents();

        int availableWidth  = getWidth()  - (PADDING * 2);
        int availableHeight = getHeight() - (PADDING * 2);
        int columnWidth = (availableWidth - COLUMN_GAP) / 2;

        EmptyComponent leftColumn  = new EmptyComponent(PADDING, PADDING, columnWidth, availableHeight);
        EmptyComponent rightColumn = new EmptyComponent(PADDING + columnWidth + COLUMN_GAP, PADDING, columnWidth, availableHeight);

        setupMarkdownColumn(leftColumn, columnWidth, availableHeight);
        setupSelectionColumn(rightColumn, columnWidth, availableHeight);

        this.addComponent(leftColumn);
        this.addComponent(rightColumn);
    }

    private void setupMarkdownColumn(EmptyComponent column, int width, int height) {
        MarkdownComponent markdownComp = new MarkdownComponent(
                0, 0, width - SCROLL_BAR_WIDTH - (PADDING / 2),
                GuiHelper.loadMarkdown(MARKDOWN_PATH, FALLBACK_MARKDOWN, LOGGER)
        );
        column.addComponent(GuiHelper.scrollWrapped(0, 0, width, height,
                scroll -> scroll.addComponent(markdownComp)));
    }

    private void setupSelectionColumn(EmptyComponent column, int width, int height) {
        boolean missingXss = !JvmArgs.hasXssAtLeast(XSS_THRESHOLD_BYTES);
        List<ResourcePackEntry> packs = discoverUserPacks(missingXss);

        if (packs.isEmpty()) {
            column.addComponent(new MultiLineTextComponent(
                    0, 0, width,
                    Component.translatable("gui.packcore.wizard.resource_pack.none_found"),
                    0xFF777777
            ));
            return;
        }

        // If -Xss4M is missing, show a banner above the list explaining how to fix it.
        int listOffsetY = 0;
        if (missingXss && packs.stream().anyMatch(ResourcePackEntry::requiresXss)) {
            JvmArgs.Launcher launcher = JvmArgs.detectLauncher();
            Component banner = buildXssBanner(launcher);

            // MultiLineTextComponent calculates its own height based on wrapped lines —
            // read it back after construction rather than manually counting newlines.
            MultiLineTextComponent bannerComp = new MultiLineTextComponent(0, 0, width, banner, 0xFFFFAA00);
            column.addComponent(bannerComp);
            listOffsetY = bannerComp.getHeight() + 6;
            height -= listOffsetY;
        }

        MultiSelectList<ResourcePackEntry> list = new MultiSelectList<>(
                0, listOffsetY, width, height,
                packs,
                MultiSelectList.RowDescriptor.of(
                        ResourcePackEntry::id,
                        ResourcePackEntry::name,
                        ResourcePackEntry::description
                ),
                state.getSelectedResourcePacks(),
                selected   -> state.addResourcePack(selected.id()),
                deselected -> state.removeResourcePack(deselected.id())
        );
        column.addComponent(list);
    }

    /**
     * Builds a multi-line warning banner using translatable components so the
     * text is localisation-friendly. The launcher name is injected as a %s arg.
     */
    private static Component buildXssBanner(JvmArgs.Launcher launcher) {
        String fixKey = switch (launcher) {
            case PRISM_POLYMC -> "gui.packcore.wizard.resource_pack.xss_fix.prism";
            case CURSEFORGE   -> "gui.packcore.wizard.resource_pack.xss_fix.curseforge";
            case ATLAUNCHER   -> "gui.packcore.wizard.resource_pack.xss_fix.atlauncher";
            case MODRINTH     -> "gui.packcore.wizard.resource_pack.xss_fix.modrinth";
            case OFFICIAL     -> "gui.packcore.wizard.resource_pack.xss_fix.official";
            default           -> "gui.packcore.wizard.resource_pack.xss_fix.unknown";
        };

        return Component.empty()
                .append(Component.translatable("gui.packcore.wizard.resource_pack.xss_banner_header"))
                .append(Component.literal("\n"))
                .append(Component.translatable("gui.packcore.wizard.resource_pack.xss_banner_launcher",
                        launcher.displayName()))
                .append(Component.literal("\n"))
                .append(Component.translatable(fixKey))
                .append(Component.literal("\n"))
                .append(Component.translatable("gui.packcore.wizard.resource_pack.xss_fix.restart"));
    }

    private List<ResourcePackEntry> discoverUserPacks(boolean missingXss) {
        return Minecraft.getInstance()
                .getResourcePackRepository()
                .getAvailablePacks()
                .stream()
                .filter(this::isUserSelectablePack)
                .sorted(Comparator.comparing(pack -> pack.getTitle().getString()))
                .map(pack -> {
                    boolean isHypixel = isHypixelPlusPack(pack);
                    return ResourcePackEntry.fromPack(pack, isHypixel, missingXss && isHypixel);
                })
                .toList();
    }

    private boolean isUserSelectablePack(Pack pack) {
        return pack.getPackSource() == PackSource.DEFAULT && !pack.getId().equals("vanilla");
    }

    /** Returns {@code true} if the pack appears to be Hypixel+. */
    private static boolean isHypixelPlusPack(Pack pack) {
        String title = pack.getTitle().getString().toLowerCase(Locale.ROOT);
        String id    = pack.getId().toLowerCase(Locale.ROOT);
        return title.contains("hypixel") || id.contains("hypixel");
    }

    public record ResourcePackEntry(String id, Component name, Component description, boolean requiresXss) {

        static ResourcePackEntry fromPack(Pack pack, boolean isHypixel, boolean warnXss) {
            Component desc = pack.getDescription();
            if (desc.getString().isBlank()) {
                desc = Component.translatable("gui.packcore.wizard.resource_pack.no_description");
            }

            if (warnXss) {
                // Per-row warning is intentionally short; the banner above gives full instructions.
                MutableComponent warning = Component.translatable("gui.packcore.wizard.resource_pack.xss_warning")
                        .withStyle(s -> s.withColor(0xFFAA00));
                desc = Component.empty()
                        .append(desc)
                        .append(Component.literal("\n"))
                        .append(warning);
            }

            return new ResourcePackEntry(pack.getId(), pack.getTitle(), desc, isHypixel);
        }
    }
}