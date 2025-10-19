package com.github.kd_gaming1.packcore.scamshield.storage;

import com.github.kd_gaming1.packcore.scamshield.detector.ScamCategory;
import com.github.kd_gaming1.packcore.scamshield.detector.ScamPattern;

import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * JSON-serializable representation of a ScamPattern
 */
public class SerializedPattern {
    private String id;
    private String regex;
    private int weight;
    private String category;
    private boolean enabled;

    public SerializedPattern() {
        // Required for Gson
    }

    public SerializedPattern(String id, String regex, int weight, String category, boolean enabled) {
        this.id = id;
        this.regex = regex;
        this.weight = weight;
        this.category = category;
        this.enabled = enabled;
    }

    /**
     * Convert from ScamPattern to serializable format
     */
    public static SerializedPattern fromPattern(ScamPattern pattern) {
        return new SerializedPattern(
                pattern.getId(),
                pattern.getRegex().pattern(),
                pattern.getWeight(),
                pattern.getCategory().name(),
                pattern.isEnabled()
        );
    }

    public static boolean isSafeRegex(String regex) {
        // Check for nested quantifiers: )+ )* )? }+ }* }?
        if (regex.matches(".*\\)[+*?].*") ||
                regex.matches(".*\\}[+*?].*")) {
            return false;
        }

        // Check for quantifier on quantifier: ++ +* +? ** etc
        if (regex.matches(".*[+*?]{2}.*")) {
            return false;
        }

        // Check for excessive quantifiers
        int quantifierCount = 0;
        for (char c : regex.toCharArray()) {
            if (c == '+' || c == '*' || c == '?') {
                quantifierCount++;
            }
        }
        if (quantifierCount > 5) {
            return false;
        }

        return true;
    }

    /**
     * Convert back to ScamPattern
     */
    public ScamPattern toPattern() {
        if (!isSafeRegex(regex)) {
            throw new IllegalArgumentException("Unsafe regex pattern detected");
        }

        // Compile with timeout
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Pattern> future = executor.submit(() -> {
                try {
                    return Pattern.compile(regex);
                } catch (PatternSyntaxException e) {
                    throw new IllegalArgumentException("Invalid regex syntax: " + e.getMessage());
                }
            });

            Pattern compiled = future.get(2, TimeUnit.SECONDS);  // 2-second timeout

            ScamPattern pattern = new ScamPattern(id, compiled, weight, ScamCategory.valueOf(category));
            pattern.setEnabled(enabled);
            return pattern;

        } catch (TimeoutException e) {
            throw new IllegalArgumentException("Regex compilation took too long (likely malicious)");
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalArgumentException("Failed to compile regex: " + e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    // Getters and setters for Gson
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRegex() { return regex; }
    public void setRegex(String regex) { this.regex = regex; }

    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}