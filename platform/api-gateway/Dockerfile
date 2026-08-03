# ============================================================
# API Gateway — Multi-Stage Dockerfile
# Reactive Spring Cloud Gateway (WebFlux) — ADR-001
# Stage 1: Build with Maven + JDK 25
# Stage 2: Run with minimal JRE 25
# ============================================================

# ── Stage 1: Build ───────────────────────────────────────────
FROM eclipse-temurin:25-jdk AS builder

ARG MAVEN_VERSION=3.9.9
RUN apt-get update && apt-get install -y wget \
    && wget -q "https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" -O /tmp/maven.tar.gz \
    && tar -xzf /tmp/maven.tar.gz -C /opt \
    && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn \
    && rm /tmp/maven.tar.gz \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /build

COPY pom.xml ./pom.xml
COPY platform/api-gateway/pom.xml ./platform/api-gateway/pom.xml

RUN mvn -B dependency:go-offline \
    -f platform/api-gateway/pom.xml \
    -Dmaven.repo.local=/root/.m2 2>/dev/null || true

COPY platform/api-gateway/src ./platform/api-gateway/src

RUN mvn -B clean package -DskipTests \
    -f platform/api-gateway/pom.xml \
    -Dmaven.repo.local=/root/.m2

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:25-jre AS runtime

RUN groupadd -r eelp && useradd -r -g eelp eelp

WORKDIR /app

COPY --from=builder /build/platform/api-gateway/target/*.jar api-gateway.jar

RUN chown eelp:eelp api-gateway.jar

USER eelp

# Single public entry-point — ADR-001: all external traffic enters here
EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "api-gateway.jar"]
