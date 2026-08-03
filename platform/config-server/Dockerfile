# ============================================================
# Config Server — Multi-Stage Dockerfile
# Spring Cloud Config Server (native filesystem backend) — ADR-004
# In containers, CONFIG_REPO_PATH is set to /config-repo (mounted volume)
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
COPY platform/config-server/pom.xml ./platform/config-server/pom.xml

RUN mvn -B dependency:go-offline \
    -f platform/config-server/pom.xml \
    -Dmaven.repo.local=/root/.m2 2>/dev/null || true

COPY platform/config-server/src ./platform/config-server/src

RUN mvn -B clean package -DskipTests \
    -f platform/config-server/pom.xml \
    -Dmaven.repo.local=/root/.m2

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:25-jre AS runtime

RUN groupadd -r eelp && useradd -r -g eelp eelp

WORKDIR /app

COPY --from=builder /build/platform/config-server/target/*.jar config-server.jar

# Bundle the config-repo into the image for self-contained deployments.
# In Docker Compose/K8s, this can be overridden by mounting a volume at /config-repo.
COPY config-repo /config-repo

RUN chown -R eelp:eelp config-server.jar /config-repo

USER eelp

EXPOSE 8888

# CONFIG_REPO_PATH env var points Config Server to the bundled config-repo.
# Override with a volume mount for externally-managed configuration.
ENV CONFIG_REPO_PATH=file:///config-repo

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "config-server.jar"]
