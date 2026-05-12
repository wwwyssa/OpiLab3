FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle settings.gradle
COPY build.gradle build.gradle
COPY src src

RUN chmod +x gradlew

FROM eclipse-temurin:17-jdk AS runtime
WORKDIR /app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/build/libs/laba4.jar"]
