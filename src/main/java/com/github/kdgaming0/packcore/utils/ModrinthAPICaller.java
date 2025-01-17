package com.github.kdgaming0.packcore.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static com.github.kdgaming0.packcore.PackCore.MOD_ID;

public class ModrinthAPICaller {
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    private static final String MODRINTH_API_BASE = "https://api.modrinth.com/v2/project/%s/version";
    private static final Gson gson = new Gson();

    // Cache duration in milliseconds (15 minutes)
    private static final long CACHE_DURATION = TimeUnit.MINUTES.toMillis(15);

    private static String cachedVersion = null;
    private static String cachedChangelog = null;
    private static long lastCacheTime = 0;

    public static class VersionResponse {
        public final String version;
        public final String changelog;
        public final boolean success;
        public final String errorMessage;

        public VersionResponse(String version, String changelog) {
            this.version = version;
            this.changelog = changelog;
            this.success = true;
            this.errorMessage = null;
        }
        public VersionResponse(String errorMessage) {
            this.version = null;
            this.changelog = null;
            this.success = false;
            this.errorMessage = errorMessage;
        }
    }

    public static VersionResponse getLatestVersion(String projectId) {
        // Check if project ID is empty or null
        if (projectId == null || projectId.trim().isEmpty()) {
            LOGGER.error("Modrinth Project ID is not set in modpack_info.json");
            return new VersionResponse("Modrinth Project ID is not configured. Please set it in the modpack_info.json file." +
                    "Please report this issue at the modpack's issue tracker. Link at the top of the page.");
        }

        // Check if cache is still valid
        if (cachedVersion != null && System.currentTimeMillis() - lastCacheTime < CACHE_DURATION) {
            return new VersionResponse(cachedVersion, cachedChangelog);
        }

        // Cache expired or doesn't exist, make new API call
        HttpURLConnection conn = null;
        try {
            URL url = new URL(String.format(MODRINTH_API_BASE, projectId));
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "ModpackUpdateChecker/1.0");
            conn.setConnectTimeout(5000); // 5 seconds timeout for connection
            conn.setReadTimeout(5000);    // 5 seconds timeout for reading

            int responseCode = conn.getResponseCode();

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    // Parse response
                    JsonArray versions = gson.fromJson(response.toString(), JsonArray.class);
                    if (versions.size() > 0) {
                        JsonObject latestVersion = versions.get(0).getAsJsonObject();

                        // Update cache
                        cachedVersion = latestVersion.get("version_number").getAsString();
                        cachedChangelog = latestVersion.has("changelog") ?
                                latestVersion.get("changelog").getAsString() : "No changelog available";
                        lastCacheTime = System.currentTimeMillis();

                        return new VersionResponse(cachedVersion, cachedChangelog);
                    } else {
                        return new VersionResponse("No versions found for this project. The API call failed to return any data.");
                    }
                }
            } else if (responseCode == 404) {
                LOGGER.error("Project not found on Modrinth. Check if the Project ID is correct: " + projectId);
                return new VersionResponse("Project not found on Modrinth. Please verify the Project ID.");
            } else {
                LOGGER.error("Modrinth API returned error code: " + responseCode);
                return new VersionResponse("Failed to fetch version data (HTTP " + responseCode + ")");
            }
        } catch (java.net.SocketTimeoutException e) {
            LOGGER.error("Connection to Modrinth API timed out", e);
            return handleError("Connection to Modrinth timed out. Please check your internet connection.");
        } catch (IOException e) {
            LOGGER.error("Error fetching version data from Modrinth", e);
            return handleError("Failed to connect to Modrinth. Please check your internet connection.");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static VersionResponse handleError(String errorMessage) {
        // If we have cached data, return it with a warning
        if (cachedVersion != null) {
            LOGGER.warn("Using cached version data due to API error");
            return new VersionResponse(cachedVersion, cachedChangelog + "\n\nNote: Using cached data due to connection issues.");
        }
        return new VersionResponse(errorMessage);
    }
}