# syntax=docker/dockerfile:1

# 1. Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy maven wrapper and pom.xml first for layer caching
COPY pom.xml .
COPY src ./src

# Install maven and package jar
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

# 2. Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for security best practices
RUN addgroup -S linksphere && adduser -S linksphere -G linksphere
USER linksphere:linksphere

COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
