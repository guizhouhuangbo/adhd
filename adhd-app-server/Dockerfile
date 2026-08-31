# syntax=docker/dockerfile:1

FROM --platform=$BUILDPLATFORM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd --system spring \
    && useradd --system --gid spring spring

COPY --from=build --chown=spring:spring \
    /workspace/target/adhd-app-server-*.jar /app/app.jar

USER spring:spring

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE="prod" \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
