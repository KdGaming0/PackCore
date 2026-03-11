package com.github.kd_gaming1.packcore.configpack;

import java.nio.file.Path;
import java.time.Instant;

public record BackupEntry(Path zipPath, String displayName, Instant timestamp) {}