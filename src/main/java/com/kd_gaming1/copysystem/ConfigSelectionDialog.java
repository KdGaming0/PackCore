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

        // Create buttons
        extractButton = new JButton("Extract Selected Config");
        skipButton = new JButton("Skip & Use Current Settings");
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
        setResizable(false);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel("<html><h2>PackCore Configuration Setup</h2></html>");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JTextArea instructionsArea = new JTextArea(
                "Choose a configuration to apply to Minecraft, or skip to use current settings.\n" +
                        "Official configs are pre-made configurations, while custom configs are user-created.\n" +
                        "After making your selection, please be patient as Minecraft loads."
        );
        instructionsArea.setEditable(false);
        instructionsArea.setOpaque(false);
        instructionsArea.setWrapStyleWord(true);
        instructionsArea.setLineWrap(true);
        instructionsArea.setFont(instructionsArea.getFont().deriveFont(12f));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
        panel.add(instructionsArea, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));

        // Official configs panel
        JPanel officialPanel = new JPanel(new BorderLayout());
        officialPanel.setBorder(BorderFactory.createTitledBorder("Official Configurations"));
        officialPanel.add(new JScrollPane(officialConfigList), BorderLayout.CENTER);

        // Custom configs panel
        JPanel customPanel = new JPanel(new BorderLayout());
        customPanel.setBorder(BorderFactory.createTitledBorder("Custom Configurations"));
        customPanel.add(new JScrollPane(customConfigList), BorderLayout.CENTER);

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
                    "Please select a configuration to extract.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Disable buttons and show progress
        setControlsEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("Extracting configuration...");

        // Perform extraction in background thread
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
            JOptionPane.showMessageDialog(this,
                    "Configuration '" + configName + "' extracted successfully!\nMinecraft will now continue loading.",
                    "Extraction Complete",
                    JOptionPane.INFORMATION_MESSAGE);
            dialogResult.set(true);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Failed to extract configuration '" + configName + "'.\nPlease check the logs for details.",
                    "Extraction Failed",
                    JOptionPane.ERROR_MESSAGE);
            setControlsEnabled(true);
            return;
        }

        finishDialog();
    }

    private void handleSkip() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to skip configuration extraction?\nMinecraft will use the current settings.",
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