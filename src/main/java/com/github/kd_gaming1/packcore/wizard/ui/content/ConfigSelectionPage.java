package com.github.kd_gaming1.packcore.wizard.ui.content;

import com.github.kd_gaming1.packcore.wizard.ui.ModpackSetupWizard;
import com.github.kd_gaming1.packcore.wizard.ui.theme.WizardTheme;
import com.github.kd_gaming1.packcore.wizard.util.MarkdownToHtmlConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ConfigSelectionPage extends MainContentInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigSelectionPage.class);

    private final ModpackSetupWizard wizard;
    private final Path runDir;

    // UI Components
    private JList<ConfigItem> configList;
    private JPanel infoPanel;
    private JLabel selectedConfigLabel;
    private JTextPane configDescriptionPane;

    // Data
    private List<ConfigItem> availableConfigs;
    private String detectedResolution;
    private ConfigItem recommendedConfig;

    public static String selectedResolution;

    public ConfigSelectionPage(Path runDir, ModpackSetupWizard wizard) {
        super();
        this.wizard = wizard;
        this.runDir = runDir;

        detectCurrentResolution();
        loadAvailableConfigs();
        findRecommendedConfig();
        createUI();
    }

    private void detectCurrentResolution() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;

        detectedResolution = categorizeResolution(width, height);
        LOGGER.info("Detected screen resolution: {}x{} -> {}", width, height, detectedResolution);
    }

    private String categorizeResolution(int width, int height) {
        if (height >= 2160) return "4k";
        else if (height >= 1440) return "1440p";
        else if (height >= 1080) return "1080p";
        else return "720p";
    }

    private void loadAvailableConfigs() {
        Path officialConfigDir = runDir.resolve("packcore/modpack_config/official_configs");
        List<String> configNames = getConfigNames(officialConfigDir);

        availableConfigs = new ArrayList<>();
        for (String name : configNames) {
            String description = loadConfigDescription(name);
            availableConfigs.add(new ConfigItem(name, description,
                    name.toLowerCase().contains(detectedResolution)));
        }

        LOGGER.info("Available configs: {}", configNames);
    }

    private String loadConfigDescription(String configName) {
        // Try to load custom markdown description from config_descriptions folder
        Path descriptionFile = runDir.resolve("packcore/config_descriptions").resolve(configName + ".md");

        try {
            if (Files.exists(descriptionFile)) {
                MarkdownToHtmlConverter converter = new MarkdownToHtmlConverter();
                String htmlContent = converter.convertMarkdownFileToHtml(descriptionFile.toString());
                LOGGER.info("Loaded custom markdown description for config: {}", configName);
                return styleConfigHTML(htmlContent);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to load custom description for {}: {}", configName, e.getMessage());
        }

        // Fall back to generated description
        return generateDefaultDescription(configName);
    }

    private String styleConfigHTML(String htmlContent) {
        // Apply dark theme styling to config description HTML
        String styledContent = "<html><head><style>" +
                "body { " +
                "  font-family: 'Segoe UI', sans-serif; " +
                "  color: " + String.format("#%06X", WizardTheme.TEXT_PRIMARY.getRGB() & 0xFFFFFF) + "; " +
                "  background-color: " + String.format("#%06X", WizardTheme.BACKGROUND_LIGHT.getRGB() & 0xFFFFFF) + "; " +
                "  line-height: 1.5; " +
                "  margin: 10px; " +
                "  padding: 5px; " +
                "} " +
                "h1, h2, h3 { color: " + String.format("#%06X", WizardTheme.ACCENT_GOLD.getRGB() & 0xFFFFFF) + "; margin-top: 10px; } " +
                "h1 { font-size: 16px; } " +
                "h2 { font-size: 14px; } " +
                "h3 { font-size: 13px; } " +
                "strong { color: " + String.format("#%06X", 0xFFFFFF) + "; } " +
                "em { color: " + String.format("#%06X", WizardTheme.TEXT_SECONDARY.getRGB() & 0xFFFFFF) + "; } " +
                "code { " +
                "  background-color: " + String.format("#%06X", WizardTheme.BACKGROUND_DARK.getRGB() & 0xFFFFFF) + "; " +
                "  color: " + String.format("#%06X", WizardTheme.ACCENT_GOLD.getRGB() & 0xFFFFFF) + "; " +
                "  padding: 2px 4px; " +
                "  border-radius: 3px; " +
                "  font-size: 11px; " +
                "} " +
                "ul li { margin-bottom: 3px; } " +
                "blockquote { " +
                "  border-left: 3px solid " + String.format("#%06X", WizardTheme.ACCENT_GOLD.getRGB() & 0xFFFFFF) + "; " +
                "  padding-left: 10px; " +
                "  margin-left: 5px; " +
                "  font-style: italic; " +
                "} " +
                "</style></head><body>" + htmlContent + "</body></html>";

        return styledContent;
    }

    private String generateDefaultDescription(String configName) {
        String markdownContent = generateDefaultMarkdown(configName);
        return styleConfigHTML(markdownContent);
    }

    private String generateDefaultMarkdown(String configName) {
        String displayName = configName.replace("_", " ");

        if (configName.toLowerCase().contains("4k")) {
            return "<h2>" + displayName + "</h2>" +
                    "<p>Configuration optimized for 4K displays and high-end systems.</p>" +
                    "<h3>Features:</h3>" +
                    "<ul>" +
                    "<li><strong>Display:</strong> 4K (3840x2160) and higher resolutions</li>" +
                    "<li><strong>Graphics:</strong> Ultra-high quality textures and settings</li>" +
                    "<li><strong>Render Distance:</strong> 20+ chunks recommended</li>" +
                    "<li><strong>UI Scaling:</strong> Large scaling for 4K clarity</li>" +
                    "<li><strong>Performance:</strong> Requires powerful graphics card</li>" +
                    "</ul>" +
                    "<blockquote><strong>Best for:</strong> High-end gaming systems with 4K monitors</blockquote>";
        } else if (configName.toLowerCase().contains("1440p")) {
            return "<h2>" + displayName + "</h2>" +
                    "<p>High-quality configuration designed for 1440p displays.</p>" +
                    "<h3>Features:</h3>" +
                    "<ul>" +
                    "<li><strong>Display:</strong> QHD (2560x1440) monitors</li>" +
                    "<li><strong>Graphics:</strong> High settings with enhanced textures</li>" +
                    "<li><strong>Render Distance:</strong> 16 chunks recommended</li>" +
                    "<li><strong>UI Scaling:</strong> Medium scaling for readability</li>" +
                    "<li><strong>Performance:</strong> Balanced quality and performance</li>" +
                    "</ul>" +
                    "<blockquote><strong>Best for:</strong> Mid-to-high-end gaming PCs with QHD displays</blockquote>";
        } else if (configName.toLowerCase().contains("1080p")) {
            return "<h2>" + displayName + "</h2>" +
                    "<p>Balanced configuration optimized for Full HD displays.</p>" +
                    "<h3>Features:</h3>" +
                    "<ul>" +
                    "<li><strong>Display:</strong> Full HD (1920x1080) monitors</li>" +
                    "<li><strong>Graphics:</strong> Medium-high settings for best balance</li>" +
                    "<li><strong>Render Distance:</strong> 12 chunks recommended</li>" +
                    "<li><strong>UI Scaling:</strong> Standard scaling</li>" +
                    "<li><strong>Performance:</strong> Excellent performance-to-quality ratio</li>" +
                    "</ul>" +
                    "<blockquote><strong>Best for:</strong> Most gaming PCs and laptops</blockquote>";
        } else {
            return "<h2>" + displayName + "</h2>" +
                    "<p>Performance-focused configuration for maximum framerate.</p>" +
                    "<h3>Features:</h3>" +
                    "<ul>" +
                    "<li><strong>Display:</strong> Lower resolution or performance-focused</li>" +
                    "<li><strong>Graphics:</strong> Performance-optimized settings</li>" +
                    "<li><strong>Render Distance:</strong> 8-10 chunks for smooth gameplay</li>" +
                    "<li><strong>UI Scaling:</strong> Compact for maximum view area</li>" +
                    "<li><strong>Performance:</strong> Maximum framerate priority</li>" +
                    "</ul>" +
                    "<blockquote><strong>Best for:</strong> Budget systems or competitive players</blockquote>";
        }
    }

    private void findRecommendedConfig() {
        recommendedConfig = availableConfigs.stream()
                .filter(config -> config.name.toLowerCase().contains(detectedResolution))
                .findFirst()
                .orElse(availableConfigs.isEmpty() ? null : availableConfigs.get(0));
    }

    private void createUI() {
        setLayout(new BorderLayout());

        // Main content split: info on left, list on right
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(WizardTheme.BACKGROUND_DARK);

        // Left side - Current selection and info
        infoPanel = createInfoPanel();

        // Right side - Available configurations
        JPanel listPanel = createConfigListPanel();

        // Use a split layout
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, infoPanel, listPanel);
        splitPane.setDividerLocation(400);
        splitPane.setBackground(WizardTheme.BACKGROUND_DARK);
        splitPane.setBorder(null);

        mainContent.add(splitPane, BorderLayout.CENTER);

        // Top explanation
        JPanel explanationPanel = createExplanationPanel();
        mainContent.add(explanationPanel, BorderLayout.NORTH);

        add(mainContent, BorderLayout.CENTER);
    }

    private JPanel createExplanationPanel() {
        JPanel panel = createTitledPanel("🎯 Choose Your Configuration",
                "Select the configuration that matches your setup and preferences");

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        // Main explanation
        JTextArea explanationText = new JTextArea(
                "Each configuration contains pre-optimized settings for different screen resolutions and system capabilities. " +
                        "Choosing the right configuration ensures the best balance of performance and visual quality for your setup. " +
                        "If you're unsure, the recommended option will work well for most users."
        );
        explanationText.setEditable(false);
        explanationText.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        explanationText.setForeground(WizardTheme.TEXT_PRIMARY);
        explanationText.setFont(WizardTheme.getBodyFont());
        explanationText.setLineWrap(true);
        explanationText.setWrapStyleWord(true);
        explanationText.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Detection info
        JPanel detectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        detectionPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

        JLabel detectedLabel = new JLabel("🖥️ Detected Resolution: " + detectedResolution);
        detectedLabel.setFont(WizardTheme.getBodyFont());
        detectedLabel.setForeground(WizardTheme.TEXT_PRIMARY);

        JLabel recommendedLabel = new JLabel("💡 Recommended: " +
                (recommendedConfig != null ? recommendedConfig.name : "None available"));
        recommendedLabel.setFont(WizardTheme.getBodyFont());
        recommendedLabel.setForeground(WizardTheme.ACCENT_GOLD);

        detectionPanel.add(detectedLabel);
        detectionPanel.add(Box.createHorizontalStrut(30));
        detectionPanel.add(recommendedLabel);

        contentPanel.add(explanationText, BorderLayout.CENTER);
        contentPanel.add(detectionPanel, BorderLayout.SOUTH);
        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createInfoPanel() {
        JPanel panel = createTitledPanel("📦 Selected Configuration",
                "Details about your chosen configuration");
        panel.setPreferredSize(new Dimension(380, 400));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        // Selected config display
        selectedConfigLabel = new JLabel("No configuration selected");
        selectedConfigLabel.setFont(WizardTheme.getHeaderFont());
        selectedConfigLabel.setForeground(WizardTheme.TEXT_MUTED);
        selectedConfigLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        // Description pane (using JTextPane for HTML support)
        configDescriptionPane = new JTextPane();
        configDescriptionPane.setContentType("text/html");
        configDescriptionPane.setEditable(false);
        configDescriptionPane.setBackground(WizardTheme.BACKGROUND_LIGHT);
        configDescriptionPane.setForeground(WizardTheme.TEXT_PRIMARY);
        configDescriptionPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String defaultHtml = styleConfigHTML(
                "<h3>Select a Configuration</h3>" +
                        "<p>Choose a configuration from the list to see detailed information here.</p>" +
                        "<h3>What's Included:</h3>" +
                        "<ul>" +
                        "<li><strong>Video Settings:</strong> Optimized graphics options</li>" +
                        "<li><strong>UI Layout:</strong> Positioned mod interfaces</li>" +
                        "<li><strong>Performance Tweaks:</strong> System optimizations</li>" +
                        "<li><strong>Resource Packs:</strong> Visual enhancements</li>" +
                        "</ul>"
        );
        configDescriptionPane.setText(defaultHtml);

        JScrollPane descScrollPane = new JScrollPane(configDescriptionPane);
        descScrollPane.setBorder(BorderFactory.createLineBorder(WizardTheme.BORDER));
        descScrollPane.setPreferredSize(new Dimension(360, 280));
        descScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        contentPanel.add(selectedConfigLabel, BorderLayout.NORTH);
        contentPanel.add(descScrollPane, BorderLayout.CENTER);

        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createConfigListPanel() {
        JPanel panel = createTitledPanel("📋 Available Configurations",
                "Choose the configuration that best matches your needs");
        panel.setPreferredSize(new Dimension(400, 400));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        configList = new JList<>(availableConfigs.toArray(new ConfigItem[0]));
        configList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configList.setBackground(WizardTheme.BACKGROUND_LIGHT);
        configList.setForeground(WizardTheme.TEXT_PRIMARY);
        configList.setFont(WizardTheme.getBodyFont());
        configList.setCellRenderer(new ConfigListCellRenderer());

        // Pre-select recommended config
        if (recommendedConfig != null) {
            configList.setSelectedValue(recommendedConfig, true);
            selectedResolution = recommendedConfig.name;
            updateInfoPanel(recommendedConfig);
        }

        // Selection listener
        configList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ConfigItem selected = configList.getSelectedValue();
                if (selected != null) {
                    selectedResolution = selected.name;
                    updateInfoPanel(selected);
                    wizard.getNavigationPanel().setConfigSelected(true);
                    LOGGER.info("Selected config: {}", selectedResolution);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(configList);
        scrollPane.setBorder(BorderFactory.createLineBorder(WizardTheme.BORDER));
        scrollPane.setPreferredSize(new Dimension(380, 300));

        contentPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private void updateInfoPanel(ConfigItem config) {
        selectedConfigLabel.setText(config.name);
        selectedConfigLabel.setForeground(WizardTheme.ACCENT_GOLD);

        configDescriptionPane.setText(config.description);

        if (config.isRecommended) {
            selectedConfigLabel.setText(config.name + " ⭐ (Recommended)");
        }
    }

    private static List<String> getConfigNames(Path dirPath) {
        List<String> configNames = new ArrayList<>();
        try (Stream<Path> paths = Files.list(dirPath)) {
            paths.filter(path -> path.toString().endsWith(".zip"))
                    .map(path -> path.getFileName().toString())
                    .map(fileName -> fileName.replace(".zip", ""))
                    .forEach(configNames::add);
        } catch (IOException e) {
            LOGGER.error("Failed to list configs in directory: {}", dirPath, e);
        }
        return configNames;
    }

    // Helper classes
    private static class ConfigItem {
        final String name;
        final String description;
        final boolean isRecommended;

        ConfigItem(String name, String description, boolean isRecommended) {
            this.name = name;
            this.description = description;
            this.isRecommended = isRecommended;
        }

        @Override
        public String toString() {
            return name + (isRecommended ? " ⭐" : "");
        }
    }

    private class ConfigListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            ConfigItem item = (ConfigItem) value;
            setText(item.toString());

            if (isSelected) {
                setBackground(WizardTheme.ACCENT_GOLD);
                setForeground(WizardTheme.BACKGROUND_DARK);
            } else {
                setBackground(WizardTheme.BACKGROUND_LIGHT);
                setForeground(item.isRecommended ? WizardTheme.ACCENT_GOLD : WizardTheme.TEXT_PRIMARY);
            }

            setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            setFont(WizardTheme.getBodyFont());

            return this;
        }
    }
}