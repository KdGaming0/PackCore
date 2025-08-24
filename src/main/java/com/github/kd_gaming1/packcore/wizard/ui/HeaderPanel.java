package com.github.kd_gaming1.packcore.wizard.ui;

import com.github.kd_gaming1.packcore.wizard.ui.theme.WizardTheme;

import javax.swing.*;
import java.awt.*;

public class HeaderPanel extends JPanel {

    public HeaderPanel() {
        setBackground(WizardTheme.BACKGROUND_MEDIUM);
        setPreferredSize(new Dimension(900, 80));
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, WizardTheme.ACCENT_GOLD));

        // Left side - icon and title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        titlePanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

        JLabel iconLabel = new JLabel("⚙️");
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 32));

        JLabel titleLabel = new JLabel("Modpack Setup Wizard");
        titleLabel.setFont(WizardTheme.getTitleFont());
        titleLabel.setForeground(WizardTheme.TEXT_PRIMARY);

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);

        // Right side - subtitle with proper padding
        JPanel subtitlePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 30));
        subtitlePanel.setBackground(WizardTheme.BACKGROUND_MEDIUM);

        JLabel subtitleLabel = new JLabel("Configure your modpack for the best experience");
        subtitleLabel.setFont(WizardTheme.getBodyFont());
        subtitleLabel.setForeground(WizardTheme.TEXT_SECONDARY);

        subtitlePanel.add(subtitleLabel);

        add(titlePanel, BorderLayout.WEST);
        add(subtitlePanel, BorderLayout.EAST);
    }
}