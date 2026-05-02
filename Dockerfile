# Stage 1: Build
FROM maven:3.9.0-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom.xml
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /build/target/*.jar app.jar

# Metadata
LABEL maintainer="pedinephro@esprit.tn"
LABEL description="PediNephro Vital Signs Service - Patient Monitoring"
LABEL service="vital-signs-service"
LABEL port="8084"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

# Expose port
EXPOSE 8084

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]