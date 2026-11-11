FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -ntp -U verify

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && groupadd --system studio && useradd --system --gid studio studio \
    && mkdir -p /app/data && chown -R studio:studio /app
WORKDIR /app
COPY --from=build /workspace/target/material-studio-0.1.0-SNAPSHOT.jar app.jar
USER studio
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
