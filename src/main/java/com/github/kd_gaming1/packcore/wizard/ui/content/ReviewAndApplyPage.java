package com.github.kd_gaming1.packcore.wizard.ui.content;

import com.github.kd_gaming1.packcore.wizard.copysystem.UnzipFiles;
import com.github.kd_gaming1.packcore.wizard.ui.ModpackSetupWizard;
import com.github.kd_gaming1.packcore.wizard.ui.theme.WizardTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReviewAndApplyPage extends MainContentInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewAndApplyPage.class);

    private final ModpackSetupWizard wizard;
    private final Path runDir;

    // UI Components
    private JLabel configSummaryLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JTextArea logArea;
    private JButton applyButton;

    // State tracking
    private boolean extractionStarted = false;
    private boolean extractionCompleted = false;

    public ReviewAndApplyPage(Path runDir, ModpackSetupWizard wizard) {
        super();
        this.wizard = wizard;
        this.runDir = runDir;
        createUI();
        updateSummary();
    }

    private void createUI() {
        setLayout(new BorderLayout());

        // Summary panel at top
        JPanel summaryPanel = createSummaryPanel();

        // Progress panel in center
        JPanel progressPanel = createProgressPanel();

        // Action panel at bottom
        JPanel actionPanel = createActionPanel();

        add(summaryPanel, BorderLayout.NORTH);
        add(progressPanel, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);
    }

    private JPanel createSummaryPanel() {
        JPanel panel = createTitledPanel("📋 Configuration Summary",
                "Review your selection before applying");
        panel.setPreferredSize(new Dimension(800, 120));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        // Config info
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        infoPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

        configSummaryLabel = new JLabel();
        configSummaryLabel.setFont(WizardTheme.getBodyFont());
        configSummaryLabel.setForeground(WizardTheme.TEXT_PRIMARY);

        statusLabel = new JLabel("Ready to apply configuration");
        statusLabel.setFont(WizardTheme.getHeaderFont());
        statusLabel.setForeground(WizardTheme.SUCCESS);

        infoPanel.add(configSummaryLabel);
        infoPanel.add(statusLabel);

        contentPanel.add(infoPanel, BorderLayout.CENTER);
        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createProgressPanel() {
        JPanel panel = createTitledPanel("⚙️ Installation Progress",
                "Track the configuration extraction process");

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Waiting to start...");
        progressBar.setFont(WizardTheme.getBodyFont());
        progressBar.setForeground(WizardTheme.ACCENT_GOLD);
        progressBar.setBackground(WizardTheme.BACKGROUND_DARK);
        progressBar.setPreferredSize(new Dimension(800, 30));

        // Log area
        logArea = new JTextArea(12, 80);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        logArea.setBackground(WizardTheme.BACKGROUND_DARK);
        logArea.setForeground(WizardTheme.SUCCESS);
        logArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        logArea.setText("🔍 Waiting for configuration extraction to begin...\n" +
                "💡 Click 'Apply Configuration' to start the process.\n");

        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createLineBorder(WizardTheme.BORDER));
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel progressWrapper = new JPanel(new BorderLayout());
        progressWrapper.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        progressWrapper.add(progressBar, BorderLayout.NORTH);
        progressWrapper.add(Box.createVerticalStrut(15), BorderLayout.CENTER);

        contentPanel.add(progressWrapper, BorderLayout.NORTH);
        contentPanel.add(logScrollPane, BorderLayout.CENTER);

        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(WizardTheme.BACKGROUND_DARK);
        panel.setPreferredSize(new Dimension(800, 80));

        applyButton = createActionButton("🚀 Apply Configuration",
                WizardTheme.ACCENT_GOLD, WizardTheme.BACKGROUND_DARK);
        applyButton.setPreferredSize(new Dimension(200, 45));
        applyButton.setFont(WizardTheme.getHeaderFont());

        applyButton.addActionListener(e -> {
            if (!extractionStarted) {
                startExtraction();
            }
        });

        // Hover effect
        applyButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (applyButton.isEnabled()) {
                    applyButton.setBackground(WizardTheme.ACCENT_HOVER);
                }
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (applyButton.isEnabled()) {
                    applyButton.setBackground(WizardTheme.ACCENT_GOLD);
                }
            }
        });

        panel.add(applyButton);
        return panel;
    }

    private void updateSummary() {
        String selectedConfig = ConfigSelectionPage.selectedResolution;

        if (selectedConfig != null) {
            configSummaryLabel.setText("🎯 Selected Configuration: " + selectedConfig);
        } else {
            configSummaryLabel.setText("⚠️ No configuration selected");
        }

        boolean canApply = selectedConfig != null;
        applyButton.setEnabled(canApply);

        if (!canApply) {
            statusLabel.setText("⚠️ Please go back and select a configuration");
            statusLabel.setForeground(WizardTheme.ERROR);
        } else {
            statusLabel.setText("✅ Ready to apply configuration");
            statusLabel.setForeground(WizardTheme.SUCCESS);
        }
    }

    private void startExtraction() {
        String selectedConfig = ConfigSelectionPage.selectedResolution;

        if (selectedConfig == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a configuration before applying.",
                    "Missing Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        extractionStarted = true;
        applyButton.setEnabled(false);
        applyButton.setText("🔄 Extracting...");
        applyButton.setBackground(WizardTheme.BACKGROUND_LIGHT);
        statusLabel.setText("🔄 Extracting configuration files...");
        statusLabel.setForeground(WizardTheme.INFO);

        Path configZipPath = runDir.resolve("packcore/modpack_config/official_configs").resolve(selectedConfig + ".zip");
        String destDir = System.getProperty("user.home") + "/Desktop/PackCoreTest";

        addLogMessage("╔══════════════════════════════════════╗");
        addLogMessage("║       Configuration Extraction       ║");
        addLogMessage("╚══════════════════════════════════════╝");
        addLogMessage("📦 Configuration: " + configZipPath.getFileName());
        addLogMessage("📁 Destination: " + destDir);
        addLogMessage("⏰ Started at: " + java.time.LocalTime.now());
        addLogMessage("");

        new Thread(() -> {
            try {
                UnzipFiles unzipper = new UnzipFiles();

                SwingUtilities.invokeLater(() -> {
                    progressBar.setString("Extracting configuration...");
                    progressBar.setForeground(WizardTheme.INFO);
                    addLogMessage("🔍 Verifying configuration file...");
                });

                if (!Files.exists(configZipPath)) {
                    throw new IOException("Configuration file not found: " + configZipPath);
                }

                SwingUtilities.invokeLater(() -> addLogMessage("✅ Configuration file verified"));
                SwingUtilities.invokeLater(() -> addLogMessage("🚀 Starting extraction process..."));

                unzipper.unzip(configZipPath.toString(), destDir, (bytesProcessed, totalBytes, percentage) -> {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(percentage);
                        progressBar.setString("Extracting: " + percentage + "% complete");

                        if (percentage % 10 == 0) {
                            addLogMessage("📊 Progress: " + percentage + "% complete");
                        }
                    });
                });

                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(100);
                    progressBar.setString("🎉 Configuration applied successfully!");
                    progressBar.setForeground(WizardTheme.SUCCESS);
                    statusLabel.setText("🎉 Configuration applied successfully!");
                    statusLabel.setForeground(WizardTheme.SUCCESS);
                    applyButton.setText("✅ Completed");
                    applyButton.setBackground(WizardTheme.SUCCESS);
                    extractionCompleted = true;

                    addLogMessage("");
                    addLogMessage("╔══════════════════════════════════════╗");
                    addLogMessage("║         Extraction Complete!         ║");
                    addLogMessage("╚══════════════════════════════════════╝");
                    addLogMessage("🎮 Your modpack is now configured and ready!");
                    addLogMessage("➡️ Click 'Continue' to proceed to the final step.");
                    addLogMessage("⏰ Completed at: " + java.time.LocalTime.now());

                    // THIS IS THE IMPORTANT FIX - call the correct method
                    wizard.getNavigationPanel().onExtractionCompleted();
                });

            } catch (IOException ex) {
                LOGGER.error("Failed to extract configuration", ex);
                SwingUtilities.invokeLater(() -> {
                    progressBar.setString("❌ Extraction failed!");
                    progressBar.setForeground(WizardTheme.ERROR);
                    statusLabel.setText("❌ Extraction failed - see log for details");
                    statusLabel.setForeground(WizardTheme.ERROR);
                    applyButton.setText("🔄 Retry");
                    applyButton.setBackground(WizardTheme.WARNING);
                    applyButton.setEnabled(true);
                    extractionStarted = false;

                    addLogMessage("");
                    addLogMessage("❌ ERROR: " + ex.getMessage());
                    addLogMessage("💡 Please check that the configuration file exists and try again.");

                    JOptionPane.showMessageDialog(this,
                            "Extraction failed: " + ex.getMessage() +
                                    "\n\nPlease check the log panel for more information.",
                            "Extraction Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void addLogMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public boolean isExtractionCompleted() {
        return extractionCompleted;
    }

    public void refreshSummary() {
        updateSummary();
    }
}