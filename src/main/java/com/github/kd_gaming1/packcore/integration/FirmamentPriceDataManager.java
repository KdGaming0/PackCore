package com.github.kd_gaming1.packcore.integration;

import com.github.kd_gaming1.packcore.PackCore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FirmamentPriceDataManager {

    private static final Gson GSON = new Gson();
    private static final String FILE_NAME = "price-data.json";

    private FirmamentPriceDataManager() {
    }

    public static boolean disableAlways(Path gameDir) {
        if (!FabricLoader.getInstance().isModLoaded("firmament")) {
            PackCore.LOGGER.info("Firmament price-data: firmament not loaded, skipping");
            return false;
        }

        Path priceDataPath = gameDir.resolve("config").resolve("firmament").resolve(FILE_NAME);
        if (!Files.exists(priceDataPath)) {
            PackCore.LOGGER.info("Firmament price-data: {} not found, skipping", priceDataPath);
            return false;
        }

        JsonObject json;
        try (Reader reader = Files.newBufferedReader(priceDataPath, StandardCharsets.UTF_8)) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException | JsonParseException | IllegalStateException e) {
            PackCore.LOGGER.warn("Firmament price-data: failed to read {}: {}", priceDataPath, e.getMessage());
            return false;
        }

        boolean enabled = json.has("enable-always") && json.get("enable-always").getAsBoolean();
        if (!enabled) {
            PackCore.LOGGER.info("Firmament price-data: enable-always already disabled");
            return true;
        }

        json.addProperty("enable-always", false);

        try (Writer writer = Files.newBufferedWriter(priceDataPath, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            PackCore.LOGGER.warn("Firmament price-data: failed to write {}: {}", priceDataPath, e.getMessage());
            return false;
        }

        PackCore.LOGGER.info("Firmament price-data: set enable-always to false");
        return true;
    }
}

