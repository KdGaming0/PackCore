package com.github.kd_gaming1.packcore.wizard.ui;

import com.github.kd_gaming1.packcore.wizard.ui.content.ConfigSelectionPage;
import com.github.kd_gaming1.packcore.wizard.ui.theme.WizardTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;

public class NavigationPanel extends JPanel implements ActionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NavigationPanel.class);
    private final ModpackSetupWizard wizard;

    private final String[] pageOrder = {"welcome", "config", "review", "success"};
    private int currentPageIndex = 0;

    // Enhanced button references
    private AnimatedButton previousButton;
    private AnimatedButton nextButton;
    private AnimatedButton finishButton;
    private AnimatedButton cancelButton;
    private AnimatedButton helpButton;

    // State tracking for better UX
    private boolean isTransitioning = false;
    private JLabel statusLabel;
    private JProgressBar microProgress;

    public NavigationPanel(ModpackSetupWizard wizard) {
        this.wizard = wizard;
        setupPanel();
        initializeButtons();
        updateButtonStates();
    }

    private void setupPanel() {
        setBackground(WizardTheme.BACKGROUND_MEDIUM);
        setPreferredSize(new Dimension(900, 90));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, WizardTheme.BORDER),
                BorderFactory.createEmptyBorder(5, 20, 5, 20)
        ));
        setLayout(new BorderLayout());
    }

    private void initializeButtons() {
        // Left panel - help and status
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

        JPanel helpPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));
        helpPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

        helpButton = new AnimatedButton("Help", "❓");
        helpButton.setButtonStyle(AnimatedButton.Style.SECONDARY);
        helpPanel.add(helpButton);

        // Status label for feedback
        statusLabel = new JLabel("");
        statusLabel.setFont(WizardTheme.getSmallFont());
        statusLabel.setForeground(WizardTheme.TEXT_MUTED);
        helpPanel.add(Box.createHorizontalStrut(20));
        helpPanel.add(statusLabel);

        leftPanel.add(helpPanel, BorderLayout.WEST);

        // Micro progress for async operations
        microProgress = new JProgressBar();
        microProgress.setIndeterminate(true);
        microProgress.setPreferredSize(new Dimension(100, 3));
        microProgress.setVisible(false);
        microProgress.setForeground(WizardTheme.ACCENT_GOLD);
        microProgress.setBackground(WizardTheme.BACKGROUND_LIGHT);
        microProgress.setBorderPainted(false);
        leftPanel.add(microProgress, BorderLayout.SOUTH);

        // Right panel - navigation buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));
        rightPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

        cancelButton = new AnimatedButton("Cancel", "✕");
        cancelButton.setButtonStyle(AnimatedButton.Style.DANGER);

        previousButton = new AnimatedButton("Previous", "◀");
        previousButton.setButtonStyle(AnimatedButton.Style.SECONDARY);

        nextButton = new AnimatedButton("Next", "▶");
        nextButton.setButtonStyle(AnimatedButton.Style.PRIMARY);

        finishButton = new AnimatedButton("Finish", "✓");
        finishButton.setButtonStyle(AnimatedButton.Style.SUCCESS);
        finishButton.setVisible(false);

        // Add keyboard shortcuts
        setupKeyboardShortcuts();

        rightPanel.add(cancelButton);
        rightPanel.add(Box.createHorizontalStrut(20));
        rightPanel.add(previousButton);
        rightPanel.add(nextButton);
        rightPanel.add(finishButton);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
    }

    private void setupKeyboardShortcuts() {
        // Alt+N for Next, Alt+P for Previous, Alt+F for Finish
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("alt N"), "next");
        inputMap.put(KeyStroke.getKeyStroke("alt P"), "previous");
        inputMap.put(KeyStroke.getKeyStroke("alt F"), "finish");
        inputMap.put(KeyStroke.getKeyStroke("F1"), "help");

        actionMap.put("next", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (nextButton.isEnabled() && nextButton.isVisible()) handleNext();
            }
        });

        actionMap.put("previous", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (previousButton.isEnabled()) handlePrevious();
            }
        });

        actionMap.put("finish", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (finishButton.isEnabled() && finishButton.isVisible()) handleFinish();
            }
        });

        actionMap.put("help", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleHelp();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isTransitioning) return; // Prevent actions during transitions

        String command = e.getActionCommand();

        if (command.contains("Previous")) {
            handlePrevious();
        } else if (command.contains("Next") || command.contains("Continue")) {
            handleNext();
        } else if (command.contains("Finish")) {
            handleFinish();
        } else if (command.contains("Cancel")) {
            handleCancel();
        } else if (command.contains("Help")) {
            handleHelp();
        }
    }

    private void handlePrevious() {
        if (currentPageIndex > 0) {
            isTransitioning = true;
            showTransitionStatus("Navigating back...");

            SwingUtilities.invokeLater(() -> {
                currentPageIndex--;
                wizard.showPage(pageOrder[currentPageIndex]);
                updateButtonStates();
                hideTransitionStatus();
                isTransitioning = false;
                LOGGER.info("Navigated to previous page: {}", pageOrder[currentPageIndex]);
            });
        }
    }

    private void handleNext() {
        if (!canProceedToNext()) {
            showValidationError();
            return;
        }

        isTransitioning = true;
        showTransitionStatus("Loading next step...");

        SwingUtilities.invokeLater(() -> {
            currentPageIndex++;
            wizard.showPage(pageOrder[currentPageIndex]);
            updateButtonStates();
            hideTransitionStatus();
            isTransitioning = false;
            LOGGER.info("Navigated to next page: {}", pageOrder[currentPageIndex]);
        });
    }

    private boolean canProceedToNext() {
        String currentPage = pageOrder[currentPageIndex];

        switch (currentPage) {
            case "welcome":
                return true;
            case "config":
                return ConfigSelectionPage.selectedResolution != null;
            case "review":
                return wizard.getReviewAndApplyPage().isExtractionCompleted();
            default:
                return true;
        }
    }

    private void showValidationError() {
        String currentPage = pageOrder[currentPageIndex];
        String message;
        String title;

        switch (currentPage) {
            case "config":
                message = "Please select a configuration before proceeding.";
                title = "Configuration Required";
                statusLabel.setText("⚠️ Select a configuration");
                statusLabel.setForeground(WizardTheme.WARNING);
                // Highlight the config selection area
                wizard.getContentPane().repaint();
                break;
            case "review":
                message = "Please apply the configuration before proceeding.";
                title = "Apply Configuration";
                statusLabel.setText("⚠️ Apply configuration first");
                statusLabel.setForeground(WizardTheme.WARNING);
                break;
            default:
                message = "Please complete the current step before proceeding.";
                title = "Incomplete Step";
                break;
        }

        // Show custom styled error dialog
        showStyledMessage(message, title, JOptionPane.WARNING_MESSAGE);

        // Clear status after delay
        Timer clearTimer = new Timer(3000, e -> {
            statusLabel.setText("");
        });
        clearTimer.setRepeats(false);
        clearTimer.start();
    }

    private void showStyledMessage(String message, String title, int messageType) {
        JDialog dialog = new JDialog(wizard, title, true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 180);
        dialog.setLocationRelativeTo(wizard);
        dialog.setUndecorated(true);

        // Main panel with rounded corners effect
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw rounded background
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
            }
        };
        mainPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WizardTheme.ACCENT_GOLD, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // Icon
        String iconText = messageType == JOptionPane.WARNING_MESSAGE ? "⚠️" : "ℹ️";
        JLabel iconLabel = new JLabel(iconText, SwingConstants.CENTER);
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 36));

        // Message
        JLabel messageLabel = new JLabel("<html><center>" + message + "</center></html>");
        messageLabel.setForeground(WizardTheme.TEXT_PRIMARY);
        messageLabel.setFont(WizardTheme.getBodyFont());

        // OK button
        AnimatedButton okButton = new AnimatedButton("OK", null);
        okButton.setButtonStyle(AnimatedButton.Style.PRIMARY);
        okButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        buttonPanel.add(okButton);

        mainPanel.add(iconLabel, BorderLayout.WEST);
        mainPanel.add(messageLabel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void handleFinish() {
        LOGGER.info("Setup wizard completed successfully");

        showTransitionStatus("Finalizing setup...");
        microProgress.setVisible(true);

        SwingUtilities.invokeLater(() -> {
            markWizardCompleted();

            showStyledMessage(
                    "Setup completed successfully!<br><br>" +
                            "Your modpack is now configured and ready to use.<br><br>" +
                            "Click 'Finish' to close this wizard and start playing!<br><br>" +
                            "It may take a minute before Minecraft starts. Please be patient.",
                    "Setup Complete",
                    JOptionPane.INFORMATION_MESSAGE
            );

            hideTransitionStatus();
            wizard.completeWizard();
        });
    }

    private void markWizardCompleted() {
        try {
            com.github.kd_gaming1.packcore.config.PackCoreConfig.haveSetupWizardCompletedSuccessfully = true;
            com.github.kd_gaming1.packcore.config.PackCoreConfig.haveSetupWizardShown = true;
            com.github.kd_gaming1.packcore.config.PackCoreConfig.isFirstStartup = false;
            com.github.kd_gaming1.packcore.config.PackCoreConfig.showInstallWizard = false;

            String selectedConfig = ConfigSelectionPage.selectedResolution;
            if (selectedConfig != null) {
                com.github.kd_gaming1.packcore.config.PackCoreConfig.appliedConfigName = selectedConfig;
                com.github.kd_gaming1.packcore.config.PackCoreConfig.lastConfigApplied = selectedConfig;
            }

            eu.midnightdust.lib.config.MidnightConfig.write("packcore");
            LOGGER.info("Wizard completion state saved successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to save wizard completion state", e);
        }
    }

    private void handleCancel() {
        showStyledConfirmation(
                "Are you sure you want to cancel the setup?<br><br>" +
                        "Any progress will be lost and you'll need to run the wizard again later.",
                "Cancel Setup",
                () -> {
                    LOGGER.info("Setup wizard cancelled by user");
                    System.exit(0);
                }
        );
    }

    private void showStyledConfirmation(String message, String title, Runnable onConfirm) {
        JDialog dialog = new JDialog(wizard, title, true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(450, 200);
        dialog.setLocationRelativeTo(wizard);
        dialog.setUndecorated(true);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WizardTheme.WARNING, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel iconLabel = new JLabel("⚠️", SwingConstants.CENTER);
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 36));

        JLabel messageLabel = new JLabel("<html><center>" + message + "</center></html>");
        messageLabel.setForeground(WizardTheme.TEXT_PRIMARY);
        messageLabel.setFont(WizardTheme.getBodyFont());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

        AnimatedButton confirmButton = new AnimatedButton("Yes, Cancel", null);
        confirmButton.setButtonStyle(AnimatedButton.Style.DANGER);
        confirmButton.addActionListener(e -> {
            dialog.dispose();
            onConfirm.run();
        });

        AnimatedButton continueButton = new AnimatedButton("Continue Setup", null);
        continueButton.setButtonStyle(AnimatedButton.Style.PRIMARY);
        continueButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(continueButton);
        buttonPanel.add(confirmButton);

        mainPanel.add(iconLabel, BorderLayout.WEST);
        mainPanel.add(messageLabel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void handleHelp() {
        String currentPage = pageOrder[currentPageIndex];
        String helpContent = getHelpMessageForPage(currentPage);

        HelpDialog helpDialog = new HelpDialog(wizard, currentPage, helpContent);
        helpDialog.setVisible(true);
    }

    private String getHelpMessageForPage(String pageName) {
        switch (pageName) {
            case "welcome":
                return "<h2>Welcome</h2>" +
                        "<p>This wizard will guide you through setting up your modpack.</p>" +
                        "<ul>" +
                        "<li><b>What's Next:</b> Click 'Next' to select your configuration</li>" +
                        "<li><b>Keyboard Shortcut:</b> Press Alt+N to continue</li>" +
                        "</ul>";

            case "config":
                return "<h2>Configuration Selection</h2>" +
                        "<p>Choose the configuration that matches your display and preferences.</p>" +
                        "<ul>" +
                        "<li><b>Recommended:</b> The wizard suggests configs based on your screen</li>" +
                        "<li><b>Custom Choice:</b> You can select any configuration you prefer</li>" +
                        "<li><b>Details:</b> Click on a config to see its description</li>" +
                        "</ul>" +
                        "<p><b>Tip:</b> Configurations with ⭐ are recommended for your system.</p>";

            case "review":
                return "<h2>Review and Apply</h2>" +
                        "<p>Review your selection and apply the configuration.</p>" +
                        "<ul>" +
                        "<li><b>Apply:</b> Click 'Apply Configuration' to extract files</li>" +
                        "<li><b>Progress:</b> Watch the progress bar and log for status</li>" +
                        "<li><b>Completion:</b> Wait for extraction before continuing</li>" +
                        "</ul>" +
                        "<p><b>Note:</b> This process may take a few moments.</p>";

            case "success":
                return "<h2>Setup Complete!</h2>" +
                        "<p>Your modpack is configured and ready!</p>" +
                        "<ul>" +
                        "<li><b>Launch:</b> Start Minecraft with your new configuration</li>" +
                        "<li><b>Tutorial:</b> Check the in-game tutorial for mod info</li>" +
                        "<li><b>Support:</b> Visit forums or docs if you need help</li>" +
                        "</ul>";

            default:
                return "<p>Help information for this page is not available.</p>";
        }
    }

    public void setConfigSelected(boolean selected) {
        updateButtonStates();
        if (selected) {
            statusLabel.setText("✓ Configuration selected");
            statusLabel.setForeground(WizardTheme.SUCCESS);

            Timer clearTimer = new Timer(2000, e -> statusLabel.setText(""));
            clearTimer.setRepeats(false);
            clearTimer.start();
        }
    }

    public void onExtractionCompleted() {
        LOGGER.info("Extraction completed, enabling navigation");
        updateButtonStates();
        statusLabel.setText("✓ Configuration applied successfully");
        statusLabel.setForeground(WizardTheme.SUCCESS);
    }

    private void updateButtonStates() {
        String currentPage = pageOrder[currentPageIndex];

        previousButton.setEnabled(currentPageIndex > 0);

        boolean canGoNext = currentPageIndex < pageOrder.length - 1 && canProceedToNext();
        nextButton.setEnabled(canGoNext);
        nextButton.setVisible(currentPageIndex < pageOrder.length - 1);

        finishButton.setVisible(currentPageIndex == pageOrder.length - 1);

        if (currentPageIndex == pageOrder.length - 2) {
            nextButton.setText("Continue");
            nextButton.setIcon("▶▶");
        } else {
            nextButton.setText("Next");
            nextButton.setIcon("▶");
        }

        // Update tooltips
        updateTooltips();
    }

    private void updateTooltips() {
        previousButton.setToolTipText("Go back (Alt+P)");
        nextButton.setToolTipText(nextButton.isEnabled() ? "Continue to next step (Alt+N)" : "Complete current step first");
        finishButton.setToolTipText("Complete setup (Alt+F)");
        helpButton.setToolTipText("Show help (F1)");
        cancelButton.setToolTipText("Cancel and exit wizard");
    }

    private void showTransitionStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(WizardTheme.INFO);
        microProgress.setVisible(true);
    }

    private void hideTransitionStatus() {
        statusLabel.setText("");
        microProgress.setVisible(false);
    }

    // Custom animated button class
    private class AnimatedButton extends JButton {
        public enum Style {
            PRIMARY(WizardTheme.ACCENT_GOLD, WizardTheme.BACKGROUND_DARK),
            SECONDARY(WizardTheme.BACKGROUND_LIGHT, WizardTheme.TEXT_PRIMARY),
            SUCCESS(WizardTheme.SUCCESS, Color.WHITE),
            DANGER(WizardTheme.ERROR, Color.WHITE);

            final Color bg;
            final Color fg;

            Style(Color bg, Color fg) {
                this.bg = bg;
                this.fg = fg;
            }
        }

        private Style style = Style.PRIMARY;
        private String icon;
        private Timer pulseTimer;
        private float pulseAlpha = 1.0f;

        public AnimatedButton(String text, String icon) {
            super(text);
            this.icon = icon;
            setupButton();
        }

        private void setupButton() {
            setFont(WizardTheme.getBodyFont());
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(110, 38));

            addActionListener(NavigationPanel.this);

            // Hover effects
            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if (isEnabled()) startPulse();
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    stopPulse();
                }
            });
        }

        public void setButtonStyle(Style style) {
            this.style = style;
            repaint();
        }

        public void setIcon(String icon) {
            this.icon = icon;
            repaint();
        }

        private void startPulse() {
            if (pulseTimer != null) return;
            pulseTimer = new Timer(50, e -> {
                pulseAlpha -= 0.05f;
                if (pulseAlpha <= 0.7f) pulseAlpha = 1.0f;
                repaint();
            });
            pulseTimer.start();
        }

        private void stopPulse() {
            if (pulseTimer != null) {
                pulseTimer.stop();
                pulseTimer = null;
                pulseAlpha = 1.0f;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw button background
            Color bgColor = isEnabled() ? style.bg : WizardTheme.BACKGROUND_LIGHT;
            g2.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(),
                    (int)(255 * pulseAlpha)));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));

            // Draw text and icon
            g2.setColor(isEnabled() ? style.fg : WizardTheme.TEXT_MUTED);
            FontMetrics fm = g2.getFontMetrics();

            String displayText = getText();
            if (icon != null) {
                displayText = icon + " " + displayText;
            }

            int textWidth = fm.stringWidth(displayText);
            int x = (getWidth() - textWidth) / 2;
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

            g2.drawString(displayText, x, y);
            g2.dispose();
        }
    }

    // Help dialog class
    private class HelpDialog extends JDialog {
        public HelpDialog(JFrame parent, String page, String content) {
            super(parent, "Help - " + page, true);
            setSize(500, 400);
            setLocationRelativeTo(parent);

            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setBackground(WizardTheme.BACKGROUND_DARK);
            mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JEditorPane helpPane = new JEditorPane("text/html",
                    "<html><body style='font-family: Segoe UI; color: #ddd; background: #2f3136;'>" +
                            content + "</body></html>");
            helpPane.setEditable(false);
            helpPane.setBackground(WizardTheme.BACKGROUND_DARK);

            JScrollPane scrollPane = new JScrollPane(helpPane);
            scrollPane.setBorder(BorderFactory.createLineBorder(WizardTheme.BORDER));

            AnimatedButton closeButton = new AnimatedButton("Close", null);
            closeButton.setButtonStyle(AnimatedButton.Style.PRIMARY);
            closeButton.addActionListener(e -> dispose());

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.setBackground(WizardTheme.BACKGROUND_DARK);
            buttonPanel.add(closeButton);

            mainPanel.add(scrollPane, BorderLayout.CENTER);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            add(mainPanel);
        }
    }
}