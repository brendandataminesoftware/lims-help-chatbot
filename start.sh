#!/bin/bash

set -e

# Check for required environment variables
if [ -z "$OPENAI_API_KEY" ]; then
    echo "Error: OPENAI_API_KEY environment variable is not set"
    echo "Usage: OPENAI_API_KEY=your-key ./start.sh"
    exit 1
fi

echo "Starting RAG Chatbot..."

# Build and start all services
docker-compose up -d --build

echo ""
echo "Services started successfully!"
echo "  - Chatbot:  http://localhost:8080"
echo "  - ChromaDB: http://localhost:8000"
echo ""
echo "View logs: docker-compose logs -f chatbot"
echo "Stop:      docker-compose down"
