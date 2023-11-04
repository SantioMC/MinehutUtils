FROM gradle:7.6.3-jdk11-alpine 
LABEL author="Santio"

WORKDIR /build

COPY . .
RUN gradle shadowJar 

FROM openjdk:17-jdk-slim
LABEL author="Santio"

WORKDIR /app
COPY --from=0 /build/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
