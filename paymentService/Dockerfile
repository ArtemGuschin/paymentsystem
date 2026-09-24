FROM eclipse-temurin:24-jdk AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .
COPY src src

RUN chmod +x gradlew
RUN ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java","-jar","app.jar"]