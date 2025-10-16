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

        for (int i = 0; i < versionsArray.size(); i++) {
            JsonObject versionObj = versionsArray.get(i).getAsJsonObject();
            String versionType = versionObj.get("version_type").getAsString();
            JsonArray gameVersions = versionObj.getAsJsonArray("game_versions");

            if (!isVersionTypeAllowed(versionType, updateChannel) || !supportsMinecraftVersion(gameVersions, minecraftVersion)) {
                continue;
            }

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

        return suitableVersions.isEmpty() ? null : suitableVersions.getFirst();
    }

    private boolean isVersionTypeAllowed(String versionType, String updateChannel) {
        return switch (updateChannel.toLowerCase()) {
            case "alpha" -> true;
            case "beta" -> versionType.equals("beta") || versionType.equals("release");
            case "release" -> versionType.equals("release");
            default -> false;
        };
    }

    private boolean supportsMinecraftVersion(JsonArray gameVersions, String targetVersion) {
        String baseVersion = targetVersion.endsWith("+") ? targetVersion.substring(0, targetVersion.length() - 1) : null;

        for (int i = 0; i < gameVersions.size(); i++) {
            String version = gameVersions.get(i).getAsString();

            if (version.equals(targetVersion) || (baseVersion != null && version.startsWith(baseVersion))) {
                return true;
            }
        }

        return false;
    }
}
