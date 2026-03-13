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

    public static Optional<VersionInfo> fetchLatestVersion(String projectId) {
        return get(API_BASE + "/project/" + projectId + "/version")
                .flatMap(element -> {
                    JsonArray versions = element.getAsJsonArray();
                    if (versions.isEmpty()) {
                        LOGGER.warn("Modrinth project '{}' has no versions listed.", projectId);
                        return Optional.empty();
                    }

                    JsonObject latest = versions.get(0).getAsJsonObject();

                    String versionNumber = latest.get("version_number").getAsString();
                    String changelog = latest.has("changelog") && !latest.get("changelog").isJsonNull()
                            ? latest.get("changelog").getAsString()
                            : null;

                    return Optional.of(new VersionInfo(versionNumber, changelog));
                });
    }

    private static Optional<JsonElement> get(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
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
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}