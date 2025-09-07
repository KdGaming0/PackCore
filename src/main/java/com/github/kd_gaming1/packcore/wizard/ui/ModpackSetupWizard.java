package com.github.kd_gaming1.packcore.wizard.ui;

import com.github.kd_gaming1.packcore.wizard.ui.content.*;
import com.github.kd_gaming1.packcore.wizard.ui.theme.WizardTheme;

import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

public class ModpackSetupWizard extends JFrame {
    private final Path runDir;
    private JPanel contentPanel;
    private CardLayout cardLayout;

    // Enhanced UI components
    private JPanel glassPane;
    private Timer fadeTimer;
    private float opacity = 0f;

    // Page references
    private ConfigSelectionPage configSelectionPage;
    private ReviewAndApplyPage reviewAndApplyPage;
    private SuccessPage successPage;
    private NavigationPanel navigationPanel;

    // Progress indicator
    private ProgressIndicator progressIndicator;

    public ModpackSetupWizard(Path runDir) {
        this.runDir = runDir;

        setupLookAndFeel();
        setupFrame();
        setupGlassPane();
        setupPanels();
        createPages();

        // Fade in animation
        startFadeIn();

        setVisible(true);
    }

    private void setupLookAndFeel() {
        try {
            // Use Nimbus for a modern look with customizations
            UIManager.setLookAndFeel(new NimbusLookAndFeel());

            // Customize Nimbus colors to match our theme
            UIManager.put("control", WizardTheme.BACKGROUND_MEDIUM);
            UIManager.put("info", WizardTheme.BACKGROUND_LIGHT);
            UIManager.put("nimbusBase", WizardTheme.BACKGROUND_DARK);
            UIManager.put("nimbusAlertYellow", WizardTheme.ACCENT_GOLD);
            UIManager.put("nimbusFocus", WizardTheme.ACCENT_GOLD);
            UIManager.put("nimbusGreen", WizardTheme.SUCCESS);
            UIManager.put("nimbusRed", WizardTheme.ERROR);
            UIManager.put("nimbusSelectedText", WizardTheme.TEXT_PRIMARY);
            UIManager.put("text", WizardTheme.TEXT_PRIMARY);

        } catch (Exception e) {
            // Fallback to system L&F
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e2) {
                // Use default
            }
        }
    }

    private void setupFrame() {
        setTitle("Modpack Setup Wizard - PackCore");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Calculate ideal size based on screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(1000, (int)(screenSize.width * 0.8));
        int height = Math.min(750, (int)(screenSize.height * 0.85));
        setSize(width, height);

        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 600));

        // Set icon if available
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/icon.png"));
            setIconImage(icon.getImage());
        } catch (Exception e) {
            // No icon available
        }

        // Modern window decorations
        getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        getRootPane().putClientProperty("apple.awt.windowTitleVisible", Boolean.FALSE);

        getContentPane().setBackground(WizardTheme.BACKGROUND_DARK);

        // Handle window close with animation
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClosing();
            }
        });

        // Responsive resizing
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                revalidate();
                repaint();
            }
        });
    }

    private void setupGlassPane() {
        glassPane = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Semi-transparent overlay for transitions
                g2.setColor(new Color(0, 0, 0, (int)(50 * (1 - opacity))));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        glassPane.setOpaque(false);
        setGlassPane(glassPane);
    }

    private void startFadeIn() {
        glassPane.setVisible(true);
        fadeTimer = new Timer(20, e -> {
            opacity += 0.05f;
            if (opacity >= 1f) {
                opacity = 1f;
                fadeTimer.stop();
                glassPane.setVisible(false);
            }
            glassPane.repaint();
        });
        fadeTimer.start();
    }

    private void handleWindowClosing() {
        // Smooth exit with confirmation
        if (reviewAndApplyPage != null && reviewAndApplyPage.isExtractionCompleted()) {
            fadeOutAndClose();
        } else {
            showExitConfirmation();
        }
    }

    private void showExitConfirmation() {
        // Custom styled dialog
        JDialog dialog = new JDialog(this, "Confirm Exit", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel iconLabel = new JLabel("⚠️", SwingConstants.CENTER);
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 48));

        JLabel messageLabel = new JLabel(
                "<html><center>The setup wizard is not yet complete.<br><br>" +
                        "Are you sure you want to exit?<br>" +
                        "You'll need to run the wizard again next time.</center></html>",
                SwingConstants.CENTER
        );
        messageLabel.setForeground(WizardTheme.TEXT_PRIMARY);
        messageLabel.setFont(WizardTheme.getBodyFont());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

        JButton continueButton = createStyledButton("Continue Setup", WizardTheme.SUCCESS, Color.WHITE);
        JButton exitButton = createStyledButton("Exit Wizard", WizardTheme.ERROR, Color.WHITE);

        continueButton.addActionListener(e -> dialog.dispose());
        exitButton.addActionListener(e -> {
            dialog.dispose();
            fadeOutAndClose();
        });

        buttonPanel.add(continueButton);
        buttonPanel.add(exitButton);

        contentPanel.add(iconLabel, BorderLayout.WEST);
        contentPanel.add(messageLabel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(contentPanel);
        dialog.setVisible(true);
    }

    private void fadeOutAndClose() {
        glassPane.setVisible(true);
        Timer fadeOut = new Timer(20, null);
        fadeOut.addActionListener(e -> {
            opacity -= 0.05f;
            if (opacity <= 0f) {
                opacity = 0f;
                fadeOut.stop();
                dispose();
            }
            glassPane.repaint();
        });
        fadeOut.start();
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(WizardTheme.getBodyFont());
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 35));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            Color original = bg;
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bg.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(original);
            }
        });

        return button;
    }

    private void setupPanels() {
        // Enhanced header with progress indicator
        JPanel headerPanel = new EnhancedHeaderPanel();

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(WizardTheme.BACKGROUND_DARK);

        // Add subtle border and shadow effect
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WizardTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        navigationPanel = new NavigationPanel(this);

        add(headerPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        add(navigationPanel, BorderLayout.SOUTH);
    }

    private void createPages() {
        // Add pages with transition effects
        contentPanel.add(new EnhancedWelcomePage(runDir), "welcome");

        configSelectionPage = new ConfigSelectionPage(runDir, this);
        contentPanel.add(configSelectionPage, "config");

        reviewAndApplyPage = new ReviewAndApplyPage(runDir, this);
        contentPanel.add(reviewAndApplyPage, "review");
    }

    public void showPage(String pageName) {
        // Add transition animation
        SwingUtilities.invokeLater(() -> {
            if ("review".equals(pageName)) {
                reviewAndApplyPage.refreshSummary();
            }

            if ("success".equals(pageName)) {
                String selectedConfig = ConfigSelectionPage.selectedResolution;

                // Remove old success page if exists
                for (Component comp : contentPanel.getComponents()) {
                    if (comp instanceof SuccessPage) {
                        contentPanel.remove(comp);
                        break;
                    }
                }

                successPage = new SuccessPage(selectedConfig, null);
                contentPanel.add(successPage, "success");
            }

            // Smooth transition
            glassPane.setVisible(true);
            opacity = 0.8f;

            Timer transitionTimer = new Timer(10, null);
            transitionTimer.addActionListener(e -> {
                opacity += 0.1f;
                if (opacity >= 1f) {
                    opacity = 1f;
                    cardLayout.show(contentPanel, pageName);
                    glassPane.setVisible(false);
                    transitionTimer.stop();

                    // Update progress indicator
                    if (progressIndicator != null) {
                        progressIndicator.updateProgress(pageName);
                    }
                }
                glassPane.repaint();
            });
            transitionTimer.start();
        });
    }

    public void completeWizard() {
        fadeOutAndClose();
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

    // Enhanced header panel with progress indicator
    private class EnhancedHeaderPanel extends JPanel {
        public EnhancedHeaderPanel() {
            setLayout(new BorderLayout());
            setBackground(WizardTheme.BACKGROUND_MEDIUM);
            setPreferredSize(new Dimension(getWidth(), 100));
            setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, WizardTheme.ACCENT_GOLD));

            // Left side - branding
            JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 25));
            brandPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

            JLabel logoLabel = new JLabel("⚙️");
            logoLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 36));

            JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, -5));
            titlePanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

            JLabel titleLabel = new JLabel("Modpack Setup Wizard");
            titleLabel.setFont(WizardTheme.getTitleFont());
            titleLabel.setForeground(WizardTheme.TEXT_PRIMARY);

            JLabel subtitleLabel = new JLabel("Configure your modpack for the best experience");
            subtitleLabel.setFont(WizardTheme.getSmallFont());
            subtitleLabel.setForeground(WizardTheme.TEXT_SECONDARY);

            titlePanel.add(titleLabel);
            titlePanel.add(subtitleLabel);

            brandPanel.add(logoLabel);
            brandPanel.add(titlePanel);

            // Center - progress indicator
            progressIndicator = new ProgressIndicator();

            add(brandPanel, BorderLayout.WEST);
            add(progressIndicator, BorderLayout.CENTER);
        }
    }

    // Progress indicator component
    private class ProgressIndicator extends JPanel {
        private final String[] steps = {"Welcome", "Configure", "Review", "Complete"};
        private int currentStep = 0;

        public ProgressIndicator() {
            setBackground(WizardTheme.BACKGROUND_MEDIUM);
            setLayout(new FlowLayout(FlowLayout.CENTER, 20, 35));
            updateDisplay();
        }

        public void updateProgress(String pageName) {
            switch (pageName) {
                case "welcome": currentStep = 0; break;
                case "config": currentStep = 1; break;
                case "review": currentStep = 2; break;
                case "success": currentStep = 3; break;
            }
            updateDisplay();
        }

        private void updateDisplay() {
            removeAll();

            for (int i = 0; i < steps.length; i++) {
                JPanel stepPanel = new JPanel(new BorderLayout());
                stepPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

                // Step indicator circle
                JLabel stepCircle = new JLabel(String.valueOf(i + 1), SwingConstants.CENTER);
                stepCircle.setPreferredSize(new Dimension(30, 30));
                stepCircle.setOpaque(true);

                if (i < currentStep) {
                    stepCircle.setText("✓");
                    stepCircle.setBackground(WizardTheme.SUCCESS);
                    stepCircle.setForeground(Color.WHITE);
                } else if (i == currentStep) {
                    stepCircle.setBackground(WizardTheme.ACCENT_GOLD);
                    stepCircle.setForeground(WizardTheme.BACKGROUND_DARK);
                } else {
                    stepCircle.setBackground(WizardTheme.BACKGROUND_LIGHT);
                    stepCircle.setForeground(WizardTheme.TEXT_MUTED);
                }

                stepCircle.setBorder(BorderFactory.createLineBorder(
                        i <= currentStep ? WizardTheme.ACCENT_GOLD : WizardTheme.BORDER, 2));

                // Step label
                JLabel stepLabel = new JLabel(steps[i], SwingConstants.CENTER);
                stepLabel.setFont(WizardTheme.getSmallFont());
                stepLabel.setForeground(i <= currentStep ? WizardTheme.TEXT_PRIMARY : WizardTheme.TEXT_MUTED);

                stepPanel.add(stepCircle, BorderLayout.CENTER);
                stepPanel.add(stepLabel, BorderLayout.SOUTH);

                add(stepPanel);

                // Add connector line
                if (i < steps.length - 1) {
                    JPanel connector = new JPanel();
                    connector.setPreferredSize(new Dimension(40, 2));
                    connector.setBackground(i < currentStep ? WizardTheme.ACCENT_GOLD : WizardTheme.BORDER);
                    add(connector);
                }
            }

            revalidate();
            repaint();
        }
    }

    // Enhanced welcome page with better visuals
    private static class EnhancedWelcomePage extends WelcomePage {
        public EnhancedWelcomePage(Path runDir) {
            super(runDir);

            // Add animation or enhanced visuals
            Timer animTimer = new Timer(50, e -> {
                // Subtle animation effects
                repaint();
            });
            animTimer.start();
        }
    }

    // Temp for debugging
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ModpackSetupWizard(Path.of("C:\\Users\\karld\\IdeaProjects\\PackCore\\run"));
        });
    }
}