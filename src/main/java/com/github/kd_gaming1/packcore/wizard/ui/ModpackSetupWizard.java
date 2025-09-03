package com.github.kd_gaming1.packcore.wizard.ui;

import com.github.kd_gaming1.packcore.wizard.ui.content.*;
import com.github.kd_gaming1.packcore.wizard.ui.theme.WizardTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

public class ModpackSetupWizard extends JFrame {
    private final Path runDir;
    private JPanel contentPanel;
    private CardLayout cardLayout;

    // Page references
    private ConfigSelectionPage configSelectionPage;
    private ReviewAndApplyPage reviewAndApplyPage;
    private SuccessPage successPage;
    private NavigationPanel navigationPanel;

    public ModpackSetupWizard(Path runDir) {
        this.runDir = runDir;

        // Set system look and feel for native appearance on each platform
        setupLookAndFeel();

        setupFrame();
        setupPanels();
        createPages();

        setVisible(true);
    }

    private void setupLookAndFeel() {
        try {
            // Use system look and feel for native appearance on Windows, macOS, Linux
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            // If system L&F fails, try cross-platform Metal L&F
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e2) {
                // If all else fails, use default - Swing will handle this gracefully
                System.err.println("Could not set look and feel, using default: " + e2.getMessage());
            }
        }
    }

    private void setupFrame() {
        setTitle("Modpack Setup Wizard");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Changed to handle closing properly
        setSize(new Dimension(900, 700));
        setLocationRelativeTo(null);
        setResizable(false);

        // Set dark background
        getContentPane().setBackground(WizardTheme.BACKGROUND_DARK);

        // Add window listener to handle close attempts
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClosing();
            }
        });
    }

    private void handleWindowClosing() {
        // Check if wizard is complete
        if (reviewAndApplyPage != null && reviewAndApplyPage.isExtractionCompleted()) {
            // Wizard completed successfully, allow close
            dispose();
        } else {
            // Wizard not complete, confirm with user
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "The setup wizard is not yet complete.\n\n" +
                            "If you close now, you'll need to run it again next time.\n" +
                            "Are you sure you want to close?",
                    "Setup Not Complete",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice == JOptionPane.YES_OPTION) {
                dispose();
            }
            // If NO, do nothing - keep wizard open
        }
    }

    // Method to properly close wizard after completion
    public void completeWizard() {
        dispose(); // This will trigger the windowClosed event
    }

    private void setupPanels() {
        HeaderPanel headerPanel = new HeaderPanel();
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(WizardTheme.BACKGROUND_DARK);
        navigationPanel = new NavigationPanel(this);

        add(headerPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        add(navigationPanel, BorderLayout.SOUTH);
    }

    private void createPages() {
        contentPanel.add(new WelcomePage(runDir), "welcome");

        configSelectionPage = new ConfigSelectionPage(runDir, this);
        contentPanel.add(configSelectionPage, "config");

        reviewAndApplyPage = new ReviewAndApplyPage(runDir, this);
        contentPanel.add(reviewAndApplyPage, "review");
    }

    public void showPage(String pageName) {
        if ("review".equals(pageName)) {
            reviewAndApplyPage.refreshSummary();
        }

        if ("success".equals(pageName)) {
            String selectedConfig = ConfigSelectionPage.selectedResolution;

            try {
                contentPanel.remove(contentPanel.getComponent(3));
            } catch (Exception e) {
                // No existing success page
            }

            successPage = new SuccessPage(selectedConfig, null);
            contentPanel.add(successPage, "success");
        }

        cardLayout.show(contentPanel, pageName);
    }

    public NavigationPanel getNavigationPanel() {
        return navigationPanel;
    }

    public Path getRunDir() {
        return runDir;
    }

    public ReviewAndApplyPage getReviewAndApplyPage() {
        return reviewAndApplyPage;
    }

    // Temp for debugging
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ModpackSetupWizard(Path.of("C:\\Users\\karld\\IdeaProjects\\PackCore\\run"));
        });
    }
}