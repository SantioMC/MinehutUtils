FROM gradle:8.11.1-jdk21 AS build

LABEL author="Santio"
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

FROM alpine/java:21-jre AS runtime

LABEL author="Santio"
WORKDIR /bot

COPY --from=build /app/build/libs/MinehutUtils.jar app.jar

RUN adduser -HD -u 1000 user \
    && chown -R user:user /bot \
    && chmod -R 777 /bot

USER user:user
CMD ["java", "-jar", "app.jar"]
