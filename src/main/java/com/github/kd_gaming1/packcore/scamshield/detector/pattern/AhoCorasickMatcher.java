package com.github.kd_gaming1.packcore.scamshield.detector.pattern;

import java.util.*;

/**
 * Aho-Corasick pattern matcher for efficient multi-pattern searching.
 * Finds all occurrences of multiple patterns in O(n + m + z) time where:
 * n = text length, m = total pattern length, z = number of matches
 */
public class AhoCorasickMatcher {
    private final TrieNode root = new TrieNode();
    private boolean compiled = false;

    static class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        TrieNode failure;
        final Set<String> patterns = new HashSet<>();
        int depth = 0;
    }

    public void addPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) return;

        TrieNode node = root;
        for (char c : pattern.toCharArray()) {
            TrieNode finalNode = node;
            node = node.children.computeIfAbsent(c, k -> {
                TrieNode child = new TrieNode();
                child.depth = finalNode.depth + 1;
                return child;
            });
        }
        node.patterns.add(pattern);
        compiled = false;
    }

    private void buildFailureLinks() {
        Queue<TrieNode> queue = new ArrayDeque<>();
        root.failure = root;

        // Initialize first level
        for (TrieNode child : root.children.values()) {
            child.failure = root;
            queue.offer(child);
        }

        // BFS to build failure links
        while (!queue.isEmpty()) {
            TrieNode current = queue.poll();

            for (Map.Entry<Character, TrieNode> entry : current.children.entrySet()) {
                char c = entry.getKey();
                TrieNode child = entry.getValue();
                queue.offer(child);

                TrieNode failure = current.failure;
                while (failure != root && !failure.children.containsKey(c)) {
                    failure = failure.failure;
                }
                child.failure = failure.children.getOrDefault(c, root);
                child.patterns.addAll(child.failure.patterns);
            }
        }
    }

    public Set<String> findMatches(String text) {
        if (text == null || text.isEmpty()) return Collections.emptySet();

        if (!compiled) {
            buildFailureLinks();
            compiled = true;
        }

        Set<String> matches = new HashSet<>();
        TrieNode current = root;

        for (char c : text.toCharArray()) {
            while (current != root && !current.children.containsKey(c)) {
                current = current.failure;
            }

            if (current.children.containsKey(c)) {
                current = current.children.get(c);
                matches.addAll(current.patterns);
            }
        }

        return matches;
    }

    public int countMatches(String text) {
        return findMatches(text).size();
    }
}