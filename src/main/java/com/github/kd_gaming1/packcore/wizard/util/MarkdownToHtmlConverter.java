package com.github.kd_gaming1.packcore.wizard.util;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MarkdownToHtmlConverter {
    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownToHtmlConverter() {
        MutableDataSet options = new MutableDataSet();
        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    /**
     * Converts a Markdown string into HTML.
     * @param markdownContent The input Markdown text.
     * @return Converted HTML string.
     */
    public String convertMarkdownToHtml(String markdownContent) {
        Node document = parser.parse(markdownContent);
        return renderer.render(document);
    }

    /**
     * Reads a Markdown file and returns its content as HTML.
     * @param filePath Path to the Markdown file.
     * @return Converted HTML as a String.
     * @throws IOException if the file cannot be read
     */
    public String convertMarkdownFileToHtml(String filePath) throws IOException {
        String markdownContent = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        return convertMarkdownToHtml(markdownContent);
    }
}
