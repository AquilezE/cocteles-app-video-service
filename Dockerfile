FROM ubuntu:latest
LABEL authors="markitotoys"

ENTRYPOINT ["top", "-b"]

FROM openjdk:17-jdk-slim AS build
WORKDIR /app

ARG JWT_SECRET
ENV JWT_SECRET=$JWT_SECRET

COPY build.gradle settings.gradle gradlew /app/
COPY gradle /app/gradle

COPY src /app/src

RUN chmod +x gradlew
RUN ./gradlew shadowJar --no-daemon

FROM openjdk:17-slim
WORKDIR /app

COPY --from=build /app/build/libs/*.jar ./grpc-video-service.jar

RUN mkdir -p /app/resources/uploads

VOLUME ["/app/resources/uploads"]

EXPOSE 50051

CMD ["java","-jar","grpc-video-service.jar"]
