# Use Java 21 base image for building
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (for better caching)
COPY pom.xml .
RUN mvn dependency:resolve

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Use lightweight Java 21 runtime image
FROM eclipse-temurin:21-jre-jammy

# Install required packages and create app user
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    useradd --create-home --shell /bin/bash app

# Set working directory
WORKDIR /app

# Copy the built JAR from the build stage  
COPY --from=build /app/target/twitch-mcp-*-runner.jar ./twitch-mcp-runner.jar

# Change ownership to app user
RUN chown -R app:app /app

# Switch to non-root user
USER app

# Expose the port (Smithery will set PORT env var)
EXPOSE 8080

# Set environment variables for optimal container performance
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication"

# Health check endpoint (will use PORT env var at runtime)
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/mcp || exit 1

# Start the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/twitch-mcp-runner.jar"]