# ============================================================
# Eureka Server — Multi-Stage Dockerfile
# Netflix Eureka service registry — ADR-004
# ============================================================

# ── Stage 1: Build ───────────────────────────────────────────
FROM eclipse-temurin:25-jdk AS builder

ARG MAVEN_VERSION=3.9.9
RUN apt-get update && apt-get install -y wget \
    && wget -q "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" -O /tmp/maven.tar.gz \
    && tar -xzf /tmp/maven.tar.gz -C /opt \
    && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn \
    && rm /tmp/maven.tar.gz \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /build

COPY pom.xml ./pom.xml
COPY platform/eureka-server/pom.xml ./platform/eureka-server/pom.xml

RUN mvn -B dependency:go-offline \
    -f platform/eureka-server/pom.xml \
    -Dmaven.repo.local=/root/.m2 2>/dev/null || true

COPY platform/eureka-server/src ./platform/eureka-server/src

RUN mvn -B clean package -DskipTests \
    -f platform/eureka-server/pom.xml \
    -Dmaven.repo.local=/root/.m2

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:25-jre AS runtime

RUN groupadd -r eelp && useradd -r -g eelp eelp

WORKDIR /app

COPY --from=builder /build/platform/eureka-server/target/*.jar eureka-server.jar

RUN chown eelp:eelp eureka-server.jar

USER eelp

# Eureka dashboard + registration endpoint — ADR-004
EXPOSE 8761

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "eureka-server.jar"]
