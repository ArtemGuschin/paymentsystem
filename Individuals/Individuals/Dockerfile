# ---------- Stage 1 ----------
FROM gradle:8.14.0-jdk24 AS builder

WORKDIR /app

COPY --chown=gradle:gradle . .

# Меняем адрес Nexus только внутри контейнера
RUN sed -i 's|http://localhost:8081|http://host.docker.internal:8081|g' build.gradle

RUN gradle clean build -x test --no-daemon

# ---------- Stage 2 ----------
FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 9093

ENTRYPOINT ["java", "-jar", "app.jar"]