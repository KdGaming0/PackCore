package com.github.kd_gaming1.packcore.wizard.ui.content;

import com.github.kd_gaming1.packcore.wizard.ui.theme.WizardTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class SuccessPage extends MainContentInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuccessPage.class);

    private final String selectedResolution;

    public SuccessPage(String selectedConfig, String selectedPerformance) {
        super();
        this.selectedResolution = selectedConfig;
        createUI();
    }

    private void createUI() {
        setLayout(new BorderLayout());

        // Success header - compact design
        JPanel headerPanel = createSuccessHeader();

        // Main content with summary and next steps
        JScrollPane contentScrollPane = createMainContent();

        // Footer with final instructions
        JPanel footerPanel = createFooter();

        add(headerPanel, BorderLayout.NORTH);
        add(contentScrollPane, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private JPanel createSuccessHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(WizardTheme.SUCCESS);
        panel.setPreferredSize(new Dimension(860, 80)); // Reduced height
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Title and subtitle in one centered panel
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(WizardTheme.SUCCESS);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 5, 0);

        // Icon and title in one line
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        titlePanel.setBackground(WizardTheme.SUCCESS);

        JLabel successIcon = new JLabel("🎉");
        successIcon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24)); // Slightly smaller

        JLabel successTitle = new JLabel("Setup Complete!");
        successTitle.setFont(new Font("Segoe UI", Font.BOLD, 24)); // Reduced size
        successTitle.setForeground(Color.WHITE);

        titlePanel.add(successIcon);
        titlePanel.add(successTitle);
        contentPanel.add(titlePanel, gbc);

        // Subtitle below
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);

        JLabel subtitle = new JLabel("The modpack is configured and ready to launch");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(Color.WHITE);
        contentPanel.add(subtitle, gbc);

        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane createMainContent() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(WizardTheme.BACKGROUND_DARK);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Load and display content from markdown - remove config summary
        JPanel contentPanel = createContentFromMarkdown();

        mainPanel.add(contentPanel, BorderLayout.NORTH); // Changed to NORTH so it starts at top

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        return scrollPane;
    }

    private JPanel createConfigSummary() {
        JPanel panel = createTitledPanel("✅ Applied Configuration",
                "Your modpack has been successfully configured");
        panel.setPreferredSize(new Dimension(820, 80)); // Reduced height

        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        contentPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

        // Single line with both pieces of info
        JLabel configLabel = new JLabel("🎯 Configuration: " + selectedResolution);
        configLabel.setFont(WizardTheme.getBodyFont());
        configLabel.setForeground(WizardTheme.ACCENT_GOLD);

        JLabel separator = new JLabel(" • ");
        separator.setForeground(WizardTheme.TEXT_SECONDARY);

        JLabel statusLabel = new JLabel("✅ Ready to use");
        statusLabel.setFont(WizardTheme.getBodyFont());
        statusLabel.setForeground(WizardTheme.SUCCESS);

        contentPanel.add(configLabel);
        contentPanel.add(separator);
        contentPanel.add(statusLabel);

        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createContentFromMarkdown() {
        JPanel panel = createTitledPanel("🚀 What's Next?",
                "Everything you need to know to get started");

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        // Load markdown content
        String markdownContent = loadFinishedContent();

        // Parse the markdown into sections
        String[] sections = parseMarkdownSections(markdownContent);

        if (sections.length >= 3) {
            // Create step panels from the sections
            JPanel stepsPanel = createStepPanels(sections);
            contentPanel.add(stepsPanel, BorderLayout.CENTER);
        } else {
            // Fallback to default content
            JPanel defaultSteps = createDefaultStepPanels();
            contentPanel.add(defaultSteps, BorderLayout.CENTER);
        }

        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    private String loadFinishedContent() {
        try {
            return com.github.kd_gaming1.packcore.wizard.util.PageContentProviders.getFinishedContent();
        } catch (Exception e) {
            LOGGER.error("Failed to load finished content", e);
            return getDefaultFinishedContent();
        }
    }

    private String[] parseMarkdownSections(String markdownContent) {
        String[] lines = markdownContent.split("\n");
        java.util.List<String> sections = new java.util.ArrayList<>();
        StringBuilder currentSection = new StringBuilder();

        for (String line : lines) {
            if (line.trim().startsWith("## ") && currentSection.length() > 0) {
                sections.add(currentSection.toString().trim());
                currentSection = new StringBuilder();
            }
            currentSection.append(line).append("\n");
        }

        if (currentSection.length() > 0) {
            sections.add(currentSection.toString().trim());
        }

        return sections.toArray(new String[0]);
    }

    private JPanel createStepPanels(String[] sections) {
        JPanel panel = new JPanel();

        // Always use 3 columns for consistent layout
        int visibleSections = Math.min(sections.length, 6); // Limit to 6 cards max
        int cols = Math.min(visibleSections, 3);
        int rows = (int) Math.ceil((double) visibleSections / 3.0);

        panel.setLayout(new GridLayout(rows, cols, 15, 15)); // Consistent spacing
        panel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

        for (int i = 0; i < visibleSections; i++) {
            String section = sections[i];
            String[] sectionLines = section.split("\n");

            String title = "Step " + (i + 1);
            String icon = getIconForStep(i);
            StringBuilder content = new StringBuilder();

            for (String line : sectionLines) {
                if (line.trim().startsWith("## ")) {
                    title = line.replace("## ", "").trim();
                } else if (!line.trim().isEmpty() && !line.trim().startsWith("#")) {
                    content.append(line.trim()).append("\n");
                }
            }

            panel.add(createStepCard(icon, title, content.toString().trim()));
        }

        return panel;
    }

    private String getIconForStep(int stepIndex) {
        String[] icons = {"🎮", "🎯", "⚙️", "💡", "🚀", "📖"};
        return stepIndex < icons.length ? icons[stepIndex] : "📝";
    }

    private JPanel createDefaultStepPanels() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 15, 0));
        panel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

        panel.add(createStepCard("🎮", "Launch Minecraft",
                "The modpack is ready!\n\n" +
                        "• Click Finished to open Minecraft\n" +
                        "• This may take a few seconds"));

        panel.add(createStepCard("🎯", "In-Game Wizard",
                "Customize further in-game!\n\n" +
                        "• Tutorial starts automatically\n" +
                        "• Do target configuration into mods!\n" +
                        "• Plus more!"));

        panel.add(createStepCard("🎯", "In-Game Tutorial",
                "Learn about your mods!\n\n" +
                        "• Open it from main menu or Esc Menu\n" +
                        "• Learn mod controls and features\n"));

        panel.add(createStepCard("⚙️", "Need Help?",
                "Support resources available!\n\n" +
                        "• Check modpack documentation\n" +
                        "• Join Discord\n"));

        return panel;
    }

    private JPanel createStepCard(String icon, String title, String description) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(WizardTheme.BACKGROUND_LIGHT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WizardTheme.BORDER),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setPreferredSize(new Dimension(240, 160)); // More consistent sizing

        // Header with icon and title
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(WizardTheme.BACKGROUND_LIGHT);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel iconLabel = new JLabel(icon, SwingConstants.CENTER);
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        iconLabel.setPreferredSize(new Dimension(240, 25));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(WizardTheme.ACCENT_GOLD);

        headerPanel.add(iconLabel, BorderLayout.NORTH);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // Content area with better text handling
        JTextArea descArea = new JTextArea(description);
        descArea.setEditable(false);
        descArea.setBackground(WizardTheme.BACKGROUND_LIGHT);
        descArea.setForeground(WizardTheme.TEXT_PRIMARY);
        descArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        card.add(headerPanel, BorderLayout.NORTH);
        card.add(descArea, BorderLayout.CENTER);

        return card;
    }

    private JPanel createFooter() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(WizardTheme.BACKGROUND_DARK);
        panel.setPreferredSize(new Dimension(860, 20));

        JLabel footerLabel = new JLabel("🎮 Click 'Finish' to close this wizard and start playing!");
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        footerLabel.setForeground(WizardTheme.TEXT_SECONDARY);

        panel.add(footerLabel);
        return panel;
    }

    private String getDefaultFinishedContent() {
        return """
                ## 🚀 Launch Minecraft
                Your modpack is now ready to play!
                
                Open your Minecraft launcher, select your modpack profile, and click 'Play' to start your adventure.
                
                ## 🎯 In-Game Tutorial
                Learn about your new mods!
                
                When you first launch, you'll see a tutorial explaining the mod features and controls. Take your time to learn the new mechanics.
                
                ## ⚙️ Need Help?
                Support resources are available!
                
                Check the modpack documentation, visit community forums, or use in-game help tooltips if you need assistance.
                """;
    }
}