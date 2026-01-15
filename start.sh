#!/bin/bash

set -e

# Load .env file if it exists
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
fi

# Check for required environment variables
if [ -z "$OPENAI_API_KEY" ]; then
    echo "Error: OPENAI_API_KEY is not set"
    echo ""
    echo "Option 1: Create a .env file:"
    echo "  echo 'OPENAI_API_KEY=your-key' > .env"
    echo ""
    echo "Option 2: Set environment variable:"
    echo "  OPENAI_API_KEY=your-key ./start.sh"
    exit 1
fi

echo "Starting RAG Chatbot..."

# Stop any existing containers
docker-compose down

# Build and start all services
docker-compose build --no-cache
docker-compose up -d

echo ""
echo "Services started successfully!"
echo "  - Chatbot:  http://localhost:8080"
echo "  - ChromaDB: http://localhost:8000"
echo ""
echo "View logs: docker-compose logs -f chatbot"
echo "Stop:      docker-compose down"
