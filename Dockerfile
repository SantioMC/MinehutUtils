FROM base AS build

LABEL author="Santio"
WORKDIR /app
COPY . .
RUN gradle openApiGenerate shadowJar --no-daemon

FROM eclipse-temurin:21-alpine AS runtime

LABEL author="Santio"
WORKDIR /home/user
RUN adduser --disabled-password --gecos "" user
COPY --from=build /app/build/libs/*.jar app.jar

USER user
ENTRYPOINT ["java", "-jar", "app.jar"]
