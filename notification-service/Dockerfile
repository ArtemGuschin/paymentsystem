# ---------- Stage 1: build ----------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN chmod +x gradlew

# Nexus запущен на Windows-хосте,
# поэтому из Docker обращаемся через host.docker.internal
RUN sed -i 's|http://localhost:8081|http://host.docker.internal:8081|g' build.gradle.kts

COPY src src

RUN ./gradlew clean bootJar -x test --no-daemon


# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8787

ENTRYPOINT ["java", "-jar", "app.jar"]