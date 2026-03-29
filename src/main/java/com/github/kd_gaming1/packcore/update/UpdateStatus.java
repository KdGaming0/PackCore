package com.github.kd_gaming1.packcore.update;

public final class UpdateStatus {

    public enum State {
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        UNKNOWN
    }

    private final State state;
    private final String installedVersion;
    private final String latestVersion;
    private final String changelog;

    private UpdateStatus(State state, String installedVersion, String latestVersion, String changelog) {
        this.state = state;
        this.installedVersion = installedVersion;
        this.latestVersion = latestVersion;
        this.changelog = changelog;
    }

    public static UpdateStatus upToDate(String version, String changelog) {
        return new UpdateStatus(State.UP_TO_DATE, version, version, changelog);
    }

    public static UpdateStatus updateAvailable(String installed, String latest, String changelog) {
        return new UpdateStatus(State.UPDATE_AVAILABLE, installed, latest, changelog);
    }

    public static UpdateStatus unknown() {
        return new UpdateStatus(State.UNKNOWN, null, null, null);
    }

    public State state() { return state; }
    public String installedVersion() { return installedVersion; }
    public String latestVersion() { return latestVersion; }
    public String changelog() { return changelog; }
    public boolean isUpdateAvailable() { return state == State.UPDATE_AVAILABLE; }
}