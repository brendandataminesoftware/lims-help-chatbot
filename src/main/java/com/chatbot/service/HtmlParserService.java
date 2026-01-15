package com.chatbot.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class HtmlParserService {

    private static final Logger log = LoggerFactory.getLogger(HtmlParserService.class);

    public record ParsedDocument(String title, String content, String filename, String filePath) {}

    public ParsedDocument parseHtmlFile(File file) throws IOException {
        log.debug("Parsing HTML file: {}", file.getAbsolutePath());

        Document doc = Jsoup.parse(file, StandardCharsets.UTF_8.name());

        String title = doc.title();
        if (title == null || title.isBlank()) {
            title = file.getName().replace(".html", "").replace(".htm", "");
        }

        // Remove script and style elements
        doc.select("script, style, nav, header, footer, aside").remove();

        // Get text content, preserving some structure
        String content = doc.body() != null ? doc.body().text() : doc.text();

        // Clean up whitespace
        content = content.replaceAll("\\s+", " ").trim();

        log.debug("Parsed document '{}' with {} characters", title, content.length());

        return new ParsedDocument(title, content, file.getName(), file.getAbsolutePath());
    }

    public ParsedDocument parseHtmlString(String html, String filename) {
        Document doc = Jsoup.parse(html);

        String title = doc.title();
        if (title == null || title.isBlank()) {
            title = filename.replace(".html", "").replace(".htm", "");
        }

        doc.select("script, style, nav, header, footer, aside").remove();

        String content = doc.body() != null ? doc.body().text() : doc.text();
        content = content.replaceAll("\\s+", " ").trim();

        return new ParsedDocument(title, content, filename, null);
    }
}
