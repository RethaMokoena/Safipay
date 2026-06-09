# ── Build stage ──────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy full project (IMPORTANT FIX)
COPY . .

# Build only this service
RUN mvn clean package -DskipTests -pl merchant-service -am

# ── Runtime stage ─────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S safipay && adduser -S safipay -G safipay

# adjust path if needed
COPY --from=build /app/merchant-service/target/*.jar app.jar

RUN chown safipay:safipay app.jar
USER safipay

EXPOSE 8080

ENTRYPOINT ["java",   "-XX:+UseContainerSupport",   "-XX:MaxRAMPercentage=75.0",   "-Djava.security.egd=file:/dev/./urandom",   "-jar", "app.jar"]