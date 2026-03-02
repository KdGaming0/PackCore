package com.github.kd_gaming1.packcore.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class ModrinthClient {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ModrinthClient");
    private static final String API_BASE = "https://api.modrinth.com/v2";
    private static final String USER_AGENT = "PackCore/1.0";
    private static final int TIMEOUT_MS = 5000;

    private ModrinthClient() {}

    public record VersionInfo(String versionNumber, String changelog) {}

    /**
     * Fetches the latest version info for the given project ID.
     * Calls /project/{id} first to get the version list, then /version/{id} for the details.
     */
    public static Optional<VersionInfo> fetchLatestVersion(String projectId) {
        Optional<JsonObject> project = fetchProject(projectId);
        if (project.isEmpty()) return Optional.empty();

        JsonArray versionIds = project.get().getAsJsonArray("versions");
        if (versionIds == null || versionIds.isEmpty()) {
            LOGGER.warn("Modrinth project '{}' has no versions listed.", projectId);
            return Optional.empty();
        }

        // Versions are listed oldest to latest, last entry is the newest
        String latestVersionId = versionIds.get(versionIds.size() - 1).getAsString();

        return fetchVersionInfo(latestVersionId);
    }

    public static Optional<JsonObject> fetchProject(String projectId) {
        return get(API_BASE + "/project/" + projectId)
                .map(JsonElement::getAsJsonObject);
    }

    private static Optional<VersionInfo> fetchVersionInfo(String versionId) {
        return get(API_BASE + "/version/" + versionId).map(element -> {
            JsonObject version = element.getAsJsonObject();

            String versionNumber = version.get("version_number").getAsString();
            String changelog = version.has("changelog") && !version.get("changelog").isJsonNull()
                    ? version.get("changelog").getAsString()
                    : null;

            return new VersionInfo(versionNumber, changelog);
        });
    }

    private static Optional<JsonElement> get(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);

            int status = connection.getResponseCode();
            if (status == 404) {
                LOGGER.warn("Modrinth returned 404 for: {}", url);
                return Optional.empty();
            }
            if (status != 200) {
                LOGGER.warn("Modrinth API returned status {} for: {}", status, url);
                return Optional.empty();
            }

            try (InputStreamReader reader = new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8)) {
                return Optional.of(JsonParser.parseReader(reader));
            }

        } catch (Exception e) {
            LOGGER.warn("Failed to reach Modrinth API: {}", e.getMessage());
            return Optional.empty();
        }
    }
}