# =========================================
# STAGE 1 - Build
# =========================================
FROM maven:3.9.8-eclipse-temurin-21 AS build

WORKDIR /workspace

# Cache de dependencias Maven (capa separada)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Fuentes + build
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# =========================================
# STAGE 2 - Runtime
# =========================================
FROM eclipse-temurin:21-jre-alpine

# Usuario no-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# JAR renombrado para ENTRYPOINT estable
COPY --from=build --chown=spring:spring /workspace/target/lms-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# JVM ajustada a 512MB RAM / 0.1 CPU (Render Free Tier)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]