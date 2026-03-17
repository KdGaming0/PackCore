package com.github.kd_gaming1.packcore.integration;

import eu.tango.scamscreener.api.ScamScreenerAlertLevel;
import eu.tango.scamscreener.api.ScamScreenerApi;
import eu.tango.scamscreener.api.ScamScreenerSettingsApi;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Arrays;
import java.util.List;

final class ScamScreenerApiBridge {

    private ScamScreenerApiBridge() {}

    static boolean isAvailable() {
        return !apis().isEmpty();
    }

    static ScamScreenerConfigurator.RuntimeSettings loadSettings() {
        ScamScreenerSettingsApi settings = api().settings();
        return new ScamScreenerConfigurator.RuntimeSettings(
                settings.alertMinimumRiskLevel().name(),
                settings.pingOnRiskWarning(),
                settings.pingOnBlacklistWarning()
        );
    }

    static List<String> availableAlertLevels() {
        return Arrays.stream(ScamScreenerAlertLevel.values())
                .map(Enum::name)
                .toList();
    }

    static boolean apply(String minimumRiskLevel, boolean pingOnRiskWarning, boolean pingOnBlacklistWarning) {
        ScamScreenerApi api = api();
        ScamScreenerSettingsApi settings = api.settings();

        ScamScreenerAlertLevel alertLevel = ScamScreenerAlertLevel.valueOf(minimumRiskLevel);
        boolean changed = false;

        if (settings.alertMinimumRiskLevel() != alertLevel) {
            settings.setAlertMinimumRiskLevel(alertLevel);
            changed = true;
        }

        if (settings.pingOnRiskWarning() != pingOnRiskWarning) {
            settings.setPingOnRiskWarning(pingOnRiskWarning);
            changed = true;
        }

        if (settings.pingOnBlacklistWarning() != pingOnBlacklistWarning) {
            settings.setPingOnBlacklistWarning(pingOnBlacklistWarning);
            changed = true;
        }

        if (changed) {
            api.reload();
        }

        return true;
    }

    private static ScamScreenerApi api() {
        List<ScamScreenerApi> apis = apis();
        if (apis.isEmpty()) {
            throw new IllegalStateException("ScamScreener API entrypoint not found");
        }
        return apis.getFirst();
    }

    private static List<ScamScreenerApi> apis() {
        return FabricLoader.getInstance().getEntrypoints(ScamScreenerApi.ENTRYPOINT_KEY, ScamScreenerApi.class);
    }
}
