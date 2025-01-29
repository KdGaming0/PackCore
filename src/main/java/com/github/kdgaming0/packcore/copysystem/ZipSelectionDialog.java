package com.github.kdgaming0.packcore.copysystem;

import com.github.kdgaming0.packcore.config.ModConfig;
import com.github.kdgaming0.packcore.copysystem.utils.ExtractionProgressListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import java.awt.*;

/**
 * This class opens a Swing window early in loading
 * that lets the user pick a ZIP file from "Skyblock Enhanced"
 * and extracts it into the Minecraft root directory with a progress bar.
 */
public class ZipSelectionDialog extends JFrame {

    private static final long serialVersionUID = 1L;

    private final File minecraftRoot;
    private final File skyblockFolder;

    // Separate lists for official and custom zip files
    private final List<String> officialZips;
    private final List<String> customZips;

    private JList<String> officialZipFileList;
    private JList<String> customZipFileList;

    private volatile boolean userFinished = false;

    // Progress bar to show extraction progress
    private JProgressBar progressBar;

    public ZipSelectionDialog(File minecraftRoot) {
        super("Skyblock Enhanced - ZIP File Selection");

        // Store references
        this.minecraftRoot = minecraftRoot;
        this.skyblockFolder = new File(minecraftRoot, "Skyblock Enhanced");

        // Initialize lists
        this.officialZips = new ArrayList<>();
        this.customZips = new ArrayList<>();

        // Prepare the Swing UI on the EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(this::initializeUI);
    }

    /**
     * Initializes the user interface components.
     */
    private void initializeUI() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        // Top panel for explanatory text (non-scrollable)
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        // Use HTML in a label to allow multi-line text
        JLabel introLabel = new JLabel("<html><body style='width:450px;'>"
                + "<h2>Welcome to Skyblock Enhanced!</h2>"
                + "Before playing, please select the ready-to-use zip file that best matches your needs for the best experience:<br><br>"
                + "<b><i>Normal (1080p / 1440p):</i></b> Recommended for most players. This option has more features enabled by default, offering a rich and complex experience.<br>"
                + "<b><i>Lite (3-5 Business Days) (1080p / 1440p):</i></b> Designed for complete beginners, with fewer features enabled to simplify the experience. This is a great starting point to get familiar with the mod features. As you gain experience, you can enable additional features to create your perfect setup.<br><br>"
                + "Custom configurations you’ve added will also appear here. (You can create them in-game via <i>/packcore</i> or the <i>Config Management</i> button on the main menu.)<br><br>"
                + "If you skip this step, mods will use their default settings, as if downloaded individually and clicked play. This will cause many features to be disabled and have overlapping GUI elements. (I can't promise there won't be any overlapping with the ready-to-use configs but there will be less.)<br><br>"
                + "<b>If you are updating from a <i>pre-2.0</i> version of <b>Skyblock Enhanced</b> and don't want to reset the configs, click <b><i>Skip</i></b>.</b><br><br>"
                + "Click <b><i>Extract</i></b> to apply your chosen configuration or <b><i>Skip</i></b> to launch Minecraft with default mod settings."
                + "</body></html>"
        );
        introLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        infoPanel.add(introLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacer

        add(infoPanel, BorderLayout.NORTH);

        // Populate lists by scanning the Skyblock Enhanced folder
        populateZipLists();

        // Middle panel with 2 columns: official vs custom
        JPanel listsPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // Column for Official Zips
        JPanel officialPanel = new JPanel(new BorderLayout());
        JLabel officialLabel = new JLabel("Official Configs", JLabel.CENTER);
        officialPanel.add(officialLabel, BorderLayout.NORTH);

        officialZipFileList = new JList<>(officialZips.toArray(new String[0]));
        officialZipFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollOfficial = new JScrollPane(officialZipFileList);
        officialPanel.add(scrollOfficial, BorderLayout.CENTER);

        // Column for Custom Zips
        JPanel customPanel = new JPanel(new BorderLayout());
        JLabel customLabel = new JLabel("Custom Configs", JLabel.CENTER);
        customPanel.add(customLabel, BorderLayout.NORTH);

        customZipFileList = new JList<>(customZips.toArray(new String[0]));
        customZipFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollCustom = new JScrollPane(customZipFileList);
        customPanel.add(scrollCustom, BorderLayout.CENTER);

        // Add both columns to the middle panel
        listsPanel.add(officialPanel);
        listsPanel.add(customPanel);

        add(listsPanel, BorderLayout.CENTER);

        // Bottom panel for buttons and progress bar
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        // Progress bar (initially hidden)
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        bottomPanel.add(progressBar);

        // Panel for buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        // Extract button
        JButton extractButton = new JButton("Extract Selected");
        extractButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performExtraction();
            }
        });
        buttonPanel.add(extractButton);

        // Close button
        JButton closeButton = new JButton("Skip");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                userFinished = true;
                ModConfig.setPromptSetDefaultConfig(false); // Disable prompt for next time
                dispose();
                System.exit(0); // Exits the game
            }
        });
        buttonPanel.add(closeButton);

        bottomPanel.add(buttonPanel);
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Scans the Skyblock Enhanced folder for .zip files and
     * separates them into official and custom categories.
     */
  private void populateZipLists() {
    File officialFolder = new File(skyblockFolder, "OfficialConfigs");
    File customFolder = new File(skyblockFolder, "CustomConfigs");

    if (officialFolder.exists() && officialFolder.isDirectory()) {
        File[] officialFiles = officialFolder.listFiles();
        if (officialFiles != null) {
            for (File f : officialFiles) {
                String name = f.getName();
                if (f.isFile() && name.toLowerCase().endsWith(".zip")) {
                    officialZips.add(name);
                }
            }
        }
    }

    if (customFolder.exists() && customFolder.isDirectory()) {
        File[] customFiles = customFolder.listFiles();
        if (customFiles != null) {
            for (File f : customFiles) {
                String name = f.getName();
                if (f.isFile() && name.toLowerCase().endsWith(".zip")) {
                    customZips.add(name);
                }
            }
        }
    }
}

    /**
     * Extracts the selected ZIP file using a background task with SwingWorker.
     * Displays a progress bar and then shows a final result dialog.
     */
    private void performExtraction() {
        // Check official list selection
        int officialSelectedIndex = officialZipFileList.getSelectedIndex();
        // Check custom list selection
        int customSelectedIndex = customZipFileList.getSelectedIndex();

        // The user can choose from either official or custom, but not both at once
        if (officialSelectedIndex < 0 && customSelectedIndex < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a ZIP file from either Official or Custom Configs.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Determine which list was selected
        String selectedZip = (officialSelectedIndex >= 0)
                ? officialZips.get(officialSelectedIndex)
                : customZips.get(customSelectedIndex);

        String subfolderName = (officialSelectedIndex >= 0) ? "OfficialConfigs" : "CustomConfigs";

        if (selectedZip != null) {
            // Display the progress bar
            progressBar.setVisible(true);
            progressBar.setValue(0);
            progressBar.setIndeterminate(false); // Set to determinate mode

            String finalSelectedZip = selectedZip;
            SwingWorker<Boolean, Integer> worker = new SwingWorker<Boolean, Integer>() {
                @Override
                protected Boolean doInBackground() {
                    // Perform the extraction using ZipExtractor and report progress
                    return ZipExtractor.extractZipContents(finalSelectedZip, subfolderName, minecraftRoot, new ExtractionProgressListener() {
                        @Override
                        public void onProgress(int progress) {
                            // Publish the current progress to the process() method
                            publish(progress);
                        }
                    });
                }

                @Override
                protected void process(List<Integer> chunks) {
                    // Update progress bar with the most recent value
                    int latestProgress = chunks.get(chunks.size() - 1);
                    progressBar.setValue(latestProgress);
                }

                @Override
                protected void done() {
                    boolean success;
                    try {
                        success = get();
                    } catch (Exception e) {
                        success = false;
                    }

                    // Show result message
                    if (success) {
                        JOptionPane.showMessageDialog(
                                ZipSelectionDialog.this,
                                "Extraction of \"" + finalSelectedZip + "\" completed successfully!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        JOptionPane.showMessageDialog(
                                ZipSelectionDialog.this,
                                "Extraction of \"" + finalSelectedZip + "\" encountered an error. Report this to the mod author.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }

                    // Hide and reset progress bar
                    progressBar.setVisible(false);
                    progressBar.setValue(0);

                    userFinished = true;
                    ModConfig.setPromptSetDefaultConfig(false); // Disable prompt for next time
                    dispose(); // Close dialog to continue Minecraft startup
                }
            };

            worker.execute();
        }
    }

    /**
     * Blocks until the user finishes (selecting a ZIP or closing the window).
     */
    public void waitForUserSelection() {
        while (!userFinished) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * Convenience method to instantiate and display the dialog,
     * then block until user finishes.
     *
     * @param minecraftRoot The Minecraft root directory (modpack folder).
     */
    public static void showDialog(File minecraftRoot) {
        ZipSelectionDialog dialog = new ZipSelectionDialog(minecraftRoot);
        dialog.waitForUserSelection();
    }
}