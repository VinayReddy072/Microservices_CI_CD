# ============================================================
# Inventory Service — Multi-Stage Dockerfile
# Stage 1: Build with Maven + JDK 25 (Eclipse Temurin)
# Stage 2: Run with minimal JRE 25 Alpine image
# ============================================================

# ── Stage 1: Build ───────────────────────────────────────────
FROM eclipse-temurin:25-jdk AS builder

# Install Maven (no official maven:3.9-temurin-25 image yet)
ARG MAVEN_VERSION=3.9.9
RUN apt-get update && apt-get install -y wget \
    && wget -q "https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" -O /tmp/maven.tar.gz \
    && tar -xzf /tmp/maven.tar.gz -C /opt \
    && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn \
    && rm /tmp/maven.tar.gz \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /build

# Copy parent POM first for dependency layer caching
COPY pom.xml ./pom.xml

# Copy module POM
COPY services/inventory-service/pom.xml ./services/inventory-service/pom.xml

# Resolve dependencies (cached unless pom files change)
RUN mvn -B dependency:go-offline \
    -f services/inventory-service/pom.xml \
    -Dmaven.repo.local=/root/.m2 2>/dev/null || true

# Copy source and build
COPY services/inventory-service/src ./services/inventory-service/src

RUN mvn -B clean package -DskipTests \
    -f services/inventory-service/pom.xml \
    -Dmaven.repo.local=/root/.m2

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:25-jre AS runtime

# Non-root user for security (ADR-001: principle of least privilege)
RUN groupadd -r eelp && useradd -r -g eelp eelp

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /build/services/inventory-service/target/*.jar inventory-service.jar

RUN chown eelp:eelp inventory-service.jar

USER eelp

# Port declared in config-repo (inventory-service-*.yml) and ADR-004
EXPOSE 8082

# Spring Boot Actuator health probes will target /actuator/health on this port
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "inventory-service.jar"]
