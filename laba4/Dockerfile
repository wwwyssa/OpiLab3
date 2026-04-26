FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle settings.gradle
COPY build.gradle build.gradle
COPY src src

RUN chmod +x gradlew
RUN ./gradlew clean bootJar -x test --no-daemon
RUN JAR_FILE=$(ls build/libs/*.jar | grep -v -- '-plain.jar' | head -n 1) && cp "$JAR_FILE" app.jar

FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY --from=builder /app/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
