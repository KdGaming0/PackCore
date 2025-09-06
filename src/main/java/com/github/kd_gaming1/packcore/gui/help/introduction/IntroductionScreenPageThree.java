package com.github.kd_gaming1.packcore.gui.help.introduction;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.gui.help.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.help.WizardDataManager;
import com.github.kd_gaming1.packcore.gui.help.WizardNavigator;
import com.github.kd_gaming1.packcore.util.MarkdownFileUtil;
import com.github.kd_gaming1.packcore.util.ModpackInfo;
import io.wispforest.lavendermd.MarkdownProcessor;
import io.wispforest.lavendermd.compiler.OwoUICompiler;
import io.wispforest.lavendermd.feature.*;
import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IntroductionScreenPageThree extends BaseWizardPage {
    private static final MarkdownProcessor<ParentComponent> MARKDOWN_PROCESSOR =
            new MarkdownProcessor<>(
                    OwoUICompiler::new,
                    new BasicFormattingFeature(),
                    new ColorFeature(),
                    new LinkFeature(),
                    new ListFeature(),
                    new BlockQuoteFeature(),
                    new ImageFeature()
            );

    private static final Map<String, ParentComponent> COMPONENT_CACHE = new ConcurrentHashMap<>();

    private final String welcomeMarkdown;
    private final ModpackInfo modpackInfo;

    // Allow multiple selections
    private final Set<String> selectedResourcePacks = new LinkedHashSet<>();
    LabelComponent headerTitle;
    private FlowLayout rightPanel;

    private static final int SELECTED_OUTLINE_COLOR = 0xFF00FF00;
    private static final int UNSELECTED_OUTLINE_COLOR = ACCENT_GOLD;

    public record OptionProfile(String key, String title, String description) {
    }

    // The selectable options
    private final List<IntroductionScreenPageThree.OptionProfile> allProfiles = List.of(
            new IntroductionScreenPageThree.OptionProfile("HypixelPlus", "Pack: Hypixel Plus", "A clean, mostly vanilla pack designed for Hypixel modes like SkyBlock. It updates items and icons for better clarity without changing the overall Minecraft feel."),
            new IntroductionScreenPageThree.OptionProfile("FurfSkyOverlay", "Pack: FurfSky Overlay", "A comprehensive resource pack for Hypixel SkyBlock, offering textures for nearly every item in the game. With full retextures for only items in a special style."),
            new IntroductionScreenPageThree.OptionProfile("FurfSkyFull", "Pack: FurfSky Full", "A comprehensive resource pack for Hypixel SkyBlock, offering textures for nearly every item in the game. With full retextures for items and menus in a special style."),
            new IntroductionScreenPageThree.OptionProfile("SkyBlockDarkUI", "Pack: SkyBlock Dark UI", " A sleek, dark-themed resource pack for Hypixel SkyBlock, enhancing all GUI elements, including mod interfaces, with a modern aesthetic. Inspired by PacksHQ Dark UI"),
            new IntroductionScreenPageThree.OptionProfile("Defrosted", "Pack: Defrosted", "Icy-themed 16x pack for Minecraft 1.21.5. It offers a frosty blue aesthetic across items and menus, maintaining a minimalist look without altering gameplay clarity."),
            new IntroductionScreenPageThree.OptionProfile("Looshy", "Pack: Looshy", "A smooth, vanilla‑like 16x resource pack with clean updates and subtle charm. It keeps Minecraft’s original style while offering refined textures that feel fresh and polished.")
    );

    public IntroductionScreenPageThree() {
        super(
                new WizardPageInfo(
                        Text.literal("Resource Packs"),
                        3,
                        5 // Total wizard steps
                ),
                Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/welcome_bg.png")
        );

        this.welcomeMarkdown = MarkdownFileUtil.readMarkdownFile("ResourcePacks.md");
        this.modpackInfo = PackCore.getModpackInfo();
    }

    @Override
    protected void buildContent(FlowLayout contentContainer) {
        contentContainer.child(createWelcomeHeader());
        contentContainer.child(createMarkdownSection());
    }

    @Override
    protected void buildContentRight(FlowLayout contentContainerRight) {
        this.rightPanel = contentContainerRight;
        rightPanel.child(createHeader());
        rightPanel.child(createProfilesScrollContainer());
    }

    private FlowLayout createWelcomeHeader() {
        FlowLayout header = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6);

        Text welcomeText = TextOps.concat(
                TextOps.withColor("Choose your prefer resource packs when using ", TEXT_WHITE),
                Text.literal(modpackInfo.getName()).setStyle(Style.EMPTY.withColor(ACCENT_GOLD).withBold(Boolean.TRUE))
        );

        LabelComponent welcomeTitle = Components.label(welcomeText);

        LabelComponent subtitle = (LabelComponent) Components.label(
                Text.literal("Please read the information below carefully before continuing. Need help? Click the discord button at the bottom.")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(Boolean.TRUE))
        ).color(Color.ofRgb(TEXT_SECONDARY)).margins(Insets.of(2, 0, 2, 0)).sizing(Sizing.expand(), Sizing.content());

        header.child(welcomeTitle).child(subtitle);

        return header;
    }

    private ScrollContainer<FlowLayout> createMarkdownSection() {
        FlowLayout markdownWrapper = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4);

        var markdownComponent = COMPONENT_CACHE.computeIfAbsent(
                welcomeMarkdown,
                MARKDOWN_PROCESSOR::process
        ).horizontalSizing(Sizing.fill(96));

        markdownWrapper.child(markdownComponent);

        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(
                Sizing.fill(100),
                Sizing.expand(),
                markdownWrapper
        );

        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scrollContainer.scrollbarThiccness(6);
        scrollContainer.surface(Surface.flat(0x40_000000).and(Surface.outline(0x30_FFFFFF)));
        scrollContainer.padding(Insets.of(8));

        return scrollContainer;
    }

    private FlowLayout createHeader() {
        FlowLayout header = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6)
                .margins(Insets.top(4));

        if (selectedResourcePacks.isEmpty()) {
            headerTitle = (LabelComponent) Components.label(TextOps.withColor("Select the resource packs you want by click the boxes below", ACCENT_GOLD)).horizontalSizing(Sizing.fill(100));
        } else {
            String joined = String.join(", ", selectedResourcePacks);
            headerTitle = (LabelComponent) Components.label(TextOps.withColor("Your selected resource packs are: " + joined, ACCENT_GOLD)).horizontalSizing(Sizing.fill(100));
        }

        header.child(headerTitle);

        return header;
    }

    private FlowLayout createProfileBox(IntroductionScreenPageThree.OptionProfile profile) {
        boolean isSelected = selectedResourcePacks.contains(profile.key);
        FlowLayout box = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        box.surface(Surface.flat(0x20_FFD700).and(Surface.outline(isSelected ? SELECTED_OUTLINE_COLOR : UNSELECTED_OUTLINE_COLOR)));
        box.padding(Insets.of(2));

        LabelComponent infoTitle = (LabelComponent) Components.label(
                TextOps.withColor(profile.title, ACCENT_GOLD).setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).margins(Insets.of(2, 2, 2, 2));
        LabelComponent infoText = (LabelComponent) Components.label(
                        TextOps.withColor(profile.description, TEXT_WHITE).setStyle(Style.EMPTY.withItalic(Boolean.TRUE))
                ).horizontalSizing(Sizing.fill(100))
                .margins(Insets.of(2, 2, 2, 2));

        box.child(infoTitle).child(infoText);

        box.mouseDown().subscribe((mouseX, mouseY, button) -> {
            toggleSelectedProfile(profile.key);
            return true;
        });

        return box;
    }

    // Scrollable container for all profile boxes
    private ScrollContainer<FlowLayout> createProfilesScrollContainer() {
        FlowLayout profilesLayout = Containers.verticalFlow(Sizing.fill(96), Sizing.content()).gap(6);

        for (IntroductionScreenPageThree.OptionProfile profile : allProfiles) {
            profilesLayout.child(createProfileBox(profile));
        }

        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(
                Sizing.fill(100),
                Sizing.expand(),
                profilesLayout
        );
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scrollContainer.scrollbarThiccness(6);
        scrollContainer.surface(Surface.flat(0x40_000000).and(Surface.outline(0x30_FFFFFF)));
        scrollContainer.padding(Insets.of(6));
        scrollContainer.margins(Insets.bottom(4));
        return scrollContainer;
    }

    // Toggle selection and update manager + UI
    private void toggleSelectedProfile(String profileKey) {
        if (selectedResourcePacks.contains(profileKey)) {
            selectedResourcePacks.remove(profileKey);
        } else {
            selectedResourcePacks.add(profileKey);
        }

        // NEW: Store in data manager using proper ordered list
        List<String> orderedList = new ArrayList<>(selectedResourcePacks);
        WizardDataManager.getInstance().setResourcePacksOrdered(orderedList);

        if (headerTitle != null) {
            if (selectedResourcePacks.isEmpty()) {
                headerTitle.text(TextOps.withColor("Select the resource packs you want by click the boxes below", ACCENT_GOLD));
            } else {
                headerTitle.text(TextOps.withColor("Your selected resource packs are: " + String.join(", ", selectedResourcePacks), ACCENT_GOLD));
            }
        }

        // Update UI outlines for profile boxes
        updateProfileBoxes();
    }

    private void updateProfileBoxes() {
        rightPanel.children().stream()
                .filter(child -> child instanceof ScrollContainer)
                .findFirst()
                .ifPresent(scrollContainer -> {
                    FlowLayout profilesLayout = (FlowLayout) ((ScrollContainer<?>) scrollContainer).child();

                    // Update existing profile boxes' surface to reflect selection state
                    for (int i = 0; i < profilesLayout.children().size() && i < allProfiles.size(); i++) {
                        Component child = profilesLayout.children().get(i);
                        if (child instanceof FlowLayout existingBox) {
                            IntroductionScreenPageThree.OptionProfile profile = allProfiles.get(i);
                            boolean isSelected = selectedResourcePacks.contains(profile.key);

                            existingBox.surface(Surface.flat(0x20_FFD700).and(
                                    Surface.outline(isSelected ? SELECTED_OUTLINE_COLOR : UNSELECTED_OUTLINE_COLOR)
                            ));
                        }
                    }
                });
    }

    @Override
    protected void onContinuePressed() {
        this.client.setScreen(WizardNavigator.createWizardPage(4));
    }

    @Override
    protected int getContentColumnWidthPercent() {
        return 55;
    }

    @Override
    protected boolean shouldShowStatusInfo() {
        return false;
    }

    @Override
    protected boolean shouldShowRightPanel() {
        return true;
    }
}