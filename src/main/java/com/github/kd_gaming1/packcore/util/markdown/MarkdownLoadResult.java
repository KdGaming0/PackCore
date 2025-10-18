package com.github.kd_gaming1.packcore.util.markdown;

public sealed interface MarkdownLoadResult permits MarkdownLoadResult.Success, MarkdownLoadResult.NotFound, MarkdownLoadResult.Error {
    record Success(String content) implements MarkdownLoadResult {}
    record NotFound(String fileName) implements MarkdownLoadResult {}
    record Error(String fileName, String message, Throwable cause) implements MarkdownLoadResult {}
}