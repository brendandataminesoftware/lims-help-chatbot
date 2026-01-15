# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RAG Chatbot - A Spring Boot application that provides AI-powered chat with Retrieval-Augmented Generation (RAG) using OpenAI and ChromaDB as the vector store.

## Build Commands

```bash
# Build the project
./mvnw.cmd compile

# Run the application
./mvnw.cmd spring-boot:run

# Run tests
./mvnw.cmd test

# Package as JAR
./mvnw.cmd package
```

## Running the Application

1. Start ChromaDB (required for vector storage):
   ```bash
   docker-compose up -d
   ```

2. Set the OpenAI API key:
   ```bash
   set OPENAI_API_KEY=your-key-here
   ```

3. Run the application:
   ```bash
   ./mvnw.cmd spring-boot:run
   ```

The server runs on port 8080 by default.

## Architecture

### Core Flow
1. **Document Ingestion**: HTML files are parsed (`HtmlParserService`), chunked with overlap (`DocumentService`), and stored as embeddings in ChromaDB
2. **Chat with RAG**: User queries trigger similarity search in ChromaDB, retrieved context is injected into the system prompt, and OpenAI generates responses (`ChatService`)

### Key Components

- **ChatService** (`service/ChatService.java`): Orchestrates RAG - retrieves relevant documents from vector store, builds context, manages conversation history, and calls OpenAI
- **DocumentService** (`service/DocumentService.java`): Handles document loading, text chunking (with configurable size/overlap), and vector store operations
- **HtmlParserService** (`service/HtmlParserService.java`): Parses HTML using Jsoup, strips nav/header/footer/scripts, extracts clean text

### API Endpoints

- `POST /api/chat` - Send chat messages (accepts `message` and optional `history`)
- `GET /api/documents` - List loaded documents
- `POST /api/documents/upload` - Upload HTML file
- `POST /api/documents/load` - Load from directory path
- `DELETE /api/documents` - Clear document registry

### CLI Commands (Spring Shell)

Run with `SHELL_INTERACTIVE=true` to enable:
- `load-docs <path>` - Load HTML files from directory
- `list-docs` - Show loaded documents
- `clear-docs` - Clear document registry

## Configuration

Key settings in `application.yml`:
- `spring.ai.openai.api-key` - OpenAI API key (use env var `OPENAI_API_KEY`)
- `spring.ai.vectorstore.chroma.url` - ChromaDB URL (default: http://localhost:8000)
- `rag.chunk-size` - Document chunk size (default: 1000)
- `rag.chunk-overlap` - Overlap between chunks (default: 200)
- `rag.max-results` - Max documents to retrieve (default: 5)
