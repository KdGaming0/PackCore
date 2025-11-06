package com.github.kd_gaming1.packcore.scamshield.debug;

import java.util.ArrayList;
import java.util.List;

public class TestCase {
    private final String name;
    private final String description;
    private final List<Message> conversation;
    private final boolean shouldTrigger;
    private final int expectedMinScore;
    private final String category;

    public TestCase(String name, String description, List<Message> conversation,
                    boolean shouldTrigger, int expectedMinScore, String category) {
        this.name = name;
        this.description = description;
        this.conversation = new ArrayList<>(conversation);
        this.shouldTrigger = shouldTrigger;
        this.expectedMinScore = expectedMinScore;
        this.category = category;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<Message> getConversation() { return conversation; }
    public boolean shouldTrigger() { return shouldTrigger; }
    public int getExpectedMinScore() { return expectedMinScore; }
    public String getCategory() { return category; }

    public static class Message {
        private final String sender;
        private final String text;
        private final long delayMs;

        public Message(String sender, String text, long delayMs) {
            this.sender = sender;
            this.text = text;
            this.delayMs = delayMs;
        }

        public String getSender() { return sender; }
        public String getText() { return text; }
        public long getDelayMs() { return delayMs; }
    }

    public static class Builder {
        private String name;
        private String description;
        private final List<Message> conversation = new ArrayList<>();
        private boolean shouldTrigger = true;
        private int expectedMinScore = 100;
        private String category = "Unknown";

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder message(String sender, String text) {
            return message(sender, text, 100);
        }

        public Builder message(String sender, String text, long delayMs) {
            conversation.add(new Message(sender, text, delayMs));
            return this;
        }

        public Builder shouldTrigger(boolean shouldTrigger) {
            this.shouldTrigger = shouldTrigger;
            return this;
        }

        public Builder expectedMinScore(int score) {
            this.expectedMinScore = score;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public TestCase build() {
            return new TestCase(name, description, conversation, shouldTrigger, expectedMinScore, category);
        }
    }
}