# Stage 1: build the jar with the full JDK and the Gradle wrapper
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Download dependencies first so this layer is cached until build files change
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# Stage 2: run it on a smaller JRE-only image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
