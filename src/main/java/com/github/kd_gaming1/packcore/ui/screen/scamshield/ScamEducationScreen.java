package com.github.kd_gaming1.packcore.ui.screen.scamshield;

import com.github.kd_gaming1.packcore.ui.screen.base.BasePackCoreScreen;
import com.github.kd_gaming1.packcore.ui.screen.components.ScreenUIComponents;
import com.github.kd_gaming1.packcore.ui.screen.components.WizardUIComponents;
import com.github.kd_gaming1.packcore.util.markdown.MarkdownService;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

/**
 * Educational screen about Hypixel SkyBlock scams.
 * Displays comprehensive information to help players identify and avoid scams.
 */
public class ScamEducationScreen extends BasePackCoreScreen {

    private String markdownContent;

    /**
     * Create education screen with full content
     */
    public ScamEducationScreen(Screen parentScreen, String markdownContent) {
        this(parentScreen);
        this.markdownContent = markdownContent;
    }

    /**
     * Create education screen with specific content
     * @param parentScreen The parent screen to return to
     */
    public ScamEducationScreen(Screen parentScreen) {
        super(parentScreen);
        MarkdownService markdownService = new MarkdownService();
        this.markdownContent = markdownService.getOrDefault("scam_education.md", FALLBACK_CONTENT);
    }

    @Override
    protected Component createTitleLabel() {
        return Components.label(
                        Text.literal("Scam Protection Education")
                                .styled(s -> s.withFont(new StyleSpriteSource.Font(Identifier.of(MOD_ID, "gallaeciaforte"))))
                ).color(Color.ofRgb(ACCENT_SECONDARY))
                .shadow(true)
                .margins(Insets.of(0, 0, 4, 4));
    }

    @Override
    protected FlowLayout createMainContent() {
        FlowLayout mainContent = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.expand())
                .gap(8)
                .padding(Insets.of(8));

        // Warning banner at the top
        mainContent.child(createWarningBanner());

        // Quick tips section
        mainContent.child(createQuickTips());

        // Main markdown content
        mainContent.child(createMarkdownContent());

        return mainContent;
    }

    /**
     * Create prominent warning banner
     */
    private FlowLayout createWarningBanner() {
        FlowLayout banner = ScreenUIComponents.createWarningCard(
                "Stay Alert!",
                "Scammers are constantly finding new ways to steal your items. " +
                        "If something seems too good to be true, it probably is. Always verify and be carefull when interacting with others."
        );

        // Make it more prominent
        banner.margins(Insets.of(4, 0, 8, 0));

        return banner;
    }

    /**
     * Create quick tips cards
     */
    private FlowLayout createQuickTips() {
        FlowLayout tipsSection = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4);

        tipsSection.child(Components.label(
                Text.literal("⚡ Quick Tips").setStyle(Style.EMPTY.withBold(true))
        ).color(Color.ofRgb(ACCENT_SECONDARY)));

        FlowLayout tipsContainer = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .gap(8);

        // Use FlowLayout tip cards (no per-card scroll)
        tipsContainer.child(createTipCard("🔒", "Protect Your Account",
                "Never share your password or session ID with anyone — even if they claim to be staff."));
        tipsContainer.child(createTipCard("🌐", "Avoid Suspicious Links",
                "Never click on unknown links or login pages sent by other players."));
        tipsContainer.child(createTipCard("💰", "If It’s Too Good to Be True...",
                "Free coins, items, or giveaways are almost always scams."));
        tipsContainer.child(createTipCard("🤝", "Trade Safely",
                "Double-check items and coins in every trade — scammers may switch items."));
        tipsContainer.child(createTipCard("🧠", "Stay Informed",
                "Learn about common scams by doing '/scamshield education' to protect yourself."));

        // Wrap the whole horizontal container in a single scroll box
        var tipsScroll = ScreenUIComponents.createScrollBoxHorizontal(tipsContainer);
        tipsScroll.sizing(Sizing.fill(100), Sizing.fixed(96));
        tipsSection.child(tipsScroll);

        return tipsSection;
    }

    /**
     * Create a single tip card
     */
    private FlowLayout createTipCard(String icon, String title, String description) {
        FlowLayout card = (FlowLayout) Containers.verticalFlow(Sizing.fixed(260), Sizing.content())
                .gap(4)
                .surface(Surface.flat(ENTRY_BACKGROUND).and(Surface.outline(ACCENT_PRIMARY)))
                .padding(Insets.of(8))
                .horizontalAlignment(HorizontalAlignment.CENTER);

        card.child(Components.label(Text.literal(icon))
                .color(Color.ofRgb(ACCENT_SECONDARY)));

        card.child(Components.label(Text.literal(title).setStyle(Style.EMPTY.withBold(true)))
                .color(Color.ofRgb(TEXT_PRIMARY)));

        card.child(Components.label(Text.literal(description))
                .color(Color.ofRgb(TEXT_SECONDARY))
                .horizontalTextAlignment(HorizontalAlignment.CENTER)
                .horizontalSizing(Sizing.fill(90)));

        return card;
    }

    /**
     * Create the main markdown content area
     */
    private ScrollContainer<FlowLayout> createMarkdownContent() {
        // Add section header
        FlowLayout wrapper = Containers.verticalFlow(Sizing.fill(100), Sizing.expand())
                .gap(4);

        wrapper.child(Components.label(
                Text.literal("📚 Complete Scam Guide").setStyle(Style.EMPTY.withBold(true))
        ).color(Color.ofRgb(ACCENT_SECONDARY)));

        // Create markdown scroll with the content
        return WizardUIComponents.createMarkdownScroll(markdownContent);
    }

    /**
     * Get default markdown content
     * You'll replace this with your actual markdown content
     */
    private static final String FALLBACK_CONTENT = """
            # Hypixel SkyBlock Scam Prevention Guide
            
            ## Introduction
            
            Scamming is the act of stealing money, items, or accounts from another player through deception or trickery.
            This guide will help you identify and avoid the most common scams in Hypixel SkyBlock.
            
            {gold}**Important:** Victims of scams will not have their items returned under any circumstances.{gold}
            
            ---
            
            ## Golden Rules
            
            1. **If it seems too good to be true, it probably is**
            2. **Never give items to someone you don't trust**
            3. **Always double-check trades before accepting**
            4. **Enable API and check player stats**
            5. **Never click suspicious links**
            
            ---
            
            ## Common Scam Types
            
            ### 🎯 Price Manipulation
            Scammers artificially inflate or deflate item prices to trick you into bad trades.
            
            **How to avoid:**
            - Check multiple sources for prices (Bazaar, Auction House)
            - Use price checking tools
            - Don't trust "limited time offers"
            
            ### 💰 Unbalanced Trades
            Offering less valuable items disguised as expensive ones.
            
            **How to avoid:**
            - Hover over items to verify their exact name
            - Check item stats and abilities
            - Use a resource pack to distinguish similar items
            
            ### 🎁 False Rewards
            Promising rare items or coins for auction bids.
            
            **How to avoid:**
            - Never bid on items expecting "bonus rewards"
            - Check if player is actually quitting (SkyCrypt)
            - Remember: legitimate players just give items away
            
            ### 🔨 Crafting/Reforging Scams
            Asking for materials to craft items but stealing them.
            
            **How to avoid:**
            - Only trade with trusted friends
            - Check if they have the required recipes (API enabled)
            - Meet the crafting requirements yourself, or ask a close friend you personally know to help
            
            ### 🤝 Borrowing/Loaning Scams
            Borrowing items and never returning them.
            
            **How to avoid:**
            - Don't lend items to strangers
            - If you must loan items, be aware that scammers may provide worthless collateral or never return items. It's recommends simply not to loan to people you don't know well.
            - Better yet: meet requirements yourself
            
            ### 🔄 Item Switching
            Showing expensive item then swapping for cheap lookalike.
            
            **How to avoid:**
            - Always verify the exact item before accepting
            - If they cancel and retry, check EXTRA carefully
            - Take your time, don't rush trades
            
            ### 👑 Rank Selling
            Claiming they can give you ranks for in-game items.
            
            **How to avoid:**
            - Ranks can ONLY be purchased on store.hypixel.net
            - Players cannot give other players ranks
            - This is always 100% a scam
            
            ### 🏰 Dungeon Carry Scams
            Not paying after carry or taking payment without carrying.
            
            **How to avoid:**
            - Use trusted carry services from official Discord
            - Check carrier's Catacombs level and gear
            - Agree on payment terms before starting
            
            ### 🏝️ Co-op Island Stealing
            Joining co-op to steal items or kick you out.
            
            **How to avoid:**
            - NEVER add strangers to co-op
            - Only co-op with people you completely trust
            - Remember: /coopadd invites them to YOUR island
            
            ### 🔗 Phishing Links
            Fake websites designed to steal your account.
            
            **How to avoid:**
            - Never click links in Minecraft chat
            - Only log in at minecraft.net and hypixel.net
            - Use URL scanners if you must check a link
            - Hypixel staff will NEVER DM you in-game
            
            ---
            
            ## Red Flags 🚩
            
            Watch out for these warning signs:
            
            - Player asks you to disable API
            - Deal requires multiple trades
            - Player is rushing you
            - "Limited time offer"
            - Asking for collateral when they have recipes
            - New account with expensive items
            - Multiple trade cancellations
            - Pressure tactics ("last chance!", "hurry!")
            
            ---
            
            ## Reporting Scammers
            
            If you encounter a scammer:
            
            1. **Screenshot evidence** (F2 in Minecraft)
            2. **Report via** `/report [player] [reason]`
            3. **Report on forums** with evidence
            4. **Warn others** in your guild/party
            
            {red}**Remember:** You won't get items back, but reporting helps protect others!{red}
            
            ---
            
            ## Protection Tools
            
            - **SkyBlockAddons** - Has scam protection features
            - **NEU (NotEnoughUpdates)** - Shows item prices
            - **SkyCrypt** - Check player profiles
            - **Hypixel API** - Verify player stats
            
            ---
            
            ## Stay Safe!
            
            The best protection is knowledge and caution. Take your time with trades, verify everything,
            and trust your instincts. If something feels wrong, it probably is.
            
            {gold}**Remember: Your items are YOUR responsibility. Stay alert and trade smart!**{gold}
            """;
}