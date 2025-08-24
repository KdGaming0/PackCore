package com.github.kd_gaming1.packcore.wizard.ui.content;

import com.github.kd_gaming1.packcore.wizard.ui.theme.WizardTheme;

import javax.swing.*;
import java.awt.*;

public class MainContentInfo extends JPanel {

    public MainContentInfo() {
        setBackground(WizardTheme.BACKGROUND_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

    protected JPanel createTitledPanel(String title, String subtitle) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(WizardTheme.BACKGROUND_MEDIUM);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WizardTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(WizardTheme.getHeaderFont());
        titleLabel.setForeground(WizardTheme.ACCENT_GOLD);

        headerPanel.add(titleLabel, BorderLayout.WEST);

        if (subtitle != null && !subtitle.isEmpty()) {
            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setFont(WizardTheme.getSmallFont());
            subtitleLabel.setForeground(WizardTheme.TEXT_MUTED);
            headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        }

        panel.add(headerPanel, BorderLayout.NORTH);

        return panel;
    }

    protected JButton createActionButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(WizardTheme.getBodyFont());
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(150, 40));

        return button;
    }
}