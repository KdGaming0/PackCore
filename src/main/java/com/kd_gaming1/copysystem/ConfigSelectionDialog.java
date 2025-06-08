package com.kd_gaming1.copysystem;

import com.kd_gaming1.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simplified dialog for config selection that focuses on user experience.
 * Uses proper threading and synchronization to avoid blocking issues.
 */
public class ConfigSelectionDialog extends JFrame {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigSelectionDialog.class);

    private final ConfigExtractionService extractionService;
    private final CountDownLatch completionLatch = new CountDownLatch(1);
    private final AtomicBoolean dialogResult = new AtomicBoolean(false);

    private JList<ConfigInfo> officialConfigList;
    private JList<ConfigInfo> customConfigList;
    private JProgressBar progressBar;
    private JButton extractButton;
    private JButton skipButton;

    public ConfigSelectionDialog(ConfigExtractionService extractionService) {
        super("PackCore - Configuration Selection");
        this.extractionService = extractionService;

        // Set up the dialog to be modal-like behavior
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
    }

    private void initializeComponents() {
        // Create lists for configs
        List<ConfigInfo> officialConfigs = extractionService.getOfficialConfigs();
        List<ConfigInfo> customConfigs = extractionService.getCustomConfigs();

        officialConfigList = new JList<>(officialConfigs.toArray(new ConfigInfo[0]));
        officialConfigList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        officialConfigList.setCellRenderer(new ConfigInfoRenderer());

        customConfigList = new JList<>(customConfigs.toArray(new ConfigInfo[0]));
        customConfigList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customConfigList.setCellRenderer(new ConfigInfoRenderer());

        // Create progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setVisible(false);

        // Create buttons with more descriptive text
        extractButton = new JButton("📦 Extract & Apply Selected Configuration");
        skipButton = new JButton("⏭️ Skip & Continue with Current Settings");

        // Make buttons a bit larger to accommodate the longer text
        Dimension buttonSize = new Dimension(280, 35);
        extractButton.setPreferredSize(buttonSize);
        skipButton.setPreferredSize(buttonSize);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        // Header panel with instructions
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Center panel with config lists
        JPanel centerPanel = createCenterPanel();
        add(centerPanel, BorderLayout.CENTER);

        // Bottom panel with controls
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setResizable(true); // Allow resizing since we have more content now
        setMinimumSize(new Dimension(700, 600)); // Set minimum size
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel("<html><h2>PackCore Configuration Management</h2></html>");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JTextArea instructionsArea = new JTextArea(
                "Welcome to PackCore! This dialog helps you manage your Minecraft mod configurations before the game starts.\n\n" +

                        "📦 CONFIGURATION TYPES:\n" +
                        "• Official Configs: Pre-made configurations that come with your modpack. These are tested setups " +
                        "created by the modpack authors to provide specific gameplay experiences or performance optimizations.\n" +
                        "• Custom Configs: Your personal configurations or ones shared by other players. These contain " +
                        "customized mod settings that you or others have fine-tuned for specific preferences.\n\n" +

                        "🎯 WHAT HAPPENS WHEN YOU:\n" +
                        "• Extract: The selected configuration will overwrite your current mod settings. All config files " +
                        "from the chosen archive will be applied to your Minecraft installation, giving you that specific setup.\n" +
                        "• Skip: Minecraft will start with whatever settings are currently in place (either default settings " +
                        "or previously applied configurations). No changes will be made.\n\n" +

                        "⚠️ IMPORTANT NOTES:\n" +
                        "• Extracting will REPLACE your current mod configurations - make a backup first if you want to keep them!\n" +
                        "• After clicking Extract or Skip, please be patient! Minecraft may take 10-60 seconds to appear.\n" +
                        "• The game is loading in the background even if nothing appears to happen immediately.\n\n" +

                        "🔧 MANAGING CONFIGURATIONS:\n" +
                        "• Create your own backup: Use '/packcore archive' in-game to save your current settings\n" +
                        "• Share with friends: Custom config archives can be shared and imported by other players\n" +
                        "• Disable this dialog: Use '/packcore dialog false' if you don't want to see this anymore\n" +
                        "• Re-enable later: Use '/packcore dialog true' to bring this dialog back\n" +
                        "• Get help: Use '/packcore help' to see all available commands and options"
        );

        instructionsArea.setEditable(false);
        instructionsArea.setOpaque(false);
        instructionsArea.setWrapStyleWord(true);
        instructionsArea.setLineWrap(true);
        instructionsArea.setFont(instructionsArea.getFont().deriveFont(11f));
        instructionsArea.setBackground(panel.getBackground());

        // Create a scrollable text area for the instructions since they're longer now
        JScrollPane instructionsScroll = new JScrollPane(instructionsArea);
        instructionsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        instructionsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        instructionsScroll.setPreferredSize(new Dimension(600, 200));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
        panel.add(instructionsScroll, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));

        // Official configs panel
        JPanel officialPanel = new JPanel(new BorderLayout());
        officialPanel.setBorder(BorderFactory.createTitledBorder("Official Configurations"));
        JScrollPane officialScroll = new JScrollPane(officialConfigList);
        officialScroll.setPreferredSize(new Dimension(250, 150));
        officialPanel.add(officialScroll, BorderLayout.CENTER);

        // Custom configs panel
        JPanel customPanel = new JPanel(new BorderLayout());
        customPanel.setBorder(BorderFactory.createTitledBorder("Custom Configurations"));
        JScrollPane customScroll = new JScrollPane(customConfigList);
        customScroll.setPreferredSize(new Dimension(250, 150));
        customPanel.add(customScroll, BorderLayout.CENTER);

        panel.add(officialPanel);
        panel.add(customPanel);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Progress bar
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(progressBar);
        panel.add(Box.createVerticalStrut(10));

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.add(extractButton);
        buttonPanel.add(skipButton);
        panel.add(buttonPanel);

        return panel;
    }

    private void setupEventHandlers() {
        // Selection synchronization - only allow one selection at a time
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
    }

    private void handleExtraction() {
        ConfigInfo selectedConfig = getSelectedConfig();
        ConfigType configType = getSelectedConfigType();

        if (selectedConfig == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a configuration from either the Official or Custom list to extract.\n\n" +
                            "Tip: Official configs are pre-made setups, while Custom configs are personalized configurations.",
                    "No Configuration Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Show confirmation dialog with more details
        String configTypeText = configType == ConfigType.OFFICIAL ? "Official" : "Custom";
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to extract the " + configTypeText.toLowerCase() + " configuration:\n" +
                        "\"" + selectedConfig.getDisplayName() + "\"?\n\n" +
                        "This will replace your current mod settings with the settings from this configuration.\n" +
                        "Make sure you've backed up any important configurations before proceeding!",
                "Confirm Configuration Extraction",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Disable buttons and show progress
        setControlsEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("Preparing to extract configuration files...");

        // Perform extraction in background thread
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return extractionService.extractConfig(selectedConfig.getName(), configType, progress -> {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(progress);
                        progressBar.setString("Extracting configuration files... " + progress + "%");
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
            JOptionPane.showMessageDialog(this,
                    "✅ Configuration Successfully Applied!\n\n" +
                            "The configuration '" + configName + "' has been extracted and applied to your Minecraft installation.\n" +
                            "All mod settings from this configuration are now active.\n\n" +
                            "Minecraft will continue loading shortly. Please be patient as this may take a moment.\n\n" +
                            "Tip: You can create your own configuration backups using '/packcore archive' in-game.",
                    "Configuration Extraction Complete",
                    JOptionPane.INFORMATION_MESSAGE);
            dialogResult.set(true);
        } else {
            JOptionPane.showMessageDialog(this,
                    "❌ Configuration Extraction Failed\n\n" +
                            "Failed to extract and apply the configuration '" + configName + "'.\n" +
                            "Your current settings remain unchanged.\n\n" +
                            "Please check the game logs for detailed error information, or try selecting a different configuration.\n" +
                            "If the problem persists, contact the modpack author or check the PackCore documentation.",
                    "Extraction Failed",
                    JOptionPane.ERROR_MESSAGE);
            setControlsEnabled(true);
            return;
        }

        finishDialog();
    }

    private void handleSkip() {
        int result = JOptionPane.showConfirmDialog(this,
                "Skip Configuration Extraction?\n\n" +
                        "Minecraft will start with your current mod settings. No configurations will be applied or changed.\n\n" +
                        "You can always apply configurations later using:\n" +
                        "• The main menu PackCore options\n" +
                        "• The '/packcore' commands in-game\n" +
                        "• Re-enabling this dialog with '/packcore dialog enabled/disabled'\n\n" +
                        "Continue without applying any configurations?",
                "Confirm Skip",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            dialogResult.set(true);
            finishDialog();
        }
    }

    private void finishDialog() {
        // Disable the prompt for next time
        ModConfig.setPromptSetDefaultConfig(false);

        // Close dialog and release waiting thread
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
        officialConfigList.setEnabled(enabled);
        customConfigList.setEnabled(enabled);
    }

    /**
     * Shows the dialog and blocks until user completes the selection
     */
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
     * Custom renderer for config info in the lists
     */
    private static class ConfigInfoRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof ConfigInfo) {
                ConfigInfo config = (ConfigInfo) value;
                setText(config.getDisplayName());
                setToolTipText("Size: " + formatFileSize(config.getSize()));
            }

            return this;
        }

        private String formatFileSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
            return (bytes / (1024 * 1024)) + " MB";
        }
    }
}