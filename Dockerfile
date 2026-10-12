# =========================================
# STAGE 1 - Build
# =========================================
FROM maven:3.9.8-eclipse-temurin-21 AS build

WORKDIR /workspace

# Cache de dependencias Maven (capa separada)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

# Fuentes + build
COPY src ./src
RUN mvn -B -q package -DskipTests && \
    find target -maxdepth 1 -name "*.jar" ! -name "original-*.jar" -exec cp {} app.jar \;

# =========================================
# STAGE 2 - Runtime
# =========================================
FROM eclipse-temurin:21-jre-alpine

# Usuario no-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# JAR renombrado para ENTRYPOINT estable
COPY --from=build --chown=spring:spring /workspace/app.jar app.jar

EXPOSE 8080

# JVM ajustada a 512MB RAM / 0.1 CPU (Render Free Tier)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Djava.security.egd=file:/dev/./urandom"

# Healthcheck usando Spring Actuator
HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=30s \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]