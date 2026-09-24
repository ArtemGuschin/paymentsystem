FROM gradle:8.14.0-jdk24 AS builder

WORKDIR /app

COPY --chown=gradle:gradle . .

RUN gradle clean bootJar -x test --no-daemon

FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java","-jar","app.jar"]