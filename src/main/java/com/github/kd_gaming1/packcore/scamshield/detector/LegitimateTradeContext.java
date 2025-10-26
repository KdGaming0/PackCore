package com.github.kd_gaming1.packcore.scamshield.detector;

import java.util.regex.Pattern;

/**
 * Detects when a message is a legitimate trade/business advertisement.
 * These messages often include commands like /visit, but are NOT scams.
 */
public class LegitimateTradeContext {

    // Lowballing - legitimate practice on Hypixel Skyblock
    private static final Pattern LOWBALLING = Pattern.compile(
            "\\b(lb|lowball|lowballing)\\s+\\d+[mkb]\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Selling/buying with prices
    private static final Pattern SELLING_BUYING = Pattern.compile(
            "\\b(selling|buying|sold|wtb|wts)\\s+.*?\\d+[mkb]\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Auction house references
    private static final Pattern AUCTION_REF = Pattern.compile(
            "\\b(ah|auction house|bin|bid)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Shop/minion selling
    private static final Pattern SHOP_REFERENCE = Pattern.compile(
            "\\b(shop|store|minions? for sale|selling minions?)\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Check if a message appears to be legitimate trade advertising.
     *
     * @param message The cleaned message text
     * @return true if this looks like legitimate trade content
     */
    public static boolean isLegitimateTradeAd(String message) {
        // Lowballing with amount = legitimate
        if (LOWBALLING.matcher(message).find()) {
            return true;
        }

        // Selling/buying with prices = legitimate
        if (SELLING_BUYING.matcher(message).find()) {
            return true;
        }

        // Auction house mention = legitimate
        if (AUCTION_REF.matcher(message).find()) {
            return true;
        }

        // Shop/store reference = legitimate
        if (SHOP_REFERENCE.matcher(message).find()) {
            return true;
        }

        return false;
    }

    /**
     * Calculate score reduction multiplier for legitimate contexts.
     *
     * @param message The message to analyze
     * @return Multiplier to apply (0.0 to 1.0, where 0 = completely ignore, 1.0 = no reduction)
     */
    public static double getScoreMultiplier(String message) {
        if (!isLegitimateTradeAd(message)) {
            return 1.0; // No reduction
        }

        // If it's lowballing or selling with prices, heavily reduce visit command scores
        if (LOWBALLING.matcher(message).find() || SELLING_BUYING.matcher(message).find()) {
            return 0.1; // Reduce to 10% (nearly ignore visit commands)
        }

        // For other legitimate contexts, moderately reduce
        return 0.3; // Reduce to 30%
    }
}