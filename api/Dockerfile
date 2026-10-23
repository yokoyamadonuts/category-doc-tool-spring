# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts ./
COPY gradle gradle
COPY domain domain
COPY api api
RUN --mount=type=cache,target=/root/.gradle ./gradlew :api:bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /src/api/build/libs/api-*.jar app.jar
USER app
EXPOSE 18081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
