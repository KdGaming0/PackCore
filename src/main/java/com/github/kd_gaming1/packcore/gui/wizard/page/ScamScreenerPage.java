package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import com.github.kd_gaming1.packcore.gui.component.MultiSelectList;
import com.github.kd_gaming1.packcore.gui.component.OptionSelectList;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.integration.ScamScreenerConfigurator;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

public class ScamScreenerPage extends BaseWizardPage {

    public static final String ALERT_LEVEL_KEY = "scamScreenerMinimumRiskLevel";
    public static final String PING_OPTIONS_KEY = "scamScreenerPingOptions";

    private static final Component PAGE_TITLE = Component.translatable("gui.packcore.wizard.page.scamscreener.title");

    private static final int PADDING = 16;
    private static final int COLUMN_GAP = 14;
    private static final int SECTION_GAP = 10;
    private static final int LABEL_GAP = 6;
    private static final int COLOR_LABEL = 0xFFCCCCCC;
    private static final int COLOR_HINT = 0xFF777777;

    public ScamScreenerPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override public Component getTitle() { return PAGE_TITLE; }
    @Override public boolean validate() { return true; }
    @Override public void onExit() {}

    @Override
    public void onEnter() {
        this.clearComponents();
        seedInitialState();

        int availableWidth = getWidth() - PADDING * 2;
        int availableHeight = getHeight() - PADDING * 2;

        MultiLineTextComponent intro = new MultiLineTextComponent(
                PADDING, PADDING, availableWidth,
                Component.translatable("gui.packcore.wizard.page.scamscreener.explanation"),
                COLOR_HINT
        );
        this.addComponent(intro);

        int columnsY = PADDING + intro.getHeight() + SECTION_GAP;
        int columnHeight = availableHeight - intro.getHeight() - SECTION_GAP;
        int columnWidth = (availableWidth - COLUMN_GAP) / 2;

        EmptyComponent leftColumn = new EmptyComponent(PADDING, columnsY, columnWidth, columnHeight);
        EmptyComponent rightColumn = new EmptyComponent(PADDING + columnWidth + COLUMN_GAP, columnsY, columnWidth, columnHeight);

        buildAlertLevelColumn(leftColumn, columnWidth, columnHeight);
        buildPingColumn(rightColumn, columnWidth, columnHeight);

        this.addComponent(leftColumn);
        this.addComponent(rightColumn);
    }

    public static Component labelForPingOption(String optionId) {
        return pingOptions().stream()
                .filter(option -> option.id().equals(optionId))
                .findFirst()
                .map(PingOption::name)
                .orElse(Component.literal(optionId));
    }

    public static Component labelForAlertLevel(String optionId) {
        return AlertLevelOption.fromId(optionId).name();
    }

    private void seedInitialState() {
        ScamScreenerConfigurator.RuntimeSettings settings = ScamScreenerConfigurator.loadSettings();

        if (state.getSelection(ALERT_LEVEL_KEY) == null) {
            state.setSelection(ALERT_LEVEL_KEY, settings.minimumRiskLevel());
        }

        if (state.getMultiSelection(PING_OPTIONS_KEY).isEmpty()) {
            if (settings.pingOnRiskWarning()) {
                state.addMultiSelection(PING_OPTIONS_KEY, "risk_warning");
            }
            if (settings.pingOnBlacklistWarning()) {
                state.addMultiSelection(PING_OPTIONS_KEY, "blacklist_warning");
            }
        }
    }

    private void buildAlertLevelColumn(EmptyComponent column, int width, int height) {
        var font = Minecraft.getInstance().font;
        List<AlertLevelOption> alertLevels = ScamScreenerConfigurator.availableAlertLevels().stream()
                .map(AlertLevelOption::fromId)
                .toList();

        String selectedAlertLevel = state.getSelection(ALERT_LEVEL_KEY);
        String selectedAlertLevelCandidate = selectedAlertLevel;
        boolean selectedValueExists = alertLevels.stream().anyMatch(option -> option.id().equals(selectedAlertLevelCandidate));
        if (!selectedValueExists && !alertLevels.isEmpty()) {
            selectedAlertLevel = alertLevels.getFirst().id();
            state.setSelection(ALERT_LEVEL_KEY, selectedAlertLevel);
        }

        column.addComponent(new TextComponent(
                0, 0,
                Component.translatable("gui.packcore.wizard.scamscreener.alerts.heading"),
                COLOR_LABEL
        ));

        int contentY = font.lineHeight + LABEL_GAP;
        int contentHeight = height - contentY;

        OptionSelectList<AlertLevelOption> list = new OptionSelectList<>(
                0, contentY, width, contentHeight,
                alertLevels,
                OptionSelectList.RowDescriptor.of(
                        AlertLevelOption::id,
                        AlertLevelOption::name,
                        AlertLevelOption::description
                ),
                selectedAlertLevel,
                selected -> state.setSelection(ALERT_LEVEL_KEY, selected.id())
        );
        column.addComponent(list);
    }

    private void buildPingColumn(EmptyComponent column, int width, int height) {
        var font = Minecraft.getInstance().font;
        int currentY = 0;

        column.addComponent(new TextComponent(
                0, currentY,
                Component.translatable("gui.packcore.wizard.scamscreener.pings.heading"),
                COLOR_LABEL
        ));
        currentY += font.lineHeight + LABEL_GAP;

        MultiLineTextComponent pingsHint = new MultiLineTextComponent(
                0, currentY,
                width,
                Component.translatable("gui.packcore.wizard.scamscreener.pings.hint"),
                COLOR_HINT
        );
        column.addComponent(pingsHint);
        currentY += pingsHint.getHeight() + LABEL_GAP;

        int pingListHeight = Math.max(100, height - currentY);
        MultiSelectList<PingOption> pingList = new MultiSelectList<>(
                0, currentY, width, pingListHeight,
                pingOptions(),
                MultiSelectList.RowDescriptor.of(
                        PingOption::id,
                        PingOption::name,
                        PingOption::description
                ),
                state.getMultiSelection(PING_OPTIONS_KEY),
                selected -> state.addMultiSelection(PING_OPTIONS_KEY, selected.id()),
                deselected -> state.removeMultiSelection(PING_OPTIONS_KEY, deselected.id())
        );
        column.addComponent(pingList);
    }

    private static List<PingOption> pingOptions() {
        return List.of(
                new PingOption(
                        "risk_warning",
                        Component.translatable("gui.packcore.wizard.scamscreener.pings.risk.name"),
                        Component.translatable("gui.packcore.wizard.scamscreener.pings.risk.desc")
                ),
                new PingOption(
                        "blacklist_warning",
                        Component.translatable("gui.packcore.wizard.scamscreener.pings.blacklist.name"),
                        Component.translatable("gui.packcore.wizard.scamscreener.pings.blacklist.desc")
                )
        );
    }

    public record AlertLevelOption(String id, Component name, Component description) {
        private static AlertLevelOption fromId(String id) {
            String normalizedId = id.toUpperCase(Locale.ROOT);
            String baseKey = "gui.packcore.wizard.scamscreener.minimum_risk." + normalizedId;
            return new AlertLevelOption(
                    normalizedId,
                    switch (normalizedId) {
                        case "LOW", "MEDIUM", "HIGH", "CRITICAL" -> Component.translatable(baseKey + ".name");
                        default -> Component.literal(prettyLabel(normalizedId));
                    },
                    switch (normalizedId) {
                        case "LOW", "MEDIUM", "HIGH", "CRITICAL" -> Component.translatable(baseKey + ".desc");
                        default -> Component.literal("Use ScamScreener's " + prettyLabel(normalizedId) + " warning threshold.");
                    }
            );
        }

        private static String prettyLabel(String id) {
            String[] parts = id.split("_");
            StringBuilder builder = new StringBuilder();
            for (String s : parts) {
                if (s.isEmpty()) {
                    continue;
                }

                if (!builder.isEmpty()) {
                    builder.append(' ');
                }

                String part = s.toLowerCase(Locale.ROOT);
                builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
            return builder.toString();
        }
    }

    public record PingOption(String id, Component name, Component description) {}
}
