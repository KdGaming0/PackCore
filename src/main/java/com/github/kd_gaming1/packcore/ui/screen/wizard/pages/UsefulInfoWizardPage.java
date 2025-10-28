package com.github.kd_gaming1.packcore.ui.screen.wizard.pages;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.ui.screen.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.ui.screen.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.ui.screen.wizard.components.WizardUIComponents;
import com.github.kd_gaming1.packcore.ui.surface.effects.TextureSurfaces;
import com.github.kd_gaming1.packcore.util.markdown.MarkdownService;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * Useful Information page - displays helpful tips and information
 */
public class UsefulInfoWizardPage extends BaseWizardPage {

    private static final String FALLBACK_CONTENT = """
            # Useful Information
            
            Useful Information! This is the default Useful Information content.
            
            Find and edit this content in `rundir/packcore/wizard_markdown_content/useful_information.md`
            """;

    private final String markdownContent;

    public UsefulInfoWizardPage() {
        super(
                new WizardPageInfo(
                        Text.literal("Useful information"),
                        5,
                        6
                ),
                Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/welcome_bg.png")
        );

        MarkdownService markdownService = new MarkdownService();
        this.markdownContent = markdownService.getOrDefault("useful_information.md", FALLBACK_CONTENT);
    }

    @Override
    protected void buildContent(FlowLayout contentContainer) {
        // Apply frame texture
        contentContainer.surface(TextureSurfaces.stretched(
                Identifier.of(MOD_ID, "textures/gui/wizard/frame.png"), 1920, 1080
        ));
        contentContainer.padding(Insets.of(24, 36, 24, 24));

        // Header
        contentContainer.child(
                WizardUIComponents.createHeader(
                        "Useful information when playing on",
                        "The pack have many mods and some features, here is a few tips to get you started."
                ).margins(Insets.horizontal(34))
        );

        // Markdown content in scrollable area
        var scrollContainer = WizardUIComponents.createMarkdownScroll(markdownContent);
        scrollContainer.margins(Insets.bottom(10));
        contentContainer.child(scrollContainer);
    }

    @Override
    protected void buildContentRight(FlowLayout contentContainerRight) {
        // No right panel for this page
    }

    @Override
    protected void onContinuePressed() {
        assert this.client != null;
        this.client.setScreen(WizardNavigator.createWizardPage(6));
    }

    @Override
    protected int getContentColumnWidthPercent() {
        return 100;
    }

    @Override
    protected boolean shouldShowStatusInfo() {
        return false;
    }
}