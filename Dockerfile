FROM gradle:8.1.1-jdk17 AS build
COPY . /app
WORKDIR /app
RUN ./gradlew bootJar

FROM openjdk:17-jdk-slim
COPY --from=build /app/build/libs/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]