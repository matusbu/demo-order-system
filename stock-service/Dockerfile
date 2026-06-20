# Stage 1 — build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY proto proto
COPY stock-service/pom.xml stock-service/pom.xml
WORKDIR /app/stock-service
RUN mvn dependency:go-offline -q
COPY stock-service/src src
RUN mvn package -DskipTests -q

# Stage 2 — runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/stock-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
