FROM maven:3.9.11-eclipse-temurin-21@sha256:6fdc855a6ed81d288ca7ca37ac6ff5e9308b612485c0801d70b25a858c83d237 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -ntp -U verify

FROM eclipse-temurin:21-jre-jammy@sha256:1eac8b8fee11ca7d6725a48bc5d41992ed1561504ebae25490d2d062847e8269
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && groupadd --system studio && useradd --system --gid studio studio \
    && mkdir -p /app/data && chown -R studio:studio /app
WORKDIR /app
COPY --from=build /workspace/target/material-studio-0.1.0-SNAPSHOT.jar app.jar
USER studio
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
