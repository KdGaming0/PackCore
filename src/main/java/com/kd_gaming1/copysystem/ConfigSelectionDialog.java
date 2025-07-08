package com.kd_gaming1.copysystem;

import com.kd_gaming1.config.PackCoreConfig;
import eu.midnightdust.lib.config.MidnightConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Modern, redesigned configuration selection dialog with improved UX.
 * Features customizable markdown content, modern styling, and better user guidance.
 */
public class ConfigSelectionDialog extends JFrame {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigSelectionDialog.class);

    // Modern color scheme
    private static final Color PRIMARY_COLOR = new Color(52, 152, 219);
    private static final Color SUCCESS_COLOR = new Color(46, 204, 113);
    private static final Color WARNING_COLOR = new Color(241, 196, 15);
    private static final Color DANGER_COLOR = new Color(231, 76, 60);
    private static final Color BACKGROUND_COLOR = new Color(249, 249, 249);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color TEXT_COLOR = new Color(44, 62, 80);
    private static final Color BORDER_COLOR = new Color(220, 221, 225);

    private final ConfigExtractionService extractionService;
    private final CountDownLatch completionLatch = new CountDownLatch(1);
    private final AtomicBoolean dialogResult = new AtomicBoolean(false);

    private JList<ConfigInfo> officialConfigList;
    private JList<ConfigInfo> customConfigList;
    private JProgressBar progressBar;
    private JButton extractButton;
    private JButton skipButton;
    private JButton helpButton;
    private JEditorPane contentPane;
    private JSplitPane mainSplitPane;

    public ConfigSelectionDialog(ConfigExtractionService extractionService) {
        super("PackCore - Configuration Manager");
        this.extractionService = extractionService;

        // Set modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.debug("Could not set system look and feel", e);
        }

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleSkip();
            }
        });

        initializeComponents();
        layoutComponents();
        setupEventHandlers();

        // Apply modern styling
        applyModernStyling();
    }

    private void initializeComponents() {
        // Create content pane for markdown content
        contentPane = new JEditorPane();
        contentPane.setContentType("text/html");
        contentPane.setEditable(false);
        contentPane.setText(extractionService.getDialogContent());
        contentPane.setBackground(CARD_BACKGROUND);
        contentPane.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Enable hyperlink support
        contentPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(new URI(e.getURL().toString()));
                } catch (Exception ex) {
                    LOGGER.debug("Could not open link: {}", e.getURL(), ex);
                }
            }
        });

        // Create config lists with improved data
        List<ConfigInfo> officialConfigs = extractionService.getOfficialConfigs();
        List<ConfigInfo> customConfigs = extractionService.getCustomConfigs();

        officialConfigList = createStyledConfigList(officialConfigs);
        customConfigList = createStyledConfigList(customConfigs);

        // Create modern progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setVisible(false);
        progressBar.setBorderPainted(false);
        progressBar.setBackground(BACKGROUND_COLOR);
        progressBar.setForeground(PRIMARY_COLOR);

        // Create modern buttons
        extractButton = createStyledButton("🚀 Extract & Apply Configuration", SUCCESS_COLOR);
        skipButton = createStyledButton("⏭️ Skip & Continue", WARNING_COLOR);
        helpButton = createStyledButton("❓ Help", PRIMARY_COLOR);
    }

    private JList<ConfigInfo> createStyledConfigList(List<ConfigInfo> configs) {
        JList<ConfigInfo> list = new JList<>(configs.toArray(new ConfigInfo[0]));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new ModernConfigRenderer());
        list.setBackground(CARD_BACKGROUND);
        list.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Add selection styling
        list.setSelectionBackground(PRIMARY_COLOR);
        list.setSelectionForeground(Color.WHITE);

        return list;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 14f));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(12, 20, 12, 20));

        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }
        });

        return button;
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND_COLOR);

        // Create header panel
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Create main content with split pane
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setLeftComponent(createContentPanel());
        mainSplitPane.setRightComponent(createConfigSelectionPanel());
        mainSplitPane.setDividerLocation(600);
        mainSplitPane.setResizeWeight(0.6);
        mainSplitPane.setBorder(null);

        add(mainSplitPane, BorderLayout.CENTER);

        // Create bottom panel
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);

        // Set window properties
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 600));
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PRIMARY_COLOR);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("PackCore Configuration Manager");
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subtitleLabel = new JLabel("Choose your perfect Minecraft configuration");
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(255, 255, 255, 180));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(titleLabel, BorderLayout.CENTER);
        panel.add(subtitleLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 10));
        panel.setBackground(BACKGROUND_COLOR);

        // Create a card-style container
        JPanel cardPanel = new JPanel(new BorderLayout());
        cardPanel.setBackground(CARD_BACKGROUND);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(0, 0, 0, 0)
        ));

        JScrollPane scrollPane = new JScrollPane(contentPane);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        cardPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(cardPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createConfigSelectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 10, 20, 20));
        panel.setBackground(BACKGROUND_COLOR);

        // Configuration selection area
        JPanel configPanel = new JPanel(new GridLayout(2, 1, 0, 15));
        configPanel.setBackground(BACKGROUND_COLOR);

        // Official configs
        JPanel officialPanel = createConfigPanel("📦 Official Configurations",
                "Recommended settings from modpack creators", officialConfigList);
        configPanel.add(officialPanel);

        // Custom configs
        JPanel customPanel = createConfigPanel("🎨 Custom Configurations",
                "Your personal or community settings", customConfigList);
        configPanel.add(customPanel);

        panel.add(configPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createConfigPanel(String title, String description, JList<ConfigInfo> list) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(CARD_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(CARD_BACKGROUND);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        titleLabel.setForeground(TEXT_COLOR);

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        descLabel.setForeground(new Color(TEXT_COLOR.getRed(), TEXT_COLOR.getGreen(), TEXT_COLOR.getBlue(), 150));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(descLabel, BorderLayout.SOUTH);

        // List container
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scrollPane.setPreferredSize(new Dimension(250, 120));

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(15, 20, 20, 20));

        // Progress bar
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBackground(BACKGROUND_COLOR);
        progressPanel.add(progressBar, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.add(extractButton);
        buttonPanel.add(skipButton);
        buttonPanel.add(helpButton);

        panel.add(progressPanel, BorderLayout.NORTH);
        panel.add(Box.createVerticalStrut(15), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void applyModernStyling() {
        // Set custom fonts if available
        try {
            Font segoeUI = new Font("Segoe UI", Font.PLAIN, 12);
            UIManager.put("defaultFont", segoeUI);
        } catch (Exception e) {
            LOGGER.debug("Could not set custom font", e);
        }
    }

    private void setupEventHandlers() {
        // Selection synchronization
        officialConfigList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && officialConfigList.getSelectedIndex() != -1) {
                customConfigList.clearSelection();
            }
        });

        customConfigList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && customConfigList.getSelectedIndex() != -1) {
                officialConfigList.clearSelection();
            }
        });

        extractButton.addActionListener(e -> handleExtraction());
        skipButton.addActionListener(e -> handleSkip());
        helpButton.addActionListener(e -> handleHelp());
    }

    private void handleExtraction() {
        ConfigInfo selectedConfig = getSelectedConfig();
        ConfigType configType = getSelectedConfigType();

        if (selectedConfig == null) {
            showModernDialog("No Configuration Selected",
                    "Please select a configuration from either the Official or Custom list.",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String configTypeText = configType == ConfigType.OFFICIAL ? "Official" : "Custom";
        String confirmationMessage;

        // Check if this is the first startup using PackCoreConfig
        if (PackCoreConfig.isFirstStartup) {
            confirmationMessage = String.format(
                    "Extract the %s configuration '%s'?\n\n" +
                            "This will set up your initial mod configuration with the selected preset.\n" +
                            "You can always change configurations later using PackCore commands.",
                    configTypeText.toLowerCase(), selectedConfig.getDisplayName());
        } else {
            confirmationMessage = String.format(
                    "Apply the %s configuration '%s'?\n\n" +
                            "This will replace your current mod settings with the settings from this configuration.\n" +
                            "Make sure you've backed up important configurations first!",
                    configTypeText.toLowerCase(), selectedConfig.getDisplayName());
        }

        int confirm = showModernConfirmDialog("Confirm Configuration Extraction", confirmationMessage);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Start extraction
        setControlsEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("Preparing extraction...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return extractionService.extractConfig(selectedConfig.getName(), configType, progress -> {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(progress);
                        progressBar.setString("Extracting... " + progress + "%");
                    });
                });
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    handleExtractionComplete(success, selectedConfig.getName());
                } catch (Exception e) {
                    LOGGER.error("Extraction failed", e);
                    handleExtractionComplete(false, selectedConfig.getName());
                }
            }
        };

        worker.execute();
    }

    private void handleExtractionComplete(boolean success, String configName) {
        progressBar.setVisible(false);

        if (success) {
            String successMessage;
            if (PackCoreConfig.isFirstStartup) {
                successMessage = String.format(
                        "✅ Initial Configuration Set Successfully!\n\n" +
                                "The configuration '%s' has been applied as your starting setup.\n" +
                                "Minecraft will continue loading shortly.\n\n" +
                                "Tip: You can create backups of your settings using '/packcore archive' in-game.",
                        configName);
            } else {
                successMessage = String.format(
                        "✅ Configuration Applied Successfully!\n\n" +
                                "The configuration '%s' has been applied and will replace your previous settings.\n" +
                                "Minecraft will continue loading shortly.",
                        configName);
            }

            showModernDialog("Configuration Applied", successMessage, JOptionPane.INFORMATION_MESSAGE);
            dialogResult.set(true);
            finishDialog();
        } else {
            showModernDialog("Extraction Failed",
                    String.format("Failed to apply configuration '%s'.\n" +
                            "Please check the logs for details.", configName),
                    JOptionPane.ERROR_MESSAGE);
            setControlsEnabled(true);
        }
    }

    private void handleSkip() {
        int result = showModernConfirmDialog("Skip Configuration?",
                "Continue without applying any configurations?\n\n" +
                        "You can always apply configurations later using the PackCore commands.");

        if (result == JOptionPane.YES_OPTION) {
            dialogResult.set(true);
            finishDialog();
        }
    }

    private void handleHelp() {
        // Get links from the markdown service
        java.util.Map<String, String> helpLinks = extractionService.getHelpLinks();
        boolean hasCustomContent = extractionService.hasCustomContent();

        // Create a modern help dialog
        JDialog helpDialog = new JDialog(this, "Help & Resources", true);
        helpDialog.setLayout(new BorderLayout());
        helpDialog.setSize(500, 400);
        helpDialog.setLocationRelativeTo(this);

        // Create content
        JEditorPane helpContent = new JEditorPane();
        helpContent.setContentType("text/html");
        helpContent.setEditable(false);
        helpContent.setBackground(CARD_BACKGROUND);
        helpContent.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Build help content with links
        StringBuilder helpHtml = new StringBuilder();
        helpHtml.append("<html><head><style>");
        helpHtml.append("body { font-family: 'Segoe UI', Arial, sans-serif; font-size: 14px; line-height: 1.6; }");
        helpHtml.append("h2 { color: #2c3e50; margin-bottom: 15px; }");
        helpHtml.append("h3 { color: #34495e; margin-top: 20px; margin-bottom: 10px; }");
        helpHtml.append("p { margin-bottom: 10px; }");
        helpHtml.append("a { color: #3498db; text-decoration: none; font-weight: bold; }");
        helpHtml.append("a:hover { text-decoration: underline; }");
        helpHtml.append("code { background-color: #ecf0f1; padding: 2px 4px; border-radius: 3px; font-family: monospace; }");
        helpHtml.append("ul { margin-left: 20px; }");
        helpHtml.append("li { margin-bottom: 5px; }");
        helpHtml.append("</style></head><body>");

        if (hasCustomContent) {
            // Custom content exists - make it feel native to the modpack
            helpHtml.append("<h2>🎮 Help & Resources</h2>");
        } else {
            // Generic content
            helpHtml.append("<h2>🎮 PackCore Help & Resources</h2>");
        }

        // PUT DISCORD SECTION FIRST - Most important for users to see immediately
        if (!helpLinks.isEmpty()) {
            // Check if there's a Discord link
            String discordLink = findDiscordLink(helpLinks);

            if (discordLink != null) {
                helpHtml.append("<h3>💬 Need Help?</h3>");
                helpHtml.append("<p>Visit our Discord server for live support and community help:</p>");
                helpHtml.append("<ul>");
                helpHtml.append("<li><a href=\"").append(discordLink).append("\">")
                        .append(getDiscordLinkName(helpLinks, discordLink)).append("</a></li>");
                helpHtml.append("</ul>");
            }
        }

        helpHtml.append("<h3>📋 Available Commands</h3>");
        helpHtml.append("<ul>");
        helpHtml.append("<li><code>/packcore help</code> - Show all available commands</li>");
        helpHtml.append("<li><code>/packcore archive</code> - Create a backup of your current configuration</li>");
        helpHtml.append("<li><code>/packcore dialog true/false</code> - Enable or disable this startup dialog</li>");
        helpHtml.append("<li><code>/packcore extract [config]</code> - Extract a specific configuration</li>");
        helpHtml.append("<li><code>/packcore list</code> - List all available configurations</li>");
        helpHtml.append("</ul>");

        if (!hasCustomContent) {
            // Only show file locations for generic content
            helpHtml.append("<h3>📁 File Locations</h3>");
            helpHtml.append("<p><strong>Configuration Folder:</strong><br>");
            helpHtml.append("<code>").append(extractionService.getMarkdownFile().getParent()).append("</code></p>");

            helpHtml.append("<p><strong>Dialog Content File:</strong><br>");
            helpHtml.append("<code>").append(extractionService.getMarkdownFile().getAbsolutePath()).append("</code></p>");
        }

        // Show other links at the bottom
        if (!helpLinks.isEmpty()) {
            String discordLink = findDiscordLink(helpLinks);
            java.util.Map<String, String> otherLinks = getOtherLinks(helpLinks, discordLink);

            if (!otherLinks.isEmpty()) {
                helpHtml.append("<h3>🔗 Other Useful Links</h3>");
                helpHtml.append("<ul>");
                for (java.util.Map.Entry<String, String> link : otherLinks.entrySet()) {
                    helpHtml.append("<li><a href=\"").append(link.getValue()).append("\">")
                            .append(link.getKey()).append("</a></li>");
                }
                helpHtml.append("</ul>");
            } else if (discordLink == null) {
                // No Discord link found and no other links, show all links normally
                helpHtml.append("<h3>🔗 Useful Links</h3>");
                helpHtml.append("<ul>");
                for (java.util.Map.Entry<String, String> link : helpLinks.entrySet()) {
                    helpHtml.append("<li><a href=\"").append(link.getValue()).append("\">")
                            .append(link.getKey()).append("</a></li>");
                }
                helpHtml.append("</ul>");
            }
        }

        if (!hasCustomContent) {
            // Only show customization info for generic content
            helpHtml.append("<h3>⚙️ Customization</h3>");
            helpHtml.append("<p>Want to customize this dialog? Click the button below to create a sample markdown file that you can edit:</p>");
        }

        helpHtml.append("</body></html>");
        helpContent.setText(helpHtml.toString());

        // IMPORTANT: Reset scroll position to top after setting content
        SwingUtilities.invokeLater(() -> {
            helpContent.setCaretPosition(0);
            helpContent.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
        });

        // Add hyperlink support
        helpContent.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(new URI(e.getURL().toString()));
                } catch (Exception ex) {
                    LOGGER.debug("Could not open link: {}", e.getURL(), ex);
                    // Show a message with the URL so user can copy it
                    JOptionPane.showMessageDialog(helpDialog,
                            "Could not open link automatically. Please copy this URL:\n" + e.getURL().toString(),
                            "Open Link", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(helpContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Ensure scroll pane starts at the top
        scrollPane.getViewport().setViewPosition(new Point(0, 0));

        // Bottom panel with buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(BACKGROUND_COLOR);
        bottomPanel.setBorder(new EmptyBorder(10, 20, 15, 20));

        // Only show "Create Sample File" button if no custom content exists
        if (!hasCustomContent) {
            JButton createSampleButton = createStyledButton("📝 Create Sample File", PRIMARY_COLOR);
            createSampleButton.addActionListener(e -> {
                if (extractionService.createSampleMarkdownFile()) {
                    JOptionPane.showMessageDialog(helpDialog,
                            "Sample markdown file created successfully!\nLocation: " + extractionService.getMarkdownFile().getAbsolutePath(),
                            "File Created", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(helpDialog,
                            "Failed to create sample file. Check the logs for details.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            bottomPanel.add(createSampleButton);
        }

        JButton closeButton = createStyledButton("✅ Close", SUCCESS_COLOR);
        closeButton.addActionListener(e -> helpDialog.dispose());
        bottomPanel.add(closeButton);

        helpDialog.add(scrollPane, BorderLayout.CENTER);
        helpDialog.add(bottomPanel, BorderLayout.SOUTH);

        // Show dialog and ensure it starts at top
        helpDialog.setVisible(true);

        // Additional scroll reset after dialog is visible (extra safety)
        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(0);
        });
    }

    /**
     * Finds Discord link in the help links map
     */
    private String findDiscordLink(java.util.Map<String, String> helpLinks) {
        for (java.util.Map.Entry<String, String> entry : helpLinks.entrySet()) {
            String linkName = entry.getKey().toLowerCase();
            String url = entry.getValue().toLowerCase();

            if (linkName.contains("discord") || url.contains("discord.gg") || url.contains("discord.com")) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Gets the display name for the Discord link
     */
    private String getDiscordLinkName(java.util.Map<String, String> helpLinks, String discordUrl) {
        for (java.util.Map.Entry<String, String> entry : helpLinks.entrySet()) {
            if (entry.getValue().equals(discordUrl)) {
                return entry.getKey();
            }
        }
        return "Discord Server";
    }

    /**
     * Gets all links except the Discord link
     */
    private java.util.Map<String, String> getOtherLinks(java.util.Map<String, String> helpLinks, String discordUrl) {
        java.util.Map<String, String> otherLinks = new java.util.LinkedHashMap<>();

        for (java.util.Map.Entry<String, String> entry : helpLinks.entrySet()) {
            if (!entry.getValue().equals(discordUrl)) {
                otherLinks.put(entry.getKey(), entry.getValue());
            }
        }

        return otherLinks;
    }

    private void showModernDialog(String title, String message, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    private int showModernConfirmDialog(String title, String message) {
        return JOptionPane.showConfirmDialog(this, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    }

    private void finishDialog() {
        // Mark that we're no longer on first startup
        if (PackCoreConfig.isFirstStartup) {
            PackCoreConfig.isFirstStartup = false;
        }

        PackCoreConfig.promptSetDefaultConfig = false;
        MidnightConfig.write("packcore");
        setVisible(false);
        dispose();
        completionLatch.countDown();
    }

    private ConfigInfo getSelectedConfig() {
        ConfigInfo selected = officialConfigList.getSelectedValue();
        if (selected == null) {
            selected = customConfigList.getSelectedValue();
        }
        return selected;
    }

    private ConfigType getSelectedConfigType() {
        if (officialConfigList.getSelectedValue() != null) {
            return ConfigType.OFFICIAL;
        }
        return ConfigType.CUSTOM;
    }

    private void setControlsEnabled(boolean enabled) {
        extractButton.setEnabled(enabled);
        skipButton.setEnabled(enabled);
        helpButton.setEnabled(enabled);
        officialConfigList.setEnabled(enabled);
        customConfigList.setEnabled(enabled);
    }

    public boolean showAndWait() {
        SwingUtilities.invokeLater(() -> setVisible(true));

        try {
            completionLatch.await();
            return dialogResult.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Dialog was interrupted", e);
            return false;
        }
    }

    /**
     * Modern renderer for config items with better visual design
     */
    private static class ModernConfigRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof ConfigInfo) {
                ConfigInfo config = (ConfigInfo) value;

                // Create display text with size info
                String displayText = String.format("<html><b>%s</b><br><small>Size: %s</small></html>",
                        config.getDisplayName(), formatFileSize(config.getSize()));
                setText(displayText);

                // Modern styling
                setBorder(new EmptyBorder(8, 12, 8, 12));

                if (!isSelected) {
                    setBackground(CARD_BACKGROUND);
                    setForeground(TEXT_COLOR);
                }
            }

            return this;
        }

        private String formatFileSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
}