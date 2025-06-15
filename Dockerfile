# Build stage
FROM maven:3.9.6-eclipse-temurin-11 AS build-stage
WORKDIR /app

# Copy Maven files for better layer caching
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Production stage
FROM eclipse-temurin:11-jre AS production-stage

RUN apt-get update && \
    apt-get install -y --no-install-recommends dumb-init && \
    groupadd --gid 1001 appuser && \
    useradd --uid 1001 --gid appuser --shell /bin/bash appuser && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy JAR file
COPY --from=build-stage /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Use dumb-init for proper signal handling
ENTRYPOINT ["dumb-init", "--"]
CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar", "--spring.profiles.active=prod"]