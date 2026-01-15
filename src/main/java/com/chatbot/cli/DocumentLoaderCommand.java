package com.chatbot.cli;

import com.chatbot.model.DocumentInfo;
import com.chatbot.model.LoadResult;
import com.chatbot.service.DocumentService;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;

@ShellComponent
public class DocumentLoaderCommand {

    private final DocumentService documentService;

    public DocumentLoaderCommand(DocumentService documentService) {
        this.documentService = documentService;
    }

    @ShellMethod(key = "load-docs", value = "Load HTML documents from a directory into the vector store")
    public String loadDocuments(
            @ShellOption(help = "Path to directory containing HTML files") String path) {

        System.out.println("Clearing existing documents...");
        documentService.clearDocuments();

        System.out.println("Loading documents from: " + path);
        System.out.println("This may take a while depending on the number and size of files...\n");

        LoadResult result = documentService.loadDocumentsFromDirectory(path);

        StringBuilder output = new StringBuilder();
        output.append("\n=== Document Loading Complete ===\n");
        output.append(String.format("Files processed: %d\n", result.getFilesProcessed()));
        output.append(String.format("Chunks created:  %d\n", result.getChunksCreated()));
        output.append(String.format("Errors:          %d\n", result.getErrors()));
        output.append(String.format("\n%s\n", result.getMessage()));

        return output.toString();
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

    @ShellMethod(key = "clear-docs", value = "Clear the document registry")
    public String clearDocuments() {
        documentService.clearDocuments();
        return "Document registry cleared. Note: Vector store data persists in ChromaDB.";
    }

    @ShellMethod(key = "help-rag", value = "Show help for RAG chatbot commands")
    public String help() {
        return """

                === RAG Chatbot CLI Commands ===

                load-docs <path>   - Load HTML documents from a directory
                                     Recursively finds all .html and .htm files

                list-docs          - List all loaded documents with chunk counts

                clear-docs         - Clear the document registry (memory only)

                Examples:
                  load-docs ./docs
                  load-docs C:\\Users\\docs\\help-files
                  load-docs /home/user/documentation

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
