# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd --system spring \
    && useradd --system --gid spring spring

COPY --chown=spring:spring \
    target/adhd-app-server-*.jar /app/app.jar

USER spring:spring

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE="prod" \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
