# Build stage
FROM eclipse-temurin:21-jdk as builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw package -DskipTests -B

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
