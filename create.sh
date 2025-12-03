#!/bin/bash
cd services/user-service || mkdir -p services/user-service && cd services/user-service

echo "Generating Maven-based user-service with all production dependencies..."

curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.3.5 \
  -d groupId=com.safipay \
  -d artifactId=user-service \
  -d name=user-service \
  -d description="SafiPay user Service - Core financial engine" \
  -d packageName=com.safipay.user \
  -d javaVersion=21 \
  -d dependencies=web,data-jpa,postgresql,validation,security \
  -d dependencies=lombok,actuator \
  -d dependencies=data-redis,cache \
  -d dependencies=amqp \
  -d dependencies=mail \
  -d dependencies=oauth2-resource-server \
  -d dependencies=observability \
  -o user-service.zip

unzip -q user-service.zip -d user-service-temp
mv user-service-temp/* .
rm -rf user-service-temp user-service.zip

echo "Maven project generated!"

# ------------------------------------------------------------------
# 1. Enhanced pom.xml – add all the missing but critical stuff
# ------------------------------------------------------------------
cat > pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
	<parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>
    <groupId>com.safipay</groupId>
    <artifactId>user-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>user-service</name>
    <description>SafiPay user Service - Core financial engine</description>
    <properties>
        <java.version>21</java.version>
        <spring-boot.version>3.3.5</spring-boot.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>

        <!-- PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Observability (Micrometer + Prometheus) -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>rabbitmq</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven Plugin -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF

# ------------------------------------------------------------------
# 2. application.yml with sane defaults
# ------------------------------------------------------------------
mkdir -p src/main/resources
cat > src/main/resources/application.yml << 'EOF'
spring:
  application:
    name: user-service
  datasource:
    url: jdbc:postgresql://localhost:5432/userdb
    username: user_user
    password: ${DB_PASSWORD:secret}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml

  data:
    redis:
      host: localhost
      port: 6379
  cache:
    type: redis
  rabbitmq:
    host: localhost
    port: 5672

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,threaddump,heapdump
  endpoint:
    health:
      show-details: always

server:
  port: 8081

logging:
  level:
    com.safipay.user: DEBUG
    org.springframework.transaction: INFO

app:
  jwt:
    secret: ${JWT_SECRET:change-me-to-a-very-long-random-string-2025}
EOF

# ------------------------------------------------------------------
# 3. Basic folder structure for a user service
# ------------------------------------------------------------------
mkdir -p src/main/java/com/safipay/user/{domain,dto,service,repository,controller,config,exception,security}
mkdir -p src/main/resources/db/changelog

# ------------------------------------------------------------------
# 4. Dockerfile (optimized multi-stage)
# ------------------------------------------------------------------
cat > Dockerfile << 'EOF'
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw -B -DskipTests clean package

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/user-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
EOF

# ------------------------------------------------------------------
# 5. docker-compose.yml (ready to run locally)
# ------------------------------------------------------------------
cat > docker-compose.yml << 'EOF'
version: '3.9'
services:
  user-service:
    build: .
    ports:
      - "8081:8081"
    environment:
      - DB_PASSWORD=secret
      - JWT_SECRET=super-long-jwt-secret-2025-change-in-production
    depends_on:
      - postgres
      - redis
      - rabbitmq
    restart: unless-stopped

  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: userdb
      POSTGRES_USER: user_user
      POSTGRES_PASSWORD: secret
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  rabbitmq:
    image: rabbitmq:3-management-alpine
    ports:
      - "5672:5672"
      - "15672:15672"

volumes:
  postgres_data:
EOF

echo "Maven user-service is ready!"
echo ""
echo "To run locally:"
echo "  Option 1 (fast): ./mvnw spring-boot:run"
echo "  Option 2 (Docker): docker compose up --build"
echo ""
echo "Expose metrics: http://localhost:8081/actuator/prometheus"
echo "Health check:    http://localhost:8081/actuator/health"
echo ""
echo "Happy coding!"