# ============================================================
# Loan Service — Multi-Stage Dockerfile
# Stage 1: Build with Maven + JDK 25 (Eclipse Temurin)
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
COPY services/loan-service/pom.xml ./services/loan-service/pom.xml

RUN mvn -B dependency:go-offline \
    -f services/loan-service/pom.xml \
    -Dmaven.repo.local=/root/.m2 2>/dev/null || true

COPY services/loan-service/src ./services/loan-service/src

RUN mvn -B clean package -DskipTests \
    -f services/loan-service/pom.xml \
    -Dmaven.repo.local=/root/.m2

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:25-jre AS runtime

RUN groupadd -r eelp && useradd -r -g eelp eelp

WORKDIR /app

COPY --from=builder /build/services/loan-service/target/*.jar loan-service.jar

RUN chown eelp:eelp loan-service.jar

USER eelp

# Port 8081 per ADR-004 service registry
EXPOSE 8081

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "loan-service.jar"]
