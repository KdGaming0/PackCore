package com.github.kd_gaming1.packcore.wizard.ui.content;

import com.github.kd_gaming1.packcore.wizard.ui.theme.WizardTheme;
import com.github.kd_gaming1.packcore.wizard.util.MarkdownToHtmlConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

public class WelcomePage extends MainContentInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(WelcomePage.class);

    public WelcomePage(Path runDir) {
        super();
        createUI(runDir);
    }

    private void createUI(Path runDir) {
        JPanel welcomePanel = createTitledPanel("🎮 Welcome to Your Modpack!",
                "Let's get your Minecraft experience perfectly configured");

        // Content area
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        // Load and display markdown content
        String htmlContent = loadWelcomeContent(runDir);

        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setText(htmlContent);
        textPane.setEditable(false);
        textPane.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        textPane.setForeground(WizardTheme.TEXT_PRIMARY);
        textPane.setFont(WizardTheme.getBodyFont());
        textPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(WizardTheme.BORDER));
        scrollPane.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setPreferredSize(new Dimension(800, 350));

        // Features panel
        JPanel featuresPanel = createFeaturesPanel();

        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(featuresPanel, BorderLayout.SOUTH);

        welcomePanel.add(contentPanel, BorderLayout.CENTER);
        add(welcomePanel, BorderLayout.CENTER);
    }

    private JPanel createFeaturesPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 15, 0));
        panel.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        panel.add(createFeatureCard("⚙️", "Easy Configuration", "Choose from pre-made configs"));
        panel.add(createFeatureCard("🎯", "Optimized", "Performance tuned for your setup"));
        panel.add(createFeatureCard("🚀", "Ready to Play", "Get started in just a few clicks"));

        return panel;
    }

    private JPanel createFeatureCard(String icon, String title, String description) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(WizardTheme.BACKGROUND_LIGHT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WizardTheme.BORDER),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel iconLabel = new JLabel(icon, SwingConstants.CENTER);
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(WizardTheme.getHeaderFont());
        titleLabel.setForeground(WizardTheme.ACCENT_GOLD);

        JLabel descLabel = new JLabel(description, SwingConstants.CENTER);
        descLabel.setFont(WizardTheme.getSmallFont());
        descLabel.setForeground(WizardTheme.TEXT_SECONDARY);

        card.add(iconLabel, BorderLayout.NORTH);
        card.add(titleLabel, BorderLayout.CENTER);
        card.add(descLabel, BorderLayout.SOUTH);

        return card;
    }

    private String loadWelcomeContent(Path runDir) {
        Path markdownFilePath = runDir.resolve("packcore/lang/welcome_en_us.md");
        String defaultContent = createDefaultWelcomeHTML();

        try {
            MarkdownToHtmlConverter converter = new MarkdownToHtmlConverter();
            String htmlText = converter.convertMarkdownFileToHtml(markdownFilePath.toString());
            return styleHTMLContent(htmlText);
        } catch (IOException e) {
            LOGGER.error("Failed to load welcome markdown file: {}", markdownFilePath, e);
            return defaultContent;
        }
    }

    private String createDefaultWelcomeHTML() {
        return styleHTMLContent(
                "<h2>Welcome to Your Modpack Setup!</h2>" +
                        "<p>This wizard will guide you through configuring your modpack for the best experience.</p>" +
                        "<h3>What This Wizard Does:</h3>" +
                        "<ul>" +
                        "<li><strong>Detects your setup</strong> - Automatically identifies your screen resolution</li>" +
                        "<li><strong>Recommends configurations</strong> - Suggests the best settings for your system</li>" +
                        "<li><strong>Applies changes</strong> - Installs your chosen configuration automatically</li>" +
                        "</ul>" +
                        "<p>Ready to get started? Click <strong>Next</strong> to continue!</p>"
        );
    }

    private String styleHTMLContent(String htmlContent) {
        // Apply dark theme styling to HTML content
        String styledContent = "<html><head><style>" +
                "body { " +
                "  font-family: 'Segoe UI', sans-serif; " +
                "  color: " + String.format("#%06X", WizardTheme.TEXT_PRIMARY.getRGB() & 0xFFFFFF) + "; " +
                "  background-color: " + String.format("#%06X", WizardTheme.BACKGROUND_MEDIUM.getRGB() & 0xFFFFFF) + "; " +
                "  line-height: 1.5; " +
                "  margin: 10px; " +
                "} " +
                "h1, h2, h3 { color: " + String.format("#%06X", WizardTheme.ACCENT_GOLD.getRGB() & 0xFFFFFF) + "; } " +
                "strong { color: " + String.format("#%06X", 0xFFFFFF) + "; } " +
                "code { " +
                "  background-color: " + String.format("#%06X", WizardTheme.BACKGROUND_DARK.getRGB() & 0xFFFFFF) + "; " +
                "  padding: 2px 4px; " +
                "  border-radius: 3px; " +
                "} " +
                "</style></head><body>" + htmlContent + "</body></html>";

        return styledContent;
    }
}