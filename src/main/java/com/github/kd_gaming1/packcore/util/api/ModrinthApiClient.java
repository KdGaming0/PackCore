package com.github.kd_gaming1.packcore.util.api;

import com.github.kd_gaming1.packcore.PackCore;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ModrinthApiClient {

    private static final String API_BASE_URL = "https://api.modrinth.com/v2";
    private static final String USER_AGENT = "kdgaming0/packcore/2.0.0";

    private final HttpClient httpClient;
    private final Gson gson;

    public ModrinthApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
    }

    // Get all versions for a project and find the latest suitable one
    public ModrinthVersion getLatestVersion(String projectId, String updateChannel, String minecraftVersion) throws IOException, InterruptedException {
        PackCore.LOGGER.info("Checking for updates - Project: {}, Channel: {}, MC Version: {}",
                projectId, updateChannel, minecraftVersion);

        String url = API_BASE_URL + "/project/" + projectId + "/version";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String error = "Modrinth API returned status: " + response.statusCode() + " for project: " + projectId;
            PackCore.LOGGER.error(error);
            throw new IOException(error);
        }

        PackCore.LOGGER.debug("API Response: {}", response.body());
        return parseLatestVersion(response.body(), updateChannel, minecraftVersion);
    }

    private ModrinthVersion parseLatestVersion(String jsonResponse, String updateChannel, String minecraftVersion) {
        JsonArray versionsArray = gson.fromJson(jsonResponse, JsonArray.class);
        List<ModrinthVersion> suitableVersions = new ArrayList<>();

        // Filter versions that match our criteria
        for (int i = 0; i < versionsArray.size(); i++) {
            JsonObject versionObj = versionsArray.get(i).getAsJsonObject();

            String versionType = versionObj.get("version_type").getAsString();
            JsonArray gameVersions = versionObj.getAsJsonArray("game_versions");

            // Check if version type matches our update channel
            if (!isVersionTypeAllowed(versionType, updateChannel)) {
                continue;
            }

            // REPLACE the inline code with this method call:
            if (!supportsMinecraftVersion(gameVersions, minecraftVersion)) {
                continue;
            }

            // This version meets our criteria
            ModrinthVersion version = new ModrinthVersion(
                    versionObj.get("version_number").getAsString(),
                    versionType,
                    versionObj.has("changelog") && !versionObj.get("changelog").isJsonNull()
                            ? versionObj.get("changelog").getAsString() : "No changelog available",
                    versionObj.get("id").getAsString(),
                    versionObj.get("date_published").getAsString()
            );

            suitableVersions.add(version);
        }

        // Return the most recent suitable version (versions are already sorted by date)
        return suitableVersions.isEmpty() ? null : suitableVersions.get(0);
    }

    private boolean isVersionTypeAllowed(String versionType, String updateChannel) {
        switch (updateChannel.toLowerCase()) {
            case "alpha":
                return true;
            case "beta":
                return versionType.equals("beta") || versionType.equals("release");
            case "release":
                return versionType.equals("release");
            default:
                return false;
        }
    }

    private boolean supportsMinecraftVersion(JsonArray gameVersions, String targetVersion) {
        // Handle exact matches first
        for (int j = 0; j < gameVersions.size(); j++) {
            String gameVersion = gameVersions.get(j).getAsString();
            if (gameVersion.equals(targetVersion)) {
                return true;
            }
        }

        // Handle pattern matching (e.g., "1.21.+" matches "1.21.0", "1.21.1", etc.)
        if (targetVersion.endsWith("+")) {
            String baseVersion = targetVersion.substring(0, targetVersion.length() - 1);
            for (int j = 0; j < gameVersions.size(); j++) {
                String gameVersion = gameVersions.get(j).getAsString();
                if (gameVersion.startsWith(baseVersion)) {
                    return true;
                }
            }
        }

        return false;
    }
}
