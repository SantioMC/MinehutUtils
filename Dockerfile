FROM gradle:8.11.1-jdk21 AS base
LABEL author="Santio"
WORKDIR /app

FROM base AS build

COPY . .
RUN ./gradlew openApiGenerate shadowJar --no-daemon

FROM base AS runtime
COPY --from=build /app/build/libs/*.jar app.jar

RUN chmod 700 /app && chown 1000:1000 /app
USER 1000:1000

ENTRYPOINT ["java", "-jar", "app.jar"]
