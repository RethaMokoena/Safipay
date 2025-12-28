#!/bin/bash
cd services/paymentgateway-service || mkdir -p services/paymentgateway-service && cd services/paymentgateway-service

echo "Generating Maven-based paymentgateway-service with all production dependencies..."

curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.3.5 \
  -d groupId=com.safipay \
  -d artifactId=paymentgateway-service \
  -d name=paymentgateway-service \
  -d description="SafiPay paymentgateway Service - Core financial engine" \
  -d packageName=com.safipay.paymentgateway \
  -d javaVersion=21 \
  -d dependencies=web,data-jpa,postgresql,validation,security \
  -d dependencies=lombok,actuator \
  -d dependencies=data-redis,cache \
  -d dependencies=amqp \
  -d dependencies=mail \
  -d dependencies=oauth2-resource-server \
  -d dependencies=observability \
  -o paymentgateway-service.zip

unzip -q paymentgateway-service.zip -d paymentgateway-service-temp
mv paymentgateway-service-temp/* .
rm -rf paymentgateway-service-temp paymentgateway-service.zip

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
    <artifactId>paymentgateway-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>paymentgateway-service</name>
    <description>SafiPay paymentgateway Service - Core financial engine</description>
    <properties>
        <java.version>21</java.version>
        <spring-boot.version>3.3.5</spring-boot.version>
        <lombok.version>1.18.34</lombok.version>  <!-- Added Lombok version property -->
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
            <version>${lombok.version}</version>  <!-- Using version property -->
            <optional>true</optional>
            <scope>provided</scope>  <!-- Changed to provided -->
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
            <!-- Maven Compiler Plugin with annotation processor -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>

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
    name: paymentgateway-service
  datasource:
    url: jdbc:postgresql://localhost:5432/paymentgatewaydb
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
    com.safipay.paymentgateway: DEBUG
    org.springframework.transaction: INFO

app:
  jwt:
    secret: ${JWT_SECRET:change-me-to-a-very-long-random-string-2025}
EOF

# ------------------------------------------------------------------
# 3. Basic folder structure for a paymentgateway service
# ------------------------------------------------------------------
mkdir -p src/main/java/com/safipay/paymentgateway/{model,dto,service,repository,controller,config,exception,security}
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
COPY --from=build /app/target/paymentgateway-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
EOF
# ------------------------------------------------------------------
# 3b. Create main application class
# ------------------------------------------------------------------
cat > src/main/java/com/safipay/merchant/merchantServiceApplication.java << 'EOF'
package com.safipay.merchant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class merchantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(merchantServiceApplication.class, args);
    }
}
EOF

# ------------------------------------------------------------------
# 5. docker-compose.yml (ready to run locally)
# ------------------------------------------------------------------
cat > docker-compose.yml << 'EOF'
version: '3.9'
services:
  paymentgateway-service:
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
      POSTGRES_DB: paymentgatewaydb
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

echo "Maven paymentgateway-service is ready!"
echo ""
echo "To run locally:"
echo "  Option 1 (fast): ./mvnw spring-boot:run"
echo "  Option 2 (Docker): docker compose up --build"
echo ""
echo "Expose metrics: http://localhost:8081/actuator/prometheus"
echo "Health check:    http://localhost:8081/actuator/health"
echo ""
echo "Happy coding!"