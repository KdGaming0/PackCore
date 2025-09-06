package com.github.kd_gaming1.packcore.wizard.ui.content;

import com.github.kd_gaming1.packcore.wizard.copysystem.UnzipFiles;
import com.github.kd_gaming1.packcore.wizard.ui.ModpackSetupWizard;
import com.github.kd_gaming1.packcore.wizard.ui.theme.WizardTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class ReviewAndApplyPage extends MainContentInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewAndApplyPage.class);

    private final ModpackSetupWizard wizard;
    private final Path runDir;

    // Enhanced UI Components
    private JLabel configNameLabel;
    private JLabel configDetailsLabel;
    private JLabel statusLabel;
    private AnimatedProgressBar progressBar;
    private StyledLogArea logArea;
    private ModernButton applyButton;
    private JPanel statusPanel;

    // State tracking
    private boolean extractionStarted = false;
    private boolean extractionCompleted = false;
    private ExtractionState currentState = ExtractionState.IDLE;

    // Animation timers
    private Timer statusAnimationTimer;
    private Timer progressAnimationTimer;

    private enum ExtractionState {
        IDLE, PREPARING, EXTRACTING, FINALIZING, COMPLETED, ERROR
    }

    public ReviewAndApplyPage(Path runDir, ModpackSetupWizard wizard) {
        super();
        this.wizard = wizard;
        this.runDir = runDir;
        createUI();
        updateSummary();
    }

    private void createUI() {
        setLayout(new BorderLayout(0, 15));
        setBackground(WizardTheme.BACKGROUND_DARK);
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Top summary card with modern design
        JPanel summaryCard = createModernSummaryCard();

        // Center progress panel with enhanced visuals
        JPanel progressPanel = createEnhancedProgressPanel();

        // Bottom action panel with animated button
        JPanel actionPanel = createModernActionPanel();

        add(summaryCard, BorderLayout.NORTH);
        add(progressPanel, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);
    }

    private JPanel createModernSummaryCard() {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Gradient background
                GradientPaint gradient = new GradientPaint(
                        0, 0, WizardTheme.BACKGROUND_MEDIUM,
                        0, getHeight(), WizardTheme.BACKGROUND_LIGHT
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(800, 120));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WizardTheme.ACCENT_GOLD, 2),
                BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));

        // Left side - config info
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 0, 5));
        infoPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("📋 Configuration Summary");
        titleLabel.setFont(WizardTheme.getHeaderFont());
        titleLabel.setForeground(WizardTheme.ACCENT_GOLD);

        configNameLabel = new JLabel("No configuration selected");
        configNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        configNameLabel.setForeground(WizardTheme.TEXT_PRIMARY);

        configDetailsLabel = new JLabel("Select a configuration to see details");
        configDetailsLabel.setFont(WizardTheme.getSmallFont());
        configDetailsLabel.setForeground(WizardTheme.TEXT_SECONDARY);

        infoPanel.add(titleLabel);
        infoPanel.add(configNameLabel);
        infoPanel.add(configDetailsLabel);

        // Right side - status indicator
        statusPanel = new StatusIndicatorPanel();

        card.add(infoPanel, BorderLayout.WEST);
        card.add(statusPanel, BorderLayout.EAST);

        return card;
    }

    private JPanel createEnhancedProgressPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(WizardTheme.BACKGROUND_DARK);

        // Progress section
        JPanel progressSection = new JPanel(new BorderLayout(0, 10));
        progressSection.setBackground(WizardTheme.BACKGROUND_DARK);

        // Status label with icon
        statusLabel = new JLabel("⏳ Waiting to begin...");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(WizardTheme.TEXT_SECONDARY);

        // Custom animated progress bar
        progressBar = new AnimatedProgressBar();

        progressSection.add(statusLabel, BorderLayout.NORTH);
        progressSection.add(progressBar, BorderLayout.CENTER);

        // Enhanced log area
        logArea = new StyledLogArea();
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WizardTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)
        ));
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(progressSection, BorderLayout.NORTH);
        panel.add(logScrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createModernActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(WizardTheme.BACKGROUND_DARK);
        panel.setPreferredSize(new Dimension(800, 80));

        applyButton = new ModernButton("Apply Configuration", "🚀");
        applyButton.addActionListener(e -> {
            if (!extractionStarted) {
                startExtraction();
            } else if (currentState == ExtractionState.ERROR) {
                retryExtraction();
            }
        });

        panel.add(applyButton);
        return panel;
    }

    private void updateSummary() {
        String selectedConfig = ConfigSelectionPage.selectedResolution;

        if (selectedConfig != null) {
            configNameLabel.setText("✅ " + selectedConfig.toUpperCase() + " Configuration");
            configDetailsLabel.setText("Ready to apply optimized settings for your system");
            applyButton.setEnabled(true);
            updateState(ExtractionState.IDLE);
        } else {
            configNameLabel.setText("⚠️ No configuration selected");
            configDetailsLabel.setText("Please go back and select a configuration");
            applyButton.setEnabled(false);
            updateState(ExtractionState.ERROR);
        }
    }

    private void updateState(ExtractionState newState) {
        currentState = newState;
        SwingUtilities.invokeLater(() -> {
            switch (newState) {
                case IDLE:
                    statusLabel.setText("⏳ Ready to apply configuration");
                    statusLabel.setForeground(WizardTheme.TEXT_SECONDARY);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                    break;

                case PREPARING:
                    statusLabel.setText("🔍 Preparing configuration files...");
                    statusLabel.setForeground(WizardTheme.INFO);
                    progressBar.setIndeterminate(true);
                    startStatusAnimation();
                    break;

                case EXTRACTING:
                    statusLabel.setText("📦 Extracting configuration...");
                    statusLabel.setForeground(WizardTheme.ACCENT_GOLD);
                    progressBar.setIndeterminate(false);
                    break;

                case FINALIZING:
                    statusLabel.setText("🔧 Finalizing setup...");
                    statusLabel.setForeground(WizardTheme.SUCCESS);
                    progressBar.setIndeterminate(true);
                    break;

                case COMPLETED:
                    statusLabel.setText("🎉 Configuration applied successfully!");
                    statusLabel.setForeground(WizardTheme.SUCCESS);
                    progressBar.setValue(100);
                    progressBar.setComplete(true);
                    stopStatusAnimation();
                    applyButton.setText("✅ Completed");
                    applyButton.setEnabled(false);
                    break;

                case ERROR:
                    statusLabel.setText("❌ An error occurred");
                    statusLabel.setForeground(WizardTheme.ERROR);
                    progressBar.setIndeterminate(false);
                    stopStatusAnimation();
                    applyButton.setText("🔄 Retry");
                    applyButton.setEnabled(true);
                    break;
            }

            if (statusPanel instanceof StatusIndicatorPanel) {
                ((StatusIndicatorPanel) statusPanel).updateStatus(newState);
            }
        });
    }

    private void startStatusAnimation() {
        if (statusAnimationTimer == null) {
            final String[] frames = {"⚙️", "🔧", "⚡", "✨"};
            final AtomicInteger frameIndex = new AtomicInteger(0);

            statusAnimationTimer = new Timer(500, e -> {
                String currentText = statusLabel.getText();
                String baseText = currentText.substring(2); // Remove emoji
                statusLabel.setText(frames[frameIndex.get()] + " " + baseText);
                frameIndex.set((frameIndex.get() + 1) % frames.length);
            });
            statusAnimationTimer.start();
        }
    }

    private void stopStatusAnimation() {
        if (statusAnimationTimer != null) {
            statusAnimationTimer.stop();
            statusAnimationTimer = null;
        }
    }

    private void startExtraction() {
        String selectedConfig = ConfigSelectionPage.selectedResolution;

        if (selectedConfig == null) {
            showErrorDialog("Please select a configuration before applying.");
            return;
        }

        extractionStarted = true;
        applyButton.setEnabled(false);
        applyButton.startLoading();

        updateState(ExtractionState.PREPARING);

        Path configZipPath = runDir.resolve("packcore/modpack_config/official_configs")
                .resolve(selectedConfig + ".zip");
        String destDir = System.getProperty("user.home") + "/Desktop/PackCoreTest";

        logArea.addHeaderLine("╔══════════════════════════════════════╗");
        logArea.addHeaderLine("║     Configuration Installation       ║");
        logArea.addHeaderLine("╚══════════════════════════════════════╝");
        logArea.addInfoLine("📦 Configuration: " + configZipPath.getFileName());
        logArea.addInfoLine("📁 Destination: " + destDir);
        logArea.addInfoLine("⏰ Started: " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("HH:mm:ss")));
        logArea.addLine("");

        new Thread(() -> performExtraction(configZipPath, destDir)).start();
    }

    private void performExtraction(Path configZipPath, String destDir) {
        try {
            Thread.sleep(1000); // Simulate preparation

            SwingUtilities.invokeLater(() -> {
                updateState(ExtractionState.EXTRACTING);
                logArea.addSuccessLine("✅ Configuration file verified");
                logArea.addInfoLine("🚀 Starting extraction process...");
            });

            if (!Files.exists(configZipPath)) {
                throw new IOException("Configuration file not found: " + configZipPath);
            }

            UnzipFiles unzipper = new UnzipFiles();
            unzipper.unzip(configZipPath.toString(), destDir, (bytesProcessed, totalBytes, percentage) -> {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(percentage);
                    progressBar.setString(String.format("Extracting: %d%% (%s / %s)",
                            percentage,
                            formatBytes(bytesProcessed),
                            formatBytes(totalBytes)));

                    if (percentage % 20 == 0 && percentage > 0) {
                        logArea.addProgressLine("📊 Progress: " + percentage + "% complete");
                    }
                });
            });

            SwingUtilities.invokeLater(() -> {
                updateState(ExtractionState.FINALIZING);
                logArea.addLine("");

                try {
                    Thread.sleep(500); // Brief pause for visual effect
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                updateState(ExtractionState.COMPLETED);
                extractionCompleted = true;
                applyButton.stopLoading();

                logArea.addHeaderLine("╔══════════════════════════════════════╗");
                logArea.addHeaderLine("║       Installation Complete!         ║");
                logArea.addHeaderLine("╚══════════════════════════════════════╝");
                logArea.addSuccessLine("🎮 Your modpack is now configured!");
                logArea.addInfoLine("➡️ Click 'Continue' to proceed");
                logArea.addInfoLine("⏰ Completed: " + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("HH:mm:ss")));

                wizard.getNavigationPanel().onExtractionCompleted();
            });

        } catch (Exception ex) {
            LOGGER.error("Extraction failed", ex);
            SwingUtilities.invokeLater(() -> {
                updateState(ExtractionState.ERROR);
                applyButton.stopLoading();
                extractionStarted = false;

                logArea.addLine("");
                logArea.addErrorLine("❌ ERROR: " + ex.getMessage());
                logArea.addWarningLine("💡 Please check the configuration and try again");

                showErrorDialog("Extraction failed: " + ex.getMessage());
            });
        }
    }

    private void retryExtraction() {
        extractionStarted = false;
        extractionCompleted = false;
        progressBar.setValue(0);
        progressBar.setComplete(false);
        logArea.clear();
        applyButton.setText("Apply Configuration");
        updateState(ExtractionState.IDLE);
        startExtraction();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public boolean isExtractionCompleted() {
        return extractionCompleted;
    }

    public void refreshSummary() {
        updateSummary();
    }

    // Custom animated progress bar
    private class AnimatedProgressBar extends JProgressBar {
        private Timer animationTimer;
        private float hue = 0.15f; // Gold hue
        private boolean isComplete = false;

        public AnimatedProgressBar() {
            super(0, 100);
            setStringPainted(true);
            setString("Waiting to start...");
            setFont(WizardTheme.getBodyFont());
            setPreferredSize(new Dimension(800, 35));
            setBorderPainted(false);
            setBackground(WizardTheme.BACKGROUND_LIGHT);
        }

        public void setComplete(boolean complete) {
            this.isComplete = complete;
            if (complete) {
                startCompletionAnimation();
            }
        }

        private void startCompletionAnimation() {
            if (animationTimer == null) {
                animationTimer = new Timer(50, e -> {
                    hue += 0.01f;
                    if (hue > 1.0f) hue = 0.0f;
                    repaint();
                });
                animationTimer.start();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background
            g2.setColor(WizardTheme.BACKGROUND_LIGHT);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

            // Progress fill
            int progress = (int) ((getWidth() - 4) * (getValue() / 100.0));
            if (progress > 0) {
                Color fillColor = isComplete ?
                        Color.getHSBColor(hue, 0.7f, 0.9f) :
                        WizardTheme.ACCENT_GOLD;

                GradientPaint gradient = new GradientPaint(
                        0, 0, fillColor,
                        0, getHeight(), fillColor.darker()
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(2, 2, progress, getHeight() - 4, 8, 8);
            }

            // Draw string
            g2.setColor(WizardTheme.TEXT_PRIMARY);
            FontMetrics fm = g2.getFontMetrics();
            String text = getString();
            int textWidth = fm.stringWidth(text);
            int x = (getWidth() - textWidth) / 2;
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(text, x, y);

            g2.dispose();
        }
    }

    // Styled log area with colored output
    private class StyledLogArea extends JTextPane {
        private final StyledDocument doc;
        private final SimpleAttributeSet normalStyle;
        private final SimpleAttributeSet headerStyle;
        private final SimpleAttributeSet successStyle;
        private final SimpleAttributeSet errorStyle;
        private final SimpleAttributeSet warningStyle;
        private final SimpleAttributeSet infoStyle;
        private final SimpleAttributeSet progressStyle;

        public StyledLogArea() {
            setEditable(false);
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            setBackground(WizardTheme.BACKGROUND_DARK);
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            doc = getStyledDocument();

            // Define styles
            normalStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(normalStyle, WizardTheme.TEXT_SECONDARY);

            headerStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(headerStyle, WizardTheme.ACCENT_GOLD);
            StyleConstants.setBold(headerStyle, true);

            successStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(successStyle, WizardTheme.SUCCESS);

            errorStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(errorStyle, WizardTheme.ERROR);
            StyleConstants.setBold(errorStyle, true);

            warningStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(warningStyle, WizardTheme.WARNING);

            infoStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(infoStyle, WizardTheme.INFO);

            progressStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(progressStyle, WizardTheme.ACCENT_GOLD);
            StyleConstants.setItalic(progressStyle, true);

            addLine("System ready. Waiting for user action...");
        }

        public void addLine(String text) {
            addStyledLine(text + "\n", normalStyle);
        }

        public void addHeaderLine(String text) {
            addStyledLine(text + "\n", headerStyle);
        }

        public void addSuccessLine(String text) {
            addStyledLine(text + "\n", successStyle);
        }

        public void addErrorLine(String text) {
            addStyledLine(text + "\n", errorStyle);
        }

        public void addWarningLine(String text) {
            addStyledLine(text + "\n", warningStyle);
        }

        public void addInfoLine(String text) {
            addStyledLine(text + "\n", infoStyle);
        }

        public void addProgressLine(String text) {
            addStyledLine(text + "\n", progressStyle);
        }

        private void addStyledLine(String text, SimpleAttributeSet style) {
            try {
                doc.insertString(doc.getLength(), text, style);
                setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                // Ignore
            }
        }

        public void clear() {
            setText("");
            addLine("Log cleared. Ready for new operation...");
        }
    }

    // Modern button with loading animation
    private class ModernButton extends JButton {
        private String icon;
        private Timer loadingTimer;
        private int loadingAngle = 0;
        private boolean isLoading = false;

        public ModernButton(String text, String icon) {
            super(text);
            this.icon = icon;
            setupButton();
        }

        private void setupButton() {
            setFont(new Font("Segoe UI", Font.BOLD, 16));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(220, 50));
        }

        public void startLoading() {
            isLoading = true;
            if (loadingTimer == null) {
                loadingTimer = new Timer(50, e -> {
                    loadingAngle = (loadingAngle + 10) % 360;
                    repaint();
                });
                loadingTimer.start();
            }
        }

        public void stopLoading() {
            isLoading = false;
            if (loadingTimer != null) {
                loadingTimer.stop();
                loadingTimer = null;
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Button background with gradient
            Color bgColor = isEnabled() ? WizardTheme.ACCENT_GOLD : WizardTheme.BACKGROUND_LIGHT;
            GradientPaint gradient = new GradientPaint(
                    0, 0, bgColor,
                    0, getHeight(), bgColor.darker()
            );
            g2.setPaint(gradient);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);

            // Shadow effect
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fillRoundRect(2, 2, getWidth() - 2, getHeight() - 2, 25, 25);

            // Loading animation
            if (isLoading) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.drawArc(cx - 15, cy - 15, 30, 30, loadingAngle, 60);
            } else {
                // Text and icon
                g2.setColor(isEnabled() ? WizardTheme.BACKGROUND_DARK : WizardTheme.TEXT_MUTED);
                FontMetrics fm = g2.getFontMetrics();
                String displayText = (icon != null ? icon + " " : "") + getText();
                int textWidth = fm.stringWidth(displayText);
                int x = (getWidth() - textWidth) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(displayText, x, y);
            }

            g2.dispose();
        }
    }

    // Status indicator panel
    private class StatusIndicatorPanel extends JPanel {
        private ExtractionState currentState = ExtractionState.IDLE;
        private Timer pulseTimer;
        private float pulseAlpha = 1.0f;

        public StatusIndicatorPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(150, 80));
            startPulseAnimation();
        }

        private void startPulseAnimation() {
            pulseTimer = new Timer(50, e -> {
                pulseAlpha -= 0.02f;
                if (pulseAlpha <= 0.3f) pulseAlpha = 1.0f;
                repaint();
            });
            pulseTimer.start();
        }

        public void updateStatus(ExtractionState state) {
            this.currentState = state;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Status circle
            int cx = getWidth() / 2;
            int cy = getHeight() / 2 - 10;
            int radius = 20;

            Color statusColor;
            String statusIcon;
            String statusText;

            switch (currentState) {
                case IDLE:
                    statusColor = WizardTheme.TEXT_SECONDARY;
                    statusIcon = "⏳";
                    statusText = "Ready";
                    break;
                case PREPARING:
                case EXTRACTING:
                case FINALIZING:
                    statusColor = new Color(
                            WizardTheme.ACCENT_GOLD.getRed(),
                            WizardTheme.ACCENT_GOLD.getGreen(),
                            WizardTheme.ACCENT_GOLD.getBlue(),
                            (int)(255 * pulseAlpha)
                    );
                    statusIcon = "⚡";
                    statusText = "Working";
                    break;
                case COMPLETED:
                    statusColor = WizardTheme.SUCCESS;
                    statusIcon = "✅";
                    statusText = "Complete";
                    break;
                case ERROR:
                    statusColor = WizardTheme.ERROR;
                    statusIcon = "❌";
                    statusText = "Error";
                    break;
                default:
                    statusColor = WizardTheme.TEXT_MUTED;
                    statusIcon = "?";
                    statusText = "Unknown";
            }

            // Draw status circle
            g2.setColor(statusColor);
            g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

            // Draw icon
            g2.setColor(Color.WHITE);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            FontMetrics fm = g2.getFontMetrics();
            int iconWidth = fm.stringWidth(statusIcon);
            g2.drawString(statusIcon, cx - iconWidth / 2, cy + 5);

            // Draw status text
            g2.setColor(WizardTheme.TEXT_SECONDARY);
            g2.setFont(WizardTheme.getSmallFont());
            fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(statusText);
            g2.drawString(statusText, cx - textWidth / 2, cy + radius + 20);

            g2.dispose();
        }
    }
}