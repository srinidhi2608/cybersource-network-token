# ============================================================
# Stage 1: Build
# Uses a full Maven + JDK image to compile and package the app
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy dependency manifests first so Docker can cache the layer
# when only source code changes (and pom.xml hasn't changed)
COPY pom.xml .

# Pre-download all dependencies (offline-friendly layer cache)
RUN mvn dependency:go-offline -q

# Copy the rest of the source tree
COPY src ./src

# Build the fat JAR, skipping tests (tests run separately)
RUN mvn clean package -DskipTests -q

# ============================================================
# Stage 2: Runtime
# Uses a slim JRE image to keep the final image small
# ============================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Add a non-root user for security best practices
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

# Copy only the packaged JAR from the builder stage
COPY --from=builder /build/target/cybersourceCashflows-*.jar app.jar

# Grant ownership to the non-root user
RUN chown appuser:appgroup app.jar

USER appuser

# Expose the default Spring Boot port
EXPOSE 8080

# Use exec form to receive OS signals properly (e.g., graceful shutdown)
ENTRYPOINT ["java", "-jar", "app.jar"]
