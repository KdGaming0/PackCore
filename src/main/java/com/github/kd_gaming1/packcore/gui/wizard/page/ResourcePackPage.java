package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.gui.component.MultiSelectList;
import com.github.kd_gaming1.packcore.gui.component.ReorderableSelectList;
import com.github.kd_gaming1.packcore.gui.component.MarkdownComponent;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.integration.ResourcePackManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Step — Resource Pack chooser.
 */
public class ResourcePackPage extends BaseWizardPage {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ResourcePackPage");

    private static final Component PAGE_TITLE = Component.translatable("gui.packcore.wizard.page.resource_pack.title");

    private static final int PADDING = 16;
    private static final int COLUMN_GAP = 14;
    private static final int SCROLL_BAR_WIDTH = 8;

    private static final String FALLBACK_MARKDOWN = "*No resource pack guide found.*";
    private static final Path MARKDOWN_PATH = PackCore.PACKCORE_DIR.resolve("markdown").resolve("resource_packs.md");

    /**
     * Guards the one-time seeding of packs already enabled in-game into the wizard state. Seeding
     * runs only on the first page entry so that unchecking a pack and navigating back does not
     * re-add it.
     */
    private boolean seededEnabledPacks;

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
        List<ResourcePackEntry> packs = discoverUserPacks();

        if (packs.isEmpty()) {
            column.addComponent(new MultiLineTextComponent(
                    0, 0, width,
                    Component.translatable("gui.packcore.wizard.resource_pack.none_found"),
                    0xFF777777
            ));
            return;
        }

        seedEnabledPacks(packs);

        ReorderableSelectList<ResourcePackEntry> list = new ReorderableSelectList<>(
                0, 0, width, height,
                packs,
                MultiSelectList.RowDescriptor.of(
                        ResourcePackEntry::id,
                        ResourcePackEntry::name,
                        ResourcePackEntry::description
                ),
                state.getResourcePackOrder(),
                selected   -> state.addResourcePack(selected.id()),
                deselected -> state.removeResourcePack(deselected.id()),
                up   -> state.moveResourcePackUp(up.id()),
                down -> state.moveResourcePackDown(down.id()),
                Component.translatable("gui.packcore.wizard.resource_pack.selected_header"),
                Component.translatable("gui.packcore.wizard.resource_pack.available_header"),
                Component.translatable("gui.packcore.wizard.resource_pack.priority_hint")
        );
        column.addComponent(list);
    }

    /**
     * On first entry, marks packs already enabled in-game as selected so reopening the page shows
     * the current selection. Only packs that appear as rows are seeded, keeping every highlight
     * mapped to a real entry.
     */
    private void seedEnabledPacks(List<ResourcePackEntry> packs) {
        if (seededEnabledPacks) return;
        seededEnabledPacks = true;

        Set<String> rowIds = packs.stream().map(ResourcePackEntry::id).collect(Collectors.toSet());
        // options.resourcePacks is ordered low→high priority (the last entry wins conflicts). Seed in
        // reverse so the highest-priority enabled pack lands at the top of the wizard's order.
        List<String> enabled = new ArrayList<>(Minecraft.getInstance().options.resourcePacks);
        for (int i = enabled.size() - 1; i >= 0; i--) {
            String enabledId = enabled.get(i);
            if (rowIds.contains(enabledId)) {
                state.addResourcePack(enabledId);
            }
        }
    }

    private List<ResourcePackEntry> discoverUserPacks() {
        return Minecraft.getInstance()
                .getResourcePackRepository()
                .getAvailablePacks()
                .stream()
                .filter(ResourcePackManager::isUserSelectable)
                .sorted(Comparator.comparing(pack -> pack.getTitle().getString()))
                .map(ResourcePackEntry::fromPack)
                .toList();
    }

    public record ResourcePackEntry(String id, Component name, Component description) {

        static ResourcePackEntry fromPack(Pack pack) {
            Component desc = pack.getDescription();
            if (desc.getString().isBlank()) {
                desc = Component.translatable("gui.packcore.wizard.resource_pack.no_description");
            }
            return new ResourcePackEntry(pack.getId(), pack.getTitle(), desc);
        }
    }
}