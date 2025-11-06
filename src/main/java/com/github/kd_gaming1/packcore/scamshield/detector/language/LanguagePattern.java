package com.github.kd_gaming1.packcore.scamshield.detector.language;

import com.github.kd_gaming1.packcore.scamshield.detector.pattern.AhoCorasickMatcher;

import java.util.*;

public class LanguagePattern {
    private List<String> phrases;
    private double weight;
    private String context;
    private List<String> examples;

    private transient AhoCorasickMatcher matcher;

    public List<String> getPhrases() { return phrases; }
    public double getWeight() { return weight; }
    public String getContext() { return context; }
    public List<String> getExamples() { return examples; }

    public boolean matches(String message) {
        ensureMatcherBuilt();
        return !matcher.findMatches(message).isEmpty();
    }

    public int countMatches(String message) {
        ensureMatcherBuilt();
        return matcher.findMatches(message).size();
    }

    private void ensureMatcherBuilt() {
        if (matcher == null) {
            matcher = new AhoCorasickMatcher();
            if (phrases != null) {
                for (String phrase : phrases) {
                    matcher.addPattern(phrase);
                }
            }
        }
    }
}