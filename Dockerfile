# Build stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Copy Maven wrapper and pom.xml first (changes rarely)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Copy frontend package files for npm dependency caching
COPY frontend/package.json frontend/package-lock.json ./frontend/

# Download all dependencies (Maven + Node + npm) - cached unless pom.xml or package.json changes
RUN --mount=type=cache,target=/root/.m2/repository \
    --mount=type=cache,target=/app/target/node \
    --mount=type=cache,target=/app/frontend/node_modules \
    ./mvnw dependency:go-offline \
        frontend:install-node-and-npm -Dfrontend.nodeVersion=v20.18.0 -Dfrontend.npmVersion=10.8.2 \
        frontend:npm -Dfrontend.arguments=install -B

# Copy frontend source (changes more often than dependencies)
COPY frontend/ ./frontend/

# Copy Java source
COPY src ./src

# Build the application (uses cached dependencies)
RUN --mount=type=cache,target=/root/.m2/repository \
    --mount=type=cache,target=/app/target/node \
    ./mvnw package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create data directory for persistent files
RUN mkdir -p /app/data

COPY --from=builder /app/target/*.jar app.jar

# Set data directory environment variable
ENV RAG_DATA_DIR=/app/data

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
