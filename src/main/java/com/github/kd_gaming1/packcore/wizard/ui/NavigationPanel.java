package com.github.kd_gaming1.packcore.wizard.ui;

import com.github.kd_gaming1.packcore.wizard.ui.content.ConfigSelectionPage;
import com.github.kd_gaming1.packcore.wizard.ui.theme.WizardTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NavigationPanel extends JPanel implements ActionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NavigationPanel.class);
    private final ModpackSetupWizard wizard;

    private final String[] pageOrder = {"welcome", "config", "review", "success"};
    private int currentPageIndex = 0;

    JButton previousButton;
    JButton nextButton;
    JButton finishButton;
    JButton cancelButton;
    JButton helpButton;

    public NavigationPanel(ModpackSetupWizard wizard) {
        this.wizard = wizard;
        setupPanel();
        initializeButtons();
        updateButtonStates();
    }

    private void setupPanel() {
        setBackground(WizardTheme.BACKGROUND_MEDIUM);
        setPreferredSize(new Dimension(900, 80));
        setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, WizardTheme.BORDER));
        setLayout(new BorderLayout());
    }

    private void initializeButtons() {
        // Left side - help button
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        leftPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

        helpButton = createStyledButton("❓ Help", WizardTheme.BACKGROUND_LIGHT, WizardTheme.TEXT_SECONDARY);
        leftPanel.add(helpButton);

        // Right side - navigation buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        rightPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

        cancelButton = createStyledButton("Cancel", WizardTheme.ERROR, Color.WHITE);
        previousButton = createStyledButton("◀ Previous", WizardTheme.BACKGROUND_LIGHT, WizardTheme.TEXT_PRIMARY);
        nextButton = createStyledButton("Next ▶", WizardTheme.ACCENT_GOLD, WizardTheme.BACKGROUND_DARK);
        finishButton = createStyledButton("✓ Finish", WizardTheme.SUCCESS, Color.WHITE);
        finishButton.setVisible(false);

        rightPanel.add(cancelButton);
        rightPanel.add(Box.createHorizontalStrut(10));
        rightPanel.add(previousButton);
        rightPanel.add(nextButton);
        rightPanel.add(finishButton);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
    }

    private JButton createStyledButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(WizardTheme.getBodyFont());
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(100, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(this);

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            Color originalColor = background;

            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    if (background.equals(WizardTheme.ACCENT_GOLD)) {
                        button.setBackground(WizardTheme.ACCENT_HOVER);
                    } else {
                        button.setBackground(background.brighter());
                    }
                }
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(originalColor);
            }
        });

        return button;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if (command.contains("Previous")) {
            handlePrevious();
        } else if (command.contains("Next") || command.contains("Continue")) {
            handleNext();
        } else if (command.contains("Finish")) {
            handleFinish();
        } else if (command.equals("Cancel")) {
            handleCancel();
        } else if (command.contains("Help")) {
            handleHelp();
        }
    }

    private void handlePrevious() {
        if (currentPageIndex > 0) {
            currentPageIndex--;
            wizard.showPage(pageOrder[currentPageIndex]);
            updateButtonStates();
            LOGGER.info("Navigated to previous page: {}", pageOrder[currentPageIndex]);
        }
    }

    private void handleNext() {
        if (canProceedToNext()) {
            currentPageIndex++;
            wizard.showPage(pageOrder[currentPageIndex]);
            updateButtonStates();
            LOGGER.info("Navigated to next page: {}", pageOrder[currentPageIndex]);
        } else {
            showCannotProceedMessage();
        }
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

    private void showCannotProceedMessage() {
        String currentPage = pageOrder[currentPageIndex];
        String message;

        switch (currentPage) {
            case "config":
                message = "Please select a configuration before proceeding.";
                break;
            case "review":
                message = "Please apply the configuration before proceeding.";
                break;
            default:
                message = "Please complete the current step before proceeding.";
                break;
        }

        JOptionPane.showMessageDialog(wizard, message, "Selection Required", JOptionPane.WARNING_MESSAGE);
    }

    private void handleFinish() {
        LOGGER.info("Setup wizard completed successfully");

        int result = JOptionPane.showConfirmDialog(
                wizard,
                "Setup completed successfully!\n\n" +
                        "Your modpack is now configured and ready to use.\n" +
                        "Would you like to close the setup wizard?",
                "Setup Complete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            // Mark wizard as completed before closing
            markWizardCompleted();
            wizard.completeWizard();
        }
    }

    private void markWizardCompleted() {
        try {
            // Import your config class
            com.github.kd_gaming1.packcore.config.PackCoreConfig.haveSetupWizardCompletedSuccessfully = true;
            com.github.kd_gaming1.packcore.config.PackCoreConfig.haveSetupWizardShown = true;
            com.github.kd_gaming1.packcore.config.PackCoreConfig.isFirstStartup = false;

            // Reset the manual trigger since wizard is now complete
            com.github.kd_gaming1.packcore.config.PackCoreConfig.showInstallWizard = false;

            // Save the selected configuration
            String selectedConfig = ConfigSelectionPage.selectedResolution;
            if (selectedConfig != null) {
                com.github.kd_gaming1.packcore.config.PackCoreConfig.appliedConfigName = selectedConfig;
                com.github.kd_gaming1.packcore.config.PackCoreConfig.lastConfigApplied = selectedConfig;
            }

            // Write config to disk
            eu.midnightdust.lib.config.MidnightConfig.write("packcore");
            LOGGER.info("Wizard completion state saved successfully, showInstallWizard reset to false");
        } catch (Exception e) {
            LOGGER.error("Failed to save wizard completion state", e);
        }
    }

    private void handleCancel() {
        int result = JOptionPane.showConfirmDialog(
                wizard,
                "Are you sure you want to cancel the setup?\n\n" +
                        "Any progress will be lost and you'll need to run the wizard again later.",
                "Cancel Setup",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            LOGGER.info("Setup wizard cancelled by user");
            System.exit(0);
        }
    }

    private void handleHelp() {
        String currentPage = pageOrder[currentPageIndex];
        String helpMessage = getHelpMessageForPage(currentPage);

        JOptionPane.showMessageDialog(
                wizard,
                helpMessage,
                "Help - " + currentPage.substring(0, 1).toUpperCase() + currentPage.substring(1),
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private String getHelpMessageForPage(String pageName) {
        switch (pageName) {
            case "welcome":
                return "Welcome to the modpack setup wizard!\n\n" +
                        "This wizard will help you configure your modpack for optimal performance.\n" +
                        "Click 'Next' to begin the setup process.";

            case "config":
                return "Configuration Selection:\n\n" +
                        "• Select the configuration that matches your setup\n" +
                        "• Each configuration is optimized for specific conditions\n" +
                        "• The recommended option is automatically selected";

            case "review":
                return "Review and Apply:\n\n" +
                        "• Review your selected configuration\n" +
                        "• Click 'Apply Configuration' to extract the files\n" +
                        "• Wait for the extraction to complete before proceeding";

            case "success":
                return "Setup Complete!\n\n" +
                        "• Your modpack is now configured and ready to use\n" +
                        "• Launch Minecraft to start playing\n" +
                        "• Check the in-game tutorial when you reach the main menu";

            default:
                return "Help information for this page is not available.";
        }
    }

    public void setConfigSelected(boolean selected) {
        updateButtonStates();
    }

    // This is the key method that was missing!
    public void onExtractionCompleted() {
        LOGGER.info("Extraction completed, enabling navigation to next page");
        updateButtonStates();
    }

    private void updateButtonStates() {
        String currentPage = pageOrder[currentPageIndex];

        previousButton.setEnabled(currentPageIndex > 0);

        boolean canGoNext = currentPageIndex < pageOrder.length - 1 && canProceedToNext();
        nextButton.setEnabled(canGoNext);
        nextButton.setVisible(currentPageIndex < pageOrder.length - 1);

        finishButton.setVisible(currentPageIndex == pageOrder.length - 1);

        if (currentPageIndex == pageOrder.length - 2) {
            nextButton.setText("Continue ▶");
        } else {
            nextButton.setText("Next ▶");
        }

        LOGGER.debug("Updated button states for page: {} (index: {}), canGoNext: {}", currentPage, currentPageIndex, canGoNext);
    }
}