# Build stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Copy Maven wrapper and pom.xml first (changes rarely)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Copy frontend package files for npm dependency caching
COPY frontend/package.json frontend/package-lock.json ./frontend/

# Download Maven dependencies only (cached layer)
RUN --mount=type=cache,target=/root/.m2/repository \
    ./mvnw dependency:go-offline -B

# Copy frontend source
COPY frontend/ ./frontend/

# Copy Java source
COPY src ./src

# Build the application (frontend + Java) with caching
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
