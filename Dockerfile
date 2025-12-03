
FROM maven:3.9.4-eclipse-temurin-21 AS build

WORKDIR /app-user
COPY . .

RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jdk-alpine

COPY --from=build /app-user/api/target/api-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]