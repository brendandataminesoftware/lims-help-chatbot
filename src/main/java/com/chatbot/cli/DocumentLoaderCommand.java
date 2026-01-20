package com.chatbot.cli;

import com.chatbot.model.DocumentInfo;
import com.chatbot.model.LoadResult;
import com.chatbot.service.CollectionMetadataService;
import com.chatbot.service.DocumentService;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@ShellComponent
public class DocumentLoaderCommand {

    private final DocumentService documentService;
    private final CollectionMetadataService collectionMetadataService;

    public DocumentLoaderCommand(DocumentService documentService, CollectionMetadataService collectionMetadataService) {
        this.documentService = documentService;
        this.collectionMetadataService = collectionMetadataService;
    }

    @ShellMethod(key = "load-docs", value = "Load HTML documents from a directory into a vector store collection")
    public String loadDocuments(
            @ShellOption(help = "Path to directory containing HTML files") String path,
            @ShellOption(help = "Name of the ChromaDB collection to store documents") String collectionName,
            @ShellOption(help = "Display title for this collection", defaultValue = ShellOption.NULL) String title,
            @ShellOption(help = "URL path to logo image for this collection", defaultValue = ShellOption.NULL) String logo) {

        System.out.println("Clearing existing documents...");
        documentService.clearDocuments();

        System.out.println("Loading documents from: " + path);
        System.out.println("Target collection: " + collectionName);
        if (title != null) {
            System.out.println("Collection title: " + title);
            collectionMetadataService.setTitle(collectionName, title);
        }
        if (logo != null) {
            System.out.println("Collection logo: " + logo);
            collectionMetadataService.setLogo(collectionName, logo);
        }
        System.out.println("This may take a while depending on the number and size of files...\n");

        LoadResult result = documentService.loadDocumentsFromDirectory(path, collectionName);

        StringBuilder output = new StringBuilder();
        output.append("\n=== Document Loading Complete ===\n");
        output.append(String.format("Collection:      %s\n", collectionName));
        if (title != null) {
            output.append(String.format("Title:           %s\n", title));
        }
        if (logo != null) {
            output.append(String.format("Logo:            %s\n", logo));
        }
        output.append(String.format("Files processed: %d\n", result.getFilesProcessed()));
        output.append(String.format("Chunks created:  %d\n", result.getChunksCreated()));
        output.append(String.format("Errors:          %d\n", result.getErrors()));
        output.append(String.format("\n%s\n", result.getMessage()));

        return output.toString();
    }

    @ShellMethod(key = "load-docs-url", value = "Download a ZIP file from URL and load HTML documents into a vector store collection")
    public String loadDocumentsFromUrl(
            @ShellOption(help = "URL to a ZIP file containing HTML files") String url,
            @ShellOption(help = "Name of the ChromaDB collection to store documents") String collectionName,
            @ShellOption(help = "Display title for this collection", defaultValue = ShellOption.NULL) String title,
            @ShellOption(help = "URL path to logo image for this collection", defaultValue = ShellOption.NULL) String logo) {

        Path tempDir = null;
        Path zipFile = null;

        try {
            System.out.println("Downloading ZIP file from: " + url);
            System.out.println("Target collection: " + collectionName);
            if (title != null) {
                System.out.println("Collection title: " + title);
                collectionMetadataService.setTitle(collectionName, title);
            }
            if (logo != null) {
                System.out.println("Collection logo: " + logo);
                collectionMetadataService.setLogo(collectionName, logo);
            }

            // Create temporary directory and file
            tempDir = Files.createTempDirectory("rag-docs-");
            zipFile = tempDir.resolve("documents.zip");

            // Download the ZIP file
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                return "Error: Failed to download ZIP file. HTTP status: " + response.statusCode();
            }

            // Save to file
            try (InputStream in = response.body();
                 FileOutputStream out = new FileOutputStream(zipFile.toFile())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                System.out.println("Downloaded " + (totalBytes / 1024) + " KB");
            }

            // Extract ZIP file
            Path extractDir = tempDir.resolve("extracted");
            Files.createDirectories(extractDir);

            System.out.println("Extracting ZIP file...");
            extractZip(zipFile, extractDir);

            // Load documents from extracted directory
            System.out.println("Clearing existing documents...");
            documentService.clearDocuments();

            System.out.println("Loading documents from extracted files...");
            System.out.println("This may take a while depending on the number and size of files...\n");

            LoadResult result = documentService.loadDocumentsFromDirectory(extractDir.toString(), collectionName);

            StringBuilder output = new StringBuilder();
            output.append("\n=== Document Loading Complete ===\n");
            output.append(String.format("Collection:      %s\n", collectionName));
            if (title != null) {
                output.append(String.format("Title:           %s\n", title));
            }
            if (logo != null) {
                output.append(String.format("Logo:            %s\n", logo));
            }
            output.append(String.format("Files processed: %d\n", result.getFilesProcessed()));
            output.append(String.format("Chunks created:  %d\n", result.getChunksCreated()));
            output.append(String.format("Errors:          %d\n", result.getErrors()));
            output.append(String.format("\n%s\n", result.getMessage()));

            return output.toString();

        } catch (Exception e) {
            return "Error loading documents from URL: " + e.getMessage();
        } finally {
            // Cleanup temporary files
            if (tempDir != null) {
                try {
                    deleteDirectory(tempDir);
                } catch (IOException e) {
                    System.err.println("Warning: Failed to cleanup temporary directory: " + e.getMessage());
                }
            }
        }
    }

    private void extractZip(Path zipFile, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path targetPath = destDir.resolve(entry.getName()).normalize();

                // Security check: prevent zip slip vulnerability
                if (!targetPath.startsWith(destDir)) {
                    throw new IOException("Invalid ZIP entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath);
                }
                zis.closeEntry();
            }
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @ShellMethod(key = "list-docs", value = "List all loaded documents")
    public String listDocuments() {
        List<DocumentInfo> documents = documentService.getLoadedDocuments();

        if (documents.isEmpty()) {
            return "No documents loaded yet. Use 'load-docs <path>' to load HTML files.";
        }

        StringBuilder output = new StringBuilder();
        output.append("\n=== Loaded Documents ===\n");
        output.append(String.format("%-40s %-30s %s\n", "FILENAME", "TITLE", "CHUNKS"));
        output.append("-".repeat(80)).append("\n");

        for (DocumentInfo doc : documents) {
            output.append(String.format("%-40s %-30s %d\n",
                    truncate(doc.getFilename(), 38),
                    truncate(doc.getTitle(), 28),
                    doc.getChunkCount()));
        }

        output.append("-".repeat(80)).append("\n");
        output.append(String.format("Total: %d documents\n", documents.size()));

        return output.toString();
    }

    @ShellMethod(key = "set-title", value = "Set the display title for a collection")
    public String setTitle(
            @ShellOption(help = "Name of the collection") String collectionName,
            @ShellOption(help = "Display title for the frontend header") String title) {
        collectionMetadataService.setTitle(collectionName, title);
        return String.format("Title set for collection '%s': %s", collectionName, title);
    }

    @ShellMethod(key = "set-logo", value = "Set the logo URL for a collection")
    public String setLogo(
            @ShellOption(help = "Name of the collection") String collectionName,
            @ShellOption(help = "URL path to logo image") String logo) {
        collectionMetadataService.setLogo(collectionName, logo);
        return String.format("Logo set for collection '%s': %s", collectionName, logo);
    }

    @ShellMethod(key = "set-alias", value = "Create an alias that points to another collection")
    public String setAlias(
            @ShellOption(help = "Name of the alias (e.g., cclas-latest)") String aliasName,
            @ShellOption(help = "Target collection name (e.g., cclas-2025-r2)") String targetCollection) {
        collectionMetadataService.setAlias(aliasName, targetCollection);
        return String.format("Alias set: '%s' -> '%s'", aliasName, targetCollection);
    }

    @ShellMethod(key = "remove-alias", value = "Remove a collection alias")
    public String removeAlias(
            @ShellOption(help = "Name of the alias to remove") String aliasName) {
        collectionMetadataService.removeAlias(aliasName);
        return String.format("Alias '%s' removed", aliasName);
    }

    @ShellMethod(key = "clear-docs", value = "Clear the document registry")
    public String clearDocuments() {
        documentService.clearDocuments();
        return "Document registry cleared. Note: Vector store data persists in ChromaDB.";
    }

    @ShellMethod(key = "help-rag", value = "Show help for RAG chatbot commands")
    public String help() {
        return """

                === Datamine Help CLI Commands ===

                load-docs <path> <collection> [--title <title>] [--logo <url>]
                    Load HTML documents from a directory into a ChromaDB collection.
                    Recursively finds all .html and .htm files.
                    Optional --title sets the display title for the frontend header.
                    Optional --logo sets the logo image URL for the frontend header.

                load-docs-url <url> <collection> [--title <title>] [--logo <url>]
                    Download a ZIP file from URL and load documents into a collection.
                    Downloads, extracts, and processes HTML files.
                    Optional --title sets the display title for the frontend header.
                    Optional --logo sets the logo image URL for the frontend header.

                set-title <collection> <title>
                    Set the display title for a collection's frontend header.

                set-logo <collection> <logo-url>
                    Set the logo image URL for a collection's frontend header.

                set-alias <alias> <target-collection>
                    Create an alias that points to another collection.
                    Useful for creating "latest" or "stable" aliases.

                remove-alias <alias>
                    Remove a collection alias.

                list-docs
                    List all loaded documents with chunk counts.

                clear-docs
                    Clear the document registry (memory only).

                Examples:
                  load-docs ./docs my-collection
                  load-docs ./docs my-collection --title "My Product Help"
                  load-docs ./docs my-collection --title "Help" --logo "https://example.com/logo.png"
                  load-docs-url https://example.com/docs.zip product-docs --title "Product Docs"
                  set-title my-collection "My Product Documentation"
                  set-logo my-collection "https://example.com/logo.png"
                  set-alias cclas-latest cclas-2025-r2
                  remove-alias cclas-latest

                After loading documents, open http://localhost:8080 in your browser
                to chat with the AI about your documents.
                """;
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}
