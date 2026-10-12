FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN chmod +x gradlew

COPY src src

RUN ./gradlew clean bootJar -x test


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/build/libs/transaction-service.jar app.jar

EXPOSE 8585

ENTRYPOINT ["java", "-jar", "app.jar"]