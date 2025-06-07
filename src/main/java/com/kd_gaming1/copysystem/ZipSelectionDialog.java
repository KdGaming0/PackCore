package com.kd_gaming1.copysystem;

import com.kd_gaming1.config.ModConfig;
import com.kd_gaming1.copysystem.utils.ExtractionProgressListener;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import java.awt.*;

/**
 * This class opens a Swing window early in loading that lets the user pick a ZIP file
 * from "Skyblock Enhanced" and extracts it into the Minecraft root directory with a
 * progress bar. If only one zip file is available, it automatically extracts it.
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

    // Proper synchronization primitive instead of busy-wait
    private final CountDownLatch completionLatch = new CountDownLatch(1);

    public ZipSelectionDialog(File minecraftRoot) {
        super("Skyblock Enhanced - ZIP File Selection");

        // Store references
        this.minecraftRoot = minecraftRoot;
        this.skyblockFolder = new File(minecraftRoot, "Skyblock Enhanced");

        // Initialize lists
        this.officialZips = new ArrayList<>();
        this.customZips = new ArrayList<>();
    }

    /**
     * Marks the dialog as finished and releases the waiting thread
     */
    private void markFinished() {
        userFinished = true;
        ModConfig.setPromptSetDefaultConfig(false);
        dispose();
        // Release the waiting thread
        completionLatch.countDown();
    }

    /**
     * Scans the Skyblock Enhanced folder for .zip files and separates them into
     * official and custom categories.
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
     * Checks if there's only one zip file available and auto-extracts it,
     * or shows the dialog if multiple files are available.
     */
    private void handleZipSelection() {
        // Check if dialog is disabled in config
        if (!ModConfig.getPromptSetDefaultConfig()) {
            System.out.println("Dialog is disabled in config. Skipping zip selection.");
            markFinished();
            return;
        }

        populateZipLists();

        int officialCount = officialZips.size();
        int customCount = customZips.size();

        if (customCount > 0) {
            // If there is ANY custom config zip, always show the dialog
            SwingUtilities.invokeLater(this::initializeUI);
        } else if (officialCount == 0) {
            // No zip files at all, proceed with default settings
            System.out.println("No zip files found in Skyblock Enhanced folder. Using default settings.");
            markFinished();
        } else if (officialCount == 1) {
            // Exactly one official zip and no custom zips, auto-extract
            String zipToExtract = officialZips.get(0);
            System.out.println("Auto-extracting single official zip file: " + zipToExtract);
            performAutoExtraction(zipToExtract, "OfficialConfigs");
        } else {
            // More than one official zip, show dialog
            SwingUtilities.invokeLater(this::initializeUI);
        }
    }

    /**
     * Performs automatic extraction when only one zip file is available.
     */
    private void performAutoExtraction(String zipFileName, String subfolderName) {
        // Use SwingWorker to avoid blocking EDT but still block main thread via latch
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return ZipExtractor.extractZipContents(zipFileName, subfolderName, minecraftRoot,
                        new ExtractionProgressListener() {
                            @Override
                            public void onProgress(int progress) {
                                System.out.println("Extraction progress: " + progress + "%");
                            }
                        });
            }

            @Override
            protected void done() {
                boolean success;
                try {
                    success = get();
                } catch (Exception e) {
                    success = false;
                    e.printStackTrace();
                }

                if (success) {
                    System.out.println("Auto-extraction of \"" + zipFileName + "\" completed successfully!");
                } else {
                    System.err.println("Auto-extraction of \"" + zipFileName + "\" failed!");
                }

                markFinished(); // This will release the latch
            }
        };

        worker.execute();
    }

    /**
     * Initializes the user interface components (only called when multiple zips exist).
     */
    private void initializeUI() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        // Top panel for explanatory text (non-scrollable)
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        JLabel introLabel = new JLabel("<html><body style='width:450px;'>"
                + "<h2>Welcome to Skyblock Enhanced!</h2>"
                + "This dialog appears when you have custom configurations available to choose from. "
                + "Custom configurations contain personalized mod settings that you or others have created.<br><br>"
                + "The configurations listed here are custom setups that override the default mod settings. "
                + "You can create your own custom configurations using the <i>/packcore help</i> command to see all available options.<br><br>"
                + "<b>What happens when you choose:</b><br>"
                + "• <b>Extract:</b> Your selected custom configuration will be applied, replacing the current mod settings, "
                + "then Minecraft will continue loading with your chosen setup.<br>"
                + "• <b>Skip:</b> Minecraft will continue loading with the current settings (either default settings or "
                + "previously applied configurations).<br><br>"
                + "<b>⚠️ IMPORTANT:</b> After clicking Extract or Skip, please wait patiently! It may take 10-30 seconds "
                + "before the Minecraft window appears. The game is loading in the background - don't worry if nothing seems "
                + "to happen immediately.<br><br>"
                + "<b>Managing this dialog:</b><br>"
                + "• To open this dialog again in the future, use the command: <b>/packcore dialog true</b><br>"
                + "• You can also access packcore options from the main menu to manage configurations<br>"
                + "• Use <b>/packcore help</b> to see all available configuration management commands"
                + "</body></html>"
        );
        introLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        infoPanel.add(introLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacer

        add(infoPanel, BorderLayout.NORTH);

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
        JButton extractButton = new JButton("Extract & Launch Minecraft");
        extractButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performExtraction();
            }
        });
        buttonPanel.add(extractButton);

        // Skip button - make it clearer what happens
        JButton closeButton = new JButton("Skip & Launch without Configs");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                markFinished(); // This will release the latch and continue Minecraft startup
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

                    markFinished(); // This will release the latch and continue Minecraft startup
                }
            };

            worker.execute();
        }
    }

    /**
     * Properly blocks until the user finishes using CountDownLatch instead of busy-wait.
     * This prevents macOS freezing while still blocking Minecraft startup.
     */
    public void waitForUserSelection() {
        try {
            // Wait for the latch to be released (when user completes dialog)
            // Timeout after 10 minutes as safety measure
            boolean completed = completionLatch.await(10, TimeUnit.MINUTES);
            if (!completed) {
                System.err.println("WARNING: Dialog timed out after 10 minutes. Continuing with default settings.");
                markFinished();
            }
        } catch (InterruptedException e) {
            System.err.println("Dialog wait was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status
            markFinished();
        }
    }

    /**
     * Convenience method to instantiate and display the dialog,
     * then block until user finishes. Auto-extracts if only one zip exists.
     *
     * @param minecraftRoot The Minecraft root directory (modpack folder).
     */
    public static void showDialog(File minecraftRoot) {
        ZipSelectionDialog dialog = new ZipSelectionDialog(minecraftRoot);
        dialog.handleZipSelection();
        dialog.waitForUserSelection();
    }
}