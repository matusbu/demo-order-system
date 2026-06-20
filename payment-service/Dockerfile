# Stage 1 — build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY payment-service/pom.xml payment-service/pom.xml
WORKDIR /app/payment-service
RUN mvn dependency:go-offline -q
COPY payment-service/src src
RUN mvn package -DskipTests -q

# Stage 2 — runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/payment-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
