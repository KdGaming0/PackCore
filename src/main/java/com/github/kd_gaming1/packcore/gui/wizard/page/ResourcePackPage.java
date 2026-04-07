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
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

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

        MultiSelectList<ResourcePackEntry> list = new MultiSelectList<>(
                0, 0, width, height,
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

    private List<ResourcePackEntry> discoverUserPacks() {
        return Minecraft.getInstance()
                .getResourcePackRepository()
                .getAvailablePacks()
                .stream()
                .filter(this::isUserSelectablePack)
                .sorted(Comparator.comparing(pack -> pack.getTitle().getString()))
                .map(ResourcePackEntry::fromPack)
                .toList();
    }

    private boolean isUserSelectablePack(Pack pack) {
        return pack.getPackSource() == PackSource.DEFAULT && !pack.getId().equals("vanilla");
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