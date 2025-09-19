package com.github.kd_gaming1.packcore.wizard;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.util.ConfigApplicationManager;
import com.github.kd_gaming1.packcore.util.PackCoreFileManager;
import com.github.kd_gaming1.packcore.wizard.ui.ModpackSetupWizard;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class PreLaunchWizardInitializer implements PreLaunchEntrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreLaunchWizardInitializer.class);
    private static final CountDownLatch wizardCompletionLatch = new CountDownLatch(1);
    private static final AtomicBoolean wizardResult = new AtomicBoolean(false);

    @Override
    public void onPreLaunch() {
        LOGGER.info("PackCore pre-launch initializer started");

        // Initialize MidnightConfig to load existing settings
        MidnightConfig.init("packcore", PackCoreConfig.class);

        Path runDir = FabricLoader.getInstance().getGameDir();

        // Always create necessary directories and files (from original code)
        createDirectoriesAndFiles(runDir);

        // CHECK FOR PENDING CONFIG APPLICATION FIRST
        boolean configApplied = ConfigApplicationManager.checkAndApplyPendingConfig(runDir);
        if (configApplied) {
            LOGGER.info("Applied pending config during pre-launch");
            // Update config to reflect successful application
            PackCoreConfig.haveSetupWizardCompletedSuccessfully = true;
            PackCoreConfig.isFirstStartup = false;
            MidnightConfig.write("packcore");
        }

        // Check if wizard should run
        if (shouldRunWizard()) {
            LOGGER.info("Starting setup wizard...");
            runSetupWizard(runDir);
        } else {
            LOGGER.info("Setup wizard already completed, skipping...");
        }

        LOGGER.info("PackCore pre-launch initialization complete");
    }

    private boolean shouldRunWizard() {
        // Run wizard if:
        // 1. User manually enabled it via config (showInstallWizard = true), OR
        // 2. It's the first startup and hasn't been completed successfully
        boolean manualTrigger = PackCoreConfig.showInstallWizard;
        boolean firstTimeNeeded = PackCoreConfig.isFirstStartup && !PackCoreConfig.haveSetupWizardCompletedSuccessfully;

        boolean needsWizard = manualTrigger || firstTimeNeeded;

        LOGGER.info("Wizard needed: {} (manual trigger: {}, first time: {}, completed: {})",
                needsWizard,
                manualTrigger,
                firstTimeNeeded,
                PackCoreConfig.haveSetupWizardCompletedSuccessfully);

        return needsWizard;
    }

    private void runSetupWizard(Path runDir) {
        try {
            // Ensure we're on the Event Dispatch Thread for Swing
            if (SwingUtilities.isEventDispatchThread()) {
                showWizardAndWait(runDir);
            } else {
                SwingUtilities.invokeAndWait(() -> showWizardAndWait(runDir));
            }

            // Wait for wizard completion
            wizardCompletionLatch.await();

            // Check if wizard was completed successfully
            if (wizardResult.get()) {
                LOGGER.info("Setup wizard completed successfully");
                updateConfigAfterWizardSuccess();
            } else {
                LOGGER.warn("Setup wizard was cancelled or failed");
                handleWizardFailure();
            }

        } catch (Exception e) {
            LOGGER.error("Failed to run setup wizard", e);
            handleWizardFailure();
        }
    }

    private void showWizardAndWait(Path runDir) {
        // Create and show the wizard
        ModpackSetupWizard wizard = new ModpackSetupWizard(runDir);

        // Add window listener to handle wizard completion/cancellation
        wizard.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                handleWizardCompletion(wizard);
            }
        });

        // Mark that wizard has been shown
        PackCoreConfig.haveSetupWizardShown = true;
        MidnightConfig.write("packcore");
    }

    private void handleWizardCompletion(ModpackSetupWizard wizard) {
        try {
            // Check if extraction was completed successfully
            boolean extractionCompleted = wizard.getReviewAndApplyPage().isExtractionCompleted();

            if (extractionCompleted) {
                // Get the selected configuration
                String selectedConfig = getSelectedConfiguration();

                LOGGER.info("Wizard completed successfully with config: {}", selectedConfig);

                // Mark wizard as shown (completion will be handled by NavigationPanel)
                PackCoreConfig.haveSetupWizardShown = true;

                // If this was manually triggered, reset the trigger
                if (PackCoreConfig.showInstallWizard) {
                    LOGGER.info("Resetting manual wizard trigger");
                }

                // Write config to disk
                MidnightConfig.write("packcore");

                wizardResult.set(true);
            } else {
                LOGGER.warn("Wizard closed without completing extraction");
                // Mark as shown but not completed
                PackCoreConfig.haveSetupWizardShown = true;
                PackCoreConfig.haveSetupWizardCompletedSuccessfully = false;

                // Reset manual trigger even if not completed (user can set it again if needed)
                PackCoreConfig.showInstallWizard = false;

                MidnightConfig.write("packcore");
                wizardResult.set(false);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling wizard completion", e);
            // Reset manual trigger on error too
            PackCoreConfig.showInstallWizard = false;
            MidnightConfig.write("packcore");
            wizardResult.set(false);
        } finally {
            // Always release the latch to prevent blocking
            wizardCompletionLatch.countDown();
        }
    }

    private String getSelectedConfiguration() {
        // Get the selected configuration from the static field
        return com.github.kd_gaming1.packcore.wizard.ui.content.ConfigSelectionPage.selectedResolution;
    }

    private void updateConfigAfterWizardSuccess() {
        // Additional post-wizard setup can go here
        LOGGER.info("Post-wizard configuration update completed");
    }

    private void handleWizardFailure() {
        // Reset wizard state to allow retry on next launch
        PackCoreConfig.haveSetupWizardCompletedSuccessfully = false;
        MidnightConfig.write("packcore");

        // You could show a simple dialog asking if they want to continue anyway
        int choice = JOptionPane.showConfirmDialog(
                null,
                "The setup wizard was not completed. Would you like to continue launching Minecraft anyway?\n\n" +
                        "You can run the wizard again by deleting the config file or changing the settings.",
                "Setup Incomplete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            LOGGER.info("User chose not to continue without setup completion");
            System.exit(0); // Exit the game
        }
    }

    // Renamed from createDirectories to be more explicit about what it does
    private void createDirectoriesAndFiles(Path runDir) {
        PackCoreFileManager.initializeFileStructure();
    }

    // Static method to check if wizard is needed (can be called from other parts of your mod)
    public static boolean isWizardNeeded() {
        return !PackCoreConfig.haveSetupWizardShown ||
                !PackCoreConfig.haveSetupWizardCompletedSuccessfully;
    }
}