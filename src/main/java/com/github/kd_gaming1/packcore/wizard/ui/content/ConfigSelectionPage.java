package com.github.kd_gaming1.packcore.wizard.ui.content;

import com.github.kd_gaming1.packcore.wizard.ui.ModpackSetupWizard;
import com.github.kd_gaming1.packcore.wizard.ui.theme.WizardTheme;
import com.github.kd_gaming1.packcore.wizard.util.ConfigMetadataReader;
import com.github.kd_gaming1.packcore.wizard.util.ConfigMetadataReader.ConfigMetadata;
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
        var toolkit = Toolkit.getDefaultToolkit();
        var screenSize = toolkit.getScreenSize();
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
            Path configZipPath = officialConfigDir.resolve(name + ".zip");

            // Try to read metadata, fallback to basic info if not available
            var metadataOpt = ConfigMetadataReader.readMetadata(configZipPath);
            ConfigMetadata metadata = metadataOpt.orElse(
                    new ConfigMetadata(name, "No metadata available", null, null, null, null, List.of(), List.of())
            );

            String htmlDescription = convertMetadataToHtml(metadata);
            boolean isRecommended = isConfigRecommended(metadata);

            availableConfigs.add(new ConfigItem(name, htmlDescription, isRecommended, metadata));
        }

        LOGGER.info("Loaded {} configs with metadata", availableConfigs.size());
    }

    /**
     * Determines if a config is recommended based on metadata
     */
    private boolean isConfigRecommended(ConfigMetadata metadata) {
        return metadata.targetResolution() != null &&
                metadata.targetResolution().equalsIgnoreCase(detectedResolution);
    }

    /**
     * Converts metadata to styled HTML for display
     */
    private String convertMetadataToHtml(ConfigMetadata metadata) {
        var html = new StringBuilder();

        // Title
        String configName = metadata.name() != null ? metadata.name() : "Unknown Configuration";
        html.append("<h2>").append(configName).append("</h2>");

        // Description
        if (metadata.description() != null && !metadata.description().trim().isEmpty()) {
            html.append("<p>").append(metadata.description()).append("</p>");
        } else {
            html.append("<p><em>No description available</em></p>");
        }

        // Basic details
        html.append("<h3>Configuration Details:</h3>");
        html.append("<ul>");

        if (metadata.targetResolution() != null) {
            html.append("<li><strong>Target Resolution:</strong> ").append(metadata.targetResolution().toUpperCase()).append("</li>");
        }

        if (metadata.version() != null) {
            html.append("<li><strong>Version:</strong> ").append(metadata.version()).append("</li>");
        }

        if (metadata.author() != null) {
            html.append("<li><strong>Author:</strong> ").append(metadata.author()).append("</li>");
        }

        if (metadata.createdDate() != null) {
            html.append("<li><strong>Created:</strong> ").append(formatDate(metadata.createdDate())).append("</li>");
        }

        html.append("</ul>");

        // Features section
        if (metadata.features() != null && !metadata.features().isEmpty()) {
            html.append("<h3>Features:</h3>");
            html.append("<ul>");
            for (String feature : metadata.features()) {
                html.append("<li>").append(feature).append("</li>");
            }
            html.append("</ul>");
        }

        // Requirements section
        if (metadata.requirements() != null && !metadata.requirements().isEmpty()) {
            html.append("<h3>Requirements:</h3>");
            html.append("<ul>");
            for (String requirement : metadata.requirements()) {
                html.append("<li>").append(requirement).append("</li>");
            }
            html.append("</ul>");
        }

        // Recommendation note
        if (isConfigRecommended(metadata)) {
            html.append("<blockquote>");
            html.append("<strong>💡 Recommended for your system</strong><br>");
            html.append("This configuration matches your detected screen resolution and should provide optimal experience.");
            html.append("</blockquote>");
        }

        return styleConfigHTML(html.toString());
    }

    /**
     * Formats a date string for display
     */
    private String formatDate(String dateString) {
        try {
            // If it's an ISO date, format it nicely
            if (dateString.contains("T")) {
                return dateString.substring(0, 10); // Just the date part
            }
            return dateString;
        } catch (Exception e) {
            return dateString; // Return as-is if parsing fails
        }
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
                "  background-color: " + String.format("#%06X", WizardTheme.BACKGROUND_MEDIUM.getRGB() & 0xFFFFFF) + "; " +
                "  padding: 8px; " +
                "  border-radius: 4px; " +
                "} " +
                "</style></head><body>" + htmlContent + "</body></html>";

        return styledContent;
    }

    private void findRecommendedConfig() {
        recommendedConfig = availableConfigs.stream()
                .filter(config -> config.isRecommended)
                .findFirst()
                .orElse(availableConfigs.isEmpty() ? null : availableConfigs.get(0));

        LOGGER.info("Recommended config: {}", recommendedConfig != null ? recommendedConfig.name : "None");
    }

    private void createUI() {
        setLayout(new BorderLayout());

        // Main content split: info on left, list on right
        var mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(WizardTheme.BACKGROUND_DARK);

        // Left side - Current selection and info
        infoPanel = createInfoPanel();

        // Right side - Available configurations
        var listPanel = createConfigListPanel();

        // Use a split layout
        var splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, infoPanel, listPanel);
        splitPane.setDividerLocation(400);
        splitPane.setBackground(WizardTheme.BACKGROUND_DARK);
        splitPane.setBorder(null);

        mainContent.add(splitPane, BorderLayout.CENTER);

        // Top explanation
        var explanationPanel = createExplanationPanel();
        mainContent.add(explanationPanel, BorderLayout.NORTH);

        add(mainContent, BorderLayout.CENTER);
    }

    private JPanel createExplanationPanel() {
        var panel = createTitledPanel("🎯 Choose Your Configuration",
                "Select the configuration that matches your setup and preferences");

        var contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        // Main explanation
        var explanationText = new JTextArea(
                "Each configuration contains pre-configured settings for a specific screen resolution. " +
                        "The configurations include metadata with detailed information about their purpose, features, and requirements. " +
                        "Select a configuration from the list to view its details and apply it to the modpack."
        );
        explanationText.setEditable(false);
        explanationText.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        explanationText.setForeground(WizardTheme.TEXT_PRIMARY);
        explanationText.setFont(WizardTheme.getBodyFont());
        explanationText.setLineWrap(true);
        explanationText.setWrapStyleWord(true);
        explanationText.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Detection info
        var detectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        detectionPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

        var detectedLabel = new JLabel("🖥️ Detected Resolution: " + detectedResolution);
        detectedLabel.setFont(WizardTheme.getBodyFont());
        detectedLabel.setForeground(WizardTheme.TEXT_PRIMARY);

        var recommendedLabel = new JLabel("💡 Recommended: " +
                (recommendedConfig != null ? recommendedConfig.metadata.name() : "None available"));
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
        var panel = createTitledPanel("📦 Selected Configuration",
                "Details about your chosen configuration");
        panel.setPreferredSize(new Dimension(380, 400));

        var contentPanel = new JPanel(new BorderLayout());
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

        // Show placeholder content when no config is selected
        String defaultHtml = styleConfigHTML(
                "<h3>🎮 Select a Configuration</h3>" +
                        "<p>Choose a configuration from the list on the right to see detailed information here.</p>" +
                        "<h3>What configurations include:</h3>" +
                        "<ul>" +
                        "<li><strong>Video Settings:</strong> Optimized graphics options for your resolution</li>" +
                        "<li><strong>UI Layouts:</strong> Properly positioned mod interfaces</li>" +
                        "<li><strong>Performance Tweaks:</strong> System-specific optimizations</li>" +
                        "<li><strong>Resource Packs:</strong> Visual enhancements and texture improvements</li>" +
                        "<li><strong>Mod Configurations:</strong> Pre-configured mod settings</li>" +
                        "</ul>" +
                        "<p><em>All configurations are created with metadata that provides detailed information about their purpose and requirements.</em></p>"
        );
        configDescriptionPane.setText(defaultHtml);

        var descScrollPane = new JScrollPane(configDescriptionPane);
        descScrollPane.setBorder(BorderFactory.createLineBorder(WizardTheme.BORDER));
        descScrollPane.setPreferredSize(new Dimension(360, 280));
        descScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        contentPanel.add(selectedConfigLabel, BorderLayout.NORTH);
        contentPanel.add(descScrollPane, BorderLayout.CENTER);

        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createConfigListPanel() {
        var panel = createTitledPanel("📋 Available Configurations",
                "Choose the configuration that best matches your needs");
        panel.setPreferredSize(new Dimension(400, 400));

        var contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        configList = new JList<>(availableConfigs.toArray(new ConfigItem[0]));
        configList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configList.setBackground(WizardTheme.BACKGROUND_LIGHT);
        configList.setForeground(WizardTheme.TEXT_PRIMARY);
        configList.setFont(WizardTheme.getBodyFont());
        configList.setCellRenderer(new ConfigListCellRenderer());

        // Pre-select recommended config if available
        if (recommendedConfig != null) {
            configList.setSelectedValue(recommendedConfig, true);
            selectedResolution = recommendedConfig.name;
            updateInfoPanel(recommendedConfig);
        }

        // Selection listener
        configList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                var selected = configList.getSelectedValue();
                if (selected != null) {
                    selectedResolution = selected.name;
                    updateInfoPanel(selected);
                    wizard.getNavigationPanel().setConfigSelected(true);
                    LOGGER.info("Selected config: {} ({})", selectedResolution, selected.metadata.name());
                }
            }
        });

        var scrollPane = new JScrollPane(configList);
        scrollPane.setBorder(BorderFactory.createLineBorder(WizardTheme.BORDER));
        scrollPane.setPreferredSize(new Dimension(380, 300));

        contentPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private void updateInfoPanel(ConfigItem config) {
        String displayName = config.metadata.name() != null ? config.metadata.name() : config.name;
        selectedConfigLabel.setText(displayName);
        selectedConfigLabel.setForeground(WizardTheme.ACCENT_GOLD);

        configDescriptionPane.setText(config.description);

        if (config.isRecommended) {
            selectedConfigLabel.setText(displayName + " ⭐ (Recommended)");
        }
    }

    private static List<String> getConfigNames(Path dirPath) {
        var configNames = new ArrayList<String>();
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
        final ConfigMetadata metadata;

        ConfigItem(String name, String description, boolean isRecommended, ConfigMetadata metadata) {
            this.name = name;
            this.description = description;
            this.isRecommended = isRecommended;
            this.metadata = metadata;
        }

        @Override
        public String toString() {
            String displayName = metadata.name() != null ? metadata.name() : name;
            return displayName + (isRecommended ? " ⭐" : "");
        }
    }

    private class ConfigListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            var item = (ConfigItem) value;
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