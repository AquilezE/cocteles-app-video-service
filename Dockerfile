# ─── DEBUG BASE ────────────────────────────────────────────────────────────────
FROM ubuntu:latest
LABEL authors="markitotoys"

# keep the container alive so you can exec in for troubleshooting
ENTRYPOINT ["top", "-b"]

# ─── BUILD STAGE ───────────────────────────────────────────────────────────────
FROM openjdk:17-jdk-slim AS build
WORKDIR /app

# copy only the Gradle wrapper and build scripts first (to leverage cache)
COPY build.gradle settings.gradle gradlew /app/
COPY gradle /app/gradle

# copy your source
COPY src /app/src

# make the wrapper executable and build
RUN chmod +x gradlew
RUN ./gradlew shadowJar --no-daemon

# ─── RUNTIME STAGE ─────────────────────────────────────────────────────────────
FROM openjdk:17-slim
WORKDIR /app

COPY --from=build /app/build/libs/*.jar ./grpc-video-service.jar

# prepare the uploads folder and seed it if you had defaults
RUN mkdir -p /app/resources/uploads

# declare uploads as a volume so data persists across container restarts
VOLUME ["/app/resources/uploads"]

# expose the gRPC port
EXPOSE 8080

# finally, start your server
CMD ["java","-jar","grpc-video-service.jar"]
