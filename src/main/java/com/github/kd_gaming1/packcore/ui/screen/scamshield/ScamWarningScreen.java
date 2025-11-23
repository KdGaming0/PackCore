package com.github.kd_gaming1.packcore.ui.screen.scamshield;

import com.github.kd_gaming1.packcore.ui.screen.components.ScreenUIComponents;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

/**
 * Enhanced warning overlay screen for detected scam attempts.
 *
 * DESIGN PHILOSOPHY:
 * - Minecraft-integrated aesthetic (not a harsh system alert)
 * - Clear visual hierarchy guiding user attention
 * - Contextual, helpful messaging instead of alarm
 * - Confidence-driven UI that adapts to threat level
 *
 * DISPLAY LOGIC:
 * - Only shown for HIGH confidence detections (95%+)
 * - Lower confidence uses chat warnings only
 * - Blocks interaction until user acknowledges
 *
 * @author KD_Gaming1
 * @version 2.0 - Refactored for better UX
 */
public class ScamWarningScreen extends BaseOwoScreen<FlowLayout> {

    private final ScamWarning warning;
    private final Runnable onDismiss;

    // Animation state for visual interest
    private int tickCount = 0;

    /**
     * Immutable data object containing scam detection information.
     *
     * @param playerName The username of the suspected scammer
     * @param scamType The category of scam detected
     * @param detectedMessage The original suspicious message
     * @param confidenceLevel Confidence score (0-100, typically 90-100 for this screen)
     */
    public record ScamWarning(
            String playerName,
            ScamType scamType,
            String detectedMessage,
            int confidenceLevel
    ) {
        /**
         * Get human-readable confidence description.
         */
        public String getConfidenceText() {
            if (confidenceLevel >= 95) return "Extremely High";
            if (confidenceLevel >= 90) return "Very High";
            if (confidenceLevel >= 80) return "High";
            if (confidenceLevel >= 70) return "Medium";
            if (confidenceLevel >= 50) return "Moderate";
            return "Low";
        }

        /**
         * Get appropriate color for confidence level display.
         */
        public int getConfidenceColor() {
            if (confidenceLevel >= 90) return 0xFF3333; // Bright red for critical
            if (confidenceLevel >= 70) return 0xFF9933; // Orange for high
            if (confidenceLevel >= 50) return 0xFFCC33; // Yellow for medium
            return TEXT_SECONDARY;
        }
    }

    /**
     * Scam type categorization with contextual information.
     * Each type has specific messaging and guidance.
     */
    public enum ScamType {
        PHISHING_LINK(
                "Phishing Link Detected",
                "🔗",
                "This player is trying to steal your account credentials.",
                "Never click suspicious links or enter login info on unfamiliar sites."
        ),
        PRICE_MANIPULATION(
                "Price Manipulation",
                "💰",
                "This player may be manipulating item values for unfair trades.",
                "Always verify market prices before making large trades."
        ),
        RANK_SELLING(
                "Rank Selling Scam",
                "👑",
                "This player is likely offering fake ranks or perks.",
                "Official ranks can only be purchased through legitimate channels."
        ),
        COOP_SCAM(
                "Co-op Access Theft",
                "🏝️",
                "This player may steal items or delete your island if given co-op access.",
                "Only add trusted friends to your island co-op."
        ),
        BORROWING(
                "Borrowing/Loaning Scam",
                "🤝",
                "This player may not return borrowed items.",
                "Don't loan valuable items to players you don't know well."
        ),
        FALSE_REWARDS(
                "Fake Giveaway/Reward",
                "🎁",
                "This player is offering rewards that don't exist.",
                "Real giveaways don't require you to send items or login elsewhere."
        ),
        CRAFTING(
                "Crafting/Reforging Scam",
                "🔨",
                "This player may keep your items without completing the service.",
                "Only use crafters you personally know and trust. Check their API to verify they have the required recipes unlocked."
        ),
        GENERAL(
                "Suspicious Activity",
                "⚠️",
                "Multiple suspicious patterns detected in this player's messages.",
                "Exercise caution when interacting with this player."
        );

        private final String displayName;
        private final String icon;
        private final String threatDescription;
        private final String guidanceText;

        ScamType(String displayName, String icon, String threatDescription, String guidanceText) {
            this.displayName = displayName;
            this.icon = icon;
            this.threatDescription = threatDescription;
            this.guidanceText = guidanceText;
        }

        public String getDisplayName() { return displayName; }
        public String getIcon() { return icon; }
        public String getThreatDescription() { return threatDescription; }
        public String getGuidanceText() { return guidanceText; }
    }

    /**
     * Create a scam warning overlay screen.
     *
     * @param warning Immutable detection data
     * @param onDismiss Callback invoked when user dismisses the warning
     */
    public ScamWarningScreen(ScamWarning warning, Runnable onDismiss) {
        this.warning = warning;
        this.onDismiss = onDismiss;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER).surface(Surface.VANILLA_TRANSLUCENT);

        FlowLayout content = createScrollableContent();
        rootComponent.child(ScreenUIComponents.createScrollContainer(content));
    }

    /**
     * Creates scrollable content with all warning information.
     * Designed to fit within a ScrollContainer for long content.
     */
    private FlowLayout createScrollableContent() {
        FlowLayout content = (FlowLayout) Containers.verticalFlow(Sizing.expand(), Sizing.content())
                .gap(16)
                .padding(Insets.of(24));

        // Build card sections in priority order
        content.child(createWarningHeader());
        content.child(createThreatInfoPanel());
        content.child(createPlayerInfoSection());
        content.child(createDetectedMessageSection());
        content.child(createActionButtonRow());
        content.child(createProtectionTipsFooter());

        return content;
    }

    /**
     * Creates an attractive card surface with proper borders.
     * Uses darker Minecraft-style panel with accent border.
     */
    private Surface createCardSurface() {
        // Dark panel background with a warning-colored border
        int borderColor = warning.confidenceLevel() >= 90 ? 0xFF4444 : WARNING_BORDER;
        return Surface.flat(0xFF_1A1A1A).and(Surface.outline(borderColor));
    }

    /**
     * Creates the prominent warning header section.
     * Displays confidence level and general alert status.
     */
    private FlowLayout createWarningHeader() {
        FlowLayout header = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .padding(Insets.bottom(8));

        // Large attention-grabbing icon with subtle pulse effect
        header.child(Components.label(Text.literal("⚠"))
                .color(Color.ofRgb(warning.getConfidenceColor()))
                .shadow(true));

        // Main title - direct and clear
        header.child(Components.label(
                Text.literal("Scam Attempt Detected").setStyle(Style.EMPTY.withBold(true))
        ).color(Color.ofRgb(0xFFFFFF)).shadow(true));

        // Confidence indicator - shows detection reliability
        FlowLayout confidenceBadge = createConfidenceBadge();
        header.child(confidenceBadge);

        return header;
    }

    /**
     * Creates a visual badge showing confidence level.
     * Helps users understand detection reliability.
     */
    private FlowLayout createConfidenceBadge() {
        FlowLayout badge = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .gap(6)
                .surface(Surface.flat(0xFF_2A2A2A).and(Surface.outline(warning.getConfidenceColor())))
                .padding(Insets.of(8));

        badge.child(Components.label(Text.literal("Confidence:"))
                .color(Color.ofRgb(TEXT_SECONDARY)));

        badge.child(Components.label(
                Text.literal(warning.getConfidenceText()).setStyle(Style.EMPTY.withBold(true))
        ).color(Color.ofRgb(warning.getConfidenceColor())));

        badge.child(Components.label(
                Text.literal("(" + warning.confidenceLevel() + "%)")
        ).color(Color.ofRgb(TEXT_SECONDARY)));

        return badge;
    }

    /**
     * Creates the threat information panel.
     * Explains WHAT this scam is and WHY it's dangerous.
     */
    private FlowLayout createThreatInfoPanel() {
        ScamType type = warning.scamType();

        FlowLayout panel = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(10)
                .surface(Surface.flat(0xFF_2A1A1A).and(Surface.outline(0xFF4444)))
                .padding(Insets.of(16));

        // Scam type header with icon
        FlowLayout typeHeader = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .verticalAlignment(VerticalAlignment.CENTER);

        typeHeader.child(Components.label(Text.literal(type.getIcon()))
                .color(Color.ofRgb(0xFF6666)));

        typeHeader.child(Components.label(
                Text.literal(type.getDisplayName()).setStyle(Style.EMPTY.withBold(true))
        ).color(Color.ofRgb(0xFFAAAA)));

        panel.child(typeHeader);

        // Threat description - explains the danger
        panel.child(Components.label(Text.literal(type.getThreatDescription()))
                .color(Color.ofRgb(0xFFCCCC))
                .horizontalSizing(Sizing.fill(96)));

        // Guidance text - tells user what to do
        FlowLayout guidanceBox = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .surface(Surface.flat(0xFF_1A2A1A).and(Surface.outline(0x44AA44)))
                .padding(Insets.of(10))
                .margins(Insets.top(6));

        guidanceBox.child(Components.label(Text.literal("💡"))
                .color(Color.ofRgb(0x77DD77)));

        guidanceBox.child(Components.label(Text.literal(type.getGuidanceText()))
                .color(Color.ofRgb(0xAAFFAA))
                .horizontalSizing(Sizing.fill(90)));

        panel.child(guidanceBox);

        return panel;
    }

    /**
     * Creates the player information section.
     * Shows WHO triggered the detection.
     */
    private FlowLayout createPlayerInfoSection() {
        FlowLayout section = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .surface(Surface.flat(0xFF_1A1A2A).and(Surface.outline(0x4466AA)))
                .padding(Insets.of(14));

        // Section title
        section.child(Components.label(
                Text.literal("Suspected Player").setStyle(Style.EMPTY.withBold(true))
        ).color(Color.ofRgb(0xAABBFF)));

        // Player name in prominent display
        FlowLayout playerDisplay = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(10)
                .verticalAlignment(VerticalAlignment.CENTER);

        playerDisplay.child(Components.label(Text.literal("👤"))
                .color(Color.ofRgb(0x6699FF)));

        playerDisplay.child(Components.label(
                Text.literal(warning.playerName()).setStyle(Style.EMPTY.withBold(true))
        ).color(Color.ofRgb(0xFFFFFF)));

        section.child(playerDisplay);

        // Warning text
        section.child(Components.label(
                Text.literal("⚠ Avoid all transactions with this player until you verify their legitimacy.")
        ).color(Color.ofRgb(0xFFAA66)).horizontalSizing(Sizing.fill(96)));

        return section;
    }

    /**
     * Creates the detected message display section.
     * Shows WHAT was said that triggered detection.
     */
    private FlowLayout createDetectedMessageSection() {
        FlowLayout section = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .surface(Surface.flat(0xFF_2A2A1A).and(Surface.outline(0xAA9944)))
                .padding(Insets.of(14));

        // Section title
        section.child(Components.label(
                Text.literal("Suspicious Message").setStyle(Style.EMPTY.withBold(true))
        ).color(Color.ofRgb(0xFFCC77)));

        // Message content in a scrollable container if needed
        String message = warning.detectedMessage();

        // Truncate very long messages
        String displayMessage = message.length() > 200
                ? message.substring(0, 197) + "..."
                : message;

        section.child(Components.label(
                Text.literal("\"" + displayMessage + "\"").setStyle(Style.EMPTY.withItalic(true))
        ).color(Color.ofRgb(0xEEEEEE)).horizontalSizing(Sizing.fill(96)));

        return section;
    }

    /**
     * Creates the action button row.
     * Provides clear next steps for the user.
     */
    private FlowLayout createActionButtonRow() {
        FlowLayout buttonRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(10)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.vertical(8));

        // Primary action: Learn More (education)
        buttonRow.child(ScreenUIComponents.createButton("🛡 Learn More", btn -> {
            MinecraftClient.getInstance().setScreen(
                    new ScamEducationScreen(null)
            );
        }, 140, 22));

        // Secondary action: Report Player
        buttonRow.child(ScreenUIComponents.createButton("📢 Report", btn -> {
            //? if >=1.21.8 {
            
            /*if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.networkHandler.sendChatCommand(
                     "report " + warning.playerName() + " Scamming"
                 );
            }
             
            *///?} else {
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.networkHandler.sendCommand(
                        "report " + warning.playerName() + " Scamming"
                );
            }
            //?}
            dismiss();
        }, 110, 22));

        // Tertiary action: Dismiss
        buttonRow.child(ScreenUIComponents.createButton("✓ Got It", btn -> dismiss(), 110, 22));

        return buttonRow;
    }

    /**
     * Creates the protection tips footer.
     * Provides quick safety reminders.
     */
    private FlowLayout createProtectionTipsFooter() {
        FlowLayout footer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6)
                .surface(Surface.flat(0xFF_1A2A1A).and(Surface.outline(0x44AA44)))
                .padding(Insets.of(12))
                .margins(Insets.top(4));

        // Footer title
        footer.child(Components.label(
                Text.literal("🛡 Stay Safe").setStyle(Style.EMPTY.withBold(true))
        ).color(Color.ofRgb(0x77DD77)));

        // Quick tips - concise and actionable
        String[] tips = {
                "• Never share your login credentials with anyone",
                "• Check Auction House and Bazaar prices before trading to verify fair value",
                "• Only use /coopadd with people you know personally",
                "• Report suspicious behavior to server staff"
        };

        for (String tip : tips) {
            footer.child(Components.label(Text.literal(tip))
                    .color(Color.ofRgb(0xAAFFAA)));
        }

        return footer;
    }

    /**
     * Dismiss the warning and return control to the player.
     * Invokes the provided callback if present.
     */
    private void dismiss() {
        if (onDismiss != null) {
            onDismiss.run();
        }
        this.close();
    }

    @Override
    public boolean shouldPause() {
        // Don't pause the game - this is an overlay, not a full pause screen
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Allow ESC to dismiss - user has read the warning
        return true;
    }

    @Override
    public void close() {
        if (onDismiss != null) {
            onDismiss.run();
        }
        super.close();
    }

    @Override
    public void tick() {
        super.tick();
        tickCount++;

        // Could add subtle animations here if desired
        // Example: pulse effect on warning icon every 2 seconds
        // if (tickCount % 40 == 0) { /* trigger animation */ }
    }
}